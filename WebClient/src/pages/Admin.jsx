import { useEffect, useState } from 'react';
import { propertyApi, locationApi } from '../api/client.js';

const emptyProperty = {
  type: 'house', size: '', price: '', buildYear: '', description: '', location: ''
};
const emptyLocation = { address: '', city: '', lng: '', lat: '' };

export default function Admin() {
  const [tab, setTab] = useState('properties');
  const [properties, setProperties] = useState([]);
  const [locations, setLocations] = useState([]);
  const [propForm, setPropForm] = useState(emptyProperty);
  const [locForm, setLocForm] = useState(emptyLocation);
  const [editingProp, setEditingProp] = useState(null);
  const [editingLoc, setEditingLoc] = useState(null);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const flash = (setter, msg) => {
    setter(msg);
    setTimeout(() => setter(''), 3000);
  };

  const fetchAll = async () => {
    try {
      const [p, l] = await Promise.all([propertyApi.list(), locationApi.list()]);
      setProperties(p.data);
      setLocations(l.data);
    } catch (err) {
      flash(setError, 'Napaka pri nalaganju.');
    }
  };

  useEffect(() => { fetchAll(); }, []);

  const submitProperty = async (e) => {
    e.preventDefault();
    setError(''); setSuccess('');
    try {
      const data = {
        ...propForm,
        size: Number(propForm.size),
        price: Number(propForm.price),
        buildYear: Number(propForm.buildYear)
      };
      if (editingProp) {
        await propertyApi.update(editingProp, data);
        flash(setSuccess, 'Nepremičnina posodobljena.');
      } else {
        await propertyApi.create(data);
        flash(setSuccess, 'Nepremičnina ustvarjena.');
      }
      setPropForm(emptyProperty);
      setEditingProp(null);
      fetchAll();
    } catch (err) {
      flash(setError, err.response?.data?.message || 'Napaka.');
    }
  };

  const submitLocation = async (e) => {
    e.preventDefault();
    setError(''); setSuccess('');
    try {
      const data = {
        address: locForm.address,
        city: locForm.city,
        coordinates: [parseFloat(locForm.lng), parseFloat(locForm.lat)]
      };
      if (editingLoc) {
        await locationApi.update(editingLoc, data);
        flash(setSuccess, 'Lokacija posodobljena.');
      } else {
        await locationApi.create(data);
        flash(setSuccess, 'Lokacija ustvarjena.');
      }
      setLocForm(emptyLocation);
      setEditingLoc(null);
      fetchAll();
    } catch (err) {
      flash(setError, err.response?.data?.message || 'Napaka.');
    }
  };

  const deleteProperty = async (id) => {
    if (!confirm('Izbrišem nepremičnino?')) return;
    try {
      await propertyApi.remove(id);
      flash(setSuccess, 'Izbrisano.');
      fetchAll();
    } catch (err) {
      flash(setError, 'Napaka pri brisanju.');
    }
  };

  const deleteLocation = async (id) => {
    if (!confirm('Izbrišem lokacijo?')) return;
    try {
      await locationApi.remove(id);
      flash(setSuccess, 'Izbrisano.');
      fetchAll();
    } catch (err) {
      flash(setError, 'Napaka pri brisanju.');
    }
  };

  const startEditProp = (p) => {
    setEditingProp(p._id);
    setPropForm({
      type: p.type,
      size: p.size,
      price: p.price,
      buildYear: p.buildYear,
      description: p.description || '',
      location: p.location?._id || p.location || ''
    });
  };

  const startEditLoc = (l) => {
    setEditingLoc(l._id);
    setLocForm({
      address: l.address || '',
      city: l.city || '',
      lng: l.location?.coordinates?.[0] || '',
      lat: l.location?.coordinates?.[1] || ''
    });
  };

  return (
    <div className="container">
      <h1>Admin vmesnik</h1>

      <div style={{ display: 'flex', gap: 10, marginBottom: 16 }}>
        <button className={tab === 'properties' ? '' : 'secondary'} onClick={() => setTab('properties')}>
          Nepremičnine
        </button>
        <button className={tab === 'locations' ? '' : 'secondary'} onClick={() => setTab('locations')}>
          Lokacije
        </button>
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      {tab === 'properties' && (
        <>
          <div className="card">
            <h2>{editingProp ? 'Uredi nepremičnino' : 'Nova nepremičnina'}</h2>
            <form onSubmit={submitProperty}>
              <div className="grid-2">
                <div className="form-group">
                  <label>Tip</label>
                  <select value={propForm.type} onChange={e => setPropForm({ ...propForm, type: e.target.value })}>
                    <option value="house">Hiša</option>
                    <option value="apartment">Stanovanje</option>
                    <option value="land">Zemljišče</option>
                    <option value="condominium">Kondominij</option>
                  </select>
                </div>
                <div className="form-group">
                  <label>Lokacija</label>
                  <select value={propForm.location} onChange={e => setPropForm({ ...propForm, location: e.target.value })} required>
                    <option value="">-- Izberi lokacijo --</option>
                    {locations.map(l => (
                      <option key={l._id} value={l._id}>{l.address}, {l.city}</option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label>Velikost (m²)</label>
                  <input type="number" value={propForm.size} onChange={e => setPropForm({ ...propForm, size: e.target.value })} required />
                </div>
                <div className="form-group">
                  <label>Cena (€)</label>
                  <input type="number" value={propForm.price} onChange={e => setPropForm({ ...propForm, price: e.target.value })} required />
                </div>
                <div className="form-group">
                  <label>Leto izgradnje</label>
                  <input type="number" value={propForm.buildYear} onChange={e => setPropForm({ ...propForm, buildYear: e.target.value })} />
                </div>
                <div className="form-group">
                  <label>Opis</label>
                  <input value={propForm.description} onChange={e => setPropForm({ ...propForm, description: e.target.value })} />
                </div>
              </div>
              <div style={{ display: 'flex', gap: 8 }}>
                <button type="submit">{editingProp ? 'Posodobi' : 'Ustvari'}</button>
                {editingProp && (
                  <button type="button" className="secondary" onClick={() => { setEditingProp(null); setPropForm(emptyProperty); }}>
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
                <tr><th>Tip</th><th>Lokacija</th><th>Velikost</th><th>Cena</th><th>Leto</th><th></th></tr>
              </thead>
              <tbody>
                {properties.map(p => (
                  <tr key={p._id}>
                    <td><span className={`badge ${p.type}`}>{p.type}</span></td>
                    <td>{p.location?.address}, {p.location?.city}</td>
                    <td>{p.size} m²</td>
                    <td>{p.price?.toLocaleString()} €</td>
                    <td>{p.buildYear}</td>
                    <td>
                      <button onClick={() => startEditProp(p)} style={{ marginRight: 6 }}>Uredi</button>
                      <button className="danger" onClick={() => deleteProperty(p._id)}>Briši</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}

      {tab === 'locations' && (
        <>
          <div className="card">
            <h2>{editingLoc ? 'Uredi lokacijo' : 'Nova lokacija'}</h2>
            <form onSubmit={submitLocation}>
              <div className="grid-2">
                <div className="form-group">
                  <label>Naslov</label>
                  <input value={locForm.address} onChange={e => setLocForm({ ...locForm, address: e.target.value })} required />
                </div>
                <div className="form-group">
                  <label>Mesto</label>
                  <input value={locForm.city} onChange={e => setLocForm({ ...locForm, city: e.target.value })} required />
                </div>
                <div className="form-group">
                  <label>Geo. dolžina (lng)</label>
                  <input type="number" step="any" value={locForm.lng} onChange={e => setLocForm({ ...locForm, lng: e.target.value })} required />
                </div>
                <div className="form-group">
                  <label>Geo. širina (lat)</label>
                  <input type="number" step="any" value={locForm.lat} onChange={e => setLocForm({ ...locForm, lat: e.target.value })} required />
                </div>
              </div>
              <div style={{ display: 'flex', gap: 8 }}>
                <button type="submit">{editingLoc ? 'Posodobi' : 'Ustvari'}</button>
                {editingLoc && (
                  <button type="button" className="secondary" onClick={() => { setEditingLoc(null); setLocForm(emptyLocation); }}>
                    Prekliči
                  </button>
                )}
              </div>
            </form>
          </div>

          <div className="card">
            <h2>Vse lokacije ({locations.length})</h2>
            <table>
              <thead>
                <tr><th>Naslov</th><th>Mesto</th><th>Koordinate (lng, lat)</th><th></th></tr>
              </thead>
              <tbody>
                {locations.map(l => (
                  <tr key={l._id}>
                    <td>{l.address}</td>
                    <td>{l.city}</td>
                    <td>{l.location?.coordinates?.join(', ')}</td>
                    <td>
                      <button onClick={() => startEditLoc(l)} style={{ marginRight: 6 }}>Uredi</button>
                      <button className="danger" onClick={() => deleteLocation(l._id)}>Briši</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  );
}
