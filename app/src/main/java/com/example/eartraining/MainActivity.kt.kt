package com.example.eartraining

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.activity.OnBackPressedCallback
import kotlin.math.min

class MainActivity : AppCompatActivity() {
    private val trainer = AdaptiveTrainer()
    private lateinit var statsStore: StatsStore
    private lateinit var stats: MutableMap<String, QuestionStats>

    private lateinit var homeContainer: LinearLayout
    private lateinit var trainingContainer: LinearLayout
    private lateinit var questionLabel: TextView
    private lateinit var answerGroup: LinearLayout
    private lateinit var streakFireLabel: TextView
    private lateinit var streakLabel: TextView
    private lateinit var difficultyLabel: TextView
    private lateinit var nextQuestionButton: Button

    private var currentQuestion: TrainingQuestion? = null
    private var currentMode: TrainingMode = TrainingMode.CHORD_PROGRESSION
    private var mediaPlayer: MediaPlayer? = null
    private var currentStreak: Int = 0
    private val answerButtons: MutableList<Button> = mutableListOf()
    private var streakWiggleAnimator: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statsStore = StatsStore(this)
        stats = statsStore.load()

        homeContainer = findViewById(R.id.homeContainer)
        trainingContainer = findViewById(R.id.trainingContainer)
        questionLabel = findViewById(R.id.questionLabel)
        answerGroup = findViewById(R.id.answerGroup)
        streakFireLabel = findViewById(R.id.streakFireLabel)
        streakLabel = findViewById(R.id.streakLabel)
        difficultyLabel = findViewById(R.id.difficultyLabel)
        nextQuestionButton = findViewById(R.id.nextQuestionButton)

        findViewById<Button>(R.id.modeChordProgressionButton).setOnClickListener { startMode(TrainingMode.CHORD_PROGRESSION) }
        findViewById<Button>(R.id.modeIntervalButton).setOnClickListener { startMode(TrainingMode.INTERVAL) }
        findViewById<Button>(R.id.modeChordTypeButton).setOnClickListener { startMode(TrainingMode.CHORD_TYPE) }
        findViewById<Button>(R.id.playAudioButton).setOnClickListener { playCurrentAudio() }
        nextQuestionButton.setOnClickListener {
            if (nextQuestionButton.isEnabled) {
                loadNewQuestion()
            }
        }

