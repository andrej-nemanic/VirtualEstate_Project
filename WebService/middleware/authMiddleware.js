var jwt = require('jsonwebtoken');

const JWT_SECRET = process.env.JWT_SECRET || 'skrivniKljuc';

module.exports = function(req, res, next) {
    var token = req.headers['authorization'];

    if (!token) {
        return res.status(403).json({ message: 'Žeton ni priložen.' });
    }

    if (token.startsWith('Bearer ')) {
        token = token.slice(7, token.length);
    }

    jwt.verify(token, JWT_SECRET, function(err, decoded) {
        if (err) {
            return res.status(401).json({ message: 'Neveljaven žeton.' });
        }
        req.user = decoded;
        next();
    });
};