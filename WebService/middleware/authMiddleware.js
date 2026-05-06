var jwt = require('jsonwebtoken');

module.exports = function(req, res, next) {
    // Preberemo žeton iz glave zahteve
    var token = req.headers['authorization'];

    if (!token) {
        return res.status(403).json({ message: 'Žeton ni priložen.' });
    }

    // Odstranimo besedo "Bearer " pred samim žetonom, če je prisotna
    if (token.startsWith('Bearer ')) {
        token = token.slice(7, token.length);
    }

    jwt.verify(token, 'skrivniKljuc', function(err, decoded) {
        if (err) {
            return res.status(401).json({ message: 'Neveljaven žeton.' });
        }
        // Podatke iz žetona shranimo v zahtevek za kasnejšo uporabo
        req.user = decoded; 
        next();
    });
};