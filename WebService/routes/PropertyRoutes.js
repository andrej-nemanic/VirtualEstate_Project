var express = require('express');
var router = express.Router();
var PropertyController = require('../controllers/PropertyController.js');
var authMiddleware = require('../middleware/authMiddleware.js');
var adminMiddleware = require('../middleware/adminMiddleware.js');
var { validateObjectId } = require('../middleware/validationMiddleware.js');

router.get('/search', PropertyController.searchByDistance);
router.get('/stats', PropertyController.stats);
router.get('/', PropertyController.list);

router.post('/ingest', PropertyController.ingestCreate);
router.put('/ingest/:id', validateObjectId('id'), PropertyController.ingestUpdate);
router.delete('/ingest/:id', validateObjectId('id'), PropertyController.ingestRemove);
router.post('/geocode-missing', PropertyController.geocodeMissing);

router.get('/:id', validateObjectId('id'), PropertyController.show);
router.post('/', authMiddleware, adminMiddleware, PropertyController.create);
router.put('/:id', validateObjectId('id'), authMiddleware, adminMiddleware, PropertyController.update);
router.delete('/:id', validateObjectId('id'), authMiddleware, adminMiddleware, PropertyController.remove);

module.exports = router;
