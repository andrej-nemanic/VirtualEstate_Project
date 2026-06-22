# VirtualEstate — digitalni dvojček nepremičninskega trga

**VirtualEstate** je študentski projekt, ki združuje **podatke iz javnih nepremičninskih oglasnikov** (web scraping) in **ročno vnesene podatke** v enotno bazo, jih vizualizira na zemljevidu in v grafih ter omogoča realnočasovno spremljanje sprememb.

> 📖 **Celotna dokumentacija je v [Wikiju projekta](https://github.com/andrej-nemanic/VirtualEstate_Project/wiki).**

## 📚 Dokumentacija (Wiki)

| Sklop | Vsebina |
|---|---|
| [1. Projektne specifikacije](https://github.com/andrej-nemanic/VirtualEstate_Project/wiki/Projektne-specifikacije) | Namen, skupine uporabnikov, opis rešitve, funkcionalne in sistemske zahteve |
| [2. Namestitev in prijava](https://github.com/andrej-nemanic/VirtualEstate_Project/wiki/Namestitev-in-prijava) | Zagon sistema (Docker / ročno) in prva prijava |
| [3. Primeri uporabe](https://github.com/andrej-nemanic/VirtualEstate_Project/wiki/Primeri-uporabe) | Pet ključnih scenarijev z navodili po korakih |
| [4. Izvedene lastnosti](https://github.com/andrej-nemanic/VirtualEstate_Project/wiki/Izvedene-lastnosti) | Podroben opis implementiranih funkcionalnosti |

Za pregled vseh strani glej [kazalo dokumentacije](https://github.com/andrej-nemanic/VirtualEstate_Project/wiki/Documentation) ali [domačo stran Wikija](https://github.com/andrej-nemanic/VirtualEstate_Project/wiki).

## 🧩 Komponente sistema

| Komponenta | Tehnologija | Vloga |
|---|---|---|
| **WebService** | Node.js, Express, MongoDB, Socket.IO | REST API + WebSocket posrednik |
| **WebClient** | React, Vite, Leaflet, Recharts | Spletni vmesnik za pregled in administracijo |
| **DesktopApplication** | Kotlin, Jetpack Compose | Namizna aplikacija za upravljanje baze, scraping in generiranje podatkov |

## 🚀 Hitri zagon (Docker Compose)

```bash
git clone https://github.com/andrej-nemanic/VirtualEstate_Project.git
cd VirtualEstate_Project

# Nastavi okoljske spremenljivke (compose ju naloži prek env_file):
#   WebService/.env  ->  DATABASE_URL, JWT_SECRET
cp WebService/.env.example WebService/.env   # nato uredi vrednosti

docker compose up --build
```

Po zagonu:

- **WebClient**: http://localhost:5173
- **WebService API**: http://localhost:3000

> ⚠️ `docker-compose.yml` zažene le `web-service` in `web-client`. **MongoDB ni vključen** — nastaviti moraš `DATABASE_URL` (lokalni Mongo ali brezplačni MongoDB Atlas). Podrobnosti in ročna namestitev so v [navodilih za namestitev](https://github.com/andrej-nemanic/VirtualEstate_Project/wiki/Namestitev-in-prijava).

## 📁 Struktura repozitorija

```
VirtualEstate_Project/
├── WebService/             # Node.js REST API + WebSocket (Express, MongoDB, Socket.IO)
├── WebClient/              # React SPA (Vite, Leaflet, Recharts)
├── DesktopApplication/     # Kotlin Compose namizna aplikacija (scraping, generator)
├── DomainSpecificLanguage/ # DSL za predmeta Principi PJ / Prevajanje
├── Documentation/          # Poročila in gradiva (PRINCIPI, SPLETNO, SISTEMSKA, PREVAJANJE)
├── docker-compose.yml      # Spletni del sistema v Dockerju
└── README.md
```

## 🔗 Povezave

- **Wiki**: https://github.com/andrej-nemanic/VirtualEstate_Project/wiki
- **Vir Wikija** (samodejna sinhronizacija prek CircleCI): https://github.com/NikSignjarZilavec/VirtualEstate-Wiki
- **Issue tracker**: Jira (SCRUM)
- **Deployment**: Microsoft Azure · containerji na DockerHub

## 👥 Avtorji

- **Nik Signjar Zilavec** — desktop aplikacija, spletni vmesnik, scraping, generator, UI/UX
- **Andrej Nemanič** — spletna storitev, baza, deployment, dokumentacija
