package com.shilapi.xcertplay

import android.content.Intent
import androidx.core.view.WindowInsetsControllerCompat
import java.util.concurrent.ExecutorService
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Regression: factory day-mode dark icons were drawn over the host's fixed dark bar. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "en-notnight")
class ZeekrSystemBarsTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private var previousPackage = ""

    @Before fun setUp() {
        previousPackage = context.applicationInfo.packageName
        context.applicationInfo.packageName = "com.openzeekr.carplay"
        context.getSharedPreferences("xcertplay_airplay", 0).edit().clear().commit()
    }

    @After fun tearDown() {
        context.applicationInfo.packageName = previousPackage
        for (name in listOf("xcertplay_airplay", "zeekr_alpha", "diplay"))
            context.getSharedPreferences(name, 0).edit().clear().commit()
    }

    @Test fun appBarsFollowSystemEvenWithTheOppositeAppTheme() {
        AirPlayPersistence.saveAppAppearance(context, AppAppearance.DARK)
        val controller = Robolectric.buildActivity(DiPlayActivity::class.java,
            Intent(context, DiPlayActivity::class.java).putExtra("page", "settings")).setup()
        val a = controller.get()
        try {
            assertEquals(DiPlayPalette.LIGHT.systemBar, a.window.statusBarColor)
            assertTrue(WindowInsetsControllerCompat(a.window, a.window.decorView).isAppearanceLightStatusBars)
            RuntimeEnvironment.setQualifiers("en-night")
            AirPlayPersistence.saveAppAppearance(a, AppAppearance.LIGHT)
            a.onConfigurationChanged(a.resources.configuration)
            assertEquals(DiPlayPalette.DARK.systemBar, a.window.statusBarColor)
            assertFalse(WindowInsetsControllerCompat(a.window, a.window.decorView).isAppearanceLightStatusBars)
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun videoHostBarsFollowSystemIndependentlyOfCarPlayDayOrNight() {
        val a = Robolectric.buildActivity(CarPlayHostActivity::class.java).get()
        val fullscreen = CarPlayHostActivity::class.java.getDeclaredMethod("applyFullscreenMode")
            .apply { isAccessible = true }
        try {
            for (carPlayNight in listOf(false, true)) {
                CarPlayHostActivity::class.java.getDeclaredField("darkMode")
                    .apply { isAccessible = true }.setBoolean(a, carPlayNight)
                RuntimeEnvironment.setQualifiers("en-notnight")
                fullscreen.invoke(a)
                assertEquals(DiPlayPalette.LIGHT.systemBar, a.window.statusBarColor)
                assertTrue(WindowInsetsControllerCompat(a.window, a.window.decorView).isAppearanceLightStatusBars)
                RuntimeEnvironment.setQualifiers("en-night")
                fullscreen.invoke(a)
                assertEquals(DiPlayPalette.DARK.systemBar, a.window.statusBarColor)
                assertFalse(WindowInsetsControllerCompat(a.window, a.window.decorView).isAppearanceLightStatusBars)
            }
        } finally {
            for (field in listOf("airPlayCommandExecutor", "teardownExecutor"))
                (CarPlayHostActivity::class.java.getDeclaredField(field)
                    .apply { isAccessible = true }.get(a) as ExecutorService).shutdownNow()
        }
    }
}
