# CONNECTOR: METAQES7 / ZERO_UNDER_TAG1

Stav: WORK IN PROGRESS

Base branch:

```text
qes-work-3-fix-rust-crate-name
```

Base commit:

```text
87315fd926129e641fc6fe359d769316f0b1a72d
```

Účel:
- přejít z legacy `METAQCS6` na `METAQES7`;
- zavést vnitřní model `ZERO_UNDER_TAG1`;
- zachovat kompatibilitu starších balíčků;
- držet TAG1/TAG2 jako skryté XOF XOR masky;
- TAG3 zatím ponechat jako legacy seal / budoucí hidden art slot.

Důležité:
- toto není finální ASCII-only formát;
- viditelné `ZERO:` a `SEAL:` zůstanou dočasně kvůli kompatibilitě;
- finální schování do ASCII artu bude další samostatná fáze.

Pravidlo pro další práci:
- nový nápad nejdřív zapsat sem nebo do `patches/`;
- zdroják měnit až po označení patch bloku jako READY;
- finální stav zapisovat až do `999_final/FINAL_CONNECTOR.md`.
