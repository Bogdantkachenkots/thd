package me.hufman.androidautoidrive.maps

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*

class AndroidLocationProvider(val locationProvider: FusedLocationProviderClient): CarLocationProvider() {
	companion object {
		fun getInstance(context: Context): AndroidLocationProvider {
			return AndroidLocationProvider(LocationServices.getFusedLocationProviderClient(context))
		}
	}
	private val locationCallback = LocationCallbackImpl()

	inner class LocationCallbackImpl: LocationCallback() {
		override fun onLocationResult(result: LocationResult) {
			result.lastLocation?.let { onLocationUpdate(it) }
		}
	}

	@SuppressLint("MissingPermission")
	override fun start() {
		val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000).apply {
			setMinUpdateIntervalMillis(500)
		}.build()

		locationProvider.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
	}

	private fun onLocationUpdate(location: Location) {
		currentLocation = location
		sendCallback()
	}

	override fun stop() {
		locationProvider.removeLocationUpdates(locationCallback)
	}
}