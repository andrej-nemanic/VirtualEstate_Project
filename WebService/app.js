require('dotenv').config();

// Na nekaterih Windows sistemih Node (c-ares) ne prebere sistemskih DNS strežnikov
// in pade na 127.0.0.1, kjer ni DNS strežnika -> SRV poizvedba za mongodb+srv:// pade
// z ECONNREFUSED. Samo v tem primeru preusmerimo na javni DNS; na deploy okolju,
// kjer je resolver pravilno nastavljen, se to ne sproži.
var dns = require('dns');
if (dns.getServers().some(function (s) { return s === '127.0.0.1' || s === '::1'; })) {
    dns.setServers(['8.8.8.8', '1.1.1.1']);
}

// MongoDB Atlas driver (SCRAM-SHA-256) potrebuje globalThis.crypto v Node 18,
// kjer Web Crypto API privzeto ni izpostavljen. V Node 19+ ni potrebno.
if (!global.crypto) {
    global.crypto = require('crypto').webcrypto;
}

var createError = require('http-errors');
var express = require('express');
var path = require('path');
var mongoose = require('mongoose');
var logger = require('morgan');
var cors = require('cors');
var helmet = require('helmet');

var userRoutes = require('./routes/UserRoutes');
var propertyRoutes = require('./routes/PropertyRoutes');
var pinoLogger = require('./services/Logger');

const dbUrl = process.env.DATABASE_URL;

function connectWithRetry(attempt = 1) {
    mongoose.connect(dbUrl)
        .then(() => pinoLogger.info('Uspešno povezan na MongoDB!'))
        .catch(err => {
            const delay = Math.min(30000, 1000 * 2 ** attempt);
            pinoLogger.error({ err: err.message, attempt, retryInMs: delay }, 'Napaka pri povezavi z bazo, poskušam znova...');
            setTimeout(() => connectWithRetry(attempt + 1), delay);
        });
}
connectWithRetry();

mongoose.connection.on('disconnected', () => {
    pinoLogger.warn('Mongoose odklopljen.');
});

var app = express();

app.use(helmet());
app.use(cors({
    origin: true,
    credentials: false,
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS']
}));
app.use(logger('dev'));
app.use(express.json({ limit: '1mb' }));
app.use(express.urlencoded({ extended: false }));
app.use(express.static(path.join(__dirname, 'public')));

app.use(function(req, res, next) {
    req.io = app.get('socketio');
    next();
});

app.get('/', function(req, res) {
    res.json({ name: 'VirtualEstate API', version: '1.0.0' });
});

app.get('/health', function(req, res) {
    const dbReady = mongoose.connection.readyState === 1;
    res.status(dbReady ? 200 : 503).json({
        status: dbReady ? 'ok' : 'degraded',
        uptime: process.uptime(),
        database: dbReady ? 'connected' : 'disconnected',
        timestamp: new Date().toISOString()
    });
});

app.use('/api/users', userRoutes);
app.use('/api/properties', propertyRoutes);

app.use(function(req, res, next) {
    next(createError(404));
});

app.use(function(err, req, res, next) {
    const status = err.status || 500;
    const body = { message: err.message || 'Notranja napaka strežnika.' };
    if (process.env.NODE_ENV === 'development' && status >= 500) {
        body.error = err.message;
    }
    if (status >= 500) {
        pinoLogger.error({ err: err.message, path: req.path, method: req.method }, 'Napaka pri obdelavi zahteve');
    }
    res.status(status).json(body);
});

module.exports = app;
