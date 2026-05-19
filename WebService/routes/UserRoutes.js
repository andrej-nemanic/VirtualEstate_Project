var express = require('express');
var router = express.Router();
var UserController = require('../controllers/UserController.js');
var authMiddleware = require('../middleware/authMiddleware.js');

router.post('/login', UserController.login);
router.post('/register', UserController.create);
router.post('/', UserController.create);

router.get('/me', authMiddleware, UserController.me);
router.get('/', authMiddleware, UserController.list);
router.get('/:id', authMiddleware, UserController.show);
router.put('/:id', authMiddleware, UserController.update);
router.delete('/:id', authMiddleware, UserController.remove);

module.exports = router;
