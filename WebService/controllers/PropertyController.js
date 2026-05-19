var PropertyModel = require('../models/PropertyModel.js');
var LocationModel = require('../models/LocationModel.js');

module.exports = {

    list: async function (req, res) {
        try {
            const filter = {};
            if (req.query.type) filter.type = req.query.type;
            if (req.query.minPrice || req.query.maxPrice) {
                filter.price = {};
                if (req.query.minPrice) filter.price.$gte = parseFloat(req.query.minPrice);
                if (req.query.maxPrice) filter.price.$lte = parseFloat(req.query.maxPrice);
            }
            if (req.query.minSize || req.query.maxSize) {
                filter.size = {};
                if (req.query.minSize) filter.size.$gte = parseFloat(req.query.minSize);
                if (req.query.maxSize) filter.size.$lte = parseFloat(req.query.maxSize);
            }

            const properties = await PropertyModel.find(filter).populate('location');
            return res.json(properties);
        } catch (err) {
            console.error('List properties error:', err);
            return res.status(500).json({ message: 'Error when getting Property.', error: err });
        }
    },

    show: async function (req, res) {
        try {
            const property = await PropertyModel.findById(req.params.id).populate('location');
            if (!property) return res.status(404).json({ message: 'No such Property' });
            return res.json(property);
        } catch (err) {
            return res.status(500).json({ message: 'Error when getting Property.', error: err });
        }
    },

    searchByDistance: async function (req, res) {
        try {
            const lat = parseFloat(req.query.lat);
            const lng = parseFloat(req.query.lng);
            const distance = parseInt(req.query.distance) || 5000;

            if (isNaN(lat) || isNaN(lng)) {
                return res.status(400).json({ message: 'lat in lng sta obvezna' });
            }

            const locations = await LocationModel.find({
                location: {
                    $near: {
                        $geometry: { type: 'Point', coordinates: [lng, lat] },
                        $maxDistance: distance
                    }
                }
            });
            const locationIds = locations.map(l => l._id);
            const properties = await PropertyModel.find({ location: { $in: locationIds } }).populate('location');
            return res.json(properties);
        } catch (err) {
            console.error('Search error:', err);
            return res.status(500).json({ message: 'Error when searching properties.', error: err });
        }
    },

    create: async function (req, res) {
        try {
            const property = new PropertyModel({
                id: req.body.id,
                location: req.body.location,
                type: req.body.type,
                size: req.body.size,
                price: req.body.price,
                buildYear: req.body.buildYear,
                description: req.body.description,
                pictures: req.body.pictures,
                dateOfPosting: req.body.dateOfPosting || new Date(),
                propertyLink: req.body.propertyLink
            });
            const saved = await property.save();
            const populated = await PropertyModel.findById(saved._id).populate('location');

            if (req.io) req.io.emit('propertyCreated', populated);

            return res.status(201).json(populated);
        } catch (err) {
            return res.status(500).json({ message: 'Error when creating Property', error: err });
        }
    },

    update: async function (req, res) {
        try {
            const property = await PropertyModel.findById(req.params.id);
            if (!property) return res.status(404).json({ message: 'No such Property' });

            const fields = ['id', 'location', 'type', 'size', 'price', 'buildYear', 'description', 'pictures', 'dateOfPosting', 'propertyLink'];
            fields.forEach(f => {
                if (req.body[f] !== undefined) property[f] = req.body[f];
            });

            const saved = await property.save();
            const populated = await PropertyModel.findById(saved._id).populate('location');

            if (req.io) req.io.emit('propertyUpdated', populated);

            return res.json(populated);
        } catch (err) {
            return res.status(500).json({ message: 'Error when updating Property.', error: err });
        }
    },

    remove: async function (req, res) {
        try {
            const property = await PropertyModel.findByIdAndDelete(req.params.id);
            if (req.io && property) req.io.emit('propertyDeleted', { _id: property._id });
            return res.status(204).json();
        } catch (err) {
            return res.status(500).json({ message: 'Error when deleting the Property.', error: err });
        }
    }
};
