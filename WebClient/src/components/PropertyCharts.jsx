import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, PieChart, Pie, Cell, Legend, CartesianGrid, ScatterChart, Scatter } from 'recharts';
import { getTypeColor } from './PropertyMap.jsx';

function EmptyChart({ title, message = 'Ni podatkov za prikaz.' }) {
  return (
    <div className="card">
      <h3 style={{ marginTop: 0 }}>{title}</h3>
      <div style={{
        height: 220, display: 'flex', alignItems: 'center', justifyContent: 'center',
        color: '#9ca3af', fontSize: 14, fontStyle: 'italic'
      }}>
        {message}
      </div>
    </div>
  );
}

export default function PropertyCharts({ properties }) {
  if (!properties || properties.length === 0) {
    return (
      <div className="grid-3">
        <EmptyChart title="Število po tipu" />
        <EmptyChart title="Povprečna cena po tipu (€)" />
        <EmptyChart title="Cena vs. velikost" />
      </div>
    );
  }

  const byType = Object.entries(
    properties.reduce((acc, p) => {
      const key = p.propertyType || 'neznano';
      acc[key] = (acc[key] || 0) + 1;
      return acc;
    }, {})
  ).map(([type, count]) => ({ type, count }));

  const avgPriceByType = Object.entries(
    properties.reduce((acc, p) => {
      const key = p.propertyType || 'neznano';
      if (!acc[key]) acc[key] = { sum: 0, count: 0 };
      if (typeof p.price === 'number') {
        acc[key].sum += p.price;
        acc[key].count += 1;
      }
      return acc;
    }, {})
  )
    .filter(([, { count }]) => count > 0)
    .map(([type, { sum, count }]) => ({
      type,
      avgPrice: Math.round(sum / count)
    }));

  const scatterByType = properties
    .filter(p => typeof p.size === 'number' && typeof p.price === 'number' && p.size > 0 && p.price > 0)
    .reduce((acc, p) => {
      const key = p.propertyType || 'neznano';
      if (!acc[key]) acc[key] = [];
      acc[key].push({ size: p.size, price: p.price });
      return acc;
    }, {});
  const scatterSeries = Object.entries(scatterByType).map(([type, data]) => ({ type, data }));

  return (
    <div className="grid-3">
      <div className="card">
        <h3 style={{ marginTop: 0 }}>Število po tipu</h3>
        <ResponsiveContainer width="100%" height={220}>
          <PieChart>
            <Pie data={byType} dataKey="count" nameKey="type" cx="50%" cy="50%" outerRadius={70} label>
              {byType.map((entry, i) => <Cell key={i} fill={getTypeColor(entry.type)} />)}
            </Pie>
            <Tooltip />
            <Legend />
          </PieChart>
        </ResponsiveContainer>
      </div>

      <div className="card">
        <h3 style={{ marginTop: 0 }}>Povprečna cena po tipu (€)</h3>
        {avgPriceByType.length === 0 ? (
          <div style={{ height: 220, display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#9ca3af', fontStyle: 'italic' }}>
            Ni cenovnih podatkov.
          </div>
        ) : (
          <ResponsiveContainer width="100%" height={220}>
            <BarChart data={avgPriceByType}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="type" />
              <YAxis />
              <Tooltip formatter={(v) => `${v.toLocaleString()} €`} />
              <Bar dataKey="avgPrice">
                {avgPriceByType.map((entry, i) => <Cell key={i} fill={getTypeColor(entry.type)} />)}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        )}
      </div>

      <div className="card">
        <h3 style={{ marginTop: 0 }}>Cena vs. velikost</h3>
        {scatterSeries.length === 0 ? (
          <div style={{ height: 220, display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#9ca3af', fontStyle: 'italic' }}>
            Ni podatkov s ceno in velikostjo.
          </div>
        ) : (
          <ResponsiveContainer width="100%" height={220}>
            <ScatterChart>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="size" name="Velikost" unit=" m²" type="number" />
              <YAxis dataKey="price" name="Cena" unit=" €" type="number" />
              <Tooltip cursor={{ strokeDasharray: '3 3' }} formatter={(v) => typeof v === 'number' ? v.toLocaleString() : v} />
              <Legend />
              {scatterSeries.map(s => (
                <Scatter key={s.type} name={s.type} data={s.data} fill={getTypeColor(s.type)} />
              ))}
            </ScatterChart>
          </ResponsiveContainer>
        )}
      </div>
    </div>
  );
}
