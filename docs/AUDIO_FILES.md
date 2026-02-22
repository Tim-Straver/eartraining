# Audio bestanden toevoegen

Plaats je bestanden hier:

- `app/src/main/assets/chords/`
- `app/src/main/assets/notes/`

Voorbeelden:
- `app/src/main/assets/chords/C.wav`
- `app/src/main/assets/chords/Bb.wav`
- `app/src/main/assets/notes/F#.wav`

## Hoe de app nu werkt

- **Chord Progressions**: bouwt een progressie uit meerdere losse chord-bestanden in `assets/chords`, start met makkelijke progressies en unlockt geleidelijk moeilijkere varianten op basis van je goede antwoorden.
- **Chord Types**: kiest 1 bestand uit `assets/chords`.
- **Notes**: kiest 1 bestand uit `assets/notes`.

Dus: geen `prog_...` bestanden nodig als je `assets/chords` gevuld is.

## Voor progressions (belangrijk)

De app verwacht voor progression-opbouw root-major bestanden zoals jij aangaf (`A`, `Ab`, `B`, `Bb`, etc.).
Bestandsnaam zonder extensie is het chord label.

Aanbevolen set voor beste dekking:
- `C, Db, D, Eb, E, F, Gb, G, Ab, A, Bb, B` (of enharmonische varianten met `#`)

Met die set kan de app progressies genereren per key (zoals I-V-vi-IV, ii-V-I, I-IV-V-I) door meerdere losse files achter elkaar af te spelen.


## Antwoordgedrag

- De feedback met het correcte antwoord blijft zichtbaar totdat je de volgende vraag beantwoordt.
- Bij progressions toont feedback nu zowel Romeinse cijfers als chordnamen, bijvoorbeeld: `I-V-vi-IV (C-G-A-F)`.


- De app speelt nu alle progressions uit 4 losse chords, zodat je niet aan de lengte kunt horen welke het is.
