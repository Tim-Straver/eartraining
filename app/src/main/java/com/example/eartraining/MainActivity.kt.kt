package com.example.eartraining

import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.activity.OnBackPressedCallback

class MainActivity : AppCompatActivity() {
    private val trainer = AdaptiveTrainer()
    private lateinit var statsStore: StatsStore
    private lateinit var stats: MutableMap<String, QuestionStats>

    private lateinit var homeContainer: LinearLayout
    private lateinit var trainingContainer: LinearLayout
    private lateinit var modeTitleLabel: TextView
    private lateinit var questionLabel: TextView
    private lateinit var answerGroup: LinearLayout
    private lateinit var feedbackLabel: TextView
    private lateinit var progressLabel: TextView
    private lateinit var streakLabel: TextView
    private lateinit var nextQuestionButton: Button

    private var currentQuestion: TrainingQuestion? = null
    private var currentMode: TrainingMode = TrainingMode.CHORD_PROGRESSION
    private var mediaPlayer: MediaPlayer? = null
    private var currentStreak: Int = 0
    private val answerButtons: MutableList<Button> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statsStore = StatsStore(this)
        stats = statsStore.load()

        homeContainer = findViewById(R.id.homeContainer)
        trainingContainer = findViewById(R.id.trainingContainer)
        modeTitleLabel = findViewById(R.id.modeTitleLabel)
        questionLabel = findViewById(R.id.questionLabel)
        answerGroup = findViewById(R.id.answerGroup)
        feedbackLabel = findViewById(R.id.feedbackLabel)
        progressLabel = findViewById(R.id.progressLabel)
        streakLabel = findViewById(R.id.streakLabel)
        nextQuestionButton = findViewById(R.id.nextQuestionButton)

        findViewById<Button>(R.id.modeChordProgressionButton).setOnClickListener { startMode(TrainingMode.CHORD_PROGRESSION) }
        findViewById<Button>(R.id.modeIntervalButton).setOnClickListener { startMode(TrainingMode.INTERVAL) }
        findViewById<Button>(R.id.modeChordTypeButton).setOnClickListener { startMode(TrainingMode.CHORD_TYPE) }
        findViewById<Button>(R.id.backToModesButton).setOnClickListener { showHome() }
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
        feedbackLabel.text = ""
        nextQuestionButton.isEnabled = false
    }

    private fun startMode(mode: TrainingMode) {
        currentMode = mode
        modeTitleLabel.text = mode.displayName
        currentStreak = 0
        updateStreakLabel()
        homeContainer.visibility = LinearLayout.GONE
        trainingContainer.visibility = LinearLayout.VISIBLE
        loadNewQuestion()
    }

    private fun loadNewQuestion() {
        val questions = when (currentMode) {
            TrainingMode.CHORD_PROGRESSION -> {
                val assetQuestions = AssetQuestionBank.questionsForMode(this, currentMode)
                if (assetQuestions.isNotEmpty()) {
                    val unlockedDifficulty = progressionUnlockedDifficulty()
                    assetQuestions.filter { it.difficulty <= unlockedDifficulty }
                } else {
                    StarterQuestionBank.allQuestions.filter { it.mode == currentMode }
                }
            }
            TrainingMode.CHORD_TYPE, TrainingMode.NOTE -> {
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
        feedbackLabel.text = ""
        answerGroup.removeAllViews()
        answerButtons.clear()
        nextQuestionButton.isEnabled = false

        question.choices.forEach { choice ->
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

        val stat = stats[question.id] ?: QuestionStats()
        progressLabel.text = getString(R.string.progress_label, stat.attempts, stat.totalWrong, stat.wrongStreak)

        if (currentMode == TrainingMode.CHORD_PROGRESSION) {
            playCurrentAudio()
        }
    }

    private fun submitAnswer(selectedAnswer: String, selectedButton: Button) {
        val question = currentQuestion ?: return
        val correct = selectedAnswer == question.correctAnswer
        val newStats = trainer.updateStats(stats[question.id], correct)
        stats[question.id] = newStats
        statsStore.save(stats)

        currentStreak = if (correct) currentStreak + 1 else 0
        updateStreakLabel()

        highlightAnswers(selectedButton, question.correctAnswer, correct)

        val shownAnswer = answerDisplay(question)
        feedbackLabel.text = if (correct) {
            "${getString(R.string.correct)} $shownAnswer"
        } else {
            getString(R.string.incorrect, shownAnswer)
        }

        progressLabel.text = getString(
            R.string.progress_label,
            newStats.attempts,
            newStats.totalWrong,
            newStats.wrongStreak
        )

        nextQuestionButton.isEnabled = true
    }

    private fun highlightAnswers(selectedButton: Button, correctAnswer: String, isSelectedCorrect: Boolean) {
        val correctColor = getColor(android.R.color.holo_green_dark)
        val wrongColor = getColor(android.R.color.holo_red_dark)

        answerButtons.forEach { button ->
            button.isClickable = false
            button.isFocusable = false
        }

        if (!isSelectedCorrect) {
            selectedButton.setBackgroundColor(wrongColor)
        }

        answerButtons.firstOrNull { (it.tag as? String) == correctAnswer }?.setBackgroundColor(correctColor)
    }

    private fun updateStreakLabel() {
        streakLabel.text = getString(R.string.streak_label, currentStreak)
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

    private fun progressionUnlockedDifficulty(): Int {
        val progressionStats = stats.filterKeys { id -> id.startsWith("asset_prog_") }.values
        val totalCorrect = progressionStats.sumOf { st -> st.attempts - st.totalWrong }
        val baseLevel = 1 + (totalCorrect / 5)
        return baseLevel.coerceIn(1, AssetQuestionBank.maxProgressionDifficulty())
    }

    private fun progressionChordNames(question: TrainingQuestion): String {
        if (question.mode != TrainingMode.CHORD_PROGRESSION || question.audioAssetSequence.isEmpty()) return ""
        return question.audioAssetSequence
            .map { assetPath -> assetPath.substringAfterLast('/').substringBeforeLast('.') }
            .joinToString("-")
    }

    private fun answerDisplay(question: TrainingQuestion): String {
        if (question.mode != TrainingMode.CHORD_PROGRESSION) return question.correctAnswer
        val chords = progressionChordNames(question)
        return if (chords.isBlank()) question.correctAnswer else "${question.correctAnswer} ($chords)"
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }
}
