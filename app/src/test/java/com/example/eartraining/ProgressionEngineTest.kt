package com.example.eartraining

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionEngineTest {
    @Test
    fun unlockedKeys_startsInCOnly() {
        val engine = ProgressionEngine()

        assertEquals(setOf(0), engine.unlockedKeys())
    }

    @Test
    fun unlockedKeys_expandsAfterConsistentMastery() {
        val engine = ProgressionEngine()

        repeat(14) {
            engine.record(ExerciseResult(0, ExerciseCategory.INTERVAL, "M3", wasCorrect = true))
        }

        assertTrue(engine.unlockedKeys().contains(7))
    }

    @Test
    fun adaptivePick_prioritizesWeakKey() {
        val engine = ProgressionEngine(Random(123))

        repeat(14) {
            engine.record(ExerciseResult(0, ExerciseCategory.INTERVAL, "M3", wasCorrect = true))
        }

        // Build strong profile in C and weak profile in G.
        repeat(40) {
            engine.record(ExerciseResult(0, ExerciseCategory.CHORD_TYPE, "Major", wasCorrect = true))
        }
        repeat(40) {
            engine.record(ExerciseResult(7, ExerciseCategory.CHORD_TYPE, "Major", wasCorrect = false))
        }

        var gSelections = 0
        repeat(200) {
            if (engine.pickAdaptiveKey() == 7) gSelections++
        }

        assertTrue(gSelections > 140)
    }

    @Test
    fun tracksPerformanceByDimension() {
        val engine = ProgressionEngine()
        engine.record(ExerciseResult(0, ExerciseCategory.INTERVAL, "P5", wasCorrect = false))
        engine.record(ExerciseResult(0, ExerciseCategory.CHORD_TYPE, "Minor", wasCorrect = true))
        engine.record(ExerciseResult(0, ExerciseCategory.PROGRESSION, "ii-V-I", wasCorrect = true))

        assertEquals(1, engine.intervalPerformance()["P5"]?.attempts)
        assertEquals(1, engine.chordPerformance()["Minor"]?.correct)
        assertEquals(1, engine.progressionPerformance()["ii-V-I"]?.correct)
    }
}
