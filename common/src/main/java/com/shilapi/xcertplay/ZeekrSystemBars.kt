package com.shilapi.xcertplay

import android.content.Context
import android.content.res.Configuration
import android.view.Window
import androidx.core.view.WindowInsetsControllerCompat

/** OEM chrome follows Android's system theme, independently of app and CarPlay appearance. */
internal object ZeekrSystemBars {
    fun palette(context: Context): DiPlayPalette = DiPlayPalette.of(
        context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES,
    )

    @Suppress("DEPRECATION") // Android 9 head units use the window's bar backgrounds.
    fun apply(context: Context, window: Window) {
        if (!ZeekrProfile.active(context)) return
        val colors = palette(context)
        window.statusBarColor = colors.systemBar
        window.navigationBarColor = colors.systemBar
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = colors.systemBarIconsAreDark
            isAppearanceLightNavigationBars = colors.systemBarIconsAreDark
        }
    }
}
