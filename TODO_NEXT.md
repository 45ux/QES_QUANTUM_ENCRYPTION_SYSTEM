# TODO NEXT – QES APK

## Priorita 1 – dokončit alfa UI
- [ ] Přidat / opravit stránku NASTAVENÍ.
- [ ] Přejmenovat ZERO v menu na NASTAVENÍ.
- [ ] Zobrazit verzi: 0.11.0-alpha.
- [ ] Zobrazit patch: P-2026-05-31-02.
- [ ] Oddělit KLÍČ od ostatních stránek.
- [ ] ART stránka nesmí zobrazovat heslo a seedy.
- [ ] SOUBOR stránka nesmí zobrazovat celé heslo a seedy.
- [ ] COVER stránka nesmí zobrazovat celé heslo a seedy.
- [ ] TEXT stránka nesmí zbytečně opakovat celé heslo a seedy.
- [ ] Přidat stavovou kartu navigace.

## Priorita 2 – funkčnost
- [ ] Text šifrování + dešifrování.
- [ ] Soubor šifrování + dešifrování.
- [ ] Cover carrier šifrování + dešifrování.
- [ ] Ukládání .qes.
- [ ] Ukládání dešifrovaného souboru.
- [ ] Ukládání MAC reportu.
- [ ] Ukládání QES-128 kapsle.
- [ ] Ukládání logu.

## Priorita 3 – ověření
- [ ] Ověření textu podle SHA-256 a keyed MAC.
- [ ] Ověření souboru podle SHA-256 a keyed MAC.
- [ ] Ověření coveru podle SHA-256 a keyed MAC.
- [ ] Jasně zobrazit OK / FAIL.

## Priorita 4 – diagnostika
- [ ] Rychlé testy.
- [ ] Standardní testy.
- [ ] Těžké testy.
- [ ] Uložit log.
- [ ] Uložit report.

## Priorita 5 – budoucnost
- [ ] AES-GCM v Rust core.
- [ ] ChaCha20-Poly1305 v Rust core.
- [ ] Komprese před šifrováním.
- [ ] Adaptive Labyrinth Cover.
- [ ] Vault / trezor.


## ZERO LOCK
- [x] Přidat ZERO LOCK stav.
- [x] Přidat Final Seal do reportu.
- [x] Přidat ZERO LOCK do MAC stránky.
- [x] Přidat ZERO LOCK do Nastavení.
- [ ] Napojit ZERO LOCK přímo do formátu .qes.
- [ ] Napojit ZERO LOCK přímo do cover formátu.
- [ ] Přidat samostatné ověření ZERO LOCK balíku.


## STREAM GUARD
- [x] Přidat Stream Guard do Nastavení.
- [x] Přidat velikost bloku 256 KB / 512 KB / 1 MB.
- [x] Přidat stream status do SOUBOR.
- [x] Přidat stream status do COVER.
- [x] Přidat test limity.
- [ ] Napojit streamové šifrování do Rust core.
- [ ] Přidat streamové ukládání bez držení celého výstupu v RAM.
- [ ] Přidat streamové ověření Zero Lock balíku.


## STREAM FILE ENGINE ALFA
- [x] Přidat stream šifrování souboru po blocích.
- [x] Přidat stream dešifrování souboru po blocích.
- [x] Přidat stream MAC.
- [x] Přidat stream footer.
- [x] Přidat progress podle bloků.
- [ ] Přenést stream engine z Java vrstvy do Rust core.
- [ ] U dešifrování zapisovat nejdřív do dočasného souboru a až po MAC ověření potvrdit výstup.
- [ ] Přidat Zero Lock ověření přímo nad stream kontejnerem.
