import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet';
import MarkerClusterGroup from 'react-leaflet-cluster';
import L from 'leaflet';
import 'leaflet-draw';
import 'leaflet-draw/dist/leaflet.draw.css';
import { useEffect, useRef } from 'react';
import { useTheme } from '../context/ThemeContext.jsx';
import { hasSize } from '../constants.js';

delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: '/leaflet/marker-icon-2x.png',
  iconUrl: '/leaflet/marker-icon.png',
  shadowUrl: '/leaflet/marker-shadow.png'
});

const KNOWN_TYPE_COLORS = {
  'Stanovanje': '#f59e0b',
  'Hiša': '#16a34a',
  'Vikend': '#10b981',
  'Počitniški objekt': '#10b981',
  'Poslovni prostor': '#0ea5e9',
  'Garaža': '#64748b',
  'Parcela': '#6366f1',
  'Soba': '#ec4899'
};

const FALLBACK_PALETTE = ['#7c3aed', '#0d9488', '#be123c', '#a16207', '#155e75', '#9333ea', '#ca8a04', '#0891b2'];

function colorForType(type) {
  if (!type) return '#2563eb';
  if (KNOWN_TYPE_COLORS[type]) return KNOWN_TYPE_COLORS[type];
  let hash = 0;
  for (let i = 0; i < type.length; i++) hash = (hash * 31 + type.charCodeAt(i)) | 0;
  return FALLBACK_PALETTE[Math.abs(hash) % FALLBACK_PALETTE.length];
}

export function getTypeColor(type) {
  return colorForType(type);
}

function makeIcon(type) {
  const color = colorForType(type);
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

function FitBounds({ properties, enabled }) {
  const map = useMap();
  useEffect(() => {
    if (!enabled) return;
    const coords = properties
      .filter(hasRealCoords)
      .map(p => [p.coordinates.coordinates[1], p.coordinates.coordinates[0]]);
    if (coords.length > 0) {
      map.fitBounds(coords, { padding: [50, 50], maxZoom: 13 });
    }
  }, [properties, map, enabled]);
  return null;
}

function DrawControl({ onAreaSelected, onAreaCleared, hasArea }) {
  const map = useMap();
  const layerRef = useRef(null);
  const selectedRef = useRef(onAreaSelected);
  const clearedRef = useRef(onAreaCleared);

  useEffect(() => { selectedRef.current = onAreaSelected; }, [onAreaSelected]);
  useEffect(() => { clearedRef.current = onAreaCleared; }, [onAreaCleared]);

  useEffect(() => {
    const drawnItems = new L.FeatureGroup();
    map.addLayer(drawnItems);
    layerRef.current = drawnItems;

    const control = new L.Control.Draw({
      position: 'topright',
      draw: {
        rectangle: { shapeOptions: { color: '#2563eb', weight: 2 } },
        polygon: { shapeOptions: { color: '#2563eb', weight: 2 }, allowIntersection: false },
        circle: { shapeOptions: { color: '#2563eb', weight: 2 } },
        marker: false,
        polyline: false,
        circlemarker: false
      },
      edit: { featureGroup: drawnItems, edit: false, remove: true }
    });
    map.addControl(control);

    const onCreated = (e) => {
      drawnItems.clearLayers();
      drawnItems.addLayer(e.layer);

      if (e.layerType === 'rectangle') {
        const b = e.layer.getBounds();
        selectedRef.current?.({
          type: 'bbox',
          value: `${b.getWest()},${b.getSouth()},${b.getEast()},${b.getNorth()}`
        });
      } else if (e.layerType === 'polygon') {
        const latlngs = e.layer.getLatLngs()[0];
        const value = latlngs.map(p => `${p.lng},${p.lat}`).join(';');
        selectedRef.current?.({ type: 'polygon', value });
      } else if (e.layerType === 'circle') {
        const c = e.layer.getLatLng();
        const r = Math.round(e.layer.getRadius());
        selectedRef.current?.({ type: 'near', value: `${c.lng},${c.lat},${r}` });
      }
    };

    const onDeleted = () => clearedRef.current?.();

    const container = map.getContainer();
    const onDrawStart = () => container.classList.add('draw-active');
    const onDrawStop = () => container.classList.remove('draw-active');

    map.on(L.Draw.Event.CREATED, onCreated);
    map.on(L.Draw.Event.DELETED, onDeleted);
    map.on(L.Draw.Event.DRAWSTART, onDrawStart);
    map.on(L.Draw.Event.DRAWSTOP, onDrawStop);

    return () => {
      map.off(L.Draw.Event.CREATED, onCreated);
      map.off(L.Draw.Event.DELETED, onDeleted);
      map.off(L.Draw.Event.DRAWSTART, onDrawStart);
      map.off(L.Draw.Event.DRAWSTOP, onDrawStop);
      container.classList.remove('draw-active');
      map.removeControl(control);
      map.removeLayer(drawnItems);
      layerRef.current = null;
    };
  }, [map]);

  useEffect(() => {
    if (!hasArea && layerRef.current) {
      layerRef.current.clearLayers();
    }
  }, [hasArea]);

  return null;
}

export default function PropertyMap({ properties, onAreaSelected, onAreaCleared, hasArea }) {
  const center = [46.5547, 15.6459];
  const drawable = typeof onAreaSelected === 'function';
  const { theme } = useTheme();
  const tileUrl = theme === 'dark'
    ? 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png'
    : 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';
  const tileAttr = theme === 'dark'
    ? '&copy; OpenStreetMap &copy; CARTO'
    : '&copy; OpenStreetMap';

  return (
    <div className="map-container">
      <MapContainer center={center} zoom={8} style={{ height: '100%', width: '100%' }}>
        <TileLayer key={theme} attribution={tileAttr} url={tileUrl} />
        <FitBounds properties={properties} enabled={!hasArea} />
        {drawable && (
          <DrawControl
            onAreaSelected={onAreaSelected}
            onAreaCleared={onAreaCleared}
            hasArea={hasArea}
          />
        )}
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
                    {hasSize(p) && <>Velikost: {p.size} m²<br /></>}
                    {p.source && <span className="badge" style={{ marginTop: 4 }}>{p.source}</span>}
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
