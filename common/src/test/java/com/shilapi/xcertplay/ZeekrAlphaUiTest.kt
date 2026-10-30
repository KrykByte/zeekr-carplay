package com.shilapi.xcertplay

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Switch
import com.shilapi.xcertplay.host.R
import com.shilapi.xcertplay.orchestration.WirelessHotspotMode
import java.io.File
import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ZeekrAlphaUiTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private var previousPackage = ""
    @After fun restore() {
        context.applicationInfo.packageName = previousPackage
        for (name in listOf("diplay", "zeekr_alpha", "xcertplay_airplay"))
            context.getSharedPreferences(name, 0).edit().clear().commit()
    }
    @Before fun reset() {
        previousPackage = context.applicationInfo.packageName
        context.applicationInfo.packageName = "com.openzeekr.carplay"
        for (name in listOf("diplay", "zeekr_alpha", "xcertplay_airplay"))
            context.getSharedPreferences(name, 0).edit().clear().commit()
    }

    @Test fun connectionAndAudioRequireExplicitChoices() {
        assertTrue(ZeekrProfile.active(context))
        val controller = Robolectric.buildActivity(DiPlayActivity::class.java,
            Intent(context, DiPlayActivity::class.java).putExtra("page", "setup")).setup()
        val a = controller.get()
        try {
            click(a, R.string.setup_next)
            assertFalse(next(a).isEnabled)
            click(a, R.string.zkr_wireless)
            assertFalse(next(a).isEnabled)
            click(a, R.string.zkr_direct)
            assertTrue(next(a).isEnabled)
            assertEquals(WirelessHotspotMode.WIFI_P2P, AirPlayPersistence.loadWirelessHotspotMode(a))
            click(a, R.string.zkr_hotspot)
            assertFalse(next(a).isEnabled)
            click(a, R.string.zkr_direct)
            assertTrue(next(a).isEnabled)
            click(a, R.string.setup_next)
            assertFalse(next(a).isEnabled)
            click(a, R.string.zkr_audio_bt)
            assertTrue(AirPlayPersistence.loadCarBluetoothAudio(a))
            assertTrue(next(a).isEnabled)
            assertFalse(DiPlayPreferences.autoConnect(a))
            assertEquals("Zeekr", AirPlayPersistence.loadOemLabel(a))
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun forkHidesOemAndUpdateControlsButKeepsAttribution() {
        val controller = Robolectric.buildActivity(DiPlayActivity::class.java,
            Intent(context, DiPlayActivity::class.java).putExtra("page", "settings")).setup()
        val a = controller.get()
        try {
            val content = LinearLayout(a)
            DiPlayActivity::class.java.getDeclaredMethod("allSettingsSections", LinearLayout::class.java)
                .apply { isAccessible = true }.invoke(a, content)
            val text = descendants(content).filterIsInstance<TextView>().joinToString("\n") { it.text }
            assertFalse(text.lineSequence().filter { it.contains("BYD") }.joinToString("\n"), text.contains("BYD"))
            assertFalse(text.lineSequence().filter { it.contains("DiLink") }.joinToString("\n"), text.contains("DiLink"))
            assertFalse(text.contains(a.getString(R.string.mfi_certificate_signing_target)))
            val about = LinearLayout(a)
            DiPlayActivity::class.java.getDeclaredMethod("about", LinearLayout::class.java)
                .apply { isAccessible = true }.invoke(a, about)
            val aboutText = descendants(about).filterIsInstance<TextView>().joinToString("\n") { it.text }
            assertTrue(aboutText.contains("GPL-3.0"))
            assertTrue(aboutText.contains("shihabal3amri/DiPlay"))
            assertFalse(aboutText.contains(a.getString(R.string.update_check)))
            assertFalse(aboutText.contains(a.getString(R.string.settings_background_update_checks)))
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun overviewBarSwitchesPreserveChoicesAndAgreeWithDisplaySettings() {
        assertFalse(AirPlayPersistence.loadHideTopBar(context))
        assertFalse(AirPlayPersistence.loadHideBottomBar(context))
        AirPlayPersistence.saveHideBottomBar(context, true)
        val controller = Robolectric.buildActivity(DiPlayActivity::class.java,
            Intent(context, DiPlayActivity::class.java).putExtra("page", "settings")).setup()
        val a = controller.get()
        try {
            fun control(id: Int) = descendants(a.window.decorView).filterIsInstance<Switch>()
                .single { it.contentDescription == a.getString(id) }
            assertFalse(control(R.string.hide_the_status_bar).isChecked)
            assertTrue(control(R.string.hide_the_navigation_bar).isChecked)
            control(R.string.hide_the_status_bar).performClick()
            control(R.string.hide_the_navigation_bar).performClick()
            assertTrue(AirPlayPersistence.loadHideTopBar(a))
            assertFalse(AirPlayPersistence.loadHideBottomBar(a))
            descendants(a.window.decorView).single { it.contentDescription == a.getString(
                R.string.settings_open_category, a.getString(R.string.settings_display)) }.performClick()
            assertTrue(control(R.string.hide_the_status_bar).isChecked)
            assertFalse(control(R.string.hide_the_navigation_bar).isChecked)
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun wizardAndSettingsRenderAtLargeFont() {
        val out = File("build/reports/zeekr-alpha-ui").apply { mkdirs() }
        for ((name, qualifiers) in listOf(
            "compact-ru" to "ru-w360dp-h760dp-mdpi",
            "wide-en" to "en-w1100dp-h800dp-mdpi",
            "compact-ar" to "ar-w360dp-h760dp-mdpi",
        )) {
            RuntimeEnvironment.setQualifiers(qualifiers)
            RuntimeEnvironment.setFontScale(1.5f)
            AppLocale.save(context, if (name == "compact-ru") AppLocale.RUSSIAN else AppLocale.ENGLISH)
            val controller = Robolectric.buildActivity(DiPlayActivity::class.java,
                Intent(context, DiPlayActivity::class.java).putExtra("page", "setup")).setup()
            val a = controller.get()
            try {
                val stepField = DiPlayActivity::class.java.getDeclaredField("setupStep").apply { isAccessible = true }
                val method = DiPlayActivity::class.java.getDeclaredMethod("zeekrSetup", LinearLayout::class.java).apply { isAccessible = true }
                for (step in 0..4) {
                    stepField.setInt(a, step)
                    if (step == 1) {
                        AirPlayPersistence.saveWirelessEnabled(a, true)
                        ZeekrProfile.choose(a, "transport")
                    }
                    val content = LinearLayout(a).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutDirection = if (name == "compact-ar") View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
                        setBackgroundColor(0xff101827.toInt())
                    }
                    method.invoke(a, content)
                    DiPlayActivity::class.java.getDeclaredMethod("normalizeSpacing", LinearLayout::class.java, Boolean::class.javaPrimitiveType)
                        .apply { isAccessible = true }.invoke(a, content, false)
                    val width = if (name == "wide-en") 1100 else 360
                    content.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
                    content.layout(0, 0, width, content.measuredHeight)
                    val bitmap = Bitmap.createBitmap(width, content.measuredHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(0xff101827.toInt())
                    content.draw(Canvas(bitmap))
                    File(out, "$name-step-$step.png").outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
                    bitmap.recycle()
                    assertTrue(content.measuredHeight > 0)
                }
                val quick = DiPlayActivity::class.java.getDeclaredMethod("quickSettingsCard")
                    .apply { isAccessible = true }.invoke(a) as LinearLayout
                quick.layoutDirection = if (name == "compact-ar") View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
                val width = if (name == "wide-en") 1100 else 360
                quick.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
                quick.layout(0, 0, width, quick.measuredHeight)
                val bitmap = Bitmap.createBitmap(width, quick.measuredHeight, Bitmap.Config.ARGB_8888)
                quick.draw(Canvas(bitmap))
                File(out, "$name-quick-settings.png").outputStream().use {
                    assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
                }
                bitmap.recycle()
            } finally { controller.pause().stop().destroy() }
        }
    }
    private fun texts(a: DiPlayActivity) = descendants(a.window.decorView).filterIsInstance<TextView>()
    private fun next(a: DiPlayActivity) = texts(a).last { it.text.toString() == a.getString(R.string.setup_next) }
    private fun click(a: DiPlayActivity, id: Int) {
        assertTrue(texts(a).last { it.text.toString() == a.getString(id) }.performClick())
    }
    private fun descendants(v: View): Sequence<View> = sequence {
        yield(v)
        if (v is ViewGroup) for (i in 0 until v.childCount) yieldAll(descendants(v.getChildAt(i)))
    }
}
