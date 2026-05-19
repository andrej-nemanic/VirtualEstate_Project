import { useEffect, useState } from 'react';
import { propertyApi } from '../api/client.js';

const emptyProperty = {
  address: '',
  city: '',
  type: 'house',
  size: '',
  price: '',
  buildYear: '',
  description: '',
  lng: '',
  lat: ''
};

export default function Admin() {
  const [properties, setProperties] = useState([]);
  const [form, setForm] = useState(emptyProperty);
  const [editingId, setEditingId] = useState(null);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const flash = (setter, msg) => {
    setter(msg);
    setTimeout(() => setter(''), 3000);
  };

  const fetchAll = async () => {
    try {
      const res = await propertyApi.list();
      setProperties(res.data);
    } catch (err) {
      flash(setError, 'Napaka pri nalaganju.');
    }
  };

  useEffect(() => { fetchAll(); }, []);

  const submit = async (e) => {
    e.preventDefault();
    setError(''); setSuccess('');
    try {
      const data = {
        address: form.address,
        city: form.city,
        type: form.type,
        size: Number(form.size),
        price: Number(form.price),
        buildYear: Number(form.buildYear),
        description: form.description
      };
      if (form.lng !== '' && form.lat !== '') {
        data.lng = parseFloat(form.lng);
        data.lat = parseFloat(form.lat);
      }
      if (editingId) {
        await propertyApi.update(editingId, data);
        flash(setSuccess, 'Nepremičnina posodobljena.');
      } else {
        await propertyApi.create(data);
        flash(setSuccess, 'Nepremičnina ustvarjena.');
      }
      setForm(emptyProperty);
      setEditingId(null);
      fetchAll();
    } catch (err) {
      flash(setError, err.response?.data?.message || 'Napaka.');
    }
  };

  const remove = async (id) => {
    if (!confirm('Izbrišem nepremičnino?')) return;
    try {
      await propertyApi.remove(id);
      flash(setSuccess, 'Izbrisano.');
      fetchAll();
    } catch (err) {
      flash(setError, 'Napaka pri brisanju.');
    }
  };

  const startEdit = (p) => {
    setEditingId(p._id);
    setForm({
      address: p.address || '',
      city: p.city || '',
      type: p.type || 'house',
      size: p.size ?? '',
      price: p.price ?? '',
      buildYear: p.buildYear ?? '',
      description: p.description || '',
      lng: p.coordinates?.coordinates?.[0] ?? '',
      lat: p.coordinates?.coordinates?.[1] ?? ''
    });
  };

  return (
    <div className="container">
      <h1>Admin vmesnik</h1>

      {error && <div className="alert alert-error">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      <div className="card">
        <h2>{editingId ? 'Uredi nepremičnino' : 'Nova nepremičnina'}</h2>
        <form onSubmit={submit}>
          <div className="grid-2">
            <div className="form-group">
              <label>Naslov</label>
              <input value={form.address} onChange={e => setForm({ ...form, address: e.target.value })} required />
            </div>
            <div className="form-group">
              <label>Mesto</label>
              <input value={form.city} onChange={e => setForm({ ...form, city: e.target.value })} required />
            </div>
            <div className="form-group">
              <label>Tip</label>
              <select value={form.type} onChange={e => setForm({ ...form, type: e.target.value })}>
                <option value="house">Hiša</option>
                <option value="apartment">Stanovanje</option>
                <option value="land">Zemljišče</option>
                <option value="condominium">Kondominij</option>
              </select>
            </div>
            <div className="form-group">
              <label>Velikost (m²)</label>
              <input type="number" value={form.size} onChange={e => setForm({ ...form, size: e.target.value })} required />
            </div>
            <div className="form-group">
              <label>Cena (€)</label>
              <input type="number" value={form.price} onChange={e => setForm({ ...form, price: e.target.value })} required />
            </div>
            <div className="form-group">
              <label>Leto izgradnje</label>
              <input type="number" value={form.buildYear} onChange={e => setForm({ ...form, buildYear: e.target.value })} />
            </div>
            <div className="form-group">
              <label>Opis</label>
              <input value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} />
            </div>
            <div className="form-group">
              <label>Geo. dolžina (lng) — opcijsko</label>
              <input type="number" step="any" value={form.lng} onChange={e => setForm({ ...form, lng: e.target.value })} placeholder="prazno → samodejno geokodiranje" />
            </div>
            <div className="form-group">
              <label>Geo. širina (lat) — opcijsko</label>
              <input type="number" step="any" value={form.lat} onChange={e => setForm({ ...form, lat: e.target.value })} placeholder="prazno → samodejno geokodiranje" />
            </div>
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            <button type="submit">{editingId ? 'Posodobi' : 'Ustvari'}</button>
            {editingId && (
              <button type="button" className="secondary" onClick={() => { setEditingId(null); setForm(emptyProperty); }}>
                Prekliči
              </button>
            )}
          </div>
        </form>
      </div>

      <div className="card">
        <h2>Vse nepremičnine ({properties.length})</h2>
        <table>
          <thead>
            <tr><th>Naslov</th><th>Mesto</th><th>Tip</th><th>Velikost</th><th>Cena</th><th>Leto</th><th>Koordinate</th><th></th></tr>
          </thead>
          <tbody>
            {properties.map(p => (
              <tr key={p._id}>
                <td>{p.address}</td>
                <td>{p.city}</td>
                <td><span className={`badge ${p.type}`}>{p.type}</span></td>
                <td>{p.size} m²</td>
                <td>{p.price?.toLocaleString()} €</td>
                <td>{p.buildYear}</td>
                <td>{p.coordinates?.coordinates?.join(', ')}</td>
                <td>
                  <button onClick={() => startEdit(p)} style={{ marginRight: 6 }}>Uredi</button>
                  <button className="danger" onClick={() => remove(p._id)}>Briši</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
