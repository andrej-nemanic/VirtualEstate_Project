var UserModel = require('../models/UserModel.js');
var jwt = require('jsonwebtoken');
var logger = require('../services/Logger');
var { validateEmail, validatePasswordStrength } = require('../middleware/validationMiddleware');

const JWT_SECRET = process.env.JWT_SECRET || 'skrivniKljuc';

function handleDuplicateOrError(res, err, fallbackMsg) {
    if (err && err.code === 11000) {
        return res.status(409).json({ message: 'Uporabnik s tem e-poštnim naslovom že obstaja.' });
    }
    logger.error({ err: err.message }, fallbackMsg);
    return res.status(500).json({ message: fallbackMsg });
}

module.exports = {

    list: async function (req, res) {
        try {
            const users = await UserModel.find().select('-password');
            return res.json(users);
        } catch (err) {
            return handleDuplicateOrError(res, err, 'Napaka pri branju uporabnikov.');
        }
    },

    show: async function (req, res) {
        try {
            const user = await UserModel.findById(req.params.id).select('-password');
            if (!user) return res.status(404).json({ message: 'Uporabnik ne obstaja.' });
            return res.json(user);
        } catch (err) {
            return handleDuplicateOrError(res, err, 'Napaka pri branju uporabnika.');
        }
    },

    create: async function (req, res) {
        try {
            const emailErr = validateEmail(req.body.email);
            if (emailErr) return res.status(400).json({ message: emailErr });
            const passErr = validatePasswordStrength(req.body.password);
            if (passErr) return res.status(400).json({ message: passErr });
            if (!req.body.name || !String(req.body.name).trim()) {
                return res.status(400).json({ message: 'Ime je obvezno.' });
            }

            const user = new UserModel({
                name: String(req.body.name).trim(),
                email: String(req.body.email).trim().toLowerCase(),
                password: req.body.password,
                isAdmin: false
            });
            const saved = await user.save();
            const obj = saved.toObject();
            delete obj.password;
            return res.status(201).json(obj);
        } catch (err) {
            return handleDuplicateOrError(res, err, 'Napaka pri ustvarjanju uporabnika.');
        }
    },

    update: async function (req, res) {
        try {
            const user = await UserModel.findById(req.params.id);
            if (!user) return res.status(404).json({ message: 'Uporabnik ne obstaja.' });

            if (req.body.email !== undefined) {
                const emailErr = validateEmail(req.body.email);
                if (emailErr) return res.status(400).json({ message: emailErr });
                user.email = String(req.body.email).trim().toLowerCase();
            }
            if (req.body.password !== undefined && req.body.password !== '') {
                const passErr = validatePasswordStrength(req.body.password);
                if (passErr) return res.status(400).json({ message: passErr });
                user.password = req.body.password;
            }
            if (req.body.name !== undefined) user.name = String(req.body.name).trim();
            if (req.body.isAdmin !== undefined) {
                if (!req.user || req.user.isAdmin !== true) {
                    return res.status(403).json({ message: 'isAdmin lahko spreminja samo administrator.' });
                }
                user.isAdmin = req.body.isAdmin === true;
            }

            const saved = await user.save();
            const obj = saved.toObject();
            delete obj.password;
            return res.json(obj);
        } catch (err) {
            return handleDuplicateOrError(res, err, 'Napaka pri posodabljanju uporabnika.');
        }
    },

    remove: async function (req, res) {
        try {
            await UserModel.findByIdAndDelete(req.params.id);
            return res.status(204).json();
        } catch (err) {
            return handleDuplicateOrError(res, err, 'Napaka pri brisanju uporabnika.');
        }
    },

    login: async function (req, res) {
        try {
            const { email, password } = req.body;
            if (!email || !password) {
                return res.status(400).json({ message: 'E-pošta in geslo sta obvezna.' });
            }
            const user = await UserModel.findOne({ email: String(email).trim().toLowerCase() });
            if (!user) return res.status(401).json({ message: 'Napačna e-pošta ali geslo.' });

            user.comparePassword(password, function (err, isMatch) {
                if (err || !isMatch) {
                    return res.status(401).json({ message: 'Napačna e-pošta ali geslo.' });
                }
                const token = jwt.sign(
                    { id: user._id, email: user.email, isAdmin: user.isAdmin },
                    JWT_SECRET,
                    { expiresIn: '24h' }
                );
                return res.json({
                    message: 'Uspešna prijava',
                    token: token,
                    user: {
                        id: user._id,
                        name: user.name,
                        email: user.email,
                        isAdmin: user.isAdmin
                    }
                });
            });
        } catch (err) {
            return handleDuplicateOrError(res, err, 'Napaka pri prijavi.');
        }
    },

    me: async function (req, res) {
        try {
            const user = await UserModel.findById(req.user.id).select('-password');
            if (!user) return res.status(404).json({ message: 'Uporabnik ne obstaja.' });
            return res.json({
                id: user._id,
                name: user.name,
                email: user.email,
                isAdmin: user.isAdmin
            });
        } catch (err) {
            return handleDuplicateOrError(res, err, 'Napaka pri branju računa.');
        }
    },

    ingestCreate: async function (req, res) {
        try {
            const body = req.body || {};
            const emailErr = validateEmail(body.email);
            if (emailErr) return res.status(400).json({ message: emailErr });
            if (!body.password) return res.status(400).json({ message: 'Geslo je obvezno.' });
            const passErr = validatePasswordStrength(body.password);
            if (passErr) return res.status(400).json({ message: passErr });
            if (!body.name || !String(body.name).trim()) {
                return res.status(400).json({ message: 'Ime je obvezno.' });
            }

            const user = new UserModel({
                name: String(body.name).trim(),
                email: String(body.email).trim().toLowerCase(),
                password: body.password,
                isAdmin: body.isAdmin === true
            });
            const saved = await user.save();
            const obj = saved.toObject();
            delete obj.password;
            return res.status(201).json(obj);
        } catch (err) {
            return handleDuplicateOrError(res, err, 'Napaka pri ustvarjanju uporabnika.');
        }
    },

    ingestUpdate: async function (req, res) {
        try {
            const body = req.body || {};
            const user = await UserModel.findById(req.params.id);
            if (!user) return res.status(404).json({ message: 'Uporabnik ne obstaja.' });

            if (body.email !== undefined) {
                const emailErr = validateEmail(body.email);
                if (emailErr) return res.status(400).json({ message: emailErr });
                user.email = String(body.email).trim().toLowerCase();
            }
            if (body.password !== undefined && body.password !== '') {
                const passErr = validatePasswordStrength(body.password);
                if (passErr) return res.status(400).json({ message: passErr });
                user.password = body.password;
            }
            if (body.name !== undefined) user.name = String(body.name).trim();
            if (body.isAdmin !== undefined) user.isAdmin = body.isAdmin === true;

            const saved = await user.save();
            const obj = saved.toObject();
            delete obj.password;
            return res.json(obj);
        } catch (err) {
            return handleDuplicateOrError(res, err, 'Napaka pri posodabljanju uporabnika.');
        }
    },

    ingestRemove: async function (req, res) {
        try {
            await UserModel.findByIdAndDelete(req.params.id);
            return res.status(204).json();
        } catch (err) {
            return handleDuplicateOrError(res, err, 'Napaka pri brisanju uporabnika.');
        }
    }
};
