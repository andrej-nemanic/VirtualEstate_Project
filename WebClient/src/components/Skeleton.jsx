export function Skeleton({ height = 16, width = '100%', radius = 6, style }) {
  return (
    <div
      className="skeleton"
      style={{
        height,
        width,
        borderRadius: radius,
        ...style
      }}
    />
  );
}

export function PropertyCardSkeleton() {
  return (
    <div className="property-card" style={{ cursor: 'default' }}>
      <Skeleton height={180} radius={0} />
      <div style={{ padding: '1rem 1.1rem 1.1rem', display: 'flex', flexDirection: 'column', gap: 8 }}>
        <Skeleton height={14} width="35%" />
        <Skeleton height={20} width="75%" />
        <Skeleton height={14} width="30%" />
        <Skeleton height={22} width="50%" style={{ marginTop: 6 }} />
      </div>
    </div>
  );
}

export function PropertyGridSkeleton({ count = 6 }) {
  return (
    <div className="grid-3">
      {Array.from({ length: count }).map((_, i) => <PropertyCardSkeleton key={i} />)}
    </div>
  );
}

export function TableRowSkeleton({ columns = 5 }) {
  return (
    <tr>
      {Array.from({ length: columns }).map((_, i) => (
        <td key={i}><Skeleton height={14} /></td>
      ))}
    </tr>
  );
}