        showHome()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (trainingContainer.visibility == LinearLayout.VISIBLE) {
                    showHome()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun showHome() {
        homeContainer.visibility = LinearLayout.VISIBLE
        trainingContainer.visibility = LinearLayout.GONE
        currentQuestion = null
        difficultyLabel.text = ""
        nextQuestionButton.isEnabled = false
    }

    private fun startMode(mode: TrainingMode) {
        currentMode = mode
        currentStreak = 0
        updateStreakLabel()
        updateDifficultyLabel()
        homeContainer.visibility = LinearLayout.GONE
        trainingContainer.visibility = LinearLayout.VISIBLE
        loadNewQuestion()
    }

    private fun loadNewQuestion() {
        val unlockedDifficulty = currentUnlockedDifficulty()
        updateDifficultyLabel(unlockedDifficulty)

        val questions = when (currentMode) {
            TrainingMode.CHORD_PROGRESSION -> {
                val assetQuestions = AssetQuestionBank.questionsForMode(this, currentMode)
                if (assetQuestions.isNotEmpty()) {
                    assetQuestions.filter { it.difficulty.toFloat() <= unlockedDifficulty }
                } else {
                    emptyList()
                }
            }
            TrainingMode.CHORD_TYPE -> {
                val assetQuestions = AssetQuestionBank.questionsForMode(this, currentMode)
                if (assetQuestions.isNotEmpty()) {
                    assetQuestions.filter { it.difficulty.toFloat() <= unlockedDifficulty }
                } else {
                    StarterQuestionBank.allQuestions.filter { it.mode == currentMode }
                }
            }
            TrainingMode.INTERVAL -> {
                val assetQuestions = AssetQuestionBank.questionsForMode(this, currentMode)
                if (assetQuestions.isNotEmpty()) {
                    assetQuestions.filter { it.difficulty.toFloat() <= unlockedDifficulty }
                } else {
                    StarterQuestionBank.allQuestions.filter { it.mode == currentMode }
                }
            }
            TrainingMode.NOTE -> {
                val assetQuestions = AssetQuestionBank.questionsForMode(this, currentMode)
                if (assetQuestions.isNotEmpty()) assetQuestions
                else StarterQuestionBank.allQuestions.filter { it.mode == currentMode }
            }
            else -> StarterQuestionBank.allQuestions.filter { it.mode == currentMode }
        }

        if (questions.isEmpty()) {
            questionLabel.text = getString(R.string.no_questions)
            answerGroup.removeAllViews()
            nextQuestionButton.isEnabled = false
            return
        }

        currentQuestion = trainer.pickQuestion(questions, stats)
        val question = currentQuestion ?: return
        questionLabel.text = question.prompt
        answerGroup.removeAllViews()
        answerButtons.clear()
        nextQuestionButton.isEnabled = false

        val displayedChoices = normalizedChoices(question)
        displayedChoices.forEach { choice ->
            val button = Button(this).apply {
                text = choice
                textSize = 26f
                minHeight = 160
                isAllCaps = false
                tag = choice
                setOnClickListener {
                    if (!nextQuestionButton.isEnabled) {
                        submitAnswer(choice, this)
                    }
                }
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 12 }
            answerGroup.addView(button, params)
            answerButtons.add(button)
        }

        if (currentMode.shouldAutoPlayOnQuestionLoad()) {
            playCurrentAudio()
        }
    }

    private fun TrainingMode.shouldAutoPlayOnQuestionLoad(): Boolean {
        return this == TrainingMode.CHORD_PROGRESSION || this == TrainingMode.CHORD_TYPE || this == TrainingMode.INTERVAL
    }

    private fun normalizedChoices(question: TrainingQuestion): List<String> {
        val correct = question.correctAnswer
        val candidates = mutableListOf<String>()
        candidates.addAll(question.choices.filter { it != correct })

        if (candidates.size < 2) {
            val fallback = StarterQuestionBank.allQuestions
                .filter { it.mode == question.mode }
                .flatMap { it.choices }
                .distinct()
                .filter { it != correct && !candidates.contains(it) }
            candidates.addAll(fallback)
        }

        if (candidates.size < 2) {
            val globalFallback = StarterQuestionBank.allQuestions
                .flatMap { it.choices }
                .distinct()
                .filter { it != correct && !candidates.contains(it) }
            candidates.addAll(globalFallback)
        }

        val distractors = candidates.take(2)
        return (distractors + correct).shuffled()
    }

    private fun submitAnswer(selectedAnswer: String, selectedButton: Button) {
        val question = currentQuestion ?: return
        val correct = selectedAnswer == question.correctAnswer
        val newStats = trainer.updateStats(stats[question.id], correct)
        stats[question.id] = newStats
        statsStore.save(stats)

        currentStreak = if (correct) currentStreak + 1 else 0
        updateStreakLabel()
        updateDifficultyLabel()

        highlightAnswers(selectedButton, question.correctAnswer, correct)

        nextQuestionButton.isEnabled = true
    }

    private fun highlightAnswers(selectedButton: Button, correctAnswer: String, isSelectedCorrect: Boolean) {
        val correctColor = getColor(android.R.color.holo_green_dark)
        val wrongColor = getColor(android.R.color.holo_red_dark)

        answerButtons.forEach { button ->
            button.isClickable = false
            button.isFocusable = false
        }

        val correctButton = answerButtons.firstOrNull { (it.tag as? String) == correctAnswer }
        correctButton?.backgroundTintList = ColorStateList.valueOf(correctColor)

        if (!isSelectedCorrect) {
            selectedButton.backgroundTintList = ColorStateList.valueOf(wrongColor)
        }
    }

    private fun updateStreakLabel() {
        val shouldShowStreak = currentStreak > 1
        streakFireLabel.visibility = if (shouldShowStreak) View.VISIBLE else View.GONE
        streakLabel.visibility = if (shouldShowStreak) View.VISIBLE else View.GONE
        streakLabel.text = "${currentStreak} streak"
        updateStreakWiggle()
    }

    private fun updateStreakWiggle() {
        streakWiggleAnimator?.cancel()
        streakFireLabel.rotation = 0f

        if (currentStreak <= 1) return

        val clampedStreak = min(currentStreak, 30)
        val amplitudeDegrees = (2f + clampedStreak * 0.3f).coerceAtMost(12f)
        val durationMs = (520L - clampedStreak * 12L).coerceAtLeast(160L)

        streakFireLabel.post {
            streakFireLabel.pivotX = streakFireLabel.width / 2f
            streakFireLabel.pivotY = streakFireLabel.height.toFloat()
            streakWiggleAnimator = ObjectAnimator.ofFloat(streakFireLabel, "rotation", -amplitudeDegrees, amplitudeDegrees).apply {
                duration = durationMs
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                start()
            }
        }
    }

    private fun playCurrentAudio() {
        val question = currentQuestion ?: return
        if (question.audioAssetSequence.isNotEmpty()) {
            playAssetSequence(question.audioAssetSequence)
            return
        }
        if (question.audioAssetPath != null) {
            playAssetAudio(question.audioAssetPath)
            return
        }

        val resId = resources.getIdentifier(question.audioResName, "raw", packageName)
        if (resId == 0) {
            Toast.makeText(this, getString(R.string.missing_audio, question.audioResName), Toast.LENGTH_LONG).show()
            return
        }

        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, resId)
        mediaPlayer?.setOnCompletionListener { mp -> mp.release(); mediaPlayer = null }
        mediaPlayer?.start()
    }

