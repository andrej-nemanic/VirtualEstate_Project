var PropertyModel = require('../models/PropertyModel.js');
var Geocoder = require('../services/Geocoder.js');
var logger = require('../services/Logger');

const PROP_FIELDS = [
    'region', 'city', 'neighborhood', 'offerType', 'propertyType',
    'size', 'price', 'description', 'propertyLink', 'imageUrl', 'source'
];

const ALLOWED_SORT_FIELDS = new Set(['price', 'size', 'createdAt', 'city', 'region', 'propertyType', 'offerType']);

async function buildCoordinates({ region, city, neighborhood, lat, lng }) {
    if (typeof lng === 'number' && typeof lat === 'number') {
        return { type: 'Point', coordinates: [lng, lat] };
    }
    const hit = await Geocoder.geocode({ region, city, neighborhood });
    if (hit) return { type: 'Point', coordinates: [hit.lng, hit.lat] };
    return null;
}

function applyFields(target, body) {
    PROP_FIELDS.forEach(f => {
        if (body[f] !== undefined) target[f] = body[f];
    });
}

function pick(obj, keys) {
    const out = {};
    keys.forEach(k => { if (obj[k] !== undefined) out[k] = obj[k]; });
    return out;
}

function buildListFilter(query) {
    const filter = {};
    if (query.propertyType) filter.propertyType = { $regex: query.propertyType, $options: 'i' };
    if (query.offerType) filter.offerType = query.offerType;
    if (query.city) filter.city = { $regex: query.city, $options: 'i' };
    if (query.region) filter.region = { $regex: query.region, $options: 'i' };
    if (query.source) filter.source = { $regex: query.source, $options: 'i' };
    if (query.description) filter.description = { $regex: query.description, $options: 'i' };
    if (query.minPrice || query.maxPrice) {
        filter.price = {};
        if (query.minPrice) filter.price.$gte = parseFloat(query.minPrice);
        if (query.maxPrice) filter.price.$lte = parseFloat(query.maxPrice);
    }
    if (query.minSize || query.maxSize) {
        filter.size = {};
        if (query.minSize) filter.size.$gte = parseFloat(query.minSize);
        if (query.maxSize) filter.size.$lte = parseFloat(query.maxSize);
    }
    return filter;
}

function buildSort(query) {
    if (!query.sortBy || !ALLOWED_SORT_FIELDS.has(query.sortBy)) return null;
    const dir = String(query.sortDir || 'asc').toLowerCase() === 'desc' ? -1 : 1;
    return { [query.sortBy]: dir };
}

