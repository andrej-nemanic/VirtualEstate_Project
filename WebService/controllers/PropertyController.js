var PropertyModel = require('../models/PropertyModel.js');
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

async function buildCoordinates({ address, city, lat, lng }) {
    if (typeof lng === 'number' && typeof lat === 'number') {
        return { type: 'Point', coordinates: [lng, lat] };
    }
    const hit = await Geocoder.geocode(address, city);
    if (hit) return { type: 'Point', coordinates: [hit.lng, hit.lat] };
    return { type: 'Point', coordinates: [0, 0] };
}

function applyCommonFields(property, body) {
    if (body.address !== undefined) property.address = body.address;
    if (body.city !== undefined) property.city = body.city;
    if (body.type !== undefined) property.type = normalizeType(body.type);
    ['size', 'price', 'buildYear', 'description', 'pictures', 'dateOfPosting', 'propertyLink'].forEach(f => {
        if (body[f] !== undefined) property[f] = body[f];
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

            const properties = await PropertyModel.find(filter);
            return res.json(properties);
        } catch (err) {
            console.error('List properties error:', err);
            return res.status(500).json({ message: 'Error when getting Property.', error: err });
        }
    },

    show: async function (req, res) {
        try {
            const property = await PropertyModel.findById(req.params.id);
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

            const properties = await PropertyModel.find({
                coordinates: {
                    $near: {
                        $geometry: { type: 'Point', coordinates: [lng, lat] },
                        $maxDistance: distance
                    }
                }
            });
            return res.json(properties);
        } catch (err) {
            console.error('Search error:', err);
            return res.status(500).json({ message: 'Error when searching properties.', error: err });
        }
    },

    create: async function (req, res) {
        try {
            const body = req.body || {};
            const coords = await buildCoordinates({
                address: body.address, city: body.city, lat: body.lat, lng: body.lng
            });
            const property = new PropertyModel({
                id: body.id,
                address: body.address,
                city: body.city,
                coordinates: coords,
                type: normalizeType(body.type),
                size: body.size,
                price: body.price,
                buildYear: body.buildYear,
                description: body.description,
                pictures: body.pictures,
                dateOfPosting: body.dateOfPosting || new Date(),
                propertyLink: body.propertyLink
            });
            const saved = await property.save();
            if (req.io) req.io.emit('propertyCreated', saved);
            return res.status(201).json(saved);
        } catch (err) {
            console.error('Create property error:', err);
            return res.status(500).json({ message: 'Error when creating Property', error: err });
        }
    },

    update: async function (req, res) {
        try {
            const property = await PropertyModel.findById(req.params.id);
            if (!property) return res.status(404).json({ message: 'No such Property' });

            const body = req.body || {};
            const addressChanged = body.address !== undefined && body.address !== property.address;
            const cityChanged = body.city !== undefined && body.city !== property.city;

            applyCommonFields(property, body);

            if (typeof body.lng === 'number' && typeof body.lat === 'number') {
                property.coordinates = { type: 'Point', coordinates: [body.lng, body.lat] };
            } else if (addressChanged || cityChanged) {
                property.coordinates = await buildCoordinates({
                    address: property.address, city: property.city
                });
            }

            const saved = await property.save();
            if (req.io) req.io.emit('propertyUpdated', saved);
            return res.json(saved);
        } catch (err) {
            console.error('Update property error:', err);
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
            const coords = await buildCoordinates({
                address: body.address, city: body.city, lat: body.lat, lng: body.lng
            });
            const property = await PropertyModel.create({
                id: body.id,
                address: body.address,
                city: body.city,
                coordinates: coords,
                type: normalizeType(body.type),
                size: body.size,
                price: body.price,
                buildYear: body.buildYear,
                description: body.description,
                pictures: body.pictures || [],
                dateOfPosting: body.dateOfPosting || new Date(),
                propertyLink: body.propertyLink
            });
            if (req.io) req.io.emit('propertyCreated', property);
            return res.status(201).json(property);
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

            const addressChanged = body.address !== undefined && body.address !== property.address;
            const cityChanged = body.city !== undefined && body.city !== property.city;

            applyCommonFields(property, body);

            if (typeof body.lng === 'number' && typeof body.lat === 'number') {
                property.coordinates = { type: 'Point', coordinates: [body.lng, body.lat] };
            } else if (addressChanged || cityChanged) {
                property.coordinates = await buildCoordinates({
                    address: property.address, city: property.city
                });
            }

            const saved = await property.save();
            if (req.io) req.io.emit('propertyUpdated', saved);
            return res.json(saved);
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
    },

    geocodeMissing: async function (req, res) {
        try {
            const limit = Math.min(parseInt(req.query.limit) || 50, 200);
            const candidates = await PropertyModel.find({
                $or: [
                    { 'coordinates.coordinates': [0, 0] },
                    { 'coordinates.coordinates': { $size: 0 } },
                    { coordinates: { $exists: false } }
                ]
            }).limit(limit);

            let updated = 0;
            const failures = [];
            for (const p of candidates) {
                const hit = await Geocoder.geocode(p.address, p.city);
                if (hit) {
                    p.coordinates = { type: 'Point', coordinates: [hit.lng, hit.lat] };
                    await p.save();
                    updated++;
                } else {
                    failures.push({ _id: p._id, address: p.address, city: p.city });
                }
            }
            return res.json({ checked: candidates.length, updated, failed: failures });
        } catch (err) {
            console.error('Geocode missing error:', err);
            return res.status(500).json({ message: 'Error during geocode backfill.', error: err });
        }
    }
};
