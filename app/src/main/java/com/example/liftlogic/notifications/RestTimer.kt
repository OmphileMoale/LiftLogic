package com.example.liftlogic.notifications

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RestState(val remainingSec: Int = 0, val totalSec: Int = 0) {
    val running: Boolean get() = remainingSec > 0
}

/**
 * App-wide rest timer. It lives in the AppContainer (not in a screen), so it keeps
 * counting when the user leaves the workout screen, and alerts them when rest is over.
 */
class RestTimer(
    private val scope: CoroutineScope,
    private val onFinished: () -> Unit
) {
    private val _state = MutableStateFlow(RestState())
    val state: StateFlow<RestState> = _state.asStateFlow()

    private var job: Job? = null
    private var endsAt = 0L

    fun start(seconds: Int) {
        if (seconds <= 0) return
        endsAt = System.currentTimeMillis() + seconds * 1000L
        _state.value = RestState(seconds, seconds)
        run()
    }

    /** Adds or removes time (e.g. +15 s / −15 s buttons). */
    fun adjust(deltaSec: Int) {
        val current = _state.value
        if (!current.running) return
        endsAt += deltaSec * 1000L
        _state.value = current.copy(totalSec = (current.totalSec + deltaSec).coerceAtLeast(1))
        run()
    }

    fun skip() {
        job?.cancel()
        _state.value = RestState()
    }

    private fun run() {
        job?.cancel()
        job = scope.launch {
            while (true) {
                val left = ((endsAt - System.currentTimeMillis() + 999) / 1000).toInt()
                if (left <= 0) {
                    _state.value = RestState()
                    onFinished()
                    break
                }
                _state.value = _state.value.copy(remainingSec = left)
                delay(250)
            }
        }
    }
}
