module.exports = function(req, res, next) {
    if (!req.user) {
        return res.status(401).json({ message: 'Manjkajoča avtentikacija.' });
    }
    if (req.user.isAdmin === true) return next();
    if (String(req.user.id) === String(req.params.id)) return next();
    return res.status(403).json({ message: 'Dovoljeno samo administratorjem ali lastniku računa.' });
};
