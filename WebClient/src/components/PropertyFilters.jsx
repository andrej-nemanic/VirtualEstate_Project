import { useMemo, useState } from 'react';
import { OFFER_TYPES } from '../constants.js';

const FILTER_LABELS = {
  propertyType: 'Vrsta',
  offerType: 'Ponudba',
  region: 'Regija',
  city: 'Mesto',
  minPrice: 'Min. cena',
  maxPrice: 'Maks. cena',
  minSize: 'Min. m²',
  maxSize: 'Maks. m²',
  description: 'Opis'
};

export default function PropertyFilters({ filters, setFilters, onReset }) {
  const [expanded, setExpanded] = useState(false);
  const update = (key, value) => setFilters({ ...filters, [key]: value });
  const removeKey = (key) => {
    const next = { ...filters };
    delete next[key];
    setFilters(next);
  };

  const activePills = useMemo(() => {
    return Object.entries(filters)
      .filter(([k, v]) => v && FILTER_LABELS[k])
      .map(([k, v]) => ({ key: k, label: FILTER_LABELS[k], value: v }));
  }, [filters]);

  const hasFilters = activePills.length > 0;

  return (
    <div className="card filters-bar">
      <div className="filter-toolbar">
        <div className="grow search-field">
          <svg
            className="search-icon"
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden="true"
          >
            <circle cx="11" cy="11" r="7" />
            <line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
          <input
            value={filters.description || ''}
            onChange={e => update('description', e.target.value)}
            placeholder="Iskanje po opisu (npr. balkon, parking, terasa…)"
          />
        </div>
        <select
          value={filters.offerType || ''}
          onChange={e => update('offerType', e.target.value)}
          style={{ width: 'auto', minWidth: 140 }}
        >
          <option value="">Vse ponudbe</option>
          {OFFER_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
        </select>
        <button
          className={expanded ? '' : 'secondary'}
          onClick={() => setExpanded(v => !v)}
          aria-expanded={expanded}
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <line x1="4" y1="21" x2="4" y2="14" /><line x1="4" y1="10" x2="4" y2="3" />
            <line x1="12" y1="21" x2="12" y2="12" /><line x1="12" y1="8" x2="12" y2="3" />
            <line x1="20" y1="21" x2="20" y2="16" /><line x1="20" y1="12" x2="20" y2="3" />
            <line x1="1" y1="14" x2="7" y2="14" /><line x1="9" y1="8" x2="15" y2="8" /><line x1="17" y1="16" x2="23" y2="16" />
          </svg>
          Filtri{hasFilters ? ` (${activePills.length})` : ''}
        </button>
        {hasFilters && (
          <button className="ghost sm" onClick={onReset}>Počisti</button>
        )}
      </div>

      {hasFilters && (
        <div className="row wrap" style={{ marginTop: '0.75rem' }}>
          {activePills.map(p => (
            <span key={p.key} className="filter-pill">
              {p.label}: <strong style={{ fontWeight: 600 }}>{p.value}</strong>
              <button onClick={() => removeKey(p.key)} aria-label={`Odstrani ${p.label}`}>×</button>
            </span>
          ))}
        </div>
      )}

      {expanded && (
        <div className="filters-grid">
          <div>
            <label>Vrsta</label>
            <input value={filters.propertyType || ''} onChange={e => update('propertyType', e.target.value)} placeholder="npr. Stanovanje" />
          </div>
          <div>
            <label>Regija</label>
            <input value={filters.region || ''} onChange={e => update('region', e.target.value)} placeholder="vse" />
          </div>
          <div>
            <label>Mesto / občina</label>
            <input value={filters.city || ''} onChange={e => update('city', e.target.value)} placeholder="vse" />
          </div>
          <div>
            <label>Ponudba</label>
            <select value={filters.offerType || ''} onChange={e => update('offerType', e.target.value)}>
              <option value="">Vse</option>
              {OFFER_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
            </select>
          </div>
          <div>
            <label>Min. cena (€)</label>
            <input type="number" value={filters.minPrice || ''} onChange={e => update('minPrice', e.target.value)} placeholder="0" />
          </div>
          <div>
            <label>Maks. cena (€)</label>
            <input type="number" value={filters.maxPrice || ''} onChange={e => update('maxPrice', e.target.value)} placeholder="∞" />
          </div>
          <div>
            <label>Min. velikost (m²)</label>
            <input type="number" value={filters.minSize || ''} onChange={e => update('minSize', e.target.value)} placeholder="0" />
          </div>
          <div>
            <label>Maks. velikost (m²)</label>
            <input type="number" value={filters.maxSize || ''} onChange={e => update('maxSize', e.target.value)} placeholder="∞" />
          </div>
        </div>
      )}
    </div>
  );
}
