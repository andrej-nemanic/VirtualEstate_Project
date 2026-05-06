var express = require('express');
var router = express.Router();
var PropertyController = require('../controllers/PropertyController.js');
var authMiddleware = require('../middleware/authMiddleware.js');


/*
 * GET
 */
router.get('/', authMiddleware, PropertyController.list);

/*
 * GET
 */
router.get('/:id', authMiddleware, PropertyController.show);

/*
 * POST
 */
router.post('/', authMiddleware, PropertyController.create);

/*
 * PUT
 */
router.put('/:id', authMiddleware, PropertyController.update);

/*
 * DELETE
 */
router.delete('/:id', authMiddleware, PropertyController.remove);

module.exports = router;
