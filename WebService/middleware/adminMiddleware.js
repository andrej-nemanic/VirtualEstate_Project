module.exports = function(req, res, next) {
    if (!req.user || req.user.isAdmin !== true) {
        return res.status(403).json({ message: 'Dostop dovoljen samo administratorjem.' });
    }
    next();
};
