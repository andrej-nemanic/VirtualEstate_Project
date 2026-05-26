var express = require('express');
var router = express.Router();
var rateLimit = require('express-rate-limit');
var UserController = require('../controllers/UserController.js');
var authMiddleware = require('../middleware/authMiddleware.js');
var adminMiddleware = require('../middleware/adminMiddleware.js');
var adminOrSelfMiddleware = require('../middleware/adminOrSelfMiddleware.js');
var { validateObjectId } = require('../middleware/validationMiddleware.js');

const loginLimiter = rateLimit({
    windowMs: 15 * 60 * 1000,
    max: 10,
    standardHeaders: true,
    legacyHeaders: false,
    message: { message: 'Preveč prijavnih poskusov, poskusi znova čez 15 minut.' }
});

router.post('/login', loginLimiter, UserController.login);
router.post('/register', UserController.create);

router.get('/me', authMiddleware, UserController.me);

router.get('/ingest', UserController.list);
router.post('/ingest', UserController.ingestCreate);
router.put('/ingest/:id', validateObjectId('id'), UserController.ingestUpdate);
router.delete('/ingest/:id', validateObjectId('id'), UserController.ingestRemove);

router.get('/', authMiddleware, adminMiddleware, UserController.list);
router.get('/:id', validateObjectId('id'), authMiddleware, adminOrSelfMiddleware, UserController.show);
router.put('/:id', validateObjectId('id'), authMiddleware, adminOrSelfMiddleware, UserController.update);
router.delete('/:id', validateObjectId('id'), authMiddleware, adminOrSelfMiddleware, UserController.remove);

module.exports = router;
