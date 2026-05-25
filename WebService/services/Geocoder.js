const { LRUCache } = require('lru-cache');
const logger = require('./Logger');

const NOMINATIM_URL = 'https://nominatim.openstreetmap.org/search';
const PHOTON_URL = 'https://photon.komoot.io/api';
const USER_AGENT = 'VirtualEstate/1.0 (FERI student project)';
const MIN_INTERVAL_MS = 1100;

const cache = new LRUCache({
    max: 1000,
    ttl: 7 * 24 * 60 * 60 * 1000
});

let lastCallAt = 0;

function wait(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

async function throttle() {
    const elapsed = Date.now() - lastCallAt;
    if (elapsed < MIN_INTERVAL_MS) {
        await wait(MIN_INTERVAL_MS - elapsed);
    }
    lastCallAt = Date.now();
}

function dedupeJoin(parts) {
    const seen = new Set();
    const out = [];
    for (const raw of parts) {
        if (!raw) continue;
        const key = String(raw).trim().toLowerCase();
        if (key && !seen.has(key)) {
            seen.add(key);
            out.push(String(raw).trim());
        }
    }
    return out.join(', ');
}

function buildCandidates({ region, city, neighborhood }) {
    const candidates = new Set();
    if (neighborhood) candidates.add(dedupeJoin([neighborhood, city, 'Slovenia']));
    if (neighborhood && region) candidates.add(dedupeJoin([neighborhood, region, 'Slovenia']));
    if (city && region) candidates.add(dedupeJoin([city, region, 'Slovenia']));
    if (city) candidates.add(dedupeJoin([city, 'Slovenia']));
    return Array.from(candidates).filter(q => q && q.toLowerCase() !== 'slovenia');
}

async function nominatim(query) {
    await throttle();
    const url = `${NOMINATIM_URL}?format=json&limit=1&countrycodes=si&q=${encodeURIComponent(query)}`;
    try {
        const res = await fetch(url, { headers: { 'User-Agent': USER_AGENT } });
        if (!res.ok) {
            logger.warn({ status: res.status, query }, 'Nominatim non-OK status');
            return null;
        }
        const json = await res.json();
        const hit = Array.isArray(json) && json[0];
        if (!hit) return null;
        const lat = parseFloat(hit.lat);
        const lng = parseFloat(hit.lon);
        if (Number.isNaN(lat) || Number.isNaN(lng)) return null;
        return { lat, lng };
    } catch (err) {
        logger.warn({ err: err.message, query }, 'Nominatim error');
        return null;
    }
}

async function photon(query) {
    const url = `${PHOTON_URL}?lang=en&limit=1&q=${encodeURIComponent(query)}`;
    try {
        const res = await fetch(url, { headers: { 'User-Agent': USER_AGENT } });
        if (!res.ok) return null;
        const json = await res.json();
        const feat = json?.features?.[0];
        if (!feat || !feat.geometry || !Array.isArray(feat.geometry.coordinates)) return null;
        const [lng, lat] = feat.geometry.coordinates;
        if (Number.isNaN(lat) || Number.isNaN(lng)) return null;
        const country = feat.properties?.countrycode || feat.properties?.country;
        if (country && String(country).toLowerCase() !== 'si' && String(country).toLowerCase() !== 'slovenia') {
            return null;
        }
        return { lat, lng };
    } catch (err) {
        logger.warn({ err: err.message, query }, 'Photon error');
        return null;
    }
}

async function geocodeQuery(query) {
    const hit = await nominatim(query);
    if (hit) return hit;
    logger.debug({ query }, 'Nominatim miss, poskušam Photon...');
    return await photon(query);
}

async function geocode({ region, city, neighborhood }) {
    const candidates = buildCandidates({ region, city, neighborhood });
    if (candidates.length === 0) return null;

    for (const query of candidates) {
        const cacheKey = query.toLowerCase();
        if (cache.has(cacheKey)) {
            const cached = cache.get(cacheKey);
            if (cached) return cached;
            continue;
        }
        const hit = await geocodeQuery(query);
        cache.set(cacheKey, hit);
        if (hit) {
            logger.info({ query, hit }, 'Geocoded');
            return hit;
        }
    }
    return null;
}

module.exports = { geocode };
