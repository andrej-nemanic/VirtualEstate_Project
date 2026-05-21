var PropertyModel = require('../models/PropertyModel.js');
var Geocoder = require('../services/Geocoder.js');

const PROP_FIELDS = [
    'region', 'city', 'neighborhood', 'offerType', 'propertyType',
    'size', 'price', 'description', 'propertyLink', 'imageUrl'
];

async function buildCoordinates({ region, city, neighborhood, lat, lng }) {
    if (typeof lng === 'number' && typeof lat === 'number') {
        return { type: 'Point', coordinates: [lng, lat] };
    }
    const hit = await Geocoder.geocode({ region, city, neighborhood });
    if (hit) return { type: 'Point', coordinates: [hit.lng, hit.lat] };
    return { type: 'Point', coordinates: [0, 0] };
}

function applyFields(target, body) {
    PROP_FIELDS.forEach(f => {
        if (body[f] !== undefined) target[f] = body[f];
    });
}

module.exports = {

    list: async function (req, res) {
        try {
            const filter = {};
            if (req.query.propertyType) filter.propertyType = req.query.propertyType;
            if (req.query.offerType) filter.offerType = req.query.offerType;
            if (req.query.city) filter.city = req.query.city;
            if (req.query.region) filter.region = req.query.region;
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
                region: body.region, city: body.city, neighborhood: body.neighborhood,
                lat: body.lat, lng: body.lng
            });
            const property = new PropertyModel({ ...pick(body, PROP_FIELDS), coordinates: coords });
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
            const locationChanged = ['region', 'city', 'neighborhood'].some(
                f => body[f] !== undefined && body[f] !== property[f]
            );

            applyFields(property, body);

            if (typeof body.lng === 'number' && typeof body.lat === 'number') {
                property.coordinates = { type: 'Point', coordinates: [body.lng, body.lat] };
            } else if (locationChanged) {
                property.coordinates = await buildCoordinates({
                    region: property.region, city: property.city, neighborhood: property.neighborhood
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
                region: body.region, city: body.city, neighborhood: body.neighborhood,
                lat: body.lat, lng: body.lng
            });
            const property = await PropertyModel.create({ ...pick(body, PROP_FIELDS), coordinates: coords });
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

            const locationChanged = ['region', 'city', 'neighborhood'].some(
                f => body[f] !== undefined && body[f] !== property[f]
            );

            applyFields(property, body);

            if (typeof body.lng === 'number' && typeof body.lat === 'number') {
                property.coordinates = { type: 'Point', coordinates: [body.lng, body.lat] };
            } else if (locationChanged) {
                property.coordinates = await buildCoordinates({
                    region: property.region, city: property.city, neighborhood: property.neighborhood
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
                const hit = await Geocoder.geocode({
                    region: p.region, city: p.city, neighborhood: p.neighborhood
                });
                if (hit) {
                    p.coordinates = { type: 'Point', coordinates: [hit.lng, hit.lat] };
                    await p.save();
                    updated++;
                } else {
                    failures.push({ _id: p._id, region: p.region, city: p.city, neighborhood: p.neighborhood });
                }
            }
            return res.json({ checked: candidates.length, updated, failed: failures });
        } catch (err) {
            console.error('Geocode missing error:', err);
            return res.status(500).json({ message: 'Error during geocode backfill.', error: err });
        }
    }
};

function pick(obj, keys) {
    const out = {};
    keys.forEach(k => { if (obj[k] !== undefined) out[k] = obj[k]; });
    return out;
}
