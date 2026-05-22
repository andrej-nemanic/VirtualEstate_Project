import { useEffect, useState } from 'react';
import { propertyApi } from '../api/client.js';

const OFFER_TYPES = ['Prodaja', 'Oddaja'];

const emptyProperty = {
  region: '',
  city: '',
  neighborhood: '',
  offerType: 'Prodaja',
  propertyType: '',
  size: '',
  price: '',
  description: '',
  propertyLink: '',
  imageUrl: '',
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
        region: form.region,
        city: form.city,
        neighborhood: form.neighborhood,
        offerType: form.offerType,
        propertyType: form.propertyType,
        size: Number(form.size),
        price: Number(form.price),
        description: form.description,
        propertyLink: form.propertyLink,
        imageUrl: form.imageUrl
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
      region: p.region || '',
      city: p.city || '',
      neighborhood: p.neighborhood || '',
      offerType: p.offerType || 'Prodaja',
      propertyType: p.propertyType || '',
      size: p.size ?? '',
      price: p.price ?? '',
      description: p.description || '',
      propertyLink: p.propertyLink || '',
      imageUrl: p.imageUrl || '',
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
              <label>Regija</label>
              <input value={form.region} onChange={e => setForm({ ...form, region: e.target.value })} required />
            </div>
            <div className="form-group">
              <label>Mesto / občina</label>
              <input value={form.city} onChange={e => setForm({ ...form, city: e.target.value })} required />
            </div>
            <div className="form-group">
              <label>Naselje (opcijsko)</label>
              <input value={form.neighborhood} onChange={e => setForm({ ...form, neighborhood: e.target.value })} />
            </div>
            <div className="form-group">
              <label>Tip ponudbe</label>
              <select value={form.offerType} onChange={e => setForm({ ...form, offerType: e.target.value })}>
                {OFFER_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
              </select>
            </div>
            <div className="form-group">
              <label>Vrsta nepremičnine</label>
              <input value={form.propertyType} onChange={e => setForm({ ...form, propertyType: e.target.value })} placeholder="npr. Stanovanje, Hiša, Parcela ..." required />
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
              <label>Opis</label>
              <input value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} />
            </div>
            <div className="form-group">
              <label>Povezava do oglasa (URL)</label>
              <input value={form.propertyLink} onChange={e => setForm({ ...form, propertyLink: e.target.value })} />
            </div>
            <div className="form-group">
              <label>URL slike</label>
              <input value={form.imageUrl} onChange={e => setForm({ ...form, imageUrl: e.target.value })} />
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
            <tr><th>Regija</th><th>Mesto / Naselje</th><th>Ponudba</th><th>Tip</th><th>m²</th><th>Cena</th><th></th></tr>
          </thead>
          <tbody>
            {properties.map(p => (
              <tr key={p._id}>
                <td>{p.region}</td>
                <td>{p.neighborhood ? `${p.neighborhood}, ${p.city}` : p.city}</td>
                <td>{p.offerType}</td>
                <td><span className="badge">{p.propertyType}</span></td>
                <td>{p.size} m²</td>
                <td>{p.price?.toLocaleString()} €</td>
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
