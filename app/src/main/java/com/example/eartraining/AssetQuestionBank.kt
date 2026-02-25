package com.example.eartraining

import android.content.Context
import java.util.Locale

object AssetQuestionBank {
    private val majorScaleOffsets = listOf(0, 2, 4, 5, 7, 9, 11)
    private const val fileNameSeparatorPattern = "[_\\-]"

    private data class ProgressionTemplate(
        val id: String,
        val label: String,
        val degrees: List<Int>,
        val difficulty: Int
    )

    private data class IntervalTemplate(
        val id: String,
        val label: String,
        val semitones: Int,
        val difficulty: Int
    )

    private val progressionTemplates = listOf(
        // Easy
        ProgressionTemplate("1_4_5_1", "I-IV-V-I", listOf(1, 4, 5, 1), difficulty = 1),
        ProgressionTemplate("1_5_1_1", "I-V-I-I", listOf(1, 5, 1, 1), difficulty = 1),
        ProgressionTemplate("1_6_4_5", "I-vi-IV-V", listOf(1, 6, 4, 5), difficulty = 1),
        ProgressionTemplate("1_4_1_5", "I-IV-I-V", listOf(1, 4, 1, 5), difficulty = 1),

        // Medium
        ProgressionTemplate("1_5_6_4", "I-V-vi-IV", listOf(1, 5, 6, 4), difficulty = 2),
        ProgressionTemplate("6_4_1_5", "vi-IV-I-V", listOf(6, 4, 1, 5), difficulty = 2),
        ProgressionTemplate("1_5_4_6", "I-V-IV-vi", listOf(1, 5, 4, 6), difficulty = 2),
        ProgressionTemplate("1_3_4_5", "I-iii-IV-V", listOf(1, 3, 4, 5), difficulty = 2),
        ProgressionTemplate("4_1_5_6", "IV-I-V-vi", listOf(4, 1, 5, 6), difficulty = 2),

        // Harder
        ProgressionTemplate("2_5_1_1", "ii-V-I-I", listOf(2, 5, 1, 1), difficulty = 3),
        ProgressionTemplate("1_6_2_5", "I-vi-ii-V", listOf(1, 6, 2, 5), difficulty = 3),
        ProgressionTemplate("3_6_2_5", "iii-vi-ii-V", listOf(3, 6, 2, 5), difficulty = 3),
        ProgressionTemplate("6_2_5_1", "vi-ii-V-I", listOf(6, 2, 5, 1), difficulty = 3),
        ProgressionTemplate("1_2_5_1", "I-ii-V-I", listOf(1, 2, 5, 1), difficulty = 3),
        ProgressionTemplate("1_4_2_5", "I-IV-ii-V", listOf(1, 4, 2, 5), difficulty = 3)
    )


    private val intervalTemplates = listOf(
        // Easy
        IntervalTemplate("m2", "Minor 2nd", semitones = 1, difficulty = 1),
        IntervalTemplate("M2", "Major 2nd", semitones = 2, difficulty = 1),
        IntervalTemplate("M3", "Major 3rd", semitones = 4, difficulty = 1),

        // Medium
        IntervalTemplate("m3", "Minor 3rd", semitones = 3, difficulty = 2),
        IntervalTemplate("P4", "Perfect 4th", semitones = 5, difficulty = 2),
        IntervalTemplate("P5", "Perfect 5th", semitones = 7, difficulty = 2),

        // Harder
        IntervalTemplate("TT", "Tritone", semitones = 6, difficulty = 3),
        IntervalTemplate("m7", "Minor 7th", semitones = 10, difficulty = 3),
        IntervalTemplate("M7", "Major 7th", semitones = 11, difficulty = 3)
    )
    fun questionsForMode(context: Context, mode: TrainingMode): List<TrainingQuestion> {
        return when (mode) {
            TrainingMode.CHORD_PROGRESSION -> buildProgressionQuestions(context)
            TrainingMode.CHORD_TYPE -> buildChordTypeQuestions(context)
            TrainingMode.INTERVAL -> buildIntervalQuestions(context)
            TrainingMode.NOTE -> buildLabelQuestions(context, "notes", mode, "Identify the note")
            else -> emptyList()
        }
    }

    fun maxProgressionDifficulty(): Int = progressionTemplates.maxOfOrNull { it.difficulty } ?: 1

    fun maxChordTypeDifficulty(): Int = 3

    fun maxIntervalDifficulty(): Int = intervalTemplates.maxOfOrNull { it.difficulty } ?: 1

