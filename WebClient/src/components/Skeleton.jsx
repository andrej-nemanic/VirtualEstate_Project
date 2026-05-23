export function Skeleton({ height = 16, width = '100%', radius = 6, style }) {
  return (
    <div
      className="skeleton"
      style={{
        height,
        width,
        borderRadius: radius,
        background: 'linear-gradient(90deg, #e5e7eb 25%, #f3f4f6 50%, #e5e7eb 75%)',
        backgroundSize: '200% 100%',
        animation: 'skeleton-shimmer 1.4s ease-in-out infinite',
        ...style
      }}
    />
  );
}

export function PropertyCardSkeleton() {
  return (
    <div className="property-card" style={{ padding: 16 }}>
      <Skeleton height={160} radius={8} style={{ marginBottom: 12 }} />
      <Skeleton height={14} width="40%" style={{ marginBottom: 8 }} />
      <Skeleton height={22} width="70%" style={{ marginBottom: 8 }} />
      <Skeleton height={14} width="30%" style={{ marginBottom: 4 }} />
      <Skeleton height={20} width="50%" />
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
