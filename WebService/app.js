var createError = require('http-errors');
var express = require('express');
var path = require('path');
var mongoose = require('mongoose');
var cookieParser = require('cookie-parser');
var logger = require('morgan');

// Uvoz usmerjevalnikov
var indexRouter = require('./routes/index');
// POPRAVLJENO: Namesto privzete 'users.js' uvozimo vaše dejanske poti 'UserRoutes.js'
var userRoutes = require('./routes/UserRoutes'); 

// Povezava na MongoDB
mongoose.connect('mongodb://127.0.0.1:27017/virtual_estate');

var app = express();

// View engine setup
app.set('views', path.join(__dirname, 'views'));
app.set('view engine', 'hbs');

app.use(logger('dev'));
app.use(express.json());
app.use(express.urlencoded({ extended: false }));
app.use(cookieParser());
app.use(express.static(path.join(__dirname, 'public')));

// Vpetje poti
app.use('/', indexRouter);
// POPRAVLJENO: Preusmeritev celotnega /users prometa na vaš UserRoutes vmesnik
app.use('/users', userRoutes); 

// Catch 404 and forward to error handler
app.use(function(req, res, next) {
  next(createError(404));
});

// Error handler
app.use(function(err, req, res, next) {
  // set locals, only providing error in development
  res.locals.message = err.message;
  res.locals.error = req.app.get('env') === 'development' ? err : {};

  // render the error page
  res.status(err.status || 500);
  res.render('error');
});

module.exports = app;