# Projektno poročilo: Vzpostavitev infrastrukture in namestitev aplikacije v okolju Azure z Dockerjem

**Predmet:** Sistemska administracija
**Skupina:** VirtualEstate
**Člani skupine:** Andrej Nemanič, Nik Šignjar Žilavec, David Bogdan
**Vodja skupine:** Andrej Nemanič

---

## 1. Upravljanje projekta in vodenje sprinta

Za uspešno izvedbo projekta in sledenje opravilom je bilo uporabljeno orodje **Jira**. Tule so prikazani sklopi, narejeni znotraj Jire:
1. Lokalna dockerizacija aplikacije.
2. Kreiranje Azure računa
3. Konfiguracija Azure računa.
4. Odgovori na vprašanja za Azure Portal
5. Vzpostavitev virtualne naprave
6. SSH dostop
7. Zagon Docker aplikacije na virtualni napravi

![Vpogled sklope, ki se tičejo Azure, v Jiri](./images/imagesPresentation/2026-05-23_06-03.png)

## 2. Lokalni razvoj in Dockerizacija aplikacije

Aplikacija je bila razvita s pomočjo tehnologije Node.js, React in Vite ter uporablja zunanjo podatkovno bazo MongoDB, s čimer je bilo zagotovljeno, da se podatki ne shranjujejo lokalno znotraj istega vsebnika brez obstojnosti. Kasneje je bil za virtualno napravo uporabljena podatkovna baza MongoDB Atlas.

![Slika glavnega repozitorija v GitHubu](./images/imagesPresentation/Screenshot_2026-05-22_181532.png)


### Git repozitorij
Za razvoj je bil uporabljen **GitHub**. V repozitorij so bile vključene izvorna koda in, ker je spletna aplikacija razdeljena na dva dela, to sta spletna storitev in spletni vmesnik, sta bili za Docker dodani dve Dockerfile datoteki za vsakega od obeh delov ter zraven še docker compose datoteka.

![Slika glavnega repozitorija v GitHubu](./images/imagesPresentation/2026-05-23_06-21_2.png)

### Dockerfile konfiguracija
Tukaj so prikazane skripte Dockerfile in docker compose datotek.
#### Spletna storitev (WebService)

```dockerfile
# Nastavitev osnovnega okolja (Node 18)
FROM node:18-alpine
# Nastavitev delovnega imenika
WORKDIR /app
# Kopiranje odvisnosti in namestitev
COPY package*.json ./
# Namestitev vseh potrebnih paketov in knjižnic
RUN npm install --production
# Kopiranje preostale izvorne kode
COPY . .
# Izpostavitev vrat (porta)
EXPOSE 3000
# Ukaz za zagon spletne storitve
CMD ["npm", "start"]
```
#### Spletni vmesnik (WebClient)
```dockerfile
# Nastavitev osnovnega okolja (Node 18)
FROM node:18-alpine

# Nastavitev delovnega imenika
WORKDIR /app

# Kopiranje odvisnosti in namestitev
COPY package*.json ./
# Namestitev vseh potrebnih paketov in knjižnic
RUN npm install

# Kopiranje preostale izvorne kode
COPY . .
# Izpostavitev vrat (porta)
EXPOSE 5173
# Ukaz za zagon spletnega vmesnika
CMD ["npm", "run", "dev", "--", "--host"]
```
#### Docker compose datoteka (docker-compose.yml)
```yaml
services:
  web-service:
    build: ./WebService
    ports:
    # Vrata spletne storitve
      - "3000:3000"
    environment:
    # Pot do podatkovne baze (MongoDB Atlas)
      - DATABASE_URL=mongodb+srv://niksignjar_db_user:2lLWNURcOZyYsJKA@virtualestate.ha8dxw2.mongodb.net/VirtualEstate?retryWrites=true&w=majority&appName=VirtualEstate
    restart: always
    networks:
      - app-network

  web-client:
    build: ./WebClient
    ports:
    # Vrata spletnega vmesnika
      - "5173:5173"
    restart: always
    depends_on:
      - web-service
    networks:
      - app-network

networks:
  app-network:
    driver: bridge
```
#### Lokalni zagon aplikacije z dockerjem
Aplikacijo smo v glavnem direktoriju lokalno uspešno zgradili in zagnali z ukazom:
Bash
```sh
docker compose up -d --build
```

