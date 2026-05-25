import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { propertyApi } from '../api/client.js';
import { socket } from '../api/socket.js';
import PropertyMap from '../components/PropertyMap.jsx';
import PropertyCharts from '../components/PropertyCharts.jsx';
import PropertyFilters from '../components/PropertyFilters.jsx';
import { PropertyGridSkeleton } from '../components/Skeleton.jsx';
import { hasSize } from '../constants.js';

const FILTER_KEYS = ['propertyType', 'offerType', 'region', 'city', 'minPrice', 'maxPrice', 'minSize', 'maxSize', 'description', 'bbox', 'polygon', 'near'];
const GEO_KEYS = ['bbox', 'polygon', 'near'];

function searchParamsToFilters(sp) {
  const f = {};
  FILTER_KEYS.forEach(k => {
    const v = sp.get(k);
    if (v) f[k] = v;
  });
  return f;
}

function filtersToSearchParams(filters) {
  const out = {};
  FILTER_KEYS.forEach(k => {
    if (filters[k]) out[k] = filters[k];
  });
  return out;
}

const CHART_LABELS = { byType: 'Število po tipu', avgPrice: 'Povprečna cena', scatter: 'Cena vs. velikost' };

export default function Dashboard() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [properties, setProperties] = useState([]);
  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState('');

  const [chartsOpen, setChartsOpen] = useState(false);
  const [chartOfferType, setChartOfferType] = useState('Prodaja');
  const [visibleCharts, setVisibleCharts] = useState({ byType: true, avgPrice: true, scatter: true });

  const filters = useMemo(() => searchParamsToFilters(searchParams), [searchParams]);

  const setFilters = (next) => {
    setSearchParams(filtersToSearchParams(next), { replace: true });
  };

  const resetFilters = () => setSearchParams({}, { replace: true });

  const activeGeoKey = GEO_KEYS.find(k => filters[k]);
  const hasArea = Boolean(activeGeoKey);

  const handleAreaSelected = ({ type, value }) => {
    const next = { ...filters };
    GEO_KEYS.forEach(k => delete next[k]);
    next[type] = value;
    setFilters(next);
  };

  const handleAreaCleared = () => {
    const next = { ...filters };
    GEO_KEYS.forEach(k => delete next[k]);
    setFilters(next);
  };

  const chartProperties = useMemo(
    () => properties.filter(p => p.offerType === chartOfferType && hasSize(p)),
    [properties, chartOfferType]
  );

  const fetchProperties = async () => {
    setLoading(true);
    try {
      const res = await propertyApi.list(filters);
      setProperties(res.data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProperties();
  }, [searchParams]);

  useEffect(() => {
    const onCreate = (p) => {
      setProperties(prev => [p, ...prev]);
      setToast(`Nova nepremičnina: ${p.propertyType} v ${p.city || ''}`);
      setTimeout(() => setToast(''), 3500);
    };
    const onUpdate = (p) => {
      setProperties(prev => prev.map(x => x._id === p._id ? p : x));
      setToast('Nepremičnina posodobljena');
      setTimeout(() => setToast(''), 2500);
    };
    const onDelete = ({ _id }) => {
      setProperties(prev => prev.filter(x => x._id !== _id));
      setToast('Nepremičnina izbrisana');
      setTimeout(() => setToast(''), 2500);
    };
    socket.on('propertyCreated', onCreate);
    socket.on('propertyUpdated', onUpdate);
    socket.on('propertyDeleted', onDelete);
    return () => {
      socket.off('propertyCreated', onCreate);
      socket.off('propertyUpdated', onUpdate);
      socket.off('propertyDeleted', onDelete);
    };
  }, []);

  return (
    <div className="container">
      <div className="page-header">
        <div>
          <h1>Nadzorna plošča</h1>
          <p className="subtitle">Pregled nepremičnin v realnem času.</p>
        </div>
        <span className="stats-pill">
          <strong>{loading ? '…' : properties.length}</strong> {loading ? '' : 'zadetkov'}
        </span>
      </div>

      <PropertyFilters
        filters={filters}
        setFilters={setFilters}
        onReset={resetFilters}
      />

      <div style={{ marginBottom: '1rem' }}>
        <button
          className={`expand-toggle ${chartsOpen ? 'open' : ''}`}
          onClick={() => setChartsOpen(v => !v)}
          aria-expanded={chartsOpen}
        >
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <line x1="18" y1="20" x2="18" y2="10" /><line x1="12" y1="20" x2="12" y2="4" /><line x1="6" y1="20" x2="6" y2="14" />
            </svg>
            <span>Grafi in statistike</span>
            <span className="badge muted">{chartOfferType}</span>
          </span>
          <svg className="chev" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="6 9 12 15 18 9" />
          </svg>
        </button>

        {chartsOpen && (
          <div className="card" style={{ marginTop: '0.5rem' }}>
            <div className="row between wrap" style={{ marginBottom: '1rem', gap: '0.75rem' }}>
              <div className="segmented" role="group" aria-label="Tip ponudbe">
                <button
                  className={chartOfferType === 'Prodaja' ? 'active' : ''}
                  onClick={() => setChartOfferType('Prodaja')}
                >Prodaja</button>
                <button
                  className={chartOfferType === 'Oddaja' ? 'active' : ''}
                  onClick={() => setChartOfferType('Oddaja')}
                >Oddaja</button>
              </div>
              <div className="chip-list">
                {Object.entries(CHART_LABELS).map(([key, label]) => (
                  <button
                    key={key}
                    className={`chip ${visibleCharts[key] ? 'on' : ''}`}
                    onClick={() => setVisibleCharts(v => ({ ...v, [key]: !v[key] }))}
                    aria-pressed={visibleCharts[key]}
                  >
                    {label}
                  </button>
                ))}
              </div>
            </div>
            {loading && properties.length === 0 ? (
              <div className="grid-3">
                <div className="card skeleton" style={{ height: 260 }} />
                <div className="card skeleton" style={{ height: 260 }} />
                <div className="card skeleton" style={{ height: 260 }} />
              </div>
            ) : (
              <PropertyCharts
                properties={chartProperties}
                visible={visibleCharts}
                offerLabel={chartOfferType}
              />
            )}
            <div className="subtle" style={{ fontSize: 12, marginTop: '0.5rem' }}>
              Grafi prikazujejo samo izbrano vrsto ponudbe, ker so cene prodaje in oddaje v različnih redih velikosti.
            </div>
          </div>
        )}
      </div>

      {hasArea && (
        <div className="alert alert-info" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.5rem' }}>
          <span>
            📍 Aktivno območje na zemljevidu ({activeGeoKey === 'bbox' ? 'pravokotnik' : activeGeoKey === 'polygon' ? 'poligon' : 'krog'}) — prikazani so samo zadetki znotraj.
          </span>
          <button className="secondary sm" onClick={handleAreaCleared}>Počisti območje</button>
        </div>
      )}

      <div style={{ padding: 0, overflow: 'hidden', borderRadius: 'var(--radius-lg)', boxShadow: 'var(--shadow-sm)', marginBottom: '1rem' }}>
        <PropertyMap
          properties={properties}
          onAreaSelected={handleAreaSelected}
          onAreaCleared={handleAreaCleared}
          hasArea={hasArea}
        />
      </div>

      <h2>Seznam ({loading ? '…' : properties.length})</h2>
      {loading && properties.length === 0 ? (
        <PropertyGridSkeleton count={6} />
      ) : properties.length === 0 ? (
        <div className="empty-state">
          <div style={{ fontSize: '2rem', marginBottom: '0.5rem' }}>🏚️</div>
          <div style={{ fontWeight: 600, color: 'var(--text)' }}>Ni rezultatov</div>
          <div style={{ fontSize: 14, marginTop: '0.25rem' }}>Poskusi spremeniti filtre ali počistiti območje na zemljevidu.</div>
        </div>
      ) : (
        <div className="grid-3">
          {properties.map(p => {
            const inner = (
              <>
                <div className="pc-image-wrap">
                  {p.imageUrl ? (
                    <img src={p.imageUrl} alt="" loading="lazy" />
                  ) : (
                    <div className="pc-image-placeholder">brez slike</div>
                  )}
                  <div className="pc-badges">
                    <span className="badge">{p.propertyType}</span>
                    <span className="badge muted">{p.offerType}</span>
                    {p.source && <span className="badge warning">{p.source}</span>}
                  </div>
                </div>
                <div className="pc-body">
                  <h3>{p.neighborhood ? `${p.neighborhood}, ${p.city}` : p.city || '—'}</h3>
                  <div className="meta">{p.region}</div>
                  <div className="price">{p.price?.toLocaleString()} €</div>
                  {hasSize(p) && <div className="meta">{p.size} m²</div>}
                  {p.description && <div className="description">{p.description}</div>}
                  <div className="pc-row">
                    {p.propertyLink && <span className="link-hint">Odpri oglas →</span>}
                  </div>
                </div>
              </>
            );
            return p.propertyLink ? (
              <a
                key={p._id}
                href={p.propertyLink}
                target="_blank"
                rel="noopener noreferrer"
                className="property-card"
              >
                {inner}
              </a>
            ) : (
              <div key={p._id} className="property-card">{inner}</div>
            );
          })}
        </div>
      )}

      {toast && <div className="toast">{toast}</div>}
    </div>
  );
}
