# QES – Quantum Encryption System
## Uživatelský a architektonický manuál prototypu

QES je výzkumný prototyp symetrického šifrovacího systému. Jeho cílem je ověřit architekturu, implementaci vrstev, práci s kapslí, kontrolními hodnotami a připravit základ pro další režim Adaptive Labyrinth Cover.

QES není hotový auditovaný standard. QES není označován jako neprolomitelný. Správná formulace je: QES je experimentální quantum-hard směřovaný symetrický systém, jehož bezpečnost závisí na entropii hesla a seedů, správnosti implementace a budoucí nezávislé kryptoanalýze.

---

## 1. Co QES dělá

```text
TAJNÁ DATA
   │
   ▼
PASSWORD + SEED1 + SEED2 + SEED3 + SEED4 + PARTICLE
   │
   ▼
ARGON2ID / KDF
   │
   ▼
MASTER SEED
   │
   ├── hidden_nonce
   ├── hidden_iv
   ├── route_seed
   ├── tag_keys
   └── mac_key
   │
   ▼
QES CORE
   │
   ▼
ENCRYPTED CORE
   │
   ▼
FINÁLNÍ QES SOUBOR
   │
   ▼
QES-128 KAPSLE + PUBLIC HASH + KEYED MAC
```

Uživatel po zašifrování dostane:

1. finální zašifrovaný soubor,
2. samostatnou QES-128 kapsli,
3. public hash,
4. keyed MAC.

Kapsle se nepřilepuje k souboru. Uživatel ji dostává zvlášť.

---

## 2. Symetrický princip

QES je symetrický systém. To znamená:

```text
ŠIFROVÁNÍ:
password + seedy + particle → master_seed → QES Core → výstup

DEŠIFROVÁNÍ:
stejný password + stejné seedy + stejná particle + kapsle → stejný master_seed → inverse QES Core → původní data
```

Pokud se změní password, jeden seed, particle, kapsle, soubor nebo režim, dešifrování se zastaví na TAG/MAC kontrole.

---

## 3. QES Core není jen XOR a ARX

QES Core je vícevrstvá vratná transformace. XOR a ARX jsou jen dílčí vrstvy. Celé jádro obsahuje:

| Vrstva | Co dělá | Dešifrování |
|---|---|---|
| Permutace | Přehází pořadí dat podle route_seed | Inverse permutace |
| Difuze | Rozšíří změnu jednoho bajtu přes další data | Inverse difuze |
| Superpozice | Kombinuje data s proudem, fází, pozicí a stavem | Opačná superpozice |
| Rotace | Otáčí bity ve vratném směru | Opačná rotace |
| ARX prvky | Add, Rotate, XOR pro další míchání hodnot | Opačné pořadí operací |
| XOR masky | Maskují bajty proudem z klíče | XOR stejnou maskou |
| TAG/MAC | Ověřují integritu | Ověření, ne transformace |

Diagram:

```text
plaintext
   │
   ▼
PERMUTACE
   │
   ▼
DIFUZE A
   │
   ▼
SUPERPOZICE
   │
   ▼
ROTACE / ARX PRVKY
   │
   ▼
DIFUZE B
   │
   ▼
TAGOVÉ A MASKOVACÍ VRSTVY
   │
   ▼
encrypted_core
```

---

## 4. Pojmy

| Pojem | Vysvětlení | Role v QES |
|---|---|---|
| Password | Tajná fráze uživatele | Základ pro KDF |
| Seed1–Seed4 | Další tajné vstupy | Mění master_seed a navigaci |
| Particle | Znak, hodnota, vektor, fáze a amplituda | Ovlivňuje trasy a kontrolní vrstvy |
| Argon2id | Paměťově náročné odvození klíče | Brání levnému hádání hesla |
| Master seed | Hlavní kořen systému | Odvozuje všechny interní klíče |
| Hidden nonce | Skrytý proměnný stav | Mění výstup i při stejném vstupu |
| Hidden IV | Inicializační stav | Start pro trasy a proudy |
| Route seed | Navigační seed | Řídí permutace, skoky a labyrint |
| Encrypted core | Vnitřní šifrované jádro | Čistý výsledek QES Core |
| QES-128 kapsle | 128B samostatný artefakt | Kotva, ověření, MAC |
| Public hash | Veřejný hash souboru | Ověření změny bez hesla |
| Keyed MAC | MAC počítaný s klíčem | Ověření souboru, kapsle a režimu |

