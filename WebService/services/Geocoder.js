const NOMINATIM_URL = 'https://nominatim.openstreetmap.org/search';
const USER_AGENT = 'VirtualEstate/1.0 (FERI student project)';
const MIN_INTERVAL_MS = 1100;

const REGION_HINTS = new Set([
    'gorenjska', 'štajerska', 'stajerska', 'primorska', 'dolenjska',
    'koroška', 'koroska', 'prekmurje', 'notranjska', 'bela krajina',
    'goriška', 'goriska', 'zasavje', 'posavje', 'savinjska', 'osrednjeslovenska',
    'jugovzhodna slovenija', 'obalno-kraška', 'obalno-kraska', 'pomurska'
]);

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

function tokens(str) {
    return String(str || '')
        .split(',')
        .map(s => s.trim())
        .filter(Boolean);
}

function dedupeJoin(parts) {
    const seen = new Set();
    const out = [];
    for (const p of parts) {
        const key = p.toLowerCase();
        if (key && !seen.has(key)) {
            seen.add(key);
            out.push(p);
        }
    }
    return out.join(', ');
}

function buildCandidates(address, city) {
    const addrTokens = tokens(address);
    const withoutRegion = addrTokens.filter(t => !REGION_HINTS.has(t.toLowerCase()));
    const candidates = new Set();

    // 1) full dedupe (address + city + Slovenia)
    candidates.add(dedupeJoin([...addrTokens, city, 'Slovenia'].filter(Boolean)));
    // 2) dedupe without region prefixes
    candidates.add(dedupeJoin([...withoutRegion, city, 'Slovenia'].filter(Boolean)));
    // 3) most specific token + city + Slovenia
    if (withoutRegion.length > 0) {
        const last = withoutRegion[withoutRegion.length - 1];
        candidates.add(dedupeJoin([last, city, 'Slovenia'].filter(Boolean)));
    }
    // 4) city only
    if (city) {
        candidates.add(dedupeJoin([city, 'Slovenia']));
    }

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

async function geocode(address, city) {
    const candidates = buildCandidates(address, city);
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
