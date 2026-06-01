# FINAL CONNECTOR

Status: NOT FINAL

Tento soubor je finální brána mezi workspace protokoly a skutečným sloučením do `main`.

Dokud zde není výslovně uvedeno:

```text
FINAL
```

nejde o finální verzi k merge do `main`.

## Co musí být před FINAL splněno

1. Workspace fáze má vyplněný `CONNECTOR.md`.
2. Každý patch má vlastní protokol v `patches/`.
3. Rychlé testy projdou v Termuxu.
4. Dlouhé testy jsou buď ručně ověřené, nebo vědomě odložené s poznámkou.
5. Není uložen žádný secret, heslo, token ani citlivý klíč.
6. Je jasné, které větve se mají spojit.
7. Je jasné, že přesun do `main` spustí GitHub Actions APK build.

## Aktuální stav

```text
NOT FINAL
```

## Aktuálně sledované pracovní větve

```text
qes-work-2-metaqes7
qes-work-3-fix-rust-crate-name
qes-work-5-workspace-connector
```

## Poznámka

`qes-workspaces/` je protokolová vrstva. Není to zdrojový kód aplikace. Source změny zůstávají v původních složkách projektu.
