const mongoose = require('mongoose');
const validator = require('validator');

function validateObjectId(paramName = 'id') {
    return function(req, res, next) {
        const id = req.params[paramName];
        if (!mongoose.isValidObjectId(id)) {
            return res.status(400).json({ message: `Neveljaven ID: ${id}` });
        }
        next();
    };
}

function validateEmail(email) {
    if (typeof email !== 'string' || !email.trim()) return 'E-pošta je obvezna.';
    if (!validator.isEmail(email)) return 'Neveljaven format e-pošte.';
    return null;
}

function validatePasswordStrength(password) {
    if (typeof password !== 'string') return 'Geslo je obvezno.';
    if (password.length < 8) return 'Geslo mora imeti vsaj 8 znakov.';
    if (!/[A-Za-z]/.test(password) || !/[0-9]/.test(password)) {
        return 'Geslo mora vsebovati vsaj eno črko in eno številko.';
    }
    return null;
}

module.exports = {
    validateObjectId,
    validateEmail,
    validatePasswordStrength
};
