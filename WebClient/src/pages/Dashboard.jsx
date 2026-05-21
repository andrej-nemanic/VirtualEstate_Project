import { useEffect, useState } from 'react';
import { propertyApi } from '../api/client.js';
import { socket } from '../api/socket.js';
import PropertyMap from '../components/PropertyMap.jsx';
import PropertyCharts from '../components/PropertyCharts.jsx';
import PropertyFilters from '../components/PropertyFilters.jsx';

export default function Dashboard() {
  const [properties, setProperties] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filters, setFilters] = useState({});
  const [toast, setToast] = useState('');

  const fetchProperties = async () => {
    setLoading(true);
    try {
      const params = {};
      Object.entries(filters).forEach(([k, v]) => { if (v) params[k] = v; });
      const res = await propertyApi.list(params);
      setProperties(res.data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProperties();
  }, [filters]);

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
        onReset={() => setFilters({})}
      />

      {loading ? (
        <div className="card">Nalaganje...</div>
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
