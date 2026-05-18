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
    create: async function (req, res) {
        var User = new UserModel({
            name : req.body.name,
            id : req.body.id,
            personalData : req.body.personalData,
            email : req.body.email,
            password : req.body.password,
            type : req.body.type
        });

        try {
            // Shranimo uporabnika z uporabo await (brez callback funkcije)
            var savedUser = await User.save();
            // Če je shranjevanje uspešno, vrnemo status 211 (ali 201 Created) in podatke
            return res.status(201).json(savedUser);
        } catch (err) {

            console.error("Točna napaka iz MongoDB:", err);
            // Lovljenje napake (npr. če email že obstaja ali podatki niso popolni)
            return res.status(500).json({
                message: 'Error when creating User',
                error: err
            });
        }
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
    login: async function (req, res) {
        var email = req.body.email;
        var password = req.body.password;

        try {
            // Poiščemo uporabnika z await namesto s callbackom
            var User = await UserModel.findOne({ email: email });
            
            if (!User) {
                return res.status(401).json({ message: 'Napačna e-pošta ali geslo.' });
            }

            // Preverimo geslo (comparePassword še vedno uporablja vaš callback, kar je v redu, ker je to vaša metoda)
            User.comparePassword(password, function(err, isMatch) {
                if (err || !isMatch) {
                    return res.status(401).json({ message: 'Napačna e-pošta ali geslo.' });
                }

                // Generiramo JWT žeton
                var token = jwt.sign(
                    { id: User._id, email: User.email, type: User.type }, 
                    'skrivniKljuc', 
                    { expiresIn: '1h' }
                );

                return res.json({
                    message: 'Uspešna prijava',
                    token: token,
                    user: {
                        id: User._id,
                        name: User.name,
                        type: User.type
                    }
                });
            });
        } catch (err) {
            return res.status(500).json({ message: 'Napaka pri iskanju uporabnika.', error: err });
        }
    },
};
