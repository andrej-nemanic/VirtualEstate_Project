var express = require('express');
var router = express.Router();
var LocationController = require('../controllers/LocationController.js');

/*
 * GET
 */
router.get('/', LocationController.list);

/*
 * GET
 */
router.get('/:id', LocationController.show);

/*
 * POST
 */
router.post('/', LocationController.create);

/*
 * PUT
 */
router.put('/:id', LocationController.update);

/*
 * DELETE
 */
router.delete('/:id', LocationController.remove);

module.exports = router;
