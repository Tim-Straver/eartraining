package com.example.eartraining

import android.media.MediaPlayer
import android.os.Bundle
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
        val questions = StarterQuestionBank.allQuestions.filter { it.mode == selectedMode }
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

        feedbackLabel.text = if (correct) {
            getString(R.string.correct)
        } else {
            getString(R.string.incorrect, question.correctAnswer)
        }

        progressLabel.text = getString(
            R.string.progress_label,
            newStats.attempts,
            newStats.totalWrong,
            newStats.wrongStreak
        )
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }
}
