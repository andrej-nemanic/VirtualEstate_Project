var UserModel = require('../models/UserModel.js');
var jwt = require('jsonwebtoken');

const JWT_SECRET = process.env.JWT_SECRET || 'skrivniKljuc';

module.exports = {

    list: async function (req, res) {
        try {
            const users = await UserModel.find().select('-password');
            return res.json(users);
        } catch (err) {
            return res.status(500).json({ message: 'Error when getting Users.', error: err });
        }
    },

    show: async function (req, res) {
        try {
            const user = await UserModel.findById(req.params.id).select('-password');
            if (!user) return res.status(404).json({ message: 'No such User' });
            return res.json(user);
        } catch (err) {
            return res.status(500).json({ message: 'Error when getting User.', error: err });
        }
    },

    create: async function (req, res) {
        try {
            const user = new UserModel({
                name: req.body.name,
                id: req.body.id,
                personalData: req.body.personalData,
                email: req.body.email,
                password: req.body.password,
                type: req.body.type
            });
            const saved = await user.save();
            const obj = saved.toObject();
            delete obj.password;
            return res.status(201).json(obj);
        } catch (err) {
            console.error('Create user error:', err);
            return res.status(500).json({ message: 'Error when creating User', error: err });
        }
    },

    update: async function (req, res) {
        try {
            const user = await UserModel.findById(req.params.id);
            if (!user) return res.status(404).json({ message: 'No such User' });

            if (req.body.name !== undefined) user.name = req.body.name;
            if (req.body.id !== undefined) user.id = req.body.id;
            if (req.body.personalData !== undefined) user.personalData = req.body.personalData;
            if (req.body.email !== undefined) user.email = req.body.email;
            if (req.body.password !== undefined) user.password = req.body.password;
            if (req.body.type !== undefined) user.type = req.body.type;

            const saved = await user.save();
            const obj = saved.toObject();
            delete obj.password;
            return res.json(obj);
        } catch (err) {
            return res.status(500).json({ message: 'Error when updating User.', error: err });
        }
    },

    remove: async function (req, res) {
        try {
            await UserModel.findByIdAndDelete(req.params.id);
            return res.status(204).json();
        } catch (err) {
            return res.status(500).json({ message: 'Error when deleting the User.', error: err });
        }
    },

    login: async function (req, res) {
        try {
            const { email, password } = req.body;
            const user = await UserModel.findOne({ email: email });
            if (!user) return res.status(401).json({ message: 'Napačna e-pošta ali geslo.' });

            user.comparePassword(password, function (err, isMatch) {
                if (err || !isMatch) {
                    return res.status(401).json({ message: 'Napačna e-pošta ali geslo.' });
                }
                const token = jwt.sign(
                    { id: user._id, email: user.email, type: user.type },
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
                        type: user.type
                    }
                });
            });
        } catch (err) {
            return res.status(500).json({ message: 'Napaka pri iskanju uporabnika.', error: err });
        }
    },

    me: async function (req, res) {
        try {
            const user = await UserModel.findById(req.user.id).select('-password');
            if (!user) return res.status(404).json({ message: 'User not found' });
            return res.json(user);
        } catch (err) {
            return res.status(500).json({ message: 'Error.', error: err });
        }
    }
};