module.exports = {

    list: async function (req, res) {
        try {
            const filter = buildListFilter(req.query);
            const sort = buildSort(req.query);

            const hasPagination = req.query.page !== undefined || req.query.limit !== undefined;

            if (hasPagination) {
                const page = Math.max(1, parseInt(req.query.page) || 1);
                const limit = Math.min(200, Math.max(1, parseInt(req.query.limit) || 20));
                const skip = (page - 1) * limit;

                const [total, data] = await Promise.all([
                    PropertyModel.countDocuments(filter),
                    PropertyModel.find(filter).sort(sort || { _id: -1 }).skip(skip).limit(limit)
                ]);
                return res.json({
                    data,
                    total,
                    page,
                    limit,
                    pages: Math.ceil(total / limit) || 1
                });
            }

            let query = PropertyModel.find(filter);
            if (sort) query = query.sort(sort);
            const properties = await query;
            return res.json(properties);
        } catch (err) {
            logger.error({ err: err.message }, 'List properties error');
            return res.status(500).json({ message: 'Napaka pri branju nepremičnin.' });
        }
    },

    stats: async function (req, res) {
        try {
            const filter = buildListFilter(req.query);
            const [total, byType, byCity, byOffer, priceAgg] = await Promise.all([
                PropertyModel.countDocuments(filter),
                PropertyModel.aggregate([
                    { $match: filter },
                    { $group: { _id: '$propertyType', count: { $sum: 1 }, avgPrice: { $avg: '$price' } } },
                    { $sort: { count: -1 } }
                ]),
                PropertyModel.aggregate([
                    { $match: filter },
                    { $group: { _id: '$city', count: { $sum: 1 }, avgPrice: { $avg: '$price' } } },
                    { $sort: { count: -1 } },
                    { $limit: 20 }
                ]),
                PropertyModel.aggregate([
                    { $match: filter },
                    { $group: { _id: '$offerType', count: { $sum: 1 } } }
                ]),
                PropertyModel.aggregate([
                    { $match: filter },
                    { $group: {
                        _id: null,
                        avgPrice: { $avg: '$price' },
                        minPrice: { $min: '$price' },
                        maxPrice: { $max: '$price' },
                        avgSize: { $avg: '$size' }
                    } }
                ])
            ]);
            const overall = priceAgg[0] || { avgPrice: 0, minPrice: 0, maxPrice: 0, avgSize: 0 };
            return res.json({
                total,
                byType: byType.map(r => ({ propertyType: r._id || 'neznano', count: r.count, avgPrice: Math.round(r.avgPrice || 0) })),
                byCity: byCity.map(r => ({ city: r._id || 'neznano', count: r.count, avgPrice: Math.round(r.avgPrice || 0) })),
                byOffer: byOffer.map(r => ({ offerType: r._id || 'neznano', count: r.count })),
                overall: {
                    avgPrice: Math.round(overall.avgPrice || 0),
                    minPrice: overall.minPrice || 0,
                    maxPrice: overall.maxPrice || 0,
                    avgSize: Math.round(overall.avgSize || 0)
                }
            });
        } catch (err) {
            logger.error({ err: err.message }, 'Stats error');
            return res.status(500).json({ message: 'Napaka pri pridobivanju statistik.' });
        }
    },

    show: async function (req, res) {
        try {
            const property = await PropertyModel.findById(req.params.id);
            if (!property) return res.status(404).json({ message: 'Nepremičnina ne obstaja.' });
            return res.json(property);
        } catch (err) {
            logger.error({ err: err.message }, 'Show property error');
            return res.status(500).json({ message: 'Napaka pri branju nepremičnine.' });
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
            logger.error({ err: err.message }, 'Search error');
            return res.status(500).json({ message: 'Napaka pri iskanju nepremičnin.' });
        }
    },

    create: async function (req, res) {
        try {
            const body = req.body || {};
            const coords = await buildCoordinates({
                region: body.region, city: body.city, neighborhood: body.neighborhood,
                lat: body.lat, lng: body.lng
            });
            const doc = { ...pick(body, PROP_FIELDS) };
            if (coords) doc.coordinates = coords;
            const property = await PropertyModel.create(doc);
            if (req.io) req.io.emit('propertyCreated', property);
            return res.status(201).json(property);
        } catch (err) {
            logger.error({ err: err.message }, 'Create property error');
            return res.status(500).json({ message: 'Napaka pri ustvarjanju nepremičnine.' });
        }
    },

    update: async function (req, res) {
        try {
            const property = await PropertyModel.findById(req.params.id);
            if (!property) return res.status(404).json({ message: 'Nepremičnina ne obstaja.' });

            const body = req.body || {};
            const locationChanged = ['region', 'city', 'neighborhood'].some(
                f => body[f] !== undefined && body[f] !== property[f]
            );

            applyFields(property, body);

            if (typeof body.lng === 'number' && typeof body.lat === 'number') {
                property.coordinates = { type: 'Point', coordinates: [body.lng, body.lat] };
            } else if (locationChanged) {
                const coords = await buildCoordinates({
                    region: property.region, city: property.city, neighborhood: property.neighborhood
                });
                if (coords) {
                    property.coordinates = coords;
                } else {
                    property.coordinates = undefined;
                }
            }

            const saved = await property.save();
            if (req.io) req.io.emit('propertyUpdated', saved);
            return res.json(saved);
        } catch (err) {
            logger.error({ err: err.message }, 'Update property error');
            return res.status(500).json({ message: 'Napaka pri posodabljanju nepremičnine.' });
        }
    },

    remove: async function (req, res) {
        try {
            const property = await PropertyModel.findByIdAndDelete(req.params.id);
            if (req.io && property) req.io.emit('propertyDeleted', { _id: property._id });
            return res.status(204).json();
        } catch (err) {
            logger.error({ err: err.message }, 'Delete property error');
            return res.status(500).json({ message: 'Napaka pri brisanju nepremičnine.' });
        }
    },

    ingestCreate: async function (req, res) {
        try {
            const body = req.body || {};
            const coords = await buildCoordinates({
                region: body.region, city: body.city, neighborhood: body.neighborhood,
                lat: body.lat, lng: body.lng
            });
            const doc = { ...pick(body, PROP_FIELDS) };
            if (coords) doc.coordinates = coords;
            const property = await PropertyModel.create(doc);
            if (req.io) req.io.emit('propertyCreated', property);
            return res.status(201).json(property);
        } catch (err) {
            logger.error({ err: err.message }, 'Ingest create error');
            return res.status(500).json({ message: 'Napaka pri ustvarjanju nepremičnine.' });
        }
    },

    ingestUpdate: async function (req, res) {
        try {
            const body = req.body || {};
            const property = await PropertyModel.findById(req.params.id);
            if (!property) return res.status(404).json({ message: 'Nepremičnina ne obstaja.' });

            const locationChanged = ['region', 'city', 'neighborhood'].some(
                f => body[f] !== undefined && body[f] !== property[f]
            );

            applyFields(property, body);

            if (typeof body.lng === 'number' && typeof body.lat === 'number') {
                property.coordinates = { type: 'Point', coordinates: [body.lng, body.lat] };
            } else if (locationChanged) {
                const coords = await buildCoordinates({
                    region: property.region, city: property.city, neighborhood: property.neighborhood
                });
                if (coords) {
                    property.coordinates = coords;
                } else {
                    property.coordinates = undefined;
                }
            }

            const saved = await property.save();
            if (req.io) req.io.emit('propertyUpdated', saved);
            return res.json(saved);
        } catch (err) {
            logger.error({ err: err.message }, 'Ingest update error');
            return res.status(500).json({ message: 'Napaka pri posodabljanju nepremičnine.' });
        }
    },

    ingestRemove: async function (req, res) {
        try {
            const property = await PropertyModel.findByIdAndDelete(req.params.id);
            if (req.io && property) req.io.emit('propertyDeleted', { _id: property._id });
            return res.status(204).json();
        } catch (err) {
            logger.error({ err: err.message }, 'Ingest delete error');
            return res.status(500).json({ message: 'Napaka pri brisanju nepremičnine.' });
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
            logger.error({ err: err.message }, 'Geocode missing error');
            return res.status(500).json({ message: 'Napaka pri geokodiranju.' });
        }
    }
};
