import { OFFER_TYPES } from '../constants.js';

export default function PropertyFilters({ filters, setFilters, onReset }) {
  const update = (key, value) => setFilters({ ...filters, [key]: value });

  return (
    <div className="card" style={{ padding: 16 }}>
      <div className="filters">
        <div>
          <label style={{ fontSize: 12, color: '#6b7280' }}>Vrsta</label>
          <input value={filters.propertyType || ''} onChange={e => update('propertyType', e.target.value)} placeholder="npr. Stanovanje" />
        </div>
        <div>
          <label style={{ fontSize: 12, color: '#6b7280' }}>Ponudba</label>
          <select value={filters.offerType || ''} onChange={e => update('offerType', e.target.value)}>
            <option value="">Vse</option>
            {OFFER_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
          </select>
        </div>
        <div>
          <label style={{ fontSize: 12, color: '#6b7280' }}>Regija</label>
          <input value={filters.region || ''} onChange={e => update('region', e.target.value)} placeholder="vse" />
        </div>
        <div>
          <label style={{ fontSize: 12, color: '#6b7280' }}>Mesto / občina</label>
          <input value={filters.city || ''} onChange={e => update('city', e.target.value)} placeholder="vse" />
        </div>
        <div>
          <label style={{ fontSize: 12, color: '#6b7280' }}>Min. cena (€)</label>
          <input type="number" value={filters.minPrice || ''} onChange={e => update('minPrice', e.target.value)} />
        </div>
        <div>
          <label style={{ fontSize: 12, color: '#6b7280' }}>Maks. cena (€)</label>
          <input type="number" value={filters.maxPrice || ''} onChange={e => update('maxPrice', e.target.value)} />
        </div>
        <div>
          <label style={{ fontSize: 12, color: '#6b7280' }}>Min. velikost (m²)</label>
          <input type="number" value={filters.minSize || ''} onChange={e => update('minSize', e.target.value)} />
        </div>
        <div>
          <label style={{ fontSize: 12, color: '#6b7280' }}>Maks. velikost (m²)</label>
          <input type="number" value={filters.maxSize || ''} onChange={e => update('maxSize', e.target.value)} />
        </div>
        <div style={{ gridColumn: 'span 3' }}>
          <label style={{ fontSize: 12, color: '#6b7280' }}>Iskanje v opisu</label>
          <input value={filters.description || ''} onChange={e => update('description', e.target.value)} placeholder="npr. balkon, parking, terasa..." />
        </div>
        <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'flex-end' }}>
          <button className="secondary" onClick={onReset}>Ponastavi filtre</button>
        </div>
      </div>
    </div>
  );
}
