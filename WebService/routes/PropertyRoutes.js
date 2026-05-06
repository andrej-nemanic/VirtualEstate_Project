var express = require('express');
var router = express.Router();
var PropertyController = require('../controllers/PropertyController.js');

/*
 * GET
 */
router.get('/', PropertyController.list);

/*
 * GET
 */
router.get('/:id', PropertyController.show);

/*
 * POST
 */
router.post('/', PropertyController.create);

/*
 * PUT
 */
router.put('/:id', PropertyController.update);

/*
 * DELETE
 */
router.delete('/:id', PropertyController.remove);

module.exports = router;
