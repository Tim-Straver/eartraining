# Ear-Training Learning Progression System

## 1) Default starting key (C-first)
- The engine starts with only key `C` unlocked (pitch class `0`).
- Interval exercises are generated relative to C first (example: C→E for major 3rd).
- Chord identification starts from chords rooted on C.
- Progression drills begin in key C before transposing to later keys.

## 2) Progressive difficulty and key unlock order
- Keys unlock in a circle-of-fifths style sequence:
  `C → G → F → D → Bb → A → Eb → E → Ab → B → F# → Db`
- Unlock gate checks two mastery metrics over currently unlocked keys:
  - average score ≥ `72`
  - average accuracy ≥ `85%`
- A minimum attempts gate (`12` attempts per unlocked key) prevents accidental unlocks.

## 3) Performance tracking model
Per-answer updates are recorded in four dimensions:
- by **key**
- by **interval subtype** (e.g., M3, P5)
- by **chord subtype** (e.g., Major, Minor, 7th)
- by **progression subtype** (e.g., I–V–vi–IV, ii–V–I)

Each bucket stores:
- attempts
- correct
- streak
- score (0–100)

Score updates:
- Correct: `+2.2` (+`0.4` bonus after streak >= 3)
- Wrong: `-3.0`
- Then clamp to `0..100`

## 4) Adaptive prioritization (weak-key targeting)
When selecting the next key among unlocked keys, the engine computes a weight:

`weight = (0.65 * weakness + 0.35 * inexperience) * masteryPenalty`

Where:
- `weakness = 1 - accuracy`
- `inexperience = 1 / (1 + attempts)`
- `masteryPenalty = 0.35` for highly mastered keys (accuracy >= 90% and attempts >= 20), else `1.0`

This causes:
- weaker keys to appear more often,
- newer keys to get early reinforcement,
- mastered keys to appear less often (but not disappear).