    private fun playAssetAudio(assetPath: String) {
        val afd = try {
            assets.openFd(assetPath)
        } catch (_: Exception) {
            Toast.makeText(this, getString(R.string.missing_asset_audio, assetPath), Toast.LENGTH_LONG).show()
            null
        } ?: return

        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            prepare()
            setOnCompletionListener { mp -> mp.release(); mediaPlayer = null }
            start()
        }
    }

    private fun playAssetSequence(assetPaths: List<String>) {
        val queue = ArrayDeque(assetPaths)

        fun playNext() {
            if (queue.isEmpty()) {
                mediaPlayer?.release()
                mediaPlayer = null
                return
            }
            val nextPath = queue.removeFirst()

            val afd = try {
                assets.openFd(nextPath)
            } catch (_: Exception) {
                Toast.makeText(this, getString(R.string.missing_asset_audio, nextPath), Toast.LENGTH_LONG).show()
                null
            } ?: return

            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                prepare()
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                    playNext()
                }
                start()
            }
        }

        playNext()
    }

    private fun progressionUnlockedDifficulty(): Float {
        return adaptiveUnlockedDifficulty(
            idPrefix = "asset_prog_",
            correctAnswersPerLevel = 5,
            maxDifficulty = AssetQuestionBank.maxProgressionDifficulty()
        )
    }

    private fun chordTypeUnlockedDifficulty(): Float {
        return adaptiveUnlockedDifficulty(
            idPrefix = "asset_chords_",
            correctAnswersPerLevel = 6,
            maxDifficulty = AssetQuestionBank.maxChordTypeDifficulty()
        )
    }

    private fun intervalUnlockedDifficulty(): Float {
        return adaptiveUnlockedDifficulty(
            idPrefix = "asset_interval_",
            correctAnswersPerLevel = 6,
            maxDifficulty = AssetQuestionBank.maxIntervalDifficulty()
        )
    }

    private fun adaptiveUnlockedDifficulty(
        idPrefix: String,
        correctAnswersPerLevel: Int,
        maxDifficulty: Int
    ): Float {
        val modeStats = stats.filterKeys { id -> id.startsWith(idPrefix) }.values
        val totalCorrect = modeStats.sumOf { st -> st.attempts - st.totalWrong }
        val totalWrong = modeStats.sumOf { st -> st.totalWrong }

        // Positive streaks move difficulty up quickly. Repeated mistakes push it down.
        val streakBonus = modeStats.sumOf { st -> st.correctStreak }
        val streakPenalty = modeStats.sumOf { st -> st.wrongStreak * 2 }

        val performanceScore = (totalCorrect * 2) + streakBonus - (totalWrong * 3) - streakPenalty
        val normalizedScore = performanceScore.coerceAtLeast(0)
        val baseLevel = 1f + (normalizedScore.toFloat() / correctAnswersPerLevel.toFloat())
        return baseLevel.coerceIn(1f, maxDifficulty.toFloat())
    }

    private fun currentUnlockedDifficulty(): Float {
        return when (currentMode) {
            TrainingMode.CHORD_PROGRESSION -> progressionUnlockedDifficulty()
            TrainingMode.CHORD_TYPE -> chordTypeUnlockedDifficulty()
            TrainingMode.INTERVAL -> intervalUnlockedDifficulty()
            else -> 1f
        }
    }

    private fun updateDifficultyLabel(level: Float = currentUnlockedDifficulty()) {
        difficultyLabel.text = getString(R.string.difficulty_label, level)
    }

    override fun onDestroy() {
        streakWiggleAnimator?.cancel()
        streakWiggleAnimator = null
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }
}
