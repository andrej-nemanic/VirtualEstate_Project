var UserModel = require('../models/UserModel.js');
var jwt = require('jsonwebtoken');
var bcrypt = require('bcrypt');

/**
 * UserController.js
 *
 * @description :: Server-side logic for managing Users.
 */
module.exports = {

    /**
     * UserController.list()
     */
    list: function (req, res) {
        UserModel.find(function (err, Users) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when getting User.',
                    error: err
                });
            }

            return res.json(Users);
        });
    },

    /**
     * UserController.show()
     */
    show: function (req, res) {
        var id = req.params.id;

        UserModel.findOne({_id: id}, function (err, User) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when getting User.',
                    error: err
                });
            }

            if (!User) {
                return res.status(404).json({
                    message: 'No such User'
                });
            }

            return res.json(User);
        });
    },

    /**
     * UserController.create()
     */
    create: function (req, res) {
        var User = new UserModel({
			name : req.body.name,
			id : req.body.id,
			personalData : req.body.personalData,
			email : req.body.email,
			password : req.body.password,
			type : req.body.type
        });

        User.save(function (err, User) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when creating User',
                    error: err
                });
            }

            return res.status(201).json(User);
        });
    },

    /**
     * UserController.update()
     */
    update: function (req, res) {
        var id = req.params.id;

        UserModel.findOne({_id: id}, function (err, User) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when getting User',
                    error: err
                });
            }

            if (!User) {
                return res.status(404).json({
                    message: 'No such User'
                });
            }

            User.name = req.body.name ? req.body.name : User.name;
			User.id = req.body.id ? req.body.id : User.id;
			User.personalData = req.body.personalData ? req.body.personalData : User.personalData;
			User.email = req.body.email ? req.body.email : User.email;
			User.password = req.body.password ? req.body.password : User.password;
			User.type = req.body.type ? req.body.type : User.type;
			
            User.save(function (err, User) {
                if (err) {
                    return res.status(500).json({
                        message: 'Error when updating User.',
                        error: err
                    });
                }

                return res.json(User);
            });
        });
    },

    /**
     * UserController.remove()
     */
    remove: function (req, res) {
        var id = req.params.id;

        UserModel.findByIdAndRemove(id, function (err, User) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when deleting the User.',
                    error: err
                });
            }

            return res.status(204).json();
        });
    },
    /**
     * UserController.login()
     */
    login: function (req, res) {
        var email = req.body.email;
        var password = req.body.password;

        // Poiščemo uporabnika po e-pošti
        UserModel.findOne({ email: email }, function (err, User) {
            if (err) {
                return res.status(500).json({ message: 'Napaka pri iskanju uporabnika.', error: err });
            }
            if (!User) {
                return res.status(401).json({ message: 'Napačna e-pošta ali geslo.' });
            }

            // Preverimo geslo
            User.comparePassword(password, function(err, isMatch) {
                if (err || !isMatch) {
                    return res.status(401).json({ message: 'Napačna e-pošta ali geslo.' });
                }

                // Generiramo JWT žeton (veljavnost npr. 1 uro)
                // OPOMBA: V produkciji 'skrivniKljuc' prenesite v okoljske spremenljivke (.env datoteka)
                var token = jwt.sign({ id: User._id, email: User.email, type: User.type }, 'skrivniKljuc', { expiresIn: '1h' });

                return res.json({
                    message: 'Uspešna prijava',
                    token: token,
                    user: { id: User._id, name: User.name, type: User.type }
                });
            });
        });
    }
};
