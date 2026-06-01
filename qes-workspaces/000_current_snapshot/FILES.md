# Relevant files

Hlavní soubory projektu:

```text
android-app/app/src/main/java/org/zero/qes/MainActivity.java
rust-core/src/cipher.rs
rust-core/src/route.rs
rust-core/Cargo.toml
rust-core/src/ascii_art.rs
rust-core/src/kdf.rs
.github/workflows/build-android-apk.yml
```

Build pravidlo:

```text
GitHub Actions staví APK automaticky jen z main/master.
Pracovní větve qes-work-* APK automaticky nestaví.
```

Workspace pravidlo:

```text
qes-workspaces = plán / protokol / konektor / patch bloky
source files = skutečný kód aplikace
999_final = až ověřený finální stav
```
