package com.parkassist.pango

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

object TtsHelper {
    private const val TAG = "TtsHelper"
    private const val UTTERANCE_ID = "pango_alert"

    fun speak(context: Context, text: String, onDone: () -> Unit) {
        var tts: TextToSpeech? = null
        tts = TextToSpeech(context.applicationContext) { status ->
            val engine = tts
            if (status != TextToSpeech.SUCCESS || engine == null) {
                Log.e(TAG, "אתחול מנוע הקראה קולית נכשל")
                engine?.shutdown()
                onDone()
                return@TextToSpeech
            }

            val localeResult = engine.setLanguage(Locale("he", "IL"))
            if (localeResult == TextToSpeech.LANG_MISSING_DATA ||
                localeResult == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                Log.w(TAG, "קול עברי לא זמין במכשיר - משתמש בשפת ברירת המחדל של המערכת")
            }

            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}

                override fun onDone(utteranceId: String?) {
                    engine.shutdown()
                    onDone()
                }

                @Deprecated("Deprecated in Java, still required to override")
                override fun onError(utteranceId: String?) {
                    engine.shutdown()
                    onDone()
                }
            })

            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
        }
    }
}
