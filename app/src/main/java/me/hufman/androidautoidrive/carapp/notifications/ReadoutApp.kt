package me.hufman.androidautoidrive.carapp.notifications

import android.os.Handler
import com.google.gson.Gson
import de.bmw.idrive.BMWRemoting
import de.bmw.idrive.BMWRemotingServer
import de.bmw.idrive.BaseBMWRemotingClient
import io.bimmergestalt.idriveconnectkit.CDS
import io.bimmergestalt.idriveconnectkit.IDriveConnection
import io.bimmergestalt.idriveconnectkit.Utils.rhmi_setResourceCached
import io.bimmergestalt.idriveconnectkit.android.CarAppResources
import io.bimmergestalt.idriveconnectkit.android.IDriveConnectionStatus
import io.bimmergestalt.idriveconnectkit.android.security.SecurityAccess
import io.bimmergestalt.idriveconnectkit.rhmi.*
import me.hufman.androidautoidrive.CarInformation
import me.hufman.androidautoidrive.carapp.*
import me.hufman.androidautoidrive.carapp.carinfo.views.CarDetailedView

class ReadoutApp(val iDriveConnectionStatus: IDriveConnectionStatus, val securityAccess: SecurityAccess, carAppAssets: CarAppResources, handler: Handler) {
	val carConnection: BMWRemotingServer
	val carApp: RHMIApplication
	val infoState: CarDetailedView
	val readoutController: ReadoutController

	init {
		val cdsData = CDSDataProvider()
		val listener = ReadoutAppListener(cdsData)
		carConnection = IDriveConnection.getEtchConnection(iDriveConnectionStatus.host ?: "127.0.0.1", iDriveConnectionStatus.port ?: 8003, listener)
		val readoutCert = carAppAssets.getAppCertificate(iDriveConnectionStatus.brand ?: "")?.readBytes() as ByteArray
		val sas_challenge = carConnection.sas_certificate(readoutCert)
		val sas_login = securityAccess.signChallenge(challenge=sas_challenge)
		carConnection.sas_login(sas_login)

		// create the app in the car
		val rhmiHandle = carConnection.rhmi_create(null, BMWRemoting.RHMIMetaData("me.hufman.androidautoidrive.notification.readout", BMWRemoting.VersionInfo(0, 1, 0),
				"me.hufman.androidautoidrive.notification.readout", "me.hufman"))
		carConnection.rhmi_setResourceCached(rhmiHandle, BMWRemoting.RHMIResourceType.DESCRIPTION, carAppAssets.getUiDescription())
		// no icons or text, so sneaky
		carConnection.rhmi_initialize(rhmiHandle)

		carApp = RHMIApplicationSynchronized(RHMIApplicationIdempotent(RHMIApplicationEtch(carConnection, rhmiHandle)), carConnection)
		carApp.loadFromXML(carAppAssets.getUiDescription()?.readBytes() as ByteArray)
		this.readoutController = ReadoutController.build(carApp, "NotificationReadout")

		val destStateId = carApp.components.values.filterIsInstance<RHMIComponent.EntryButton>().first().getAction()?.asHMIAction()?.target!!
		this.infoState = CarDetailedView(carApp.states[destStateId] as RHMIState, CarInformation(), handler)

		initWidgets()

		// register for readout updates
		cdsData.setConnection(CDSConnectionEtch(carConnection))
		cdsData.subscriptions[CDS.HMI.TTS] = {
			val state = try {
				Gson().fromJson(it["TTSState"], TTSState::class.java)
			} catch (e: Exception) { null }
			if (state != null) {
				readoutController.onTTSEvent(state)
			}
		}
	}

	class ReadoutAppListener(val cdsEventHandler: CDSEventHandler): BaseBMWRemotingClient() {
		override fun cds_onPropertyChangedEvent(handle: Int?, ident: String?, propertyName: String?, propertyValue: String?) {
			cdsEventHandler.onPropertyChangedEvent(ident, propertyValue)
		}
	}

	fun initWidgets() {
		infoState.initWidgets()
	}

	fun disconnect() {
		try {
			IDriveConnection.disconnectEtchConnection(carConnection)
		} catch ( e: java.io.IOError) {
		} catch (e: RuntimeException) {}
	}
}