# Patch 004 — ASCII hidden transition v1

Status: PLANNED

Cíl:
Nově generovaný ASCII nosič nemá viditelně ukazovat řádky `ZERO:` a `SEAL:`. Venku má být vidět jen ASCII art obal, zatímco skrytá hlavička a TAG3 seal zůstanou v souboru zakódované jako art-like řádky.

Dotčené soubory:

```text
rust-core/src/ascii_art.rs
rust-core/src/cipher.rs
tools/qes_patch_runner.py
tools/qes_patch_ascii_hidden_v1.py
```

Plánovaná změna:
- `ZERO:` řádek bude u nových balíčků nahrazen art řádkem přes `DATA_PALETTE`;
- `SEAL:` řádek bude u nových balíčků nahrazen art řádkem přes `DATA_PALETTE`;
- parser bude dál umět číst staré legacy balíčky s `ZERO:` a `SEAL:`;
- test `ascii_roundtrip` ověří, že nový výstup neobsahuje `ZERO:` ani `SEAL:`;
- core šifrování a MAC výpočty se nemění.

Poznámka:
Toto je přechodový ASCII-only krok. Nejde o změnu kryptografického jádra, ale o skrytí viditelných formátových štítků v nosiči.
