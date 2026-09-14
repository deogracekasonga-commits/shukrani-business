package com.shukranibusiness.app.util

import android.os.Handler
import android.os.Looper
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.TextView

/**
 * Fait défiler une liste de textes dans une TextView, en fondu enchaîné (apparaît, reste
 * affiché, disparaît, passe au suivant). Démarrer avec [start], toujours arrêter avec [stop]
 * (dans onDestroy) pour ne pas laisser de callbacks actifs sur une vue détruite.
 */
class SloganRotator(
    private val textView: TextView,
    private val slogans: List<String>,
    private val displayMillis: Long = 4000L,
    private val fadeMillis: Long = 600L
) {
    private val handler = Handler(Looper.getMainLooper())
    private var index = 0
    private var running = false

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            val fadeOut = AlphaAnimation(1f, 0f).apply {
                duration = fadeMillis
                fillAfter = true
                setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) = Unit
                    override fun onAnimationRepeat(animation: Animation?) = Unit
                    override fun onAnimationEnd(animation: Animation?) {
                        if (!running) return
                        index = (index + 1) % slogans.size
                        textView.text = slogans[index]
                        textView.startAnimation(
                            AlphaAnimation(0f, 1f).apply {
                                duration = fadeMillis
                                fillAfter = true
                            }
                        )
                    }
                })
            }
            textView.startAnimation(fadeOut)
            handler.postDelayed(this, displayMillis + fadeMillis * 2)
        }
    }

    fun start() {
        if (running || slogans.isEmpty()) return
        running = true
        index = 0
        textView.text = slogans[0]
        textView.alpha = 1f
        handler.postDelayed(tick, displayMillis)
    }

    fun stop() {
        running = false
        handler.removeCallbacksAndMessages(null)
    }
}
