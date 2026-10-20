package com.today.app.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Keeps the launcher icon in sync with the character's outfit.
 *
 * Android only allows switching between icons declared in the manifest (one activity-alias per
 * preset outfit), so presets get their exact icon and photo / fine-tuned outfits get the closest
 * preset. Switch while the app is in the background: toggling the alias that launched the
 * visible task can make some launchers close it.
 */
object AppIconManager {

    /** Preset key -> activity-alias class (declared in AndroidManifest.xml). */
    private val aliases = mapOf(
        "default" to "com.today.app.LauncherDefault",
        "street" to "com.today.app.LauncherStreet",
        "ocean" to "com.today.app.LauncherOcean",
        "forest" to "com.today.app.LauncherForest",
        "sunset" to "com.today.app.LauncherSunset",
        "neon" to "com.today.app.LauncherNeon",
    )

    /** Which preset icon best represents [outfit]. */
    fun iconKeyFor(outfit: Outfit): String {
        Outfits.presets.firstOrNull { it == outfit.withoutAccessories() }?.let { return it.name }
        // What the icon shows: hair (and its length), skin, top, collar.
        return Outfits.presets.minBy { p ->
            StyleAnalyzer.colorDist(p.hair, outfit.hair) * 1.5 +
                StyleAnalyzer.colorDist(p.shirt, outfit.shirt) * 1.5 +
                StyleAnalyzer.colorDist(p.accent, outfit.accent) * 0.5 +
                StyleAnalyzer.colorDist(p.skin, outfit.skin) * 0.5 +
                (if (p.longHair != outfit.longHair) 250.0 else 0.0)
        }.name
    }

    /** Enables the alias for [outfit]'s icon and disables the rest (no-op when already right). */
    fun sync(context: Context, outfit: Outfit) {
        val target = aliases[iconKeyFor(outfit)] ?: return
        val pm = context.packageManager
        // Enable the new one first so there is never a moment without a launcher entry.
        setEnabled(pm, context, target, true)
        aliases.values.filter { it != target }.forEach { setEnabled(pm, context, it, false) }
    }

    private fun setEnabled(pm: PackageManager, context: Context, cls: String, enabled: Boolean) {
        val component = ComponentName(context, cls)
        val wanted = if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        val current = pm.getComponentEnabledSetting(component)
        // DEFAULT means "as declared in the manifest": only LauncherDefault is enabled there.
        val effectivelyEnabled = when (current) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> cls == aliases["default"]
            else -> false
        }
        if (effectivelyEnabled == enabled) return
        runCatching { pm.setComponentEnabledSetting(component, wanted, PackageManager.DONT_KILL_APP) }
    }
}
