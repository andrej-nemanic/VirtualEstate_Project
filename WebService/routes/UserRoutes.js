var express = require('express');
var router = express.Router();
var UserController = require('../controllers/UserController.js');
var authMiddleware = require('../middleware/authMiddleware.js');

/*
 * GET
 */
router.get('/', UserController.list);

/*
 * GET
 */
router.get('/:id', UserController.show);
router.post('/login', authMiddleware, UserController.login);
/*
 * POST
 */
router.post('/', UserController.create);

/*
 * PUT
 */
router.put('/:id', UserController.update);

/*
 * DELETE
 */
router.delete('/:id', UserController.remove);

module.exports = router;
