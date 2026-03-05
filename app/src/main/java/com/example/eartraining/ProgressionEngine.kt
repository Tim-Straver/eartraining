package com.example.eartraining

import kotlin.math.max
import kotlin.random.Random

enum class ExerciseCategory {
    INTERVAL,
    CHORD_TYPE,
    PROGRESSION
}

data class PerformanceSnapshot(
    val attempts: Int = 0,
    val correct: Int = 0,
    val streak: Int = 0,
    val score: Double = 50.0
) {
    val accuracy: Double
        get() = if (attempts == 0) 0.0 else correct.toDouble() / attempts.toDouble()
}

data class ExerciseResult(
    val key: Int,
    val category: ExerciseCategory,
    val subtype: String,
    val wasCorrect: Boolean
)

/**
 * Logic-only progression engine:
 * - Starts in C only.
 * - Unlocks keys following a circle-of-fifths style sequence.
 * - Tracks performance by key + category + subtype.
 * - Increases weak-key frequency while reducing mastered-key repetition.
 */
class ProgressionEngine(
    private val random: Random = Random.Default,
    private val keyOrder: List<Int> = listOf(0, 7, 5, 2, 10, 9, 3, 4, 8, 11, 6, 1)
) {
    private val keyStats = mutableMapOf<Int, PerformanceSnapshot>()
    private val intervalStats = mutableMapOf<String, PerformanceSnapshot>()
    private val chordStats = mutableMapOf<String, PerformanceSnapshot>()
    private val progressionStats = mutableMapOf<String, PerformanceSnapshot>()

    fun unlockedKeys(): Set<Int> {
        val count = unlockedKeyCount()
        return keyOrder.take(count).toSet()
    }

    fun record(result: ExerciseResult) {
        require(result.key in keyOrder) { "Unknown key ${result.key}" }

        keyStats[result.key] = updated(keyStats[result.key], result.wasCorrect)

        when (result.category) {
            ExerciseCategory.INTERVAL -> intervalStats[result.subtype] = updated(intervalStats[result.subtype], result.wasCorrect)
            ExerciseCategory.CHORD_TYPE -> chordStats[result.subtype] = updated(chordStats[result.subtype], result.wasCorrect)
            ExerciseCategory.PROGRESSION -> progressionStats[result.subtype] = updated(progressionStats[result.subtype], result.wasCorrect)
        }
    }

    fun keyPerformance(): Map<Int, PerformanceSnapshot> = keyStats.toMap()

    fun intervalPerformance(): Map<String, PerformanceSnapshot> = intervalStats.toMap()

    fun chordPerformance(): Map<String, PerformanceSnapshot> = chordStats.toMap()

    fun progressionPerformance(): Map<String, PerformanceSnapshot> = progressionStats.toMap()

    fun pickAdaptiveKey(): Int {
        val unlocked = unlockedKeys().toList()
        val weighted = unlocked.map { key -> key to keyWeight(key) }
        val totalWeight = weighted.sumOf { it.second }

        var draw = random.nextDouble(totalWeight)
        for ((key, weight) in weighted) {
            draw -= weight
            if (draw <= 0) return key
        }
        return unlocked.last()
    }

    private fun keyWeight(key: Int): Double {
        val stats = keyStats[key] ?: PerformanceSnapshot()
        val attempts = stats.attempts
        val weakness = 1.0 - stats.accuracy
        val inexperience = 1.0 / (1 + attempts)
        val masteryPenalty = if (stats.accuracy >= 0.9 && attempts >= 20) 0.35 else 1.0

        return max(0.1, (0.65 * weakness + 0.35 * inexperience) * masteryPenalty)
    }

    private fun unlockedKeyCount(): Int {
        if (keyOrder.isEmpty()) return 0

        var unlocked = 1 // always start from C
        while (unlocked < keyOrder.size) {
            val opened = keyOrder.take(unlocked)
            val aggregateAttempts = opened.sumOf { keyStats[it]?.attempts ?: 0 }
            if (aggregateAttempts < unlocked * 12) break

            val aggregateScore = opened.map { keyStats[it]?.score ?: 50.0 }.average()
            val aggregateAccuracy = opened.map {
                val stat = keyStats[it] ?: PerformanceSnapshot()
                if (stat.attempts == 0) 0.0 else stat.accuracy
            }.average()

            // Unlock only when currently unlocked keys are solidly mastered.
            if (aggregateScore >= 72.0 && aggregateAccuracy >= 0.85) {
                unlocked += 1
            } else {
                break
            }
        }

        return unlocked
    }

    private fun updated(previous: PerformanceSnapshot?, correct: Boolean): PerformanceSnapshot {
        val old = previous ?: PerformanceSnapshot()
        val nextAttempts = old.attempts + 1
        val nextCorrect = old.correct + if (correct) 1 else 0
        val nextStreak = if (correct) old.streak + 1 else 0

        val delta = if (correct) 2.2 else -3.0
        val streakBonus = if (correct && nextStreak >= 3) 0.4 else 0.0
        val nextScore = (old.score + delta + streakBonus).coerceIn(0.0, 100.0)

        return PerformanceSnapshot(
            attempts = nextAttempts,
            correct = nextCorrect,
            streak = nextStreak,
            score = nextScore
        )
    }
}
