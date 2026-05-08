var PropertyModel = require('../models/PropertyModel.js');
var LocationModel = require('../models/LocationModel.js');
/**
 * PropertyController.js
 *
 * @description :: Server-side logic for managing Propertys.
 */
module.exports = {

    /**
     * PropertyController.list()
     */
    list: function (req, res) {
        PropertyModel.find(function (err, Propertys) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when getting Property.',
                    error: err
                });
            }

            return res.json(Propertys);
        });
    },

    /**
     * PropertyController.show()
     */
    show: function (req, res) {
        var id = req.params.id;

        PropertyModel.findOne({_id: id}, function (err, Property) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when getting Property.',
                    error: err
                });
            }

            if (!Property) {
                return res.status(404).json({
                    message: 'No such Property'
                });
            }

            return res.json(Property);
        });
    },




    /**
     * PropertyController.create()
     */
    create: function (req, res) {
        var Property = new PropertyModel({
			id : req.body.id,
            location : req.body.location,
            type : req.body.type,
            size : req.body.size,
            price : req.body.price,
            buildYear : req.body.buildYear,
            description : req.body.description,
            pictures : req.body.pictures,
            dateOfPosting : req.body.dateOfPosting,
            propertyLink : req.body.propertyLink
        });

        Property.save(function (err, Property) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when creating Property',
                    error: err
                });
            }

            return res.status(201).json(Property);
        });

        const io = req.app.get('io');
        io.emit('property_update', { action: 'A new property has been added!', propertyId: Property._id });
    },

    /**
     * PropertyController.update()
     */
    update: function (req, res) {
        var id = req.params.id;

        PropertyModel.findOne({_id: id}, function (err, Property) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when getting Property',
                    error: err
                });
            }

            if (!Property) {
                return res.status(404).json({
                    message: 'No such Property'
                });
            }

            Property.id = req.body.id ? req.body.id : Property.id;
            Property.location = req.body.location ? req.body.location : Property.location;
            Property.type = req.body.type ? req.body.type : Property.type;
            Property.size = req.body.size ? req.body.size : Property.size;
            Property.price = req.body.price ? req.body.price : Property.price;
            Property.buildYear = req.body.buildYear ? req.body.buildYear : Property.buildYear;
            Property.description = req.body.description ? req.body.description : Property.description;
            Property.pictures = req.body.pictures ? req.body.pictures : Property.pictures;
            Property.dateOfPosting = req.body.dateOfPosting ? req.body.dateOfPosting : Property.dateOfPosting;
            Property.propertyLink = req.body.propertyLink ? req.body.propertyLink : Property.propertyLink;
			
            Property.save(function (err, Property) {
                if (err) {
                    return res.status(500).json({
                        message: 'Error when updating Property.',
                        error: err
                    });
                }

                return res.json(Property);
            });
        });
    },

    /**
     * PropertyController.remove()
     */
    remove: function (req, res) {
        var id = req.params.id;

        PropertyModel.findByIdAndRemove(id, function (err, Property) {
            if (err) {
                return res.status(500).json({
                    message: 'Error when deleting the Property.',
                    error: err
                });
            }

            return res.status(204).json();
        });
    },
    
    searchByDistance: function (req, res) {
        var lat = parseFloat(req.query.lat);
        var lng = parseFloat(req.query.lng);
        var dist = parseFloat(req.query.dist) || 5000; // privzeto 5km

        if (!lat || !lng) {
            return res.status(400).json({ message: 'Manjkajo parametri lat in lng.' });
        }

        // 1. Najprej poiščemo ID-je lokacij, ki so v bližini
        LocationModel.find({
            location: {
                $near: {
                    $geometry: { type: "Point", coordinates: [lng, lat] },
                    $maxDistance: dist
                }
            }
        }, function (err, locations) {
            if (err) return res.status(500).json(err);

            var locationIds = locations.map(loc => loc._id);

            // 2. Nato poiščemo nepremičnine, ki se nahajajo na teh lokacijah
            PropertyModel.find({ location: { $in: locationIds } })
                .populate('location') // Pridruži podatke o lokaciji (naslov, mesto)
                .exec(function (err, properties) {
                    if (err) return res.status(500).json(err);
                    return res.json(properties);
                });
        });
    },
};
