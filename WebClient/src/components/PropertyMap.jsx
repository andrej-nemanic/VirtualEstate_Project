import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet';
import MarkerClusterGroup from 'react-leaflet-cluster';
import L from 'leaflet';
import { useEffect } from 'react';

delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png'
});

const typeColors = {
  'Stanovanje': '#f59e0b',
  'Hiša': '#16a34a',
  'Vikend': '#10b981',
  'Počitniški objekt': '#10b981',
  'Poslovni prostor': '#0ea5e9',
  'Garaža': '#64748b',
  'Parcela': '#6366f1',
  'Soba': '#ec4899'
};

function makeIcon(type) {
  const color = typeColors[type] || '#2563eb';
  return L.divIcon({
    className: 'custom-marker',
    html: `<div style="background:${color};width:24px;height:24px;border-radius:50% 50% 50% 0;transform:rotate(-45deg);border:2px solid white;box-shadow:0 2px 4px rgba(0,0,0,0.3);"></div>`,
    iconSize: [24, 24],
    iconAnchor: [12, 24]
  });
}

function hasRealCoords(p) {
  const c = p.coordinates?.coordinates;
  return Array.isArray(c) && c.length === 2 && !(c[0] === 0 && c[1] === 0);
}

function FitBounds({ properties }) {
  const map = useMap();
  useEffect(() => {
    const coords = properties
      .filter(hasRealCoords)
      .map(p => [p.coordinates.coordinates[1], p.coordinates.coordinates[0]]);
    if (coords.length > 0) {
      map.fitBounds(coords, { padding: [50, 50], maxZoom: 13 });
    }
  }, [properties, map]);
  return null;
}

export default function PropertyMap({ properties }) {
  const center = [46.5547, 15.6459];

  return (
    <div className="map-container">
      <MapContainer center={center} zoom={8} style={{ height: '100%', width: '100%' }}>
        <TileLayer
          attribution='&copy; OpenStreetMap'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <FitBounds properties={properties} />
        <MarkerClusterGroup
          chunkedLoading
          showCoverageOnHover={false}
          spiderfyOnMaxZoom={true}
          maxClusterRadius={40}
        >
          {properties.map(p => {
            if (!hasRealCoords(p)) return null;
            const coords = p.coordinates.coordinates;
            return (
              <Marker key={p._id} position={[coords[1], coords[0]]} icon={makeIcon(p.propertyType)}>
                <Popup>
                  <div style={{ minWidth: 200 }}>
                    {p.imageUrl && (
                      <img
                        src={p.imageUrl}
                        alt=""
                        style={{ width: '100%', height: 120, objectFit: 'cover', borderRadius: 4, marginBottom: 6 }}
                      />
                    )}
                    <strong>{p.propertyType} — {p.offerType}</strong><br />
                    {p.neighborhood ? `${p.neighborhood}, ${p.city}` : p.city}
                    {p.region && <> ({p.region})</>}<br />
                    Cena: <b>{p.price?.toLocaleString()} €</b><br />
                    Velikost: {p.size} m²<br />
                    {p.description && <div style={{ marginTop: 4, fontStyle: 'italic', fontSize: 12 }}>{p.description.substring(0, 120)}{p.description.length > 120 ? '…' : ''}</div>}
                    {p.propertyLink && (
                      <a href={p.propertyLink} target="_blank" rel="noopener noreferrer" style={{ display: 'inline-block', marginTop: 6 }}>
                        Odpri oglas →
                      </a>
                    )}
                  </div>
                </Popup>
              </Marker>
            );
          })}
        </MarkerClusterGroup>
      </MapContainer>
    </div>
  );
}
