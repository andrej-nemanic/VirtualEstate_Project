var createError = require('http-errors');
var express = require('express');
var path = require('path');
var mongoose = require('mongoose');
var cookieParser = require('cookie-parser');
var logger = require('morgan');
var cors = require('cors');
global.crypto = require('crypto');

var userRoutes = require('./routes/UserRoutes');
var propertyRoutes = require('./routes/PropertyRoutes');

//mongoose.connect('mongodb://127.0.0.1:27017/virtual_estate');



const dbUrl = process.env.DATABASE_URL || 'mongodb://127.0.0.1:27017/virtual_estate';

mongoose.connect(dbUrl)
  .then(() => console.log('Uspešno povezan na MongoDB!'))
  .catch(err => console.error('Napaka pri povezavi z bazo:', err));

var app = express();

app.use(cors());
app.use(logger('dev'));
app.use(express.json());
app.use(express.urlencoded({ extended: false }));
app.use(cookieParser());
app.use(express.static(path.join(__dirname, 'public')));

app.use(function(req, res, next) {
    req.io = app.get('socketio');
    next();
});

app.get('/', function(req, res) {
    res.json({ name: 'VirtualEstate API', version: '1.0.0' });
});

app.use('/api/users', userRoutes);
app.use('/api/properties', propertyRoutes);

app.use(function(req, res, next) {
    next(createError(404));
});

app.use(function(err, req, res, next) {
    res.status(err.status || 500).json({
        message: err.message,
        error: req.app.get('env') === 'development' ? err : {}
    });
});

module.exports = app;