    private fun buildProgressionQuestions(context: Context): List<TrainingQuestion> {
        val chordFiles = context.assets.list("chords").orEmpty()
            .filter { it.isNotBlank() }
            .sorted()

        if (chordFiles.isEmpty()) return emptyList()

        val rootFilesByPitchClass = chordFiles.mapNotNull { file ->
            val rawLabel = file.substringBeforeLast('.').trim()
            val (rootToken, qualityToken) = parseChordFileName(rawLabel) ?: return@mapNotNull null
            val pitchClass = parsePitchClass(rootToken) ?: return@mapNotNull null
            val quality = normalizeChordQuality(qualityToken)
            Triple(pitchClass, file, quality)
        }
            .groupBy({ it.first }, { it.second to it.third })
            .mapValues { (_, entries) ->
                val majors = entries.filter { (_, quality) -> quality == "Major" }.map { it.first }
                if (majors.isNotEmpty()) majors else entries.map { it.first }
            }

        if (rootFilesByPitchClass.size < 6) return emptyList()

        val availableKeys = rootFilesByPitchClass.keys.sorted()

        return availableKeys.flatMap { keyPc ->
            progressionTemplates.mapNotNull { template ->
                val sequenceFiles = template.degrees.mapNotNull { degree ->
                    val offset = majorScaleOffsets.getOrNull(degree - 1) ?: return@mapNotNull null
                    val chordPc = mod12(keyPc + offset)
                    rootFilesByPitchClass[chordPc]?.randomOrNull()
                }

                if (sequenceFiles.size != template.degrees.size) {
                    null
                } else {
                    val keyName = prettifyLabel(
                        normalizeLabel(rootFilesByPitchClass[keyPc]?.firstOrNull()?.substringBeforeLast('.') ?: "Key")
                    )
                    val allChoices = progressionTemplates
                        .filter { it.difficulty <= template.difficulty }
                        .map { it.label }
                        .distinct()
                    TrainingQuestion(
                        id = "asset_prog_${keyPc}_${template.id}",
                        mode = TrainingMode.CHORD_PROGRESSION,
                        prompt = "Identify the progression in key $keyName",
                        audioResName = "",
                        audioAssetPath = null,
                        audioAssetSequence = sequenceFiles.map { "chords/$it" },
                        difficulty = template.difficulty,
                        choices = allChoices,
                        correctAnswer = template.label
                    )
                }
            }
        }
    }

    private fun buildLabelQuestions(
        context: Context,
        folder: String,
        mode: TrainingMode,
        prompt: String
    ): List<TrainingQuestion> {
        val files = context.assets.list(folder).orEmpty()
            .filter { it.isNotBlank() }
            .sorted()

        if (files.isEmpty()) return emptyList()

        val labelsByFile = files.associateWith { file ->
            normalizeLabel(file.substringBeforeLast('.').trim())
        }

        val labels = labelsByFile.values
            .map { prettifyLabel(it) }
            .distinct()

        return files.map { file ->
            val answer = prettifyLabel(labelsByFile.getValue(file))
            TrainingQuestion(
                id = "asset_${folder}_${file}",
                mode = mode,
                prompt = prompt,
                audioResName = "",
                audioAssetPath = "$folder/$file",
                audioAssetSequence = emptyList(),
                difficulty = 1,
                choices = buildChoices(answer, labels),
                correctAnswer = answer
            )
        }
    }

    private fun buildIntervalQuestions(context: Context): List<TrainingQuestion> {
        val files = context.assets.list("notes").orEmpty()
            .filter { it.isNotBlank() }
            .sorted()

        if (files.size < 3) return emptyList()

        val noteFilesByPitchClass = files.mapNotNull { file ->
            val baseName = file.substringBeforeLast('.').trim()
            val pitchClass = parsePitchClass(baseName) ?: return@mapNotNull null
            pitchClass to file
        }.groupBy({ it.first }, { it.second })

        if (noteFilesByPitchClass.size < 3) return emptyList()

        val labelsByDifficulty = intervalTemplates
            .groupBy { it.difficulty }
            .mapValues { (_, templates) -> templates.map { it.label }.distinct() }

        val roots = noteFilesByPitchClass.keys.sorted()

        return roots.flatMap { rootPc ->
            intervalTemplates.mapNotNull { template ->
                val upperPc = mod12(rootPc + template.semitones)
                val rootFile = noteFilesByPitchClass[rootPc]?.randomOrNull() ?: return@mapNotNull null
                val upperFile = noteFilesByPitchClass[upperPc]?.randomOrNull() ?: return@mapNotNull null
                val unlockedLabels = labelsByDifficulty
                    .filterKeys { it <= template.difficulty }
                    .values
                    .flatten()
                    .distinct()
                TrainingQuestion(
                    id = "asset_interval_${rootPc}_${template.id}",
                    mode = TrainingMode.INTERVAL,
                    prompt = "Identify the interval",
                    audioResName = "",
                    audioAssetPath = null,
                    audioAssetSequence = listOf("notes/$rootFile", "notes/$upperFile"),
                    difficulty = template.difficulty,
                    choices = buildChoices(template.label, unlockedLabels),
                    correctAnswer = template.label
                )
            }
        }
    }

