const NOMINATIM_URL = 'https://nominatim.openstreetmap.org/search';
const USER_AGENT = 'VirtualEstate/1.0 (FERI student project)';
const MIN_INTERVAL_MS = 1100;

const cache = new Map();
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
    // 1) most specific: neighborhood + city + Slovenia
    if (neighborhood) candidates.add(dedupeJoin([neighborhood, city, 'Slovenia']));
    // 2) neighborhood + region + Slovenia (in case city is too generic)
    if (neighborhood && region) candidates.add(dedupeJoin([neighborhood, region, 'Slovenia']));
    // 3) city + region + Slovenia
    if (city && region) candidates.add(dedupeJoin([city, region, 'Slovenia']));
    // 4) just city + Slovenia
    if (city) candidates.add(dedupeJoin([city, 'Slovenia']));
    return Array.from(candidates).filter(q => q && q.toLowerCase() !== 'slovenia');
}

async function nominatim(query) {
    await throttle();
    const url = `${NOMINATIM_URL}?format=json&limit=1&countrycodes=si&q=${encodeURIComponent(query)}`;
    try {
        const res = await fetch(url, { headers: { 'User-Agent': USER_AGENT } });
        if (!res.ok) {
            console.warn('Nominatim status', res.status, 'for', query);
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
        console.warn('Geocode error for', query, err.message);
        return null;
    }
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
        const hit = await nominatim(query);
        cache.set(cacheKey, hit);
        if (hit) {
            console.log('Geocoded', JSON.stringify(query), '->', hit);
            return hit;
        }
    }
    return null;
}

module.exports = { geocode };
