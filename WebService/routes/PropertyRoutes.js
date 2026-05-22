var express = require('express');
var router = express.Router();
var PropertyController = require('../controllers/PropertyController.js');
var authMiddleware = require('../middleware/authMiddleware.js');
var adminMiddleware = require('../middleware/adminMiddleware.js');

router.get('/search', PropertyController.searchByDistance);
router.get('/', PropertyController.list);

router.post('/ingest', PropertyController.ingestCreate);
router.put('/ingest/:id', PropertyController.ingestUpdate);
router.delete('/ingest/:id', PropertyController.ingestRemove);
router.post('/geocode-missing', PropertyController.geocodeMissing);

router.get('/:id', PropertyController.show);
router.post('/', authMiddleware, adminMiddleware, PropertyController.create);
router.put('/:id', authMiddleware, adminMiddleware, PropertyController.update);
router.delete('/:id', authMiddleware, adminMiddleware, PropertyController.remove);

module.exports = router;
