import { useEffect, useMemo, useState } from 'react';
import { propertyApi } from '../api/client.js';
import { socket } from '../api/socket.js';
import { OFFER_TYPES, SOURCE_OPTIONS, hasSize } from '../constants.js';
import ConfirmDialog from '../components/ConfirmDialog.jsx';
import { TableRowSkeleton } from '../components/Skeleton.jsx';

const ADMIN_PAGE_SIZE = 10;

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
  source: 'ročno',
  lng: '',
  lat: ''
};

export default function Admin() {
  const [properties, setProperties] = useState([]);
  const [form, setForm] = useState(emptyProperty);
  const [editingId, setEditingId] = useState(null);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(true);
  const [pendingDelete, setPendingDelete] = useState(null);
  const [formOpen, setFormOpen] = useState(false);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(1);
  const [sortKey, setSortKey] = useState(null);
  const [sortDir, setSortDir] = useState('asc');

  const onSort = (key) => {
    if (sortKey === key) {
      setSortDir(d => d === 'asc' ? 'desc' : 'asc');
    } else {
      setSortKey(key);
      setSortDir('asc');
    }
  };

  const flash = (setter, msg) => {
    setter(msg);
    setTimeout(() => setter(''), 3000);
  };

  const fetchAll = async () => {
    setLoading(true);
    try {
      const res = await propertyApi.list();
      setProperties(res.data);
    } catch (err) {
      flash(setError, 'Napaka pri nalaganju.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchAll(); }, []);

  useEffect(() => {
    const onCreate = (p) => setProperties(prev => prev.some(x => x._id === p._id) ? prev : [p, ...prev]);
    const onUpdate = (p) => setProperties(prev => prev.map(x => x._id === p._id ? p : x));
    const onDelete = ({ _id }) => setProperties(prev => prev.filter(x => x._id !== _id));
    socket.on('propertyCreated', onCreate);
    socket.on('propertyUpdated', onUpdate);
    socket.on('propertyDeleted', onDelete);
    return () => {
      socket.off('propertyCreated', onCreate);
      socket.off('propertyUpdated', onUpdate);
      socket.off('propertyDeleted', onDelete);
    };
  }, []);

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
        imageUrl: form.imageUrl,
        source: form.source
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
      setFormOpen(false);
    } catch (err) {
      flash(setError, err.response?.data?.message || 'Napaka.');
    }
  };

  const confirmRemove = async () => {
    if (!pendingDelete) return;
    try {
      await propertyApi.remove(pendingDelete._id);
      flash(setSuccess, 'Izbrisano.');
    } catch (err) {
      flash(setError, err.response?.data?.message || 'Napaka pri brisanju.');
    } finally {
      setPendingDelete(null);
    }
  };

  const startEdit = (p) => {
    setEditingId(p._id);
    setFormOpen(true);
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
      source: p.source || 'ročno',
      lng: p.coordinates?.coordinates?.[0] ?? '',
      lat: p.coordinates?.coordinates?.[1] ?? ''
    });
    if (typeof window !== 'undefined') window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const cancelEdit = () => {
    setEditingId(null);
    setForm(emptyProperty);
    setFormOpen(false);
  };

  const filteredProperties = useMemo(() => {
    const q = search.trim().toLowerCase();
    const base = !q ? properties : properties.filter(p =>
      (p.region || '').toLowerCase().includes(q) ||
      (p.city || '').toLowerCase().includes(q) ||
      (p.neighborhood || '').toLowerCase().includes(q) ||
      (p.propertyType || '').toLowerCase().includes(q) ||
      (p.source || '').toLowerCase().includes(q)
    );
    if (!sortKey) return base;
    const dir = sortDir === 'asc' ? 1 : -1;
    return [...base].sort((a, b) => {
      const va = a[sortKey];
      const vb = b[sortKey];
      if (typeof va === 'number' && typeof vb === 'number') return (va - vb) * dir;
      const sa = (va ?? '').toString().toLowerCase();
      const sb = (vb ?? '').toString().toLowerCase();
      if (sa < sb) return -1 * dir;
      if (sa > sb) return 1 * dir;
      return 0;
    });
  }, [properties, search, sortKey, sortDir]);

  useEffect(() => { setPage(1); }, [search, sortKey, sortDir]);

  const totalPages = Math.max(1, Math.ceil(filteredProperties.length / ADMIN_PAGE_SIZE));
  const currentPage = Math.min(page, totalPages);
  const pagedProperties = useMemo(
    () => filteredProperties.slice((currentPage - 1) * ADMIN_PAGE_SIZE, currentPage * ADMIN_PAGE_SIZE),
    [filteredProperties, currentPage]
  );

  const sortIndicator = (key) => {
    if (sortKey !== key) return null;
    return (
      <svg className="th-sort-icon" width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        {sortDir === 'asc'
          ? <polyline points="18 15 12 9 6 15" />
          : <polyline points="6 9 12 15 18 9" />}
      </svg>
    );
  };

  const SortableTh = ({ field, label, align = 'left' }) => (
    <th>
      <button
        className="th-sort"
        onClick={() => onSort(field)}
        style={{ justifyContent: align === 'right' ? 'flex-end' : 'flex-start' }}
        aria-sort={sortKey === field ? (sortDir === 'asc' ? 'ascending' : 'descending') : 'none'}
      >
        <span>{label}</span>
        {sortIndicator(field)}
      </button>
    </th>
  );

  return (
    <div className="container">
      <div className="page-header">
        <h1>Upravitelj nepremičnin</h1>
        <span className="stats-pill"><strong>{properties.length}</strong> zapisov</span>
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      {!formOpen ? (
        <button onClick={() => { setFormOpen(true); setEditingId(null); setForm(emptyProperty); }} style={{ marginBottom: '1rem' }}>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
          </svg>
          Nova nepremičnina
        </button>
      ) : (
      <div className="card">
        <div className="card-header">
          <h2>{editingId ? 'Uredi nepremičnino' : 'Nova nepremičnina'}</h2>
          <div className="row" style={{ gap: 8 }}>
            {editingId && <span className="badge warning">#{editingId.slice(-6)}</span>}
            <button className="ghost sm" onClick={cancelEdit} aria-label="Zapri">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            </button>
          </div>
        </div>
        <form onSubmit={submit}>
          <div className="grid-3">
            <div className="form-group">
              <label>Regija</label>
              <input value={form.region} onChange={e => setForm({ ...form, region: e.target.value })} required />
            </div>
            <div className="form-group">
              <label>Mesto / občina</label>
              <input value={form.city} onChange={e => setForm({ ...form, city: e.target.value })} required />
            </div>
            <div className="form-group">
              <label>Naselje <span className="subtle">(opcijsko)</span></label>
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
              <input value={form.propertyType} onChange={e => setForm({ ...form, propertyType: e.target.value })} placeholder="npr. Stanovanje, Hiša…" required />
            </div>
            <div className="form-group">
              <label>Vir podatka</label>
              <input
                value={form.source}
                onChange={e => setForm({ ...form, source: e.target.value })}
                list="source-options"
                placeholder="npr. ročno"
              />
              <datalist id="source-options">
                {SOURCE_OPTIONS.map(s => <option key={s} value={s} />)}
              </datalist>
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
              <label>Geo. dolžina <span className="subtle">(opcijsko)</span></label>
              <input type="number" step="any" value={form.lng} onChange={e => setForm({ ...form, lng: e.target.value })} placeholder="samodejno" />
            </div>
            <div className="form-group">
              <label>Geo. širina <span className="subtle">(opcijsko)</span></label>
              <input type="number" step="any" value={form.lat} onChange={e => setForm({ ...form, lat: e.target.value })} placeholder="samodejno" />
            </div>
            <div className="form-group" style={{ gridColumn: 'span 2' }}>
              <label>Opis</label>
              <input value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} placeholder="Kratek opis nepremičnine…" />
            </div>
            <div className="form-group" style={{ gridColumn: 'span 2' }}>
              <label>Povezava do oglasa <span className="subtle">(URL)</span></label>
              <input value={form.propertyLink} onChange={e => setForm({ ...form, propertyLink: e.target.value })} placeholder="https://…" />
            </div>
            <div className="form-group">
              <label>URL slike</label>
              <input value={form.imageUrl} onChange={e => setForm({ ...form, imageUrl: e.target.value })} placeholder="https://…" />
            </div>
          </div>
          <div className="row end" style={{ marginTop: '0.5rem' }}>
            <button type="button" className="secondary" onClick={cancelEdit}>
              Prekliči
            </button>
            <button type="submit">{editingId ? 'Posodobi' : 'Ustvari'}</button>
          </div>
        </form>
      </div>
      )}

      <div className="card">
        <div className="card-header">
          <h2>Vse nepremičnine</h2>
          <div className="row" style={{ gap: 8, flex: 1, justifyContent: 'flex-end', maxWidth: 400 }}>
            <div className="search-field" style={{ flex: 1 }}>
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
                value={search}
                onChange={e => setSearch(e.target.value)}
                placeholder="Iskanje po mestu, regiji, tipu…"
              />
            </div>
            <span className="badge muted">{filteredProperties.length}</span>
          </div>
        </div>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <SortableTh field="region" label="Regija" />
                <SortableTh field="city" label="Mesto / Naselje" />
                <SortableTh field="offerType" label="Ponudba" />
                <SortableTh field="propertyType" label="Tip" />
                <SortableTh field="source" label="Vir" />
                <SortableTh field="size" label="m²" align="right" />
                <SortableTh field="price" label="Cena" align="right" />
                <th></th>
              </tr>
            </thead>
            <tbody>
              {loading && properties.length === 0 ? (
                <>
                  <TableRowSkeleton columns={8} />
                  <TableRowSkeleton columns={8} />
                  <TableRowSkeleton columns={8} />
                </>
              ) : (
                pagedProperties.map(p => (
                  <tr key={p._id}>
                    <td>{p.region}</td>
                    <td>{p.neighborhood ? `${p.neighborhood}, ${p.city}` : p.city}</td>
                    <td>{p.offerType}</td>
                    <td><span className="badge">{p.propertyType}</span></td>
                    <td className="muted">{p.source || '—'}</td>
                    <td style={{ textAlign: 'right' }} className={hasSize(p) ? '' : 'muted'}>{hasSize(p) ? p.size : '—'}</td>
                    <td style={{ textAlign: 'right', fontWeight: 600 }}>{p.price?.toLocaleString()} €</td>
                    <td>
                      <div className="row end" style={{ gap: 6 }}>
                        <button className="secondary sm" onClick={() => startEdit(p)}>Uredi</button>
                        <button className="danger sm" onClick={() => setPendingDelete(p)}>Briši</button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
              {!loading && filteredProperties.length === 0 && (
                <tr><td colSpan={8} style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '2rem' }}>
                  {search ? `Ni zadetkov za "${search}".` : 'Še ni nobene nepremičnine.'}
                </td></tr>
              )}
            </tbody>
          </table>
        </div>
        {filteredProperties.length > ADMIN_PAGE_SIZE && (
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
            <span className="pagination-info" aria-live="polite">Stran <strong>{currentPage}</strong> od {totalPages}</span>
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
      </div>

      <ConfirmDialog
        open={!!pendingDelete}
        title="Brisanje nepremičnine"
        message={pendingDelete ? `Ali res želiš izbrisati zapis za ${pendingDelete.city}${pendingDelete.neighborhood ? ` (${pendingDelete.neighborhood})` : ''}?` : ''}
        confirmLabel="Izbriši"
        danger
        onConfirm={confirmRemove}
        onCancel={() => setPendingDelete(null)}
      />
    </div>
  );
}
