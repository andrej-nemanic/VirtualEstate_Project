# Virtual Estate - WebClient (React)

React frontend za VirtualEstate REST API.

## Razvoj

```bash
npm install
npm run dev
```

Aplikacija se zažene na http://localhost:5173

API se pričakuje na http://localhost:3000 (lahko se spremeni z `VITE_API_URL` env spremenljivko).

## Funkcionalnosti

- **Avtentikacija (JWT)** — login, register
- **Nadzorna plošča** — pregled nepremičnin z:
  - interaktivnim zemljevidom (Leaflet)
  - grafi (Recharts): pie chart po tipu, povprečna cena, scatter cena/velikost
  - filtri po tipu, ceni in velikosti
  - realnočasovnim posodabljanjem preko Socket.io
- **Admin vmesnik** — CRUD operacije za nepremičnine in lokacije

## Tehnologije

- React 18 + Vite
- React Router 6
- Axios
- Leaflet + React-Leaflet (zemljevidi)
- Recharts (grafi)
- Socket.io-client (realtime)
