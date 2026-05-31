# QES pro Tails/Linux

Pro Tails má být hlavní vrstva CLI. Lokální UI je pouze pohodlný režim přes `127.0.0.1`.

Požadavky:

- žádná telemetrie
- žádné vzdálené fonty
- žádné CDN
- žádné externí API
- žádný internetový provoz
- lokální server jen na 127.0.0.1
- soubory zpracovávat streamově v dalších verzích

Základní příkazy:

```bash
qes server
qes self-test
qes encrypt --in tajne.pdf --out tajne.qes --capsule tajne.qes128 --password ... --seed1 ... --seed2 ... --seed3 ... --seed4 ...
qes decrypt --in tajne.qes --capsule tajne.qes128 --out obnovene.pdf --password ... --seed1 ... --seed2 ... --seed3 ... --seed4 ...
```


## QES v8.1 carrier MAC
Carrier režim obsahuje metadata QES-CARRIER-META-V2 s keyed MAC přes cover část i ASCII payload. Dešifrování starých carrier souborů bez této autentizace je odmítnuto, aby změna libovolného bajtu nemohla projít tichým roundtripem.
