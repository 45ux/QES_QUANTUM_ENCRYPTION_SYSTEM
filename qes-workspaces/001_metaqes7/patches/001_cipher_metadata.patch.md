# Patch 001 — cipher metadata

Status: APPLIED IN WORK BRANCH

Cíl:
- přejít z legacy metadata modelu `METAQCS6` na nový `METAQES7`;
- zavést `ZERO_UNDER_TAG1` jako vnitřní metadata visačku;
- ponechat kompatibilitu starších balíčků.

Dotčený soubor:

```text
rust-core/src/cipher.rs
```

Změny:
- `build_metadata()` emituje `METAQES7`;
- metadata obsahují `zero_model=ZERO_UNDER_TAG1`;
- metadata obsahují stav vrstev `pre_core_otp=on`, `tag1_xof_mask=on`, `tag2_xof_mask=on`;
- `verify_metadata()` přijímá staré `METAQCS6` i nové `METAQES7`;
- diagnostický stage byl přejmenován na `ASCII_ART_CARRIER_TAG3_SEAL`.

Poznámka:
Tento patch zatím nemění finální viditelný ASCII formát. `ZERO:` a `SEAL:` mohou zůstat v legacy nosiči kvůli zpětné kompatibilitě. Finální ASCII-only schování bude samostatný patch.
