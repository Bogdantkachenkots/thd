package me.hufman.androidautoidrive.carapp.carinfo.views

import android.os.Handler
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import io.bimmergestalt.idriveconnectkit.rhmi.FocusCallback
import io.bimmergestalt.idriveconnectkit.rhmi.RHMIComponent
import io.bimmergestalt.idriveconnectkit.rhmi.RHMIModel
import io.bimmergestalt.idriveconnectkit.rhmi.RHMIState
import me.hufman.androidautoidrive.CarInformation
import me.hufman.androidautoidrive.carapp.L
import me.hufman.androidautoidrive.carapp.carinfo.CarDetailedInfo

/**
 * This state shows detailed car information as part of ReadoutApp
 * ReadoutApp's EntryButton is hardcoded to a specific RHMIState
 * so we must take whatever we are given
 */
class CarDetailedView(val state: RHMIState, val carInfo: CarInformation, val handler: Handler) {

	private val list = state.componentsList.filterIsInstance<RHMIComponent.List>().first()

	private val info = CarDetailedInfo(carInfo)

	private val fields: List<List<LiveData<String>>> = listOf(
			listOf(info.engineTemp, info.tempExterior),
			listOf(info.oilTemp, info.tempInterior),
			listOf(info.accBatteryLevelLabel, info.tempExchanger),
			listOf(info.fuelLevelLabel, info.drivingGearLabel),
	)
	private val displayList = object: RHMIModel.RaListModel.RHMIListAdapter<List<LiveData<String>>>(2, fields) {
		override fun convertRow(index: Int, item: List<LiveData<String>>): Array<Any> {
			return item.map { cell ->
				cell.value ?: ""
			}.toTypedArray()
		}
	}
	private val fieldsList: List<LiveData<String>> = fields.flatten()
	private val observer = Observer<String> { scheduleRedraw() }
	private var dirty = false

	fun initWidgets() {
		state.getTextModel()?.asRaDataModel()?.value = L.CARINFO_TITLE
		list.setEnabled(true)
		list.setVisible(true)
		state.focusCallback = FocusCallback { visible ->
			if (visible) {
				onShow()
			} else {
				onHide()
			}
		}
	}

	private fun onShow() {
		fieldsList.forEach {
			it.observeForever(observer)
		}
	}
	private fun onHide() {
		fieldsList.forEach {
			it.removeObserver(observer)
		}
	}

	private val redraw = Runnable {
		dirty = false
		list.getModel()?.value = displayList
	}
	private fun scheduleRedraw() {
		if (!dirty) {
			handler.postDelayed(redraw, 300)
		}
		dirty = true
	}
}