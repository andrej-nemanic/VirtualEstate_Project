1. Kje in kako omogočite "port forwarding" (odpiranje vrat)?

Na Azure se to ureja preko Network Security Group (NSG), ki deluje kot požarni zid pred virtualno napravo.
V portalu odpremo našo virtualno napravo.
V levem meniju izberemo Networking (ali Network settings).
Kliknemo na Create Inbound port rule.
Nastavimo:
- Source: Any
- Source port ranges: *
- Destination: Any
- Service: Custom
- Destination port ranges: 3000 (ali vrata, kjer teče vaša aplikacija, npr. 80, 8080)
- Protocol: TCP
- Action: Allow
- Priority: 1000 (ali poljubna nizka številka)
- Name: Allow(Web)Traffic

[SLIKA: Zaslonski posnetek nastavitve Inbound port rule za vrata vaše aplikacije]
2. Kakšen tip diska je bil dodan vaši navidezni napravi in kakšna je njegova kapaciteta?

    Na strani virtualne naprave v levem meniju izberemo Disks.

    V tabeli vidimo OS disk:

        Tip diska: Premium SSD LRS (ali Standard SSD, odvisno od privzete izbire ob kreiranju).

        Kapaciteta: 30 GiB (privzeta velikost za Ubuntu sliko).

[SLIKA: Zaslonski posnetek razdelka Disks, kjer se vidi tip in velikost diska]
3. Kje preverimo stanje trenutne porabe virov v naši naročnini ("Azure for students")?

Stanje porabe dobroimetja (100 €) in preostalih brezplačnih ur preverimo na portalu Azure Microsoft Education Hub ali preko razdelka Cost Management + Billing.

    V iskalno vrstico na vrhu Azure portala vpišemo Education.

    Kliknemo na Overview.

    Tukaj je viden graf porabe preostalega dobroimetja v evrih in časovni okvir naročnine.

[SLIKA: Zaslonski posnetek Education portala, ki prikazuje porabo dobroimetja]
(Opomba: Kot rečeno v namigu, bo poraba realno vidna šele po 24 urah delovanja).
