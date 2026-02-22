# Audio bestanden toevoegen

Plaats alle audiobestanden hier:

- `app/src/main/res/raw/`

De app zoekt audio op basis van `audioResName` in `TrainerModels.kt` via Android resource lookup (`resources.getIdentifier(..., "raw", ...)`).

## Belangrijke naamregels (Android `res/raw`)

Bestandsnamen moeten:

- alleen kleine letters, cijfers en underscores bevatten
- starten met een letter
- eindigen op een ondersteund audioformaat (bijv. `.mp3`, `.wav`, `.ogg`)

Voorbeeld:

- `c_maj.mp3`
- `fsharp_min.wav` (liever geen `#` in bestandsnaam)

## Namen die nu al verwacht worden

Zolang je de huidige starter-vragen gebruikt, moeten deze resource-namen bestaan in `res/raw`:

- `prog_1_5_6_4`
- `prog_2_5_1`
- `prog_1_4_5_1`
- `int_second`
- `int_third`
- `int_fourth`
- `int_fifth`
- `int_seventh`
- `chord_major`
- `chord_minor`
- `chord_dim`
- `chord_sus2`
- `chord_sus4`
- `chord_7th`

> Let op: de extensie tel je niet mee in `audioResName`.

## Aanpak voor losse akkoorden (jouw plan)

Als je losse akkoordbestanden aanlevert om later dynamisch progressies te bouwen, gebruik dan een consistente naamconventie, bijvoorbeeld:

- `<toon>_<kwaliteit>_<octaafoptie>`
- voorbeelden: `c_maj_root`, `a_min_first_inv`, `g_dom7_root`

En voor meerdere keys later:

- begin met alleen C (`c_...`)
- breid daarna uit met andere roots (`d_...`, `e_...`, etc.)

Zo kun je straks programmatisch filteren op moeilijkheid (eerst C, daarna alle keys) zonder bestanden te hernoemen.
