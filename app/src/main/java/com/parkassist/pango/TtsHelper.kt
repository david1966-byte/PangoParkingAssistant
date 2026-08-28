package com.parkassist.pango

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

object TtsHelper {
    private const val TAG = "TtsHelper"
    private const val UTTERANCE_ID = "pango_alert"

    private var tts: TextToSpeech? = null

    @Volatile
    private var isReady = false
    private val pendingTexts = mutableListOf<String>()

    fun init(context: Context) {
        if (tts != null) return

        tts = TextToSpeech(context.applicationContext) { status ->
            val engine = tts
            if (status != TextToSpeech.SUCCESS || engine == null) {
                Log.e(TAG, "אתחול מנוע הקראה קולית נכשל")
                return@TextToSpeech
            }

            val localeResult = engine.setLanguage(Locale("he", "IL"))
            if (localeResult == TextToSpeech.LANG_MISSING_DATA ||
                localeResult == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                Log.w(TAG, "קול עברי לא זמין במכשיר - משתמש בשפת ברירת המחדל")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                engine.setAudioAttributes(attributes)
            }

            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {}

                @Deprecated("Deprecated in Java, still required to override")
                override fun onError(utteranceId: String?) {}
            })

            isReady = true
            synchronized(pendingTexts) {
                pendingTexts.forEach { text ->
                    engine.speak(text, TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID)
                }
                pendingTexts.clear()
            }
        }
    }

    fun speak(context: Context, text: String) {
        val engine = tts
        if (!isReady || engine == null) {
            synchronized(pendingTexts) { pendingTexts.add(text) }
            init(context)
            return
        }
        engine.speak(text, TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID)
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        isReady = false
        synchronized(pendingTexts) { pendingTexts.clear() }
    }
}
