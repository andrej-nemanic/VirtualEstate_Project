export default function PropertyFilters({ filters, setFilters, onReset }) {
  const update = (key, value) => setFilters({ ...filters, [key]: value });

  return (
    <div className="filters">
      <div>
        <label style={{ fontSize: 12, color: '#6b7280' }}>Tip</label>
        <select value={filters.type || ''} onChange={e => update('type', e.target.value)}>
          <option value="">Vsi</option>
          <option value="Stanovanje">Stanovanje</option>
          <option value="Hiša">Hiša</option>
          <option value="Vikend">Vikend</option>
          <option value="Poslovni prostor">Poslovni prostor</option>
          <option value="Garaža">Garaža</option>
          <option value="Zemljišče">Zemljišče</option>
        </select>
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
      <div style={{ gridColumn: 'span 4', display: 'flex', justifyContent: 'flex-end' }}>
        <button className="secondary" onClick={onReset}>Ponastavi filtre</button>
      </div>
    </div>
  );
}
