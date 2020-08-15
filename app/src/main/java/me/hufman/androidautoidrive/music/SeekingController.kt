package me.hufman.androidautoidrive.music

import android.os.Handler

class SeekingController(val handler: Handler, val controller: MusicController) {
	companion object {
		// how many milliseconds per half-second interval to seek, at each button-hold time threshold
		private val SEEK_THRESHOLDS = listOf(
				0 to 4000,
				2000 to 7000,
				5000 to 13000,
				8000 to 30000
		)
	}
	var timeProvider: () -> Long = { System.currentTimeMillis() }   // for unit tests

	private var startedSeekingTime: Long = 0    // determine how long the user has been holding the seek button
	private var seekingDirectionForward = true
	private val seekingRunnable = object : Runnable {
		override fun run() {
			if (startedSeekingTime > 0) {
				val holdTime = timeProvider() - startedSeekingTime
				holdSeek(holdTime)
				if (startedSeekingTime > 0) {
					handler.postDelayed(this, 300)
				}
			}
		}
	}


	fun startRewind() {
		startedSeekingTime = timeProvider()
		seekingDirectionForward = false
		seekingRunnable.run()
	}
	fun startFastForward() {
		startedSeekingTime = timeProvider()
		seekingDirectionForward = true
		seekingRunnable.run()
	}
	fun stopSeeking() {
		startedSeekingTime = 0
		controller.play()
	}

	fun holdSeek(holdTime: Long) {
		val seekTime = SEEK_THRESHOLDS.lastOrNull { holdTime >= it.first }?.second ?: 2000
		val delta = if (seekingDirectionForward) { 1 } else { -1 } * seekTime
		seek(delta)
	}

	fun seek(delta: Int) {
		val curPos = controller.getPlaybackPosition().getPosition()
		val newPos = curPos + delta
		// handle seeking past beginning of song
		if (newPos < 0) {
			controller.skipToPrevious()
			startedSeekingTime = 0  // cancel seeking, because skipToPrevious starts at the beginning of the previous song
		} else {
			controller.seekTo(newPos)
		}
	}
}