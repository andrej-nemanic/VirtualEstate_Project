var express = require('express');
var router = express.Router();
var auth = require('../middleware/authMiddleware'); // Preverite, če je pot do middleware-a pravilna
var propertyController = require('../controllers/PropertyController');
var locationController = require('../controllers/LocationController'); // Uvoz krmilnika za lokacije

// GET strani (Prikazi vmesnikov)
router.get('/login', function(req, res) {
  res.render('login', { title: 'Prijava' });
});

router.get('/register', function(req, res) {
  res.render('register', { title: 'Registracija' });
});

router.get('/', function(req, res, next) {
  res.render('index', { title: 'Virtual Estate' });
});

// Javna nadzorna plošča (V njej se izvaja preverjanje žetona preko JS na front-endu)
router.get('/dashboard', function(req, res) {
    res.render('dashboard');
});

// Zaščiten admin vmesnik
router.get('/admin', auth, function(req, res) {
    res.render('admin', { user: req.user });
});


/* === API POTI ZA PODATKE (Nepremičnine in Lokacije) === */

// Pridobivanje vseh nepremičnin (Zahteva prijavo, da zaščitimo podatke)
router.get('/properties', auth, propertyController.list);

// Pridobivanje ene nepremičnine
router.get('/properties/:id', auth, propertyController.show);

// Ustvarjanje nove nepremičnine (Zaščiteno - le za prijavljene uporabnike/lastnike)
router.post('/properties', auth, propertyController.create);

// POPRAVEK: Brisanje nepremičnine (Nujno potrebno za administrativni del nadzorne plošče)
router.delete('/properties/:id', auth, propertyController.remove);

// POPRAVEK: Ustvarjanje lokacije (Potrebno, ko admin vpiše naslov in koordinate nepremičnine)
router.post('/locations', auth, locationController.create);

module.exports = router;