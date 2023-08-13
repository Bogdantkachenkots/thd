package me.hufman.androidautoidrive

import android.content.Context
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.android.core.SentryAndroid
import io.sentry.protocol.Message
import me.hufman.androidautoidrive.music.MusicAppInfo

object Analytics: AnalyticsProvider {
	override fun init(context: Context) {
		SentryAndroid.init(context) { options ->
			options.dsn = context.applicationInfo.metaData.getString("io.sentry.dsn")
			options.isEnableAutoSessionTracking = false
			options.enableAllAutoBreadcrumbs(false)
			options.isAttachServerName = false
			options.isAttachStacktrace = false
		}
	}

	override fun reportMusicAppProbe(appInfo: MusicAppInfo) {
		val event = SentryEvent().apply {
			message = Message().also { it.message = "Probed music app"}
			setTag("appId", appInfo.packageName)
			setTag("appName", appInfo.name)
			setTag("controllable", if (appInfo.controllable) "true" else "false")
			setTag("connectable", if (appInfo.connectable) "true" else "false")
			setTag("browseable", if (appInfo.browseable) "true" else "false")
			setTag("searchable", if (appInfo.searchable) "true" else "false")
			setTag("playsearchable", if (appInfo.playsearchable) "true" else "false")
			level = SentryLevel.DEBUG
		}
		Sentry.captureEvent(event)
	}

	override fun reportCarProbeFailure(port: Int, message: String?, throwable: Throwable?) {
		val event = SentryEvent().apply {
			this.message = Message().also { it.message = "Failed to probe car: $message" }
			setTag("port", port.toString())
			level = SentryLevel.WARNING
			throwable?.let { setThrowable(it) }
		}
		Sentry.captureEvent(event)
	}

	override fun reportCarProbeDiscovered(port: Int?, vehicleType: String?, hmiType: String?) {
		val event = SentryEvent().apply {
			message = Message().also { it.message = "Successfully probed to connect to car" }
			setTag("vehicleType", vehicleType ?: "")
			setTag("hmiType", hmiType ?: "")
			setTag("port", port.toString())
			level = SentryLevel.DEBUG
		}
		Sentry.captureEvent(event)
	}

	override fun reportCarCapabilities(capabilities: Map<String, String?>) {
		val event = SentryEvent().apply {
			message = Message().also { it.message = "Car capabilities" }
			level = SentryLevel.DEBUG
		}
		capabilities.keys.forEach { key ->
			val value = capabilities[key]
			if (value != null) {
				val keyName = key.replace(Regex("[^a-zA-Z0-9_]"), "_")
				event.setTag(keyName, value.toString())
			}
		}
		Sentry.captureEvent(event)
	}

}