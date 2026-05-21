import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, PieChart, Pie, Cell, Legend, CartesianGrid, ScatterChart, Scatter } from 'recharts';

const COLORS = ['#16a34a', '#f59e0b', '#6366f1', '#ec4899', '#06b6d4'];

export default function PropertyCharts({ properties }) {
  const byType = Object.entries(
    properties.reduce((acc, p) => {
      acc[p.propertyType] = (acc[p.propertyType] || 0) + 1;
      return acc;
    }, {})
  ).map(([type, count]) => ({ type, count }));

  const avgPriceByType = Object.entries(
    properties.reduce((acc, p) => {
      if (!acc[p.propertyType]) acc[p.propertyType] = { sum: 0, count: 0 };
      acc[p.propertyType].sum += p.price || 0;
      acc[p.propertyType].count += 1;
      return acc;
    }, {})
  ).map(([type, { sum, count }]) => ({
    type,
    avgPrice: Math.round(sum / count)
  }));

  const scatter = properties
    .filter(p => p.size && p.price)
    .map(p => ({ size: p.size, price: p.price, type: p.propertyType }));

  return (
    <div className="grid-3">
      <div className="card">
        <h3 style={{ marginTop: 0 }}>Število po tipu</h3>
        <ResponsiveContainer width="100%" height={220}>
          <PieChart>
            <Pie data={byType} dataKey="count" nameKey="type" cx="50%" cy="50%" outerRadius={70} label>
              {byType.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
            </Pie>
            <Tooltip />
            <Legend />
          </PieChart>
        </ResponsiveContainer>
      </div>

      <div className="card">
        <h3 style={{ marginTop: 0 }}>Povprečna cena po tipu (€)</h3>
        <ResponsiveContainer width="100%" height={220}>
          <BarChart data={avgPriceByType}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="type" />
            <YAxis />
            <Tooltip formatter={(v) => `${v.toLocaleString()} €`} />
            <Bar dataKey="avgPrice" fill="#2563eb" />
          </BarChart>
        </ResponsiveContainer>
      </div>

      <div className="card">
        <h3 style={{ marginTop: 0 }}>Cena vs. velikost</h3>
        <ResponsiveContainer width="100%" height={220}>
          <ScatterChart>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="size" name="Velikost" unit=" m²" />
            <YAxis dataKey="price" name="Cena" unit=" €" />
            <Tooltip cursor={{ strokeDasharray: '3 3' }} />
            <Scatter data={scatter} fill="#16a34a" />
          </ScatterChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
