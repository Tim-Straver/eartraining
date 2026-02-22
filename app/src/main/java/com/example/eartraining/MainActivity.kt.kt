package com.example.eartraining

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private val trainer = AdaptiveTrainer()
    private lateinit var statsStore: StatsStore
    private lateinit var stats: MutableMap<String, QuestionStats>

    private lateinit var modeSpinner: Spinner
    private lateinit var questionLabel: TextView
    private lateinit var answerGroup: RadioGroup
    private lateinit var feedbackLabel: TextView
    private lateinit var progressLabel: TextView

    private var currentQuestion: TrainingQuestion? = null
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statsStore = StatsStore(this)
        stats = statsStore.load()

        modeSpinner = findViewById(R.id.modeSpinner)
        questionLabel = findViewById(R.id.questionLabel)
        answerGroup = findViewById(R.id.answerGroup)
        feedbackLabel = findViewById(R.id.feedbackLabel)
        progressLabel = findViewById(R.id.progressLabel)

        val modeNames = TrainingMode.entries.map { it.displayName }
        modeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, modeNames)

        findViewById<Button>(R.id.newQuestionButton).setOnClickListener {
            loadNewQuestion()
        }

        findViewById<Button>(R.id.playAudioButton).setOnClickListener {
            playCurrentAudio()
        }

        findViewById<Button>(R.id.submitButton).setOnClickListener {
            submitAnswer()
        }

        loadNewQuestion()
    }

    private fun loadNewQuestion() {
        val selectedMode = TrainingMode.fromDisplayName(modeSpinner.selectedItem?.toString().orEmpty())
        val questions = when (selectedMode) {
            TrainingMode.CHORD_PROGRESSION, TrainingMode.CHORD_TYPE, TrainingMode.NOTE -> {
                val assetQuestions = AssetQuestionBank.questionsForMode(this, selectedMode)
                if (assetQuestions.isNotEmpty()) {
                    assetQuestions
                } else {
                    StarterQuestionBank.allQuestions.filter { it.mode == selectedMode }
                }
            }
            else -> StarterQuestionBank.allQuestions.filter { it.mode == selectedMode }
        }
        if (questions.isEmpty()) {
            questionLabel.text = getString(R.string.no_questions)
            return
        }

        currentQuestion = trainer.pickQuestion(questions, stats)
        val question = currentQuestion ?: return
        questionLabel.text = question.prompt
        feedbackLabel.text = ""
        answerGroup.removeAllViews()

        question.choices.forEachIndexed { index, choice ->
            val button = RadioButton(this).apply {
                id = 1000 + index
                text = choice
                textSize = 18f
            }
            answerGroup.addView(button)
        }

        val stat = stats[question.id] ?: QuestionStats()
        progressLabel.text = getString(
            R.string.progress_label,
            stat.attempts,
            stat.totalWrong,
            stat.wrongStreak
        )
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
            Toast.makeText(
                this,
                getString(R.string.missing_audio, question.audioResName),
                Toast.LENGTH_LONG
            ).show()
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

    private fun progressionChordNames(question: TrainingQuestion): String {
        if (question.mode != TrainingMode.CHORD_PROGRESSION || question.audioAssetSequence.isEmpty()) return ""
        return question.audioAssetSequence
            .map { assetPath ->
                assetPath.substringAfterLast('/').substringBeforeLast('.')
            }
            .joinToString("-")
    }

    private fun answerDisplay(question: TrainingQuestion): String {
        if (question.mode != TrainingMode.CHORD_PROGRESSION) return question.correctAnswer
        val chords = progressionChordNames(question)
        return if (chords.isBlank()) {
            question.correctAnswer
        } else {
            "${question.correctAnswer} ($chords)"
        }
    }

    private fun submitAnswer() {
        val question = currentQuestion ?: return
        val selectedId = answerGroup.checkedRadioButtonId
        if (selectedId == -1) {
            Toast.makeText(this, R.string.select_answer, Toast.LENGTH_SHORT).show()
            return
        }

        val selectedAnswer = findViewById<RadioButton>(selectedId).text.toString()
        val correct = selectedAnswer == question.correctAnswer
        val newStats = trainer.updateStats(stats[question.id], correct)
        stats[question.id] = newStats
        statsStore.save(stats)

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


        if (correct) {
            Handler(Looper.getMainLooper()).postDelayed({
                loadNewQuestion()
            }, 900)
        }
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }
}
