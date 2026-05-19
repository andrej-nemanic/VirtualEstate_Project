import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet';
import L from 'leaflet';
import { useEffect } from 'react';

delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png'
});

const typeColors = {
  house: '#16a34a',
  apartment: '#f59e0b',
  land: '#6366f1',
  condominium: '#ec4899'
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

function FitBounds({ properties }) {
  const map = useMap();
  useEffect(() => {
    const coords = properties
      .filter(p => p.location?.location?.coordinates)
      .map(p => [p.location.location.coordinates[1], p.location.location.coordinates[0]]);
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
        {properties.map(p => {
          const coords = p.location?.location?.coordinates;
          if (!coords) return null;
          return (
            <Marker key={p._id} position={[coords[1], coords[0]]} icon={makeIcon(p.type)}>
              <Popup>
                <strong>{p.type.toUpperCase()}</strong><br />
                {p.location?.address}, {p.location?.city}<br />
                Cena: <b>{p.price?.toLocaleString()} €</b><br />
                Velikost: {p.size} m²<br />
                Leto: {p.buildYear}<br />
                {p.description && <em>{p.description}</em>}
              </Popup>
            </Marker>
          );
        })}
      </MapContainer>
    </div>
  );
}
