# Audio bestanden toevoegen

Je kunt bestanden nu in mappen zetten zoals je wilde:

- `app/src/main/assets/chords/` (bijv. `C.wav`, `Cm.wav`, `G7.mp3`)
- `app/src/main/assets/notes/` (bijv. `C.wav`, `D#.wav`, `F.mp3`)

De app leest deze mappen dynamisch en maakt automatisch vragen + antwoordopties.

## Hoe de app kiest

- **Chord Types** mode: kiest willekeurig uit `assets/chords/*`
- **Notes** mode: kiest willekeurig uit `assets/notes/*`
- Het correcte antwoord wordt uit de bestandsnaam gehaald (zonder extensie), bijvoorbeeld:
  - `chords/Cm.wav` → `Cm`
  - `notes/C.wav` → `C`

## Best practices

- Gebruik duidelijke bestandsnamen per label (`C`, `Cm`, `F#`, `Bb`, etc.)
- Je mag `.wav`, `.mp3` of `.ogg` gebruiken
- Vermijd dubbele labels met verschillende spellingen voor hetzelfde akkoord/noot

## Opbouw van moeilijkheid

Je kunt simpel starten met alleen C-gerelateerde files en later uitbreiden:

1. eerst alleen `C`, `Cm`, etc.
2. daarna extra roots toevoegen (`D`, `E`, `F#`, `Bb`, ...)

Zo groeit de moeilijkheid vanzelf mee met je dataset.


## Belangrijk voor jouw melding over `prog_...`

- In **Chord Progressions** mode gebruikt de app nu ook `assets/chords/*` zodra daar bestanden staan.
- Daardoor krijg je niet meer de melding over missende `prog_...` raw files als je met assets werkt.
- Alleen als `assets/chords` leeg is, valt de app terug op de oude startervragen in `res/raw`.
