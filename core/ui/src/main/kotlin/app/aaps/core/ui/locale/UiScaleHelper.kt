package app.aaps.core.ui.locale

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration

/**
 * Watch adaptation: on very narrow screens (smallest screen width <= 190dp, which is a watch
 * showing phone UI at native density), render this app at a lower density so the phone-sized
 * layouts fit the watch screen.
 *
 * Per-app only: system-wide density, other apps (including the IME) and watchfaces are NOT affected.
 * Target 190dpi verified best readability on 378x496 @ 320dpi (OPPO Watch 3 Pro).
 */
object UiScaleHelper {

    private const val MAX_SW_DP = 190
    private const val TARGET_DPI = 190

    fun wrap(ctx: Context): Context {
        val configuration = Configuration(ctx.resources.configuration)
        return if (configuration.smallestScreenWidthDp in 1..MAX_SW_DP && configuration.densityDpi > TARGET_DPI) {
            configuration.densityDpi = TARGET_DPI
            ContextWrapper(ctx.createConfigurationContext(configuration))
        } else {
            ctx
        }
    }
}
