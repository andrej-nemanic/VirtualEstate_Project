import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, PieChart, Pie, Cell, Legend, CartesianGrid, ScatterChart, Scatter } from 'recharts';
import { getTypeColor } from './PropertyMap.jsx';
import { useTheme } from '../context/ThemeContext.jsx';

function ChartCard({ title, children }) {
  return (
    <div className="card">
      <h3 style={{ marginTop: 0, color: 'var(--text-muted)', fontSize: 13, fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>{title}</h3>
      {children}
    </div>
  );
}

function EmptyArea({ message = 'Ni podatkov.' }) {
  return (
    <div style={{ height: 220, display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text-subtle)', fontSize: 13, fontStyle: 'italic' }}>
      {message}
    </div>
  );
}

const DEFAULT_VISIBLE = { byType: true, avgPrice: true, scatter: true };

export default function PropertyCharts({ properties, visible = DEFAULT_VISIBLE, offerLabel }) {
  const { theme } = useTheme();
  const gridColor = theme === 'dark' ? '#28324a' : '#e5e7eb';
  const textColor = theme === 'dark' ? '#94a3b8' : '#64748b';
  const tooltipBg = theme === 'dark' ? '#131c30' : '#ffffff';
  const tooltipBorder = theme === 'dark' ? '#28324a' : '#e5e7eb';

  const labelColor = theme === 'dark' ? '#f1f5f9' : '#0f172a';
  const itemColor = theme === 'dark' ? '#e2e8f0' : '#0f172a';

  const tooltipStyle = {
    backgroundColor: tooltipBg,
    border: `1px solid ${tooltipBorder}`,
    borderRadius: 8,
    fontSize: 13,
    color: labelColor,
    boxShadow: theme === 'dark' ? '0 12px 24px rgba(0,0,0,0.5)' : '0 8px 16px rgba(15,23,42,0.1)'
  };
  const labelStyle = { color: labelColor, fontWeight: 600 };
  const itemStyle = { color: itemColor };

  const visibleCount = Object.values(visible).filter(Boolean).length;
  if (visibleCount === 0) return null;

  const gridClass = visibleCount === 1 ? '' : visibleCount === 2 ? 'grid-2' : 'grid-3';

  if (!properties || properties.length === 0) {
    return (
      <div className={gridClass}>
        {visible.byType && <ChartCard title={`Število po tipu${offerLabel ? ` — ${offerLabel}` : ''}`}><EmptyArea /></ChartCard>}
        {visible.avgPrice && <ChartCard title={`Povprečna cena po tipu (€)${offerLabel ? ` — ${offerLabel}` : ''}`}><EmptyArea /></ChartCard>}
        {visible.scatter && <ChartCard title={`Cena vs. velikost${offerLabel ? ` — ${offerLabel}` : ''}`}><EmptyArea /></ChartCard>}
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
    .map(([type, { sum, count }]) => ({ type, avgPrice: Math.round(sum / count) }));

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
    <div className={gridClass}>
      {visible.byType && (
        <ChartCard title={`Število po tipu${offerLabel ? ` — ${offerLabel}` : ''}`}>
          <ResponsiveContainer width="100%" height={220}>
            <PieChart>
              <Pie data={byType} dataKey="count" nameKey="type" cx="50%" cy="50%" outerRadius={70} label>
                {byType.map((entry, i) => <Cell key={i} fill={getTypeColor(entry.type)} />)}
              </Pie>
              <Tooltip contentStyle={tooltipStyle} labelStyle={labelStyle} itemStyle={itemStyle} />
              <Legend wrapperStyle={{ fontSize: 12, color: textColor }} />
            </PieChart>
          </ResponsiveContainer>
        </ChartCard>
      )}

      {visible.avgPrice && (
        <ChartCard title={`Povprečna cena po tipu (€)${offerLabel ? ` — ${offerLabel}` : ''}`}>
          {avgPriceByType.length === 0 ? (
            <EmptyArea message="Ni cenovnih podatkov." />
          ) : (
            <ResponsiveContainer width="100%" height={220}>
              <BarChart data={avgPriceByType}>
                <CartesianGrid strokeDasharray="3 3" stroke={gridColor} />
                <XAxis dataKey="type" stroke={textColor} fontSize={12} />
                <YAxis stroke={textColor} fontSize={12} tickFormatter={(v) => v >= 1000 ? `${Math.round(v / 1000)}k` : v} />
                <Tooltip contentStyle={tooltipStyle} labelStyle={labelStyle} itemStyle={itemStyle} formatter={(v) => `${v.toLocaleString()} €`} />
                <Bar dataKey="avgPrice" radius={[6, 6, 0, 0]}>
                  {avgPriceByType.map((entry, i) => <Cell key={i} fill={getTypeColor(entry.type)} />)}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          )}
        </ChartCard>
      )}

      {visible.scatter && (
        <ChartCard title={`Cena vs. velikost${offerLabel ? ` — ${offerLabel}` : ''}`}>
          {scatterSeries.length === 0 ? (
            <EmptyArea message="Ni podatkov s ceno in velikostjo." />
          ) : (
            <ResponsiveContainer width="100%" height={220}>
              <ScatterChart>
                <CartesianGrid strokeDasharray="3 3" stroke={gridColor} />
                <XAxis dataKey="size" name="Velikost" unit=" m²" type="number" stroke={textColor} fontSize={12} />
                <YAxis dataKey="price" name="Cena" unit=" €" type="number" stroke={textColor} fontSize={12} tickFormatter={(v) => v >= 1000 ? `${Math.round(v / 1000)}k` : v} />
                <Tooltip contentStyle={tooltipStyle} labelStyle={labelStyle} itemStyle={itemStyle} cursor={{ strokeDasharray: '3 3' }} formatter={(v) => typeof v === 'number' ? v.toLocaleString() : v} />
                <Legend wrapperStyle={{ fontSize: 12, color: textColor }} />
                {scatterSeries.map(s => (
                  <Scatter key={s.type} name={s.type} data={s.data} fill={getTypeColor(s.type)} />
                ))}
              </ScatterChart>
            </ResponsiveContainer>
          )}
        </ChartCard>
      )}
    </div>
  );
}
