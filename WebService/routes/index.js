var express = require('express');
var router = express.Router();
var auth = require('../middleware/authMiddleware');
var propertyController = require('../controllers/PropertyController');
// GET login page
router.get('/login', function(req, res) {
  res.render('login', { title: 'Prijava' });
});

// GET register page
router.get('/register', function(req, res) {
  res.render('register', { title: 'Registracija' });
});
/* GET home page. */
router.get('/', function(req, res, next) {
  res.render('index', { title: 'Virtual Estate' });
});
// Javna nadzorna plošča
router.get('/dashboard', function(req, res) {
    res.render('dashboard');
});

// Zaščiten admin vmesnik
router.get('/admin', auth, function(req, res) {
    res.render('admin', { user: req.user });
});
/* GET properties. */
router.get('/properties', propertyController.list);
router.post('/properties', propertyController.create);
router.get('/properties/:id', propertyController.show);
module.exports = router;