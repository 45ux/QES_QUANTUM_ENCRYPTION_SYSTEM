# Snapshot

Datum: 2026-06-01

Aktuální pracovní báze:

```text
qes-work-3-fix-rust-crate-name
```

Aktuální base commit:

```text
87315fd926129e641fc6fe359d769316f0b1a72d
```

Workspace commit start:

```text
1353c58de76c547d4178dff94243e4e416024196
```

Cíl workspace systému:
- každá nová fáze má vlastní podsložku;
- změny se nepíšou chaoticky přímo do finálního kódu;
- nejdřív vznikne protokol / patch / konektor;
- až ověřený stav se přenese do source souborů;
- finální stav se označí slovem FINAL.
