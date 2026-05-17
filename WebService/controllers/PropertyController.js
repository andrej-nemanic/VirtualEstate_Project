var PropertyModel = require('../models/PropertyModel.js');

/**
 * PropertyController.js
 *
 * @description :: Server-side logic for managing Propertys.
 */
module.exports = {

    /**
     * PropertyController.list()
     */
    list: async function (req, res) {
        try {
            // POPRAVLJENO: exec() ne sprejema več callbacka, uporabimo await
            var Propertys = await PropertyModel.find().populate('location').exec();
            return res.json(Propertys);
        } catch (err) {
            console.error("Napaka pri populaciji:", err);
            return res.status(500).json({
                message: 'Error when getting Property.',
                error: err
            });
        }
    },

    /**
     * PropertyController.show()
     */
    show: async function (req, res) {
        var id = req.params.id;

        try {
            var Property = await PropertyModel.findOne({_id: id}).exec();
            if (!Property) {
                return res.status(404).json({
                    message: 'No such Property'
                });
            }
            return res.json(Property);
        } catch (err) {
            return res.status(500).json({
                message: 'Error when getting Property.',
                error: err
            });
        }
    },

    /**
     * PropertyController.create()
     */
    create: async function (req, res) {
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

        try {
            var savedProperty = await Property.save();
            
            // POPRAVLJENO: Pred oddajanjem preko Web Socketov moramo populirati lokacijo,
            // da odjemalec dobi GeoJSON koordinate za takojšen izris na Leaflet zemljevidu.
            const populatedProperty = await PropertyModel.findById(savedProperty._id)
                                                         .populate('location')
                                                         .exec();
            
            // PROŽENJE REALNOČASOVNEGA DOGODKA
            if (req.io) {
                req.io.emit('propertyCreated', populatedProperty);
            }

            return res.status(201).json(populatedProperty);
        } catch (err) {
            return res.status(500).json({
                message: 'Error when creating Property',
                error: err
            });
        }
    },

    /**
     * PropertyController.update()
     */
    update: async function (req, res) {
        var id = req.params.id;

        try {
            var Property = await PropertyModel.findOne({_id: id}).exec();
            if (!Property) {
                return res.status(404).json({
                    message: 'No such Property'
                });
            }

            // Posodobitev polj
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

            var updatedProperty = await Property.save();
            return res.json(updatedProperty);
        } catch (err) {
            return res.status(500).json({
                message: 'Error when updating Property.',
                error: err
            });
        }
    },

    /**
     * PropertyController.remove()
     */
    remove: async function (req, res) {
        var id = req.params.id;

        try {
            // POPRAVLJENO: findByIdAndRemove je bil v Mongoose odstranjen/zastaran, uporabimo findByIdAndDelete
            var Property = await PropertyModel.findByIdAndDelete(id).exec();
            return res.status(204).json();
        } catch (err) {
            return res.status(500).json({
                message: 'Error when deleting the Property.',
                error: err
            });
        }
    }
};