    private fun buildChordTypeQuestions(context: Context): List<TrainingQuestion> {
        val files = context.assets.list("chords").orEmpty()
            .filter { it.isNotBlank() }
            .sorted()

        if (files.isEmpty()) return emptyList()

        val qualityByFile = files.mapNotNull { file ->
            val baseName = file.substringBeforeLast('.').trim()
            val (_, qualityToken) = parseChordFileName(baseName) ?: return@mapNotNull null
            val normalizedQuality = normalizeChordQuality(qualityToken) ?: return@mapNotNull null
            file to normalizedQuality
        }.toMap()

        if (qualityByFile.isEmpty()) return emptyList()

        val labelsByDifficulty = qualityByFile.values
            .distinct()
            .groupBy { qualityDifficulty(it) }

        return qualityByFile.entries.map { (file, answer) ->
            val difficulty = qualityDifficulty(answer)
            val unlockedLabels = labelsByDifficulty
                .filterKeys { it <= difficulty }
                .values
                .flatten()
                .distinct()
            TrainingQuestion(
                id = "asset_chords_${file}",
                mode = TrainingMode.CHORD_TYPE,
                prompt = "Identify the chord type",
                audioResName = "",
                audioAssetPath = "chords/$file",
                audioAssetSequence = emptyList(),
                difficulty = difficulty,
                choices = buildChoices(answer, unlockedLabels),
                correctAnswer = answer
            )
        }
    }

    private fun buildChoices(correctAnswer: String, labels: List<String>): List<String> {
        val distractors = labels.filterNot { it == correctAnswer }.shuffled().take(3)
        return (distractors + correctAnswer).shuffled()
    }

    private fun parsePitchClass(label: String): Int? {
        val trimmed = normalizeLabel(label)
        val regex = Regex("^([A-Ga-g])([#b]?)$")
        val match = regex.matchEntire(trimmed) ?: return null
        val base = when (match.groupValues[1].uppercase(Locale.ROOT)) {
            "C" -> 0
            "D" -> 2
            "E" -> 4
            "F" -> 5
            "G" -> 7
            "A" -> 9
            "B" -> 11
            else -> return null
        }
        val accidental = match.groupValues[2]
        return when (accidental) {
            "#" -> mod12(base + 1)
            "b" -> mod12(base - 1)
            else -> base
        }
    }

    private fun parseChordFileName(name: String): Pair<String, String>? {
        val normalizedName = normalizeLabel(name)
        val regex = Regex("^([A-Ga-g](?:#|b)?)(.*)$")
        val match = regex.matchEntire(normalizedName) ?: return null
        val root = match.groupValues[1]
        val quality = match.groupValues[2]
            .trim()
            .replaceFirst(Regex("^$fileNameSeparatorPattern"), "")
        return root to quality
    }

    private fun normalizeChordQuality(rawQuality: String): String? {
        return when (rawQuality.lowercase(Locale.ROOT)) {
            "" -> "Major"
            "maj", "major" -> "Major"
            "min", "minor", "m" -> "Minor"
            "sus2" -> "Sus2"
            "sus4" -> "Sus4"
            "dim", "diminished" -> "Diminished"
            "aug", "augmented", "+" -> "Augmented"
            "7", "7th", "dom7", "dominant7" -> "7th"
            else -> null
        }
    }

    private fun qualityDifficulty(quality: String): Int {
        return when (quality) {
            "Major", "Minor" -> 1
            "Sus2", "Sus4", "7th" -> 2
            "Diminished", "Augmented" -> 3
            else -> 1
        }
    }

    private fun mod12(value: Int): Int = ((value % 12) + 12) % 12

    private fun prettifyLabel(raw: String): String {
        return raw
            .replace('_', ' ')
            .split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { token -> token.replaceFirstChar { it.titlecase(Locale.ROOT) } }
    }

    private fun normalizeLabel(raw: String): String {
        return raw.trim().replace(Regex("\\s+\\d+$"), "")
    }
}
