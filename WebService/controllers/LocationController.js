var LocationModel = require('../models/LocationModel.js');

/**
 * LocationController.js
 *
 * @description :: Server-side logic for managing Locations.
 */
module.exports = {

    /**
     * LocationController.list()
     */
    list: function (req, res) {
        LocationModel.find(function (err, Locations) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when getting Location.',
                    error: err
                });
            }

            return res.json(Locations);
        });
    },

    /**
     * LocationController.show()
     */
    show: function (req, res) {
        var id = req.params.id;

        LocationModel.findOne({_id: id}, function (err, Location) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when getting Location.',
                    error: err
                });
            }

            if (!Location) {
                return res.status(404).json({
                    message: 'No such Location'
                });
            }

            return res.json(Location);
        });
    },

    /**
     * LocationController.create()
     */
    create: function (req, res) {
        var Location = new LocationModel({
			Coordinates : req.body.Coordinates,
            address : req.body.address,
            city : req.body.city
        });

        Location.save(function (err, Location) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when creating Location',
                    error: err
                });
            }

            return res.status(201).json(Location);
        });
    },

    /**
     * LocationController.update()
     */
    update: function (req, res) {
        var id = req.params.id;

        LocationModel.findOne({_id: id}, function (err, Location) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when getting Location',
                    error: err
                });
            }

            if (!Location) {
                return res.status(404).json({
                    message: 'No such Location'
                });
            }

            Location.Coordinates = req.body.Coordinates ? req.body.Coordinates : Location.Coordinates;
			Location.address = req.body.address ? req.body.address : Location.address;
			Location.city = req.body.city ? req.body.city : Location.city;
			
            Location.save(function (err, Location) {
                if (err) {
                    return res.status(500).json({
                        message: 'Error when updating Location.',
                        error: err
                    });
                }

                return res.json(Location);
            });
        });
    },

    /**
     * LocationController.remove()
     */
    remove: function (req, res) {
        var id = req.params.id;

        LocationModel.findByIdAndRemove(id, function (err, Location) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when deleting the Location.',
                    error: err
                });
            }

            return res.status(204).json();
        });
    }
};
