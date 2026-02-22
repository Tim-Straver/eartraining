package com.example.eartraining

import android.content.Context

class StatsStore(context: Context) {
    private val prefs = context.getSharedPreferences("trainer_stats", Context.MODE_PRIVATE)

    fun load(): MutableMap<String, QuestionStats> {
        val encoded = prefs.getString(KEY_STATS, null) ?: return mutableMapOf()
        if (encoded.isBlank()) return mutableMapOf()

        return encoded
            .lineSequence()
            .mapNotNull { line ->
                val parts = line.split("|")
                if (parts.size != 4) return@mapNotNull null
                val id = parts[0]
                val attempts = parts[1].toIntOrNull() ?: return@mapNotNull null
                val totalWrong = parts[2].toIntOrNull() ?: return@mapNotNull null
                val wrongStreak = parts[3].toIntOrNull() ?: return@mapNotNull null
                id to QuestionStats(attempts, totalWrong, wrongStreak)
            }
            .toMap()
            .toMutableMap()
    }

    fun save(stats: Map<String, QuestionStats>) {
        val serialized = stats.entries.joinToString("\n") { (id, stat) ->
            "$id|${stat.attempts}|${stat.totalWrong}|${stat.wrongStreak}"
        }
        prefs.edit().putString(KEY_STATS, serialized).apply()
    }

    companion object {
        private const val KEY_STATS = "stats"
    }
}
