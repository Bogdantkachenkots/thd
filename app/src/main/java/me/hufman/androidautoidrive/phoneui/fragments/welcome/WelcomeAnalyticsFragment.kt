package me.hufman.androidautoidrive.phoneui.fragments.welcome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import me.hufman.androidautoidrive.BuildConfig
import me.hufman.androidautoidrive.R

class WelcomeAnalyticsFragment: Fragment() {
	companion object {
		fun isSupported(): Boolean {
			return BuildConfig.FLAVOR_analytics != "nonalytics"
		}
	}
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		return inflater.inflate(R.layout.fragment_welcome_analytics, container, false)
	}
}