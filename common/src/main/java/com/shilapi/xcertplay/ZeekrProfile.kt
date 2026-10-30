package com.shilapi.xcertplay

import android.content.Context

/** Presentation policy for the standalone Zeekr fork; receiver/OEM implementations stay intact. */
internal object ZeekrProfile {
    fun active(context: Context) = context.applicationInfo.packageName == "com.openzeekr.carplay"
    fun prefs(context: Context) = context.getSharedPreferences("zeekr_alpha", Context.MODE_PRIVATE)
    fun completed(context: Context) = prefs(context).getBoolean("completed", false)
    fun chosen(context: Context, key: String) = prefs(context).getBoolean(key, false)
    fun choose(context: Context, key: String) { prefs(context).edit().putBoolean(key, true).apply() }
    fun connectionChosen(context: Context) = chosen(context, "transport") &&
        (!AirPlayPersistence.loadWirelessEnabled(context) || chosen(context, "wireless"))

    fun migrate(context: Context) {
        if (!active(context)) return
        if (AppLocale.preference(context) !in listOf(AppLocale.ENGLISH, AppLocale.RUSSIAN)) {
            val system = android.content.res.Resources.getSystem().configuration.locales[0].language
            AppLocale.save(context, if (system == "ru") AppLocale.RUSSIAN else AppLocale.ENGLISH)
        }
        val prefs = prefs(context)
        if (prefs.getBoolean("identity_migrated", false)) return
        if (AirPlayPersistence.loadManufacturer(context) == AirPlayPersistence.DEFAULT_MANUFACTURER)
            AirPlayPersistence.saveManufacturer(context, "Zeekr")
        if (AirPlayPersistence.loadModel(context) == AirPlayPersistence.DEFAULT_MODEL)
            AirPlayPersistence.saveModel(context, "Zeekr CarPlay")
        if (AirPlayPersistence.loadOemLabel(context) == AirPlayPersistence.DEFAULT_OEM_LABEL)
            AirPlayPersistence.saveOemLabel(context, "Zeekr")
        prefs.edit().putBoolean("identity_migrated", true).apply()
    }

    val hiddenSections = setOf(SettingsSection.BYD_ADB, SettingsSection.BYD_NAVIGATION,
        SettingsSection.WHEEL_KEYS, SettingsSection.NAVIGATION_WHEEL, SettingsSection.CLUSTER_MAP,
        SettingsSection.AMBIENT_LIGHTING, SettingsSection.EXPERIMENTAL_DISPLAY)
}
