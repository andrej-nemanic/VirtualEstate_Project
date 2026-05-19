var express = require('express');
var router = express.Router();
var PropertyController = require('../controllers/PropertyController.js');
var authMiddleware = require('../middleware/authMiddleware.js');

router.get('/search', PropertyController.searchByDistance);
router.get('/', PropertyController.list);

router.post('/ingest', PropertyController.ingestCreate);
router.put('/ingest/:id', PropertyController.ingestUpdate);
router.delete('/ingest/:id', PropertyController.ingestRemove);

router.get('/:id', PropertyController.show);
router.post('/', authMiddleware, PropertyController.create);
router.put('/:id', authMiddleware, PropertyController.update);
router.delete('/:id', authMiddleware, PropertyController.remove);

module.exports = router;