---

## 5. QES-128 kapsle

Kapsle je samostatný artefakt pro uživatele. Neobsahuje plaintext, heslo, master_seed ani mapu trasy.

Návrh struktury:

```text
QES-128 CAPSULE

0–3      magic              "QES1"
4        version            1
5        mode               normal / cover / diagnostic
6        flags              režim kapacity
7        capsule_len        128
8–23     public_anchor      veřejná kotva
24–39    curve_anchor       kotva labyrintu
40–55    final_phase        konečná fáze
56–63    masked_payload_len maskovaná délka
64–95    file_commitment    hash / commitment souboru
96–127   keyed_mac          MAC kapsle + souboru + režimu
```

Kapsle pomáhá programu ověřit, že soubor, režim a klíč patří k sobě.

---

## 6. Public hash a keyed MAC

Public hash:

```text
public_hash = hash(final_file)
```

Použije se k ověření, jestli byl soubor změněn. Nepotřebuje klíč.

Keyed MAC:

```text
keyed_mac = MAC(mac_key, final_file || capsule || mode)
```

Použije se k ověření, že finální soubor, kapsle, režim a tajná navigace sedí. Pokud někdo změní soubor nebo kapsli, MAC nesedí.

---

## 7. Režimy QES

| Režim | Výstup | Stav |
|---|---|---|
| Normal Mode | QES soubor + QES-128 kapsle + hash/MAC | Aktivní |
| Carrier Compatibility | Carrier soubor + kapsle | Experimentální kompatibilita |
| Adaptive Labyrinth Cover | Cover soubor s payloadem v bodech | Další etapa |
| Diagnostic Mode | Report, testy, skoky, rychlost | Aktivní |
| Tails CLI | Offline příkazový režim | Základ připraven |

---

## 8. Adaptive Labyrinth Cover – cílový směr

Adaptive Labyrinth Cover chápe cover soubor jako prostor bodů.

```text
cover soubor = velký prostor
klíč = navigace
seed = motor labyrintu
startovní bod = začátek trasy
body na trase = úložiště payloadu
```

Tok:

```text
TAJNÁ DATA → QES CORE → payload stream
                         │
                         ▼
PASSWORD + SEEDY + PARTICLE → start → labyrint → body
                         │
                         ▼
PAYLOAD SE ROZLOŽÍ DO BODŮ COVER SOUBORU
                         │
                         ▼
FINÁLNÍ COVER SOUBOR + QES-128 KAPSLE
```

Tato část bude implementována až po samostatné konzultaci, protože vyžaduje streamové zpracování, kapacitní testy a bezpečné zacházení s velkými soubory.

---

## 9. Co testy prokazují

Testy ověřují:

- roundtrip šifrování/dešifrování,
- vratnost permutace,
- vratnost difuze,
- vratnost superpozice,
- vratnost celého kola,
- 1:1 encrypted core,
- TAG/MAC kontrolu,
- detekci špatného hesla,
- detekci špatné particle,
- detekci změněné kapsle,
- lavinový efekt,
- citlivost na změnu klíče.

Testy nejsou formální kryptografický důkaz. Ukazují správnost implementace a detekci změn. Formální bezpečnost vyžaduje audit.

---

## 10. Bezpečnostní formulace

Správně:

```text
QES je experimentální quantum-hard symetrický šifrovací systém.
Jeho praktická odolnost závisí na entropii hesla, délce seedů,
správnosti implementace a nezávislém kryptografickém auditu.
```

Nesprávně bez auditu:

```text
QES je neprolomitelný.
QES je hotový vojenský standard.
QES formálně zaručeně odolá všem aktérům.
```

---

## 11. Věta do aplikace

QES – Quantum Encryption System je výzkumný symetrický quantum-hard prototyp, který z hesla, seedů a particle vytváří skrytou navigaci pro šifrování, dešifrování, kontrolu integrity a volitelné rozložení dat do cover prostoru. Výstupem je finální soubor, samostatná QES-128 kapsle, public hash a keyed MAC.
