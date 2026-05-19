var LocationModel = require('../models/LocationModel.js');
var Geocoder = require('../services/Geocoder.js');

module.exports = {

    near: async function (req, res) {
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
            return res.json(locations);
        } catch (err) {
            return res.status(500).json({ message: 'Error when getting locations.', error: err });
        }
    },

    list: async function (req, res) {
        try {
            const locations = await LocationModel.find();
            return res.json(locations);
        } catch (err) {
            return res.status(500).json({ message: 'Error when getting Location.', error: err });
        }
    },

    show: async function (req, res) {
        try {
            const location = await LocationModel.findById(req.params.id);
            if (!location) return res.status(404).json({ message: 'No such Location' });
            return res.json(location);
        } catch (err) {
            return res.status(500).json({ message: 'Error when getting Location.', error: err });
        }
    },

    create: async function (req, res) {
        try {
            const coords = req.body.coordinates || req.body.Coordinates;
            const location = new LocationModel({
                location: { type: 'Point', coordinates: coords },
                address: req.body.address,
                city: req.body.city
            });
            const saved = await location.save();
            return res.status(201).json(saved);
        } catch (err) {
            return res.status(500).json({ message: 'Error when creating Location', error: err });
        }
    },

    update: async function (req, res) {
        try {
            const location = await LocationModel.findById(req.params.id);
            if (!location) return res.status(404).json({ message: 'No such Location' });

            const coords = req.body.coordinates || req.body.Coordinates;
            if (coords) location.location = { type: 'Point', coordinates: coords };
            if (req.body.address !== undefined) location.address = req.body.address;
            if (req.body.city !== undefined) location.city = req.body.city;

            const saved = await location.save();
            return res.json(saved);
        } catch (err) {
            return res.status(500).json({ message: 'Error when updating Location.', error: err });
        }
    },

    remove: async function (req, res) {
        try {
            await LocationModel.findByIdAndDelete(req.params.id);
            return res.status(204).json();
        } catch (err) {
            return res.status(500).json({ message: 'Error when deleting the Location.', error: err });
        }
    },

    geocodeMissing: async function (req, res) {
        try {
            const limit = Math.min(parseInt(req.query.limit) || 50, 200);
            const candidates = await LocationModel.find({
                $or: [
                    { 'location.coordinates': { $size: 0 } },
                    { 'location.coordinates': [0, 0] },
                    { location: { $exists: false } }
                ]
            }).limit(limit);

            let updated = 0;
            const failures = [];
            for (const loc of candidates) {
                const hit = await Geocoder.geocode(loc.address, loc.city);
                if (hit) {
                    loc.location = { type: 'Point', coordinates: [hit.lng, hit.lat] };
                    await loc.save();
                    updated++;
                } else {
                    failures.push({ _id: loc._id, address: loc.address, city: loc.city });
                }
            }
            return res.json({ checked: candidates.length, updated, failed: failures });
        } catch (err) {
            console.error('Geocode missing error:', err);
            return res.status(500).json({ message: 'Error during geocode backfill.', error: err });
        }
    }
};
