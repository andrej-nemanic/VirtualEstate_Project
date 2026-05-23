import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { propertyApi } from '../api/client.js';
import { socket } from '../api/socket.js';
import PropertyMap from '../components/PropertyMap.jsx';
import PropertyCharts from '../components/PropertyCharts.jsx';
import PropertyFilters from '../components/PropertyFilters.jsx';
import { PropertyGridSkeleton } from '../components/Skeleton.jsx';

const FILTER_KEYS = ['propertyType', 'offerType', 'region', 'city', 'minPrice', 'maxPrice', 'minSize', 'maxSize', 'description'];

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

export default function Dashboard() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [properties, setProperties] = useState([]);
  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState('');

  const filters = useMemo(() => searchParamsToFilters(searchParams), [searchParams]);

  const setFilters = (next) => {
    setSearchParams(filtersToSearchParams(next), { replace: true });
  };

  const resetFilters = () => setSearchParams({}, { replace: true });

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
      <h1>Nadzorna plošča</h1>
      <p style={{ color: '#6b7280' }}>Pregled nepremičnin v realnem času.</p>

      <PropertyFilters
        filters={filters}
        setFilters={setFilters}
        onReset={resetFilters}
      />

      {loading ? (
        <>
          <div className="grid-3">
            <div className="card" style={{ height: 260 }} />
            <div className="card" style={{ height: 260 }} />
            <div className="card" style={{ height: 260 }} />
          </div>
          <div className="card" style={{ height: 500, marginTop: 16 }} />
          <h2>Seznam</h2>
          <PropertyGridSkeleton count={6} />
        </>
      ) : (
        <>
          <PropertyCharts properties={properties} />

          <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
            <PropertyMap properties={properties} />
          </div>

          <h2>Seznam ({properties.length})</h2>
          <div className="grid-3">
            {properties.map(p => {
              const inner = (
                <>
                  {p.imageUrl && <img src={p.imageUrl} alt="" style={{ width: '100%', height: 160, objectFit: 'cover', borderRadius: 8, marginBottom: 8 }} />}
                  <span className="badge">{p.propertyType}</span> <span className="badge" style={{ marginLeft: 4 }}>{p.offerType}</span>
                  {p.source && <span className="badge" style={{ marginLeft: 4, background: '#fef3c7', color: '#92400e' }}>{p.source}</span>}
                  <h3>{p.neighborhood ? `${p.neighborhood}, ${p.city}` : p.city}</h3>
                  <div className="meta">{p.region}</div>
                  <div className="price">{p.price?.toLocaleString()} €</div>
                  <div className="meta">{p.size} m²</div>
                  {p.description && <div className="meta" style={{ marginTop: 6 }}>{p.description}</div>}
                  {p.propertyLink && <div className="meta" style={{ marginTop: 6 }}>Odpri oglas →</div>}
                </>
              );
              return p.propertyLink ? (
                <a
                  key={p._id}
                  href={p.propertyLink}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="property-card"
                  style={{ color: 'inherit', textDecoration: 'none', display: 'block', cursor: 'pointer' }}
                >
                  {inner}
                </a>
              ) : (
                <div key={p._id} className="property-card">{inner}</div>
              );
            })}
            {properties.length === 0 && <div className="card">Ni rezultatov.</div>}
          </div>
        </>
      )}

      {toast && <div className="toast">{toast}</div>}
    </div>
  );
}
