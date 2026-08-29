package com.parkassist.pango

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

object TtsHelper {
    private const val TAG = "TtsHelper"
    private const val UTTERANCE_ID = "pango_alert"

    private var tts: TextToSpeech? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null

    @Volatile
    private var isReady = false
    private val pendingTexts = mutableListOf<String>()

    fun init(context: Context) {
        if (tts != null) return
        audioManager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        tts = TextToSpeech(context.applicationContext) { status ->
            val engine = tts
            if (status != TextToSpeech.SUCCESS || engine == null) {
                Log.e(TAG, "אתחול מנוע הקראה קולית נכשל - סטטוס: $status")
                tts = null
                isReady = false
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
                override fun onDone(utteranceId: String?) {
                    abandonAudioFocus()
                }

                @Deprecated("Deprecated in Java, still required to override")
                override fun onError(utteranceId: String?) {
                    abandonAudioFocus()
                }
            })

            isReady = true
            synchronized(pendingTexts) {
                pendingTexts.forEach { text -> speakInternal(engine, text) }
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
        speakInternal(engine, text)
    }

    private fun speakInternal(engine: TextToSpeech, text: String) {
        requestAudioFocus()
        engine.speak(text, TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID)
    }

    private fun requestAudioFocus() {
        val manager = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attributes)
                .build()
            focusRequest = request
            manager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            manager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        }
    }

    private fun abandonAudioFocus() {
        val manager = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { manager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            manager.abandonAudioFocus(null)
        }
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        isReady = false
        synchronized(pendingTexts) { pendingTexts.clear() }
    }
}
