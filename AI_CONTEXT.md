# QES AI CONTEXT

## Název projektu
QES – Quantum Encryption System

## Stav
Android APK alfa prototyp.

Aktuální cíl: dokončit aplikaci jako použitelnou alfa verzi s nativním Android UI, Rust core přes JNI, textovým režimem, souborovým režimem, cover režimem, MAC reportem, kapslí, ověřením, testy, logy, nastavením a přehlednou architekturou.

## Verze
QES ALFA PROTOTYP
Verze: 0.11.0-alpha
Patch: P-2026-05-31-02

## Důležité pravidlo UI
Heslo a seedy patří pouze na stránku KLÍČ / NAVIGACE.

Na stránkách ART, TEXT, SOUBOR a COVER se nemá znovu zobrazovat celé zadání hesla a seedů. Má se tam zobrazovat pouze stavová karta:

- Navigace: aktivní / neaktivní
- Password: nastaveno / chybí
- Seedů: počet
- ART profil: název
- MAC: zapnuto / vypnuto
- Tlačítko: Změnit v KLÍČI

## Stránky aplikace
- PŘEHLED
- KLÍČ
- ART
- TEXT
- SOUBOR
- COVER
- OVĚŘENÍ
- TESTY
- MAC
- LOG
- ARCH
- NASTAVENÍ

## Funkční cíle
1. Text:
   - šifrovat text
   - dešifrovat text
   - kopírovat výstup
   - uložit výstup
   - vytvořit MAC report
   - vytvořit kapsli

2. Soubor:
   - vybrat vstupní soubor
   - zašifrovat do .qes
   - dešifrovat .qes
   - uložit výstup
   - uložit MAC report
   - uložit QES-128 kapsli

3. Cover:
   - vybrat tajný soubor
   - vybrat cover soubor
   - vytvořit finální cover soubor
   - připojit QES payload + kapsli
   - dešifrovat finální cover
   - uložit MAC report
   - uložit kapsli

4. Ověření:
   - ověřit text
   - ověřit soubor
   - ověřit cover
   - porovnat public SHA-256
   - porovnat keyed MAC

5. Testy:
   - Rust self test
   - text roundtrip
   - file roundtrip
   - cover roundtrip
   - špatný klíč
   - tamper test
   - nonce divergence
   - entropy
   - monobit
   - byte diversity
   - chi-square
   - serial correlation
   - uložit log

6. Nastavení:
   - verze aplikace
   - patch verze
   - režim: normální / experimentální / vývojářský
   - motiv: tmavý / světlý / vysoký kontrast
   - bezpečnost: heslo, seed, MAC, stop při chybě
   - výstupy: MAC, kapsle, public hash, log
   - ART režim: vizuální / navigační
   - crypto profil: QES CORE / AES plán / ChaCha plán / hybrid plán
   - AES mód: GCM / CTR / CBC kompatibilita
   - komprese: vypnuto / deflate / zstd plán / lzma plán
   - reset logu, navigace, ART, MAC, kapsle

## Bezpečnostní formulace
Aplikace nesmí tvrdit, že je formálně kryptograficky auditovaná.

Správná formulace:
QES je alfa prototyp. Testy ověřují implementační chování, roundtrip, integritu, citlivost na změnu a statistické indikátory. Nejde o formální kryptografický audit.

## AES a další šifry
AES-GCM, AES-CTR, ChaCha20-Poly1305 a komprese mohou být v UI jako plánované profily, ale nesmí se tvářit jako aktivní, dokud nejsou skutečně napojené v Rust core.

Aktivní jádro je zatím QES Rust Core.

## Vault budoucnost
Vault / trezor bude pozdější prémiová část:
- šifrovaný trezor souborů
- složky
- poznámky
- fotky / videa
- MAC pro každou položku
- vault manifest
- vault MAC
- QES-128 kapsle
- export / import
- offline režim
- PIN / biometrie přes Android
