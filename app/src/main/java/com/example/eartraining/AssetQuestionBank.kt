package com.example.eartraining

import android.content.Context
import java.util.Locale

object AssetQuestionBank {
    fun questionsForMode(context: Context, mode: TrainingMode): List<TrainingQuestion> {
        return when (mode) {
            TrainingMode.CHORD_PROGRESSION -> buildLabelQuestions(
                context,
                "chords",
                mode,
                "Identify the chord used in the progression"
            )
            TrainingMode.CHORD_TYPE -> buildLabelQuestions(context, "chords", mode, "Identify the chord")
            TrainingMode.NOTE -> buildLabelQuestions(context, "notes", mode, "Identify the note")
            else -> emptyList()
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
                choices = buildChoices(answer, labels),
                correctAnswer = answer
            )
        }
    }

    private fun buildChoices(correctAnswer: String, labels: List<String>): List<String> {
        val distractors = labels.filterNot { it == correctAnswer }.shuffled().take(3)
        return (distractors + correctAnswer).shuffled()
    }

    private fun prettifyLabel(raw: String): String {
        return raw
            .replace('_', ' ')
            .lowercase(Locale.ROOT)
            .split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { token -> token.replaceFirstChar { it.titlecase(Locale.ROOT) } }
    }
}