![Slika zagona aplikacije z Dockerjem](./images/imagesPresentation/2026-05-23_06-35.png)
![Slika aplikacije, zagnane lokalno](./images/imagesPresentation/2026-05-23_06-38.png)
![Slika nadzorne plošče aplikacije, zagnane lokalno](./images/imagesPresentation/2026-05-23_07-32.png)

## 3. Vzpostavitev navidezne naprave (VM) na Azure

S študentskim elektronskim naslovom se je ustvaril brezplačen račun Azure for Students (brez vnosa plačilnih kartic), ki vključuje 100 € dobroimetja in brezplačne storitve.

Znotraj portala je bila ustvarjena nova virtualna naprava z naslednjimi specifikacijami:
- Subscription: Azure for Students
- Resource group: VirtualEstate
- Virtual machine name: VirtualEstateMachine
- Region: Austria East
- Image: Ubuntu Server 22.04 LTS - x64 Gen2
- Size: imeli težave s pomanjkanjem strežnikov z velikostjo, zapisano v nalogi, izbrali alternativo (Standard B2ats v2 (2 vcpus, 1 GiB memory))
- Authentication type: Password
- Public inbound ports: SSH (22)

![Obrazec za kreiranje nove Virtualne naprave 1](./images/imagesPresentation/Screenshot_2026-05-22_145553.png)
![Obrazec za kreiranje nove Virtualne naprave 2](./images/imagesPresentation/Screenshot_2026-05-22_145558.png)
![Celoten pregled ustvarjene virtualne naprave](./images/imagesPresentation/Screenshot_2026-05-22_150750.png)
## 4. SSH dostop za vse člane skupine
Za dostop do virtualne naprave uporabljamo SSH protokola preko privzetih vrat (22). Ob prvi povezavi na oddaljeni strežnik preko terminala nas sistem lahko opozori na preverjanje prstnega odtisa ključa (ECDSA/ED25519 fingerprint) za potrditev identitete strežnika.

Povezavo vzpostavimo z ukazom:
```sh
ssh VirtualEstate@68.210.138.100
```
Po potrditvi s yes in vnosu poverilnic se uspešno prijavimo v sejo oddaljenega uporabnika VirtualEstate@VirtualEstateMachine.

![Dostop do virtualne naprave](./images/imagesPresentation/image5.png)

## 5. Analiza portala Azure
### Vprašanje 1: Kje in kako omogočite "port forwarding" (posredovanje vrat)?
V okolju Azure se posredovanje vrat in odpiranje omrežnega prometa izvaja preko Network Security Group (NSG), ki deluje kot požarni zid za virtualno napravo.
Postopek:
1. V stranskem meniju virtualne naprave izberemo Network settings pod zavihkom Networking.
2. Kliknemo na gumb Create port rule -> Inbound port rule.
3. Nastavimo:
    - Source: Any
    - Source port ranges: *
    - Destination: Any
    - Service: Custom
    - Destination port ranges: [Vpišemo vrata naše aplikacije, npr. 3000]
    - Protocol: TCP
    - Action: Allow
4. Kliknemo Save.
![Prvi korak](./images/imagesAzure/image4.png)
![Drugi korak](./images/imagesAzure/image3.png)
![Tretji in četrti korak](./images/imagesAzure/image2.png)
### Vprašanje 2: Kakšen tip diska je bil dodan vaši navidezni napravi in kakšna je njegova kapaciteta?
Podatke o disku najdemo, če v meniju virtualne naprave kliknemo na zavihek Disks.
- Tip diska: Premium SSD LRS.
- Kapaciteta: 30 GiB.
![Prikaz tipa in velikosti OS diska](./images/imagesAzure/Screenshot_2026-05-22_145455.png)

