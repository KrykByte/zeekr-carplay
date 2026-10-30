package com.shilapi.xcertplay

import android.content.Context
import android.net.wifi.p2p.WifiP2pManager
import android.os.Handler
import android.os.Looper
import com.shilapi.xcertplay.compat.closeCompat
import com.shilapi.xcertplay.orchestration.CarPlayTransport

/** User-requested wireless pause. Never changes the global Wi-Fi or Bluetooth switches. */
internal object ZeekrWirelessMode {
    @Volatile var busy = false
        private set
    private val waiters = mutableListOf<(OwnedP2pRelease.Result) -> Unit>()

    fun disabled(context: Context) = ZeekrProfile.active(context) &&
        ZeekrProfile.prefs(context).getBoolean("wireless_disabled", false)

    fun enable(context: Context) {
        ZeekrProfile.prefs(context).edit().putBoolean("wireless_disabled", false).apply()
    }

    fun selectUsb(context: Context) {
        ZeekrProfile.prefs(context).edit().putBoolean("wireless_disabled", true).apply()
        AirPlayPersistence.saveWirelessEnabled(context, false)
        DiPlayPreferences.saveDefaultConnectionMode(context, DefaultConnectionMode.USB)
    }

    /** Must be called on the UI thread; the existing session stop callback waits for teardown. */
    fun disable(context: Context, completion: (OwnedP2pRelease.Result) -> Unit) {
        check(Looper.myLooper() == Looper.getMainLooper())
        val app = context.applicationContext
        val wirelessSession = CarPlayBackgroundSession.snapshot()?.controller?.transport
            ?.let { it == CarPlayTransport.WIRELESS }
            ?: (CarPlayBackgroundSession.hasSession() && AirPlayPersistence.loadWirelessEnabled(app))
        selectUsb(app) // Persist before stopping so wake events cannot recreate wireless CarPlay.
        waiters.add(completion)
        if (busy) return
        busy = true
        val handler = Handler(Looper.getMainLooper())
        var completed = false
        var releaseStarted = false
        lateinit var stopTimeout: Runnable
        fun finish(result: OwnedP2pRelease.Result) {
            if (completed) return
            completed = true
            handler.removeCallbacks(stopTimeout)
            busy = false
            val callbacks = waiters.toList()
            waiters.clear()
            callbacks.forEach { it(result) }
        }
        stopTimeout = Runnable { finish(OwnedP2pRelease.Result.FAILED) }
        handler.postDelayed(stopTimeout, 12_000L)
        val release = {
            handler.post {
                if (!completed && !releaseStarted) {
                    releaseStarted = true
                    handler.removeCallbacks(stopTimeout)
                    OwnedP2pRelease.release(app, ::finish)
                }
            }
            Unit
        }
        try {
            if (wirelessSession) CarPlayBackgroundSession.stop(release) else release()
        } catch (_: RuntimeException) { finish(OwnedP2pRelease.Result.FAILED) }
    }
}

/** Releases only the exact group recorded by our receiver; verifies disappearance after onSuccess. */
internal object OwnedP2pRelease {
    enum class Result { CLEAR, OTHER_APP, FAILED }

    fun release(context: Context, completion: (Result) -> Unit) {
        val recorded = context.getSharedPreferences("carplay_wifi_p2p", Context.MODE_PRIVATE)
            .getString("owned_ssid", null)?.takeIf { it.isNotBlank() }
        if (recorded == null) { completion(Result.CLEAR); return }
        val manager = context.getSystemService(WifiP2pManager::class.java)
        if (manager == null) { completion(Result.CLEAR); return }
        val handler = Handler(Looper.getMainLooper())
        var channel: WifiP2pManager.Channel? = null
        var finished = false
        var removalRequested = false
        lateinit var timeout: Runnable
        fun finish(result: Result) {
            if (finished) return
            finished = true
            handler.removeCallbacks(timeout)
            runCatching { channel?.closeCompat() }
            completion(result)
        }
        timeout = Runnable { finish(Result.FAILED) }
        fun poll() {
            if (finished) return
            val active = channel ?: return finish(Result.FAILED)
            try {
                manager.requestGroupInfo(active) { group ->
                    if (finished) return@requestGroupInfo
                    when {
                        group == null -> finish(Result.CLEAR)
                        !group.isGroupOwner || group.networkName != recorded -> finish(Result.OTHER_APP)
                        removalRequested -> handler.postDelayed({ poll() }, 150L)
                        else -> {
                            removalRequested = true
                            try {
                                manager.removeGroup(active, object : WifiP2pManager.ActionListener {
                                    override fun onSuccess() {
                                        if (!finished) handler.postDelayed({ poll() }, 150L)
                                    }
                                    override fun onFailure(reason: Int) { finish(Result.FAILED) }
                                })
                            } catch (_: RuntimeException) { finish(Result.FAILED) }
                        }
                    }
                }
            } catch (_: RuntimeException) { finish(Result.FAILED) }
        }
        handler.postDelayed(timeout, 4_000L)
        try {
            channel = manager.initialize(context.applicationContext, Looper.getMainLooper()) { finish(Result.FAILED) }
            poll()
        } catch (_: RuntimeException) { finish(Result.FAILED) }
    }
}
