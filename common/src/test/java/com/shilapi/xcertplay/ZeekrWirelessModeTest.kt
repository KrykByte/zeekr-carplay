package com.shilapi.xcertplay

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pManager
import android.os.Looper
import com.shilapi.xcertplay.media.AndroidMediaSink
import com.shilapi.xcertplay.orchestration.CarPlayController
import com.shilapi.xcertplay.orchestration.CarPlayTransport
import java.time.Duration
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowWifiP2pManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], shadows = [ZeekrWirelessModeTest.Radio::class, ZeekrWirelessModeTest.Channel::class])
class ZeekrWirelessModeTest {
    private val app get() = RuntimeEnvironment.getApplication()
    private val radio get() = shadowOf(app.getSystemService(WifiP2pManager::class.java)) as Radio
    private var previousPackage = ""
    private var finishStop: (() -> Unit)? = null
    private val ours = "DIRECT-owned-carplay"
    @Before fun reset() {
        previousPackage = app.applicationInfo.packageName
        app.applicationInfo.packageName = "com.openzeekr.carplay"
        CarPlayBackgroundSession.clear()
        for (name in listOf("zeekr_alpha", "diplay", "xcertplay_airplay", "carplay_wifi_p2p"))
            app.getSharedPreferences(name, 0).edit().clear().commit()
        app.getSharedPreferences("carplay_wifi_p2p", 0).edit().putString("owned_ssid", ours).commit()
        app.getSystemService(WifiManager::class.java).isWifiEnabled = true
    }
    @After fun close() {
        finishStop?.invoke()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(5))
        CarPlayBackgroundSession.clear()
        app.applicationInfo.packageName = previousPackage
        for (name in listOf("zeekr_alpha", "diplay", "xcertplay_airplay", "carplay_wifi_p2p"))
            app.getSharedPreferences(name, 0).edit().clear().commit()
    }
    private fun group(name: String = ours, owner: Boolean = true) = WifiP2pGroup().apply {
        shadowOf(this).setNetworkName(name)
        shadowOf(this).setIsGroupOwner(owner)
    }
    private fun release(): MutableList<OwnedP2pRelease.Result> = mutableListOf<OwnedP2pRelease.Result>().also { result ->
        OwnedP2pRelease.release(app, result::add)
    }
    private fun idle(ms: Long = 200) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    @Test fun removesOnlyOurGroupAndWaitsForItsDisappearance() {
        radio.group = group()
        val result = release()
        assertTrue(result.isEmpty())
        assertEquals(1, radio.removals)
        idle()
        assertEquals(listOf(OwnedP2pRelease.Result.CLEAR), result)
        assertTrue(app.getSystemService(WifiManager::class.java).isWifiEnabled)
    }
    @Test fun leavesZeekrTvGroupUntouched() {
        radio.group = group("DIRECT-ZeekrTV")
        assertEquals(listOf(OwnedP2pRelease.Result.OTHER_APP), release())
        assertEquals(0, radio.removals)
    }
    @Test fun matchingNameWithoutGroupOwnershipIsNotOurs() {
        radio.group = group(owner = false)
        assertEquals(listOf(OwnedP2pRelease.Result.OTHER_APP), release())
        assertEquals(0, radio.removals)
    }
    @Test fun anAcknowledgedRemovalThatLeavesTheGroupActiveIsNotSuccess() {
        radio.group = group()
        radio.keepGroup = true
        val result = release()
        idle(300)
        assertTrue(result.isEmpty())
        idle(4_000)
        assertEquals(listOf(OwnedP2pRelease.Result.FAILED), result)
        assertEquals(1, radio.removals)
    }
    @Test fun aReplacementGroupIsNotRemovedWhileWaitingForCleanup() {
        radio.group = group()
        val result = release()
        val tv = group("DIRECT-ZeekrTV")
        radio.group = tv
        idle()
        assertEquals(listOf(OwnedP2pRelease.Result.OTHER_APP), result)
        assertSame(tv, radio.group)
        assertEquals(1, radio.removals)
    }
    @Test fun unansweredRequestTimesOutAndLateCallbacksCannotRemoveAnything() {
        radio.group = group()
        radio.silent = true
        val result = release()
        idle(4_100)
        assertEquals(listOf(OwnedP2pRelease.Result.FAILED), result)
        radio.pending!!.onGroupInfoAvailable(radio.group)
        assertEquals(0, radio.removals)
        assertEquals(1, result.size)
    }
    @Test fun rejectedRemovalReportsFailure() {
        radio.group = group()
        radio.reject = true
        assertEquals(listOf(OwnedP2pRelease.Result.FAILED), release())
    }
    @Test fun absentGroupCompletesWithoutRemovingOtherResources() {
        assertEquals(listOf(OwnedP2pRelease.Result.CLEAR), release())
        assertEquals(0, radio.removals)
    }
    @Test fun disablingWaitsForSessionStopAndKeepsAutomaticWirelessOff() {
        val controller = mock(CarPlayController::class.java)
        `when`(controller.transport).thenReturn(CarPlayTransport.WIRELESS)
        val display = mock(CarPlaySessionDisplay::class.java)
        var stops = 0
        CarPlayBackgroundSession.store(controller, mock(AndroidMediaSink::class.java), 800, 480, Any(), display) { done ->
            stops++
            finishStop = { CarPlayBackgroundSession.clear(); done() }
        }
        radio.group = group()
        val result = mutableListOf<OwnedP2pRelease.Result>()
        ZeekrWirelessMode.disable(app, result::add)
        assertEquals(1, stops)
        assertTrue(ZeekrWirelessMode.disabled(app))
        assertTrue(ZeekrWirelessMode.busy)
        assertFalse(AirPlayPersistence.loadWirelessEnabled(app))
        assertEquals(DefaultConnectionMode.USB, DiPlayPreferences.defaultConnectionMode(app))
        assertEquals(0, radio.removals)
        finishStop!!.invoke(); finishStop = null
        idle(250)
        assertEquals(listOf(OwnedP2pRelease.Result.CLEAR), result)
        assertFalse(ZeekrWirelessMode.busy)
        assertTrue(ZeekrWirelessMode.disabled(app))
        ZeekrWirelessMode.enable(app)
        assertFalse(ZeekrWirelessMode.disabled(app))
    }
    @Test fun turningOffWirelessDoesNotStopAnActiveUsbSessionEvenWithStalePreferences() {
        val controller = mock(CarPlayController::class.java)
        `when`(controller.transport).thenReturn(CarPlayTransport.WIRED)
        var stops = 0
        AirPlayPersistence.saveWirelessEnabled(app, true)
        CarPlayBackgroundSession.store(controller, mock(AndroidMediaSink::class.java), 800, 480, Any(),
            mock(CarPlaySessionDisplay::class.java)) { stops++; it() }
        ZeekrWirelessMode.disable(app) {}
        idle()
        assertEquals(0, stops)
        assertSame(controller, CarPlayBackgroundSession.snapshot()!!.controller)
    }
    @Test fun missingSessionStopCallbackTimesOutWithoutLateGroupRemoval() {
        val controller = mock(CarPlayController::class.java)
        `when`(controller.transport).thenReturn(CarPlayTransport.WIRELESS)
        CarPlayBackgroundSession.store(controller, mock(AndroidMediaSink::class.java), 800, 480, Any(),
            mock(CarPlaySessionDisplay::class.java)) { done ->
            finishStop = { CarPlayBackgroundSession.clear(); done() }
        }
        radio.group = group()
        val result = mutableListOf<OwnedP2pRelease.Result>()
        ZeekrWirelessMode.disable(app, result::add)
        idle(12_100)
        assertEquals(listOf(OwnedP2pRelease.Result.FAILED), result)
        assertFalse(ZeekrWirelessMode.busy)
        finishStop!!.invoke(); finishStop = null
        idle()
        assertEquals(0, radio.removals)
        assertEquals(1, result.size)
    }
    @Test fun bluetoothWakeCannotRestartManuallyDisabledWireless() {
        ZeekrWirelessMode.selectUsb(app)
        DiPlayPreferences.saveConnectOnPhoneBluetooth(app, true)
        val address = "12:34:56:78:9A:BC"
        DiPlayPreferences.savePhone(app, address, "iPhone")
        val launches = mutableListOf<Intent>()
        val context = object : ContextWrapper(app) {
            override fun startActivity(intent: Intent) { launches.add(intent) }
        }
        PhoneBluetoothReceiver().onReceive(context, Intent(BluetoothDevice.ACTION_ACL_CONNECTED)
            .putExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address)))
        assertTrue(launches.isEmpty())
    }

    @Implements(WifiP2pManager.Channel::class)
    class Channel { @Implementation protected fun close() {} }
    @Implements(WifiP2pManager::class)
    class Radio : ShadowWifiP2pManager() {
        var group: WifiP2pGroup? = null
        var removals = 0
        var keepGroup = false
        var reject = false
        var silent = false
        var pending: WifiP2pManager.GroupInfoListener? = null
        @Implementation override fun requestGroupInfo(channel: WifiP2pManager.Channel, listener: WifiP2pManager.GroupInfoListener) {
            if (silent) pending = listener else listener.onGroupInfoAvailable(group)
        }
        @Implementation override fun removeGroup(channel: WifiP2pManager.Channel, listener: WifiP2pManager.ActionListener) {
            removals++
            if (reject) listener.onFailure(WifiP2pManager.ERROR) else {
                if (!keepGroup) group = null
                listener.onSuccess()
            }
        }
    }
}
