package com.example.eartraining

import kotlin.random.Random

enum class TrainingMode(val displayName: String) {
    CHORD_PROGRESSION("Chord Progressions"),
    INTERVAL("Intervals"),
    CHORD_TYPE("Chord Types");

    companion object {
        fun fromDisplayName(name: String): TrainingMode {
            return entries.firstOrNull { it.displayName == name } ?: CHORD_PROGRESSION
        }
    }
}

data class TrainingQuestion(
    val id: String,
    val mode: TrainingMode,
    val prompt: String,
    val audioResName: String,
    val choices: List<String>,
    val correctAnswer: String
)

data class QuestionStats(
    val attempts: Int = 0,
    val totalWrong: Int = 0,
    val wrongStreak: Int = 0
)

class AdaptiveTrainer(
    private val random: Random = Random.Default
) {
    fun pickQuestion(
        questions: List<TrainingQuestion>,
        stats: Map<String, QuestionStats>
    ): TrainingQuestion {
        require(questions.isNotEmpty()) { "Questions cannot be empty" }

        val weightedPool = questions.map { question ->
            val stat = stats[question.id] ?: QuestionStats()
            val weight = 1 + stat.totalWrong * 2 + stat.wrongStreak * 3
            question to weight
        }

        val totalWeight = weightedPool.sumOf { it.second }
        var draw = random.nextInt(totalWeight)

        for ((question, weight) in weightedPool) {
            draw -= weight
            if (draw < 0) return question
        }

        return questions.last()
    }

    fun updateStats(previous: QuestionStats?, wasCorrect: Boolean): QuestionStats {
        val old = previous ?: QuestionStats()
        return if (wasCorrect) {
            old.copy(attempts = old.attempts + 1, wrongStreak = 0)
        } else {
            old.copy(
                attempts = old.attempts + 1,
                totalWrong = old.totalWrong + 1,
                wrongStreak = old.wrongStreak + 1
            )
        }
    }
}

object StarterQuestionBank {
    val allQuestions: List<TrainingQuestion> = listOf(
        TrainingQuestion(
            id = "prog_1_5_6_4",
            mode = TrainingMode.CHORD_PROGRESSION,
            prompt = "Identify the progression (Roman numerals)",
            audioResName = "prog_1_5_6_4",
            choices = listOf("I-V-vi-IV", "ii-V-I", "I-vi-ii-V", "I-IV-V-I"),
            correctAnswer = "I-V-vi-IV"
        ),
        TrainingQuestion(
            id = "prog_2_5_1",
            mode = TrainingMode.CHORD_PROGRESSION,
            prompt = "Identify the progression (Roman numerals)",
            audioResName = "prog_2_5_1",
            choices = listOf("ii-V-I", "I-V-vi-IV", "I-vi-ii-V", "vi-IV-I-V"),
            correctAnswer = "ii-V-I"
        ),
        TrainingQuestion(
            id = "prog_1_4_5_1",
            mode = TrainingMode.CHORD_PROGRESSION,
            prompt = "Identify the progression (Roman numerals)",
            audioResName = "prog_1_4_5_1",
            choices = listOf("I-IV-V-I", "ii-V-I", "I-V-vi-IV", "vi-IV-I-V"),
            correctAnswer = "I-IV-V-I"
        ),
        TrainingQuestion(
            id = "int_m2",
            mode = TrainingMode.INTERVAL,
            prompt = "Identify the interval",
            audioResName = "int_second",
            choices = listOf("Second", "Third", "Fourth", "Fifth", "Seventh"),
            correctAnswer = "Second"
        ),
        TrainingQuestion(
            id = "int_m3",
            mode = TrainingMode.INTERVAL,
            prompt = "Identify the interval",
            audioResName = "int_third",
            choices = listOf("Second", "Third", "Fourth", "Fifth", "Seventh"),
            correctAnswer = "Third"
        ),
        TrainingQuestion(
            id = "int_p4",
            mode = TrainingMode.INTERVAL,
            prompt = "Identify the interval",
            audioResName = "int_fourth",
            choices = listOf("Second", "Third", "Fourth", "Fifth", "Seventh"),
            correctAnswer = "Fourth"
        ),
        TrainingQuestion(
            id = "int_p5",
            mode = TrainingMode.INTERVAL,
            prompt = "Identify the interval",
            audioResName = "int_fifth",
            choices = listOf("Second", "Third", "Fourth", "Fifth", "Seventh"),
            correctAnswer = "Fifth"
        ),
        TrainingQuestion(
            id = "int_m7",
            mode = TrainingMode.INTERVAL,
            prompt = "Identify the interval",
            audioResName = "int_seventh",
            choices = listOf("Second", "Third", "Fourth", "Fifth", "Seventh"),
            correctAnswer = "Seventh"
        ),
        TrainingQuestion(
            id = "chord_maj",
            mode = TrainingMode.CHORD_TYPE,
            prompt = "Identify the chord type",
            audioResName = "chord_major",
            choices = listOf("Major", "Minor", "Diminished", "Sus2", "Sus4", "7th"),
            correctAnswer = "Major"
        ),
        TrainingQuestion(
            id = "chord_min",
            mode = TrainingMode.CHORD_TYPE,
            prompt = "Identify the chord type",
            audioResName = "chord_minor",
            choices = listOf("Major", "Minor", "Diminished", "Sus2", "Sus4", "7th"),
            correctAnswer = "Minor"
        ),
        TrainingQuestion(
            id = "chord_dim",
            mode = TrainingMode.CHORD_TYPE,
            prompt = "Identify the chord type",
            audioResName = "chord_dim",
            choices = listOf("Major", "Minor", "Diminished", "Sus2", "Sus4", "7th"),
            correctAnswer = "Diminished"
        ),
        TrainingQuestion(
            id = "chord_sus2",
            mode = TrainingMode.CHORD_TYPE,
            prompt = "Identify the chord type",
            audioResName = "chord_sus2",
            choices = listOf("Major", "Minor", "Diminished", "Sus2", "Sus4", "7th"),
            correctAnswer = "Sus2"
        ),
        TrainingQuestion(
            id = "chord_sus4",
            mode = TrainingMode.CHORD_TYPE,
            prompt = "Identify the chord type",
            audioResName = "chord_sus4",
            choices = listOf("Major", "Minor", "Diminished", "Sus2", "Sus4", "7th"),
            correctAnswer = "Sus4"
        ),
        TrainingQuestion(
            id = "chord_7",
            mode = TrainingMode.CHORD_TYPE,
            prompt = "Identify the chord type",
            audioResName = "chord_7th",
            choices = listOf("Major", "Minor", "Diminished", "Sus2", "Sus4", "7th"),
            correctAnswer = "7th"
        )
    )
}
