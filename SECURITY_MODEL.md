# QES SECURITY MODEL

## Aktivní stav
QES je alfa prototyp.

## Aktivní jádro
QES Rust Core.

## Základní tok
data
→ password + seed + další seedy + particle
→ KDF / odvození navigace
→ hidden state
→ QES Core
→ encrypted core
→ režim text / file / cover
→ výstup + MAC + kapsle

## Co je password
Hlavní tajná fráze uživatele.

## Co je seed
Doplňkový vstup do navigace. V základu jeden hlavní seed. Další seedy se mohou přidávat dynamicky.

## Co je particle
Doplňkový navigační prvek:
- glyph
- value
- vector
- phase
- amplitude

## Co je ART profil
ART profil je vizuální nebo navigační profil. Nemá být zaměňován s heslem.

Režimy:
- ART pouze vizuální
- ART jako součást navigace
- ART uložený do kapsle jako metadata

## MAC
Každé šifrování má vytvářet keyed MAC.

MAC se používá pro:
- text
- soubor
- cover
- kapsli
- ověření změny

## Public hash
Veřejný SHA-256 otisk výstupu.

## Kapsle
QES-128 kapsle je 128B artefakt. Nemá obsahovat heslo ani plaintext.

## Cover carrier
Aktuální cover režim:
cover soubor + QES kapsle + QES payload → finální cover soubor.

## Adaptive Labyrinth Cover
Budoucí režim:
cover soubor se analyzuje jako prostor bodů. Klíč určí start, seed určí trasu a payload se rozloží po křivce.

## Bezpečnostní upozornění
Testy nejsou formální kryptografický důkaz.
Pro ostré nasazení je nutná nezávislá kryptografická analýza.


## QES ZERO LOCK

QES ZERO LOCK je ochranný obal výstupu.

ZERO LOCK váže dohromady:
- encrypted payload
- QES-128 kapsli
- režim TEXT / FILE / COVER / VAULT
- verzi aplikace
- patch verzi
- ART profil
- kryptografický profil
- délku payloadu
- public hash
- keyed MAC
- final seal

ZERO LOCK není absolutní neproniknutelná zeď. Je to kryptografická hranice integrity. Pokud se změní payload, kapsle, režim nebo metadata, final seal nesouhlasí.


## QES STREAM GUARD

QES Stream Guard je vrstva pro budoucí blokové zpracování souborů.

Cíle:
- nečíst celý soubor do RAM
- zpracovávat po blocích 256 KB / 512 KB / 1 MB
- průběžně počítat MAC
- průběžně počítat public hash
- zobrazovat progress jen podle veřejného počtu bloků
- neukazovat tajné interní trasy
- snížit riziko pádu telefonu u velkých souborů

Aktuální alfa stav:
UI a limity jsou připravené. Další patch má napojit stream engine přímo do Rust core.
