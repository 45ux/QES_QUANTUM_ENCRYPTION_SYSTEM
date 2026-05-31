# QES v8.2 – Architektura aplikace

QES – Quantum Encryption System je výzkumný prototyp symetrického šifrovacího systému.

Tato verze sjednocuje názvy, popis pojmů, vysvětlení QES Core, QES-128 kapsle, public hash, keyed MAC a princip budoucího Adaptive Labyrinth Cover.

## Zásadní pravidla

1. QES Core není pouze XOR ani pouze ARX.
2. QES je symetrický systém: stejný password, seedy a particle slouží k šifrování i dešifrování.
3. QES-128 kapsle je samostatný artefakt pro uživatele, nikoli přilepený payload v souboru.
4. Public hash ověřuje změnu souboru bez klíče.
5. Keyed MAC ověřuje soubor, kapsli, režim a klíčovou navigaci.
6. Aplikace je prototyp a musí to uživateli jasně říkat.
7. Tabulky v UI jsou modulární a dají se rozšířit přes obrazovku.

## Tok

```text
TAJNÁ DATA
   ↓
PASSWORD + SEEDY + PARTICLE
   ↓
ARGON2ID / KDF
   ↓
MASTER_SEED
   ↓
QES CORE
   ↓
ENCRYPTED_CORE
   ↓
NORMAL MODE nebo ADAPTIVE LABYRINTH COVER
   ↓
FINÁLNÍ SOUBOR + QES-128 KAPSLE + HASH + MAC
```

## QES Core

QES Core je vícevrstvá vratná konstrukce:

```text
permutace → difuze → superpozice → rotace → ARX prvky → XOR masky → tagové vrstvy
```

## Stav vývoje

- Normal Mode: aktivní.
- Diagnostika: aktivní.
- Carrier Compatibility: experimentální přechodový režim.
- Adaptive Labyrinth Cover: další etapa po konzultaci.
- Tails CLI: základ připraven.

Podrobný manuál je v souboru `QES_ARCHITEKTURA_UZIVATELSKY_MANUAL.md`.
