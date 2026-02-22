package com.example.eartraining

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun updateStats_incrementsWrongAndStreakOnMiss() {
        val trainer = AdaptiveTrainer()

        val result = trainer.updateStats(QuestionStats(), wasCorrect = false)

        assertEquals(1, result.attempts)
        assertEquals(1, result.totalWrong)
        assertEquals(1, result.wrongStreak)
    }

    @Test
    fun updateStats_resetsStreakOnCorrect() {
        val trainer = AdaptiveTrainer()

        val result = trainer.updateStats(QuestionStats(attempts = 3, totalWrong = 2, wrongStreak = 2), wasCorrect = true)

        assertEquals(4, result.attempts)
        assertEquals(2, result.totalWrong)
        assertEquals(0, result.wrongStreak)
    }

    @Test
    fun pickQuestion_prefersOftenMissedQuestion() {
        val trainer = AdaptiveTrainer(Random(123))
        val easy = TrainingQuestion("easy", TrainingMode.INTERVAL, "", "", listOf("A"), "A")
        val hard = TrainingQuestion("hard", TrainingMode.INTERVAL, "", "", listOf("A"), "A")
        val stats = mapOf(
            "easy" to QuestionStats(attempts = 10, totalWrong = 0, wrongStreak = 0),
            "hard" to QuestionStats(attempts = 10, totalWrong = 8, wrongStreak = 3)
        )

        var hardHits = 0
        repeat(200) {
            if (trainer.pickQuestion(listOf(easy, hard), stats).id == "hard") hardHits++
        }

        assertTrue(hardHits > 120)
    }
}