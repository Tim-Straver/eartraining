package com.example.eartraining

import android.content.Context
import java.util.Locale

object AssetQuestionBank {
    private val majorScaleOffsets = listOf(0, 2, 4, 5, 7, 9, 11)

    private data class ProgressionTemplate(
        val id: String,
        val label: String,
        val degrees: List<Int>,
        val difficulty: Int
    )

    private val progressionTemplates = listOf(
        // Easy
        ProgressionTemplate("1_4_5_1", "I-IV-V-I", listOf(1, 4, 5, 1), difficulty = 1),
        ProgressionTemplate("1_5_1", "I-V-I", listOf(1, 5, 1), difficulty = 1),

        // Medium
        ProgressionTemplate("1_5_6_4", "I-V-vi-IV", listOf(1, 5, 6, 4), difficulty = 2),
        ProgressionTemplate("6_4_1_5", "vi-IV-I-V", listOf(6, 4, 1, 5), difficulty = 2),

        // Harder
        ProgressionTemplate("2_5_1", "ii-V-I", listOf(2, 5, 1), difficulty = 3),
        ProgressionTemplate("1_6_2_5", "I-vi-ii-V", listOf(1, 6, 2, 5), difficulty = 3)
    )

    fun questionsForMode(context: Context, mode: TrainingMode): List<TrainingQuestion> {
        return when (mode) {
            TrainingMode.CHORD_PROGRESSION -> buildProgressionQuestions(context)
            TrainingMode.CHORD_TYPE -> buildLabelQuestions(context, "chords", mode, "Identify the chord")
            TrainingMode.NOTE -> buildLabelQuestions(context, "notes", mode, "Identify the note")
            else -> emptyList()
        }
    }

    fun maxProgressionDifficulty(): Int = progressionTemplates.maxOfOrNull { it.difficulty } ?: 1

    private fun buildProgressionQuestions(context: Context): List<TrainingQuestion> {
        val chordFiles = context.assets.list("chords").orEmpty()
            .filter { it.contains('.') }
            .sorted()

        if (chordFiles.isEmpty()) return emptyList()

        val rootFileByPitchClass = chordFiles.mapNotNull { file ->
            val rawLabel = file.substringBeforeLast('.').trim()
            val pitchClass = parsePitchClass(rawLabel) ?: return@mapNotNull null
            pitchClass to file
        }.toMap()

        if (rootFileByPitchClass.size < 6) return emptyList()

        val availableKeys = rootFileByPitchClass.keys.sorted()

        return availableKeys.flatMap { keyPc ->
            progressionTemplates.mapNotNull { template ->
                val sequenceFiles = template.degrees.mapNotNull { degree ->
                    val offset = majorScaleOffsets.getOrNull(degree - 1) ?: return@mapNotNull null
                    val chordPc = mod12(keyPc + offset)
                    rootFileByPitchClass[chordPc]
                }

                if (sequenceFiles.size != template.degrees.size) {
                    null
                } else {
                    val keyName = prettifyLabel(rootFileByPitchClass[keyPc]?.substringBeforeLast('.') ?: "Key")
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
            .filter { it.contains('.') }
            .sorted()

        if (files.isEmpty()) return emptyList()

        val labelsByFile = files.associateWith { file ->
            file.substringBeforeLast('.').trim()
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

    private fun buildChoices(correctAnswer: String, labels: List<String>): List<String> {
        val distractors = labels.filterNot { it == correctAnswer }.shuffled().take(3)
        return (distractors + correctAnswer).shuffled()
    }

    private fun parsePitchClass(label: String): Int? {
        val trimmed = label.trim()
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

    private fun mod12(value: Int): Int = ((value % 12) + 12) % 12

    private fun prettifyLabel(raw: String): String {
        return raw
            .replace('_', ' ')
            .split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { token -> token.replaceFirstChar { it.titlecase(Locale.ROOT) } }
    }
}
