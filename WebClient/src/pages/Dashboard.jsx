import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { propertyApi } from '../api/client.js';
import { socket } from '../api/socket.js';
import PropertyMap from '../components/PropertyMap.jsx';
import PropertyCharts from '../components/PropertyCharts.jsx';
import PropertyFilters from '../components/PropertyFilters.jsx';
import { PropertyGridSkeleton } from '../components/Skeleton.jsx';
import { hasSize } from '../constants.js';

const PAGE_SIZE = 6;

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
  const [toast, setToast] = useState(null);

  const showToast = (message, type = 'info') => {
    setToast({ message, type });
    const ms = type === 'success' ? 3500 : 2500;
    setTimeout(() => setToast(null), ms);
  };

  const [chartsOpen, setChartsOpen] = useState(false);
  const [chartOfferType, setChartOfferType] = useState('Prodaja');
  const [visibleCharts, setVisibleCharts] = useState({ byType: true, avgPrice: true, scatter: true });
  const [page, setPage] = useState(1);

  const filters = useMemo(() => searchParamsToFilters(searchParams), [searchParams]);

  const setFilters = (next) => {
    setSearchParams(filtersToSearchParams(next), { replace: true });
  };

  const resetFilters = () => setSearchParams({}, { replace: true });

  const hasArea = GEO_KEYS.some(k => filters[k]);

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
    setPage(1);
  }, [searchParams]);

  const totalPages = Math.max(1, Math.ceil(properties.length / PAGE_SIZE));
  const currentPage = Math.min(page, totalPages);
  const pagedProperties = useMemo(
    () => properties.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE),
    [properties, currentPage]
  );

  useEffect(() => {
    const onCreate = (p) => {
      setProperties(prev => [p, ...prev]);
      showToast(`Nova nepremičnina: ${p.propertyType} v ${p.city || ''}`, 'success');
    };
    const onUpdate = (p) => {
      setProperties(prev => prev.map(x => x._id === p._id ? p : x));
      showToast('Nepremičnina posodobljena', 'info');
    };
    const onDelete = ({ _id }) => {
      setProperties(prev => prev.filter(x => x._id !== _id));
      showToast('Nepremičnina izbrisana', 'warning');
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
        <h1>Nepremičnine</h1>
        {loading && properties.length === 0 ? (
          <span className="stats-pill stats-pill-loading">
            <span className="skeleton" style={{ width: 80, height: 14, display: 'inline-block' }} />
          </span>
        ) : (
          <span className="stats-pill">
            <strong>{properties.length}</strong> zadetkov
          </span>
        )}
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
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem' }}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" />
              <circle cx="12" cy="10" r="3" />
            </svg>
            Izbrano območje za prikaz zadetkov.
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

      <h2>Seznam</h2>
      {loading && properties.length === 0 ? (
        <PropertyGridSkeleton count={6} />
      ) : properties.length === 0 ? (
        <div className="empty-state">
          <svg className="empty-state-icon" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <circle cx="11" cy="11" r="7" />
            <line x1="21" y1="21" x2="16.65" y2="16.65" />
            <line x1="8" y1="11" x2="14" y2="11" />
          </svg>
          <div className="empty-state-title">Ni rezultatov</div>
          {(Object.keys(filters).length > 0) && (
            <button className="secondary sm" onClick={resetFilters} style={{ marginTop: '1rem' }}>
              Počisti filtre
            </button>
          )}
        </div>
      ) : (
        <div className="property-grid-2x3">
          {pagedProperties.map(p => {
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
                  {p.propertyLink && (
                    <div className="pc-row">
                      <span className="link-hint">Odpri oglas →</span>
                    </div>
                  )}
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

      {properties.length > PAGE_SIZE && (
        <nav className="pagination" aria-label="Paginacija nepremičnin">
          <button
            className="secondary sm"
            onClick={() => setPage(p => Math.max(1, p - 1))}
            disabled={currentPage === 1}
            aria-label="Prejšnja stran"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <polyline points="15 18 9 12 15 6" />
            </svg>
            Prejšnja
          </button>
          <span className="pagination-info" aria-live="polite">
            Stran <strong>{currentPage}</strong> od {totalPages}
          </span>
          <button
            className="secondary sm"
            onClick={() => setPage(p => Math.min(totalPages, p + 1))}
            disabled={currentPage === totalPages}
            aria-label="Naslednja stran"
          >
            Naslednja
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <polyline points="9 18 15 12 9 6" />
            </svg>
          </button>
        </nav>
      )}

      {toast && (
        <div className={`toast toast-${toast.type}`} role="status" aria-live="polite">
          <svg className="toast-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            {toast.type === 'success' && <polyline points="20 6 9 17 4 12" />}
            {toast.type === 'warning' && (<>
              <path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
              <line x1="12" y1="9" x2="12" y2="13" />
              <line x1="12" y1="17" x2="12.01" y2="17" />
            </>)}
            {toast.type === 'info' && (<>
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="16" x2="12" y2="12" />
              <line x1="12" y1="8" x2="12.01" y2="8" />
            </>)}
          </svg>
          <span>{toast.message}</span>
        </div>
      )}
    </div>
  );
}
