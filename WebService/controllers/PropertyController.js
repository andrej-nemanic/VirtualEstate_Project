var PropertyModel = require('../models/PropertyModel.js');
var LocationModel = require('../models/LocationModel.js');
var Geocoder = require('../services/Geocoder.js');

const TYPE_ALIASES = {
    'stanovanje': 'apartment',
    'apartment': 'apartment',
    'apartma': 'apartment',
    'vikend': 'apartment',
    'poslovni prostor': 'apartment',
    'garaža': 'apartment',
    'garaza': 'apartment',
    'hiša': 'house',
    'hisa': 'house',
    'house': 'house',
    'zemljišče': 'land',
    'zemljisce': 'land',
    'land': 'land',
    'condominium': 'condominium'
};

function normalizeType(raw) {
    if (!raw) return 'house';
    const key = String(raw).trim().toLowerCase();
    return TYPE_ALIASES[key] || 'house';
}

async function findOrCreateLocation({ address, city, lat, lng }) {
    let location = await LocationModel.findOne({ address: address || '', city: city || '' });
    if (location) {
        const existing = location.location && location.location.coordinates;
        const hasZero = !existing || (existing[0] === 0 && existing[1] === 0);
        if (hasZero && typeof lng !== 'number' && typeof lat !== 'number') {
            const hit = await Geocoder.geocode(address, city);
            if (hit) {
                location.location = { type: 'Point', coordinates: [hit.lng, hit.lat] };
                await location.save();
            }
        }
        return location;
    }

    let coords;
    if (typeof lng === 'number' && typeof lat === 'number') {
        coords = [lng, lat];
    } else {
        const hit = await Geocoder.geocode(address, city);
        coords = hit ? [hit.lng, hit.lat] : [0, 0];
    }

    return await LocationModel.create({
        address: address || '',
        city: city || '',
        location: { type: 'Point', coordinates: coords }
    });
}

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
    },

    ingestCreate: async function (req, res) {
        try {
            const body = req.body || {};
            const location = await findOrCreateLocation({
                address: body.address,
                city: body.city,
                lat: body.lat,
                lng: body.lng
            });
            const property = await PropertyModel.create({
                id: body.id,
                location: location._id,
                type: normalizeType(body.type),
                size: body.size,
                price: body.price,
                buildYear: body.buildYear,
                description: body.description,
                pictures: body.pictures || [],
                dateOfPosting: body.dateOfPosting || new Date(),
                propertyLink: body.propertyLink
            });
            const populated = await PropertyModel.findById(property._id).populate('location');
            if (req.io) req.io.emit('propertyCreated', populated);
            return res.status(201).json(populated);
        } catch (err) {
            console.error('Ingest create error:', err);
            return res.status(500).json({ message: 'Error when ingesting Property.', error: err });
        }
    },

    ingestUpdate: async function (req, res) {
        try {
            const body = req.body || {};
            const property = await PropertyModel.findById(req.params.id);
            if (!property) return res.status(404).json({ message: 'No such Property' });

            if (body.address !== undefined || body.city !== undefined || body.lat !== undefined || body.lng !== undefined) {
                const location = await findOrCreateLocation({
                    address: body.address,
                    city: body.city,
                    lat: body.lat,
                    lng: body.lng
                });
                property.location = location._id;
            }
            if (body.type !== undefined) property.type = normalizeType(body.type);
            ['size', 'price', 'buildYear', 'description', 'pictures', 'dateOfPosting', 'propertyLink'].forEach(f => {
                if (body[f] !== undefined) property[f] = body[f];
            });

            await property.save();
            const populated = await PropertyModel.findById(property._id).populate('location');
            if (req.io) req.io.emit('propertyUpdated', populated);
            return res.json(populated);
        } catch (err) {
            console.error('Ingest update error:', err);
            return res.status(500).json({ message: 'Error when updating Property.', error: err });
        }
    },

    ingestRemove: async function (req, res) {
        try {
            const property = await PropertyModel.findByIdAndDelete(req.params.id);
            if (req.io && property) req.io.emit('propertyDeleted', { _id: property._id });
            return res.status(204).json();
        } catch (err) {
            return res.status(500).json({ message: 'Error when deleting Property.', error: err });
        }
    }
};
