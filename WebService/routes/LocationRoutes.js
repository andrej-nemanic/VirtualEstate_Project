var express = require('express');
var router = express.Router();
var LocationController = require('../controllers/LocationController.js');
var authMiddleware = require('../middleware/authMiddleware.js');

router.get('/near', LocationController.near);
router.post('/geocode-missing', LocationController.geocodeMissing);
router.get('/', LocationController.list);
router.get('/:id', LocationController.show);

router.post('/', authMiddleware, LocationController.create);
router.put('/:id', authMiddleware, LocationController.update);
router.delete('/:id', authMiddleware, LocationController.remove);

module.exports = router;
