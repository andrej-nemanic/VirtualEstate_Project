var express = require('express');
var router = express.Router();
var UserController = require('../controllers/UserController.js');
var authMiddleware = require('../middleware/authMiddleware.js');

router.post('/login', UserController.login);
/*
 * GET
 */
router.get('/', authMiddleware, UserController.list);

/*
 * GET
 */
router.get('/:id', authMiddleware, UserController.show);

/*
 * POST
 */
//Register route
router.post('/', UserController.create);

/*
 * PUT
 */
router.put('/:id', authMiddleware, UserController.update);

/*
 * DELETE
 */
router.delete('/:id', authMiddleware, UserController.remove);

module.exports = router;
