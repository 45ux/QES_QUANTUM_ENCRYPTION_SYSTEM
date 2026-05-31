# QES – Quantum Encryption System

QES je výzkumný prototyp symetrického šifrovacího systému v Rustu.

Tato verze obsahuje:

- QES Core pro text a soubory,
- QES-128 kapsli jako samostatný výstup,
- public hash,
- keyed MAC,
- normální režim,
- diagnostický režim,
- carrier kompatibilní režim,
- moderní lokální UI,
- modulární rozšiřitelné tabulky v dokumentaci UI,
- základ CLI pro Tails/Linux.

## Důležitá formulace

QES není hotový auditovaný standard a není prezentován jako neprolomitelný. Jde o experimentální quantum-hard směřovaný symetrický systém. Bezpečnost závisí na entropii hesla a seedů, správnosti implementace a budoucím nezávislém auditu.

## Spuštění v Termuxu

```bash
cd ~/projects
pkill -9 -f qes_quantum_encryption_system
pkill -9 -f cargo
rm -rf qes_quantum_encryption_system

cp /sdcard/Download/qes_quantum_encryption_system_v8_2_docs_symmetric_ui.zip .
unzip qes_quantum_encryption_system_v8_2_docs_symmetric_ui.zip
cd qes_quantum_encryption_system

cargo test --release
cargo run --release
```

Otevři:

```text
http://127.0.0.1:8787
```

## CLI

Self-test:

```bash
cargo run --release -- self-test
```

Šifrování:

```bash
cargo run --release -- encrypt \
  --in tajne.pdf \
  --out tajne.qes \
  --capsule tajne.qes128 \
  --password heslo \
  --seed1 seed-111 --seed2 seed-222 --seed3 seed-333 --seed4 seed-444
```

Dešifrování:

```bash
cargo run --release -- decrypt \
  --in tajne.qes \
  --capsule tajne.qes128 \
  --out obnovene.pdf \
  --password heslo \
  --seed1 seed-111 --seed2 seed-222 --seed3 seed-333 --seed4 seed-444
```

## Dokumentace

- `QES_V8_ARCHITEKTURA.md`
- `QES_ARCHITEKTURA_UZIVATELSKY_MANUAL.md`
- `QES_TAILS_CLI.md`