### Vprašanje 3: Kje preverimo stanje trenutne porabe virov v naši naročnini ("Azure for students")? Namig: stanje porabe bo vidno komaj 24ur po vpostavitvi.
Na Azure portalu v iskalno polje vpišemo Cost Management + Billing, nato na Cost Management v levem meniju. Tam nato pod Reporting + analytics zavihkom kliknem na Cost Analysis, in nato na Accumulated costs vidimo grafe porabe.

![Prikaz grafa porabe in preostalega dobroimetja na Azure Sponsorship portalu](./images/imagesPresentation/image.png)

## 6. Namestitev in zagon aplikacije na Azure VM
- **Korak 1: Namestitev orodja Git in okolja Docker**

Najprej smo posodobili pakete in zagotovili prisotnost sistema Git:
```sh
sudo apt install -y git
```
Nato smo uporabili uradno Docker skripto za avtomatizirano namestitev najnovejše produkcijske različice Docker okolja:

```sh
curl -fsSL [https://get.docker.com](https://get.docker.com) | sudo sh
```
![Namestitev Docker okolja](./images/imagesPresentation/Screenshot_2026-05-22_180409.png)

Po uspešni namestitvi smo preverili različici obeh komponent z ukazoma:
```sh
docker --version
docker compose version
```
![Verzija Dockerja in docker compose](./images/imagesPresentation/Screenshot_2026-05-22_180501.png)

- **Korak 2: Klonsko kopiranje repozitorija in postavitev okolja**

Preko Git protokola smo prenesli celotno izvorno kodo projekta na strežnik, se postavili v mapo projekta ter preklopili na delovno vejo develop, kjer se nahajajo zadnje stabilne spremembe:
```sh
git clone [https://github.com/andrej-nemanic/VirtualEstate_Project](https://github.com/andrej-nemanic/VirtualEstate_Project)
cd VirtualEstate_Project
git checkout develop
git pull
```

Pregled vsebine mape (ls) na zaslonski sliki potrjuje uspešen prenos celotne arhitekture aplikacije (Dockerfile, docker-compose.yml, WebService, WebClient, DesktopApplication):

![Prenos kode preko Git-a in pregled strukture projekta](./images/imagesPresentation/Screenshot_2026-05-22_181220.png)

- **Korak 3: Zagon vsebnika**

Aplikacijo in pripadajoče servise zaženemo v ozadju z ukazom:
```sh
docker compose up -d --build
```

### Dodajanje uporabnika v docker skupino, da ne potrebujemo 'sudo' za vsak ukaz
```sh
sudo usermod -aG docker $USER
```
(Po tem koraku se je bilo potrebno ponovno odjaviti in prijaviti preko SSH, da so spremembe skupine stopile v veljavo)
## 7. Zaključek

Aplikacija se izvaja znotraj Docker vsebnika na oddaljenem Azure strežniku, podatke pa uspešno zapisuje in bere iz zunanje podatkovne baze.

Dostopna na http://68.210.138.100:5173

![Dostop do spletne strani preko IP strežnika](./images/imagesPresentation/zagon.png)


## 8. Težave in rešitve med projektom:
- Težava: Eden izmed članov skupine je kljub opozorilom popolnoma prenehal sodelovati (David Bogdan), zato sva celoten projekt od začetka do konca bila primorana izvesti le dva (Nik Šignjar Žilavec, Andrej Nemanič).
- Težava: Ni bilo na voljo več zahtevane velikosti v nalogi za srežnik
- Rešitev: Vzeli smo alternativo
- Težava: Docker ukazi so sprva javljali napako glede pravic (permission denied).
- Rešitev: Trenutnega uporabnika smo dodali v skupino docker z ukazom usermod in osvežili SSH sejo.
- Težava: Aplikacija po zagonu na VM ni bila dostopna preko brskalnika.
- Rešitev: V Azure NSG (Network Security Group) požarnem zidu smo morali eksplicitno dodati novo Inbound pravilo za promet na vratih 3000/5173 (HTTP). Po tem je aplikacija takoj postala javno dostopna.