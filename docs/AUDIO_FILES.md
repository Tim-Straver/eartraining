# Audio bestanden toevoegen

Plaats je bestanden hier:

- `app/src/main/assets/chords/`
- `app/src/main/assets/notes/`

Voorbeelden:
- `app/src/main/assets/chords/C.wav`
- `app/src/main/assets/chords/Asus2.wav`
- `app/src/main/assets/chords/Asus2 2.wav` (2e octaaf/variant)
- `app/src/main/assets/notes/A#.wav`
- `app/src/main/assets/notes/A# 2.wav` (2e octaaf/variant)

## Hoe de app nu werkt

- **Chord Progressions**: bouwt een progressie uit meerdere losse chord-bestanden in `assets/chords`, start met makkelijke progressies en unlockt geleidelijk moeilijkere varianten op basis van je goede antwoorden.
- **Chord Types**: kiest 1 bestand uit `assets/chords`.
- **Notes**: kiest 1 bestand uit `assets/notes`.

Dus: geen `prog_...` bestanden nodig als je `assets/chords` gevuld is.
Als er geen bruikbare chord-roots gevonden worden, toont de app geen progression-vragen (in plaats van terugvallen op `prog_...` raw files).

## Naamgeving

- Sharps (`#`) worden volledig ondersteund (bijv. `A#`, `C#`, `F#`).
- Flats (`b`) blijven ook werken, maar je kunt alles in sharps aanleveren.
- Een optionele octaaf/variant suffix met spatie + nummer wordt genegeerd voor het label, bijv. `A# 2.wav` en `Asus2 2.wav`.

## Voor progressions (belangrijk)

De app verwacht voor progression-opbouw root-major bestanden (`A`, `A#`, `B`, etc.).
Bestandsnaam zonder extensie (en zonder optionele ` 2`) is het chord label.

Aanbevolen set voor beste dekking:
- `C, C#, D, D#, E, F, F#, G, G#, A, A#, B`

Met die set kan de app progressies genereren per key (zoals I-V-vi-IV, ii-V-I, I-IV-V-I) door meerdere losse files achter elkaar af te spelen.

## Antwoordgedrag

- De feedback met het correcte antwoord blijft zichtbaar totdat je de volgende vraag beantwoordt.
- Bij progressions toont feedback nu zowel Romeinse cijfers als chordnamen, bijvoorbeeld: `I-V-vi-IV (C-G-A-F)`.
- De app speelt alle progressions uit 4 losse chords, zodat je niet aan de lengte kunt horen welke het is.
