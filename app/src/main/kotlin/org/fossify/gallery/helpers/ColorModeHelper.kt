package org.fossify.gallery.helpers

import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi
import org.fossify.commons.helpers.isUpsideDownCakePlus

/**
 * Helper class to manage color modes for HDR and wide color gamut images.
 */
object ColorModeHelper {

    fun isGainmapSupported() = isUpsideDownCakePlus()

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun hasHdrContent(bitmap: Bitmap?): Boolean {
        return bitmap?.hasGainmap() == true
    }

    fun hasWideColorGamut(bitmap: Bitmap?): Boolean {
        return bitmap?.colorSpace?.isWideGamut == true
    }

    fun setColorMode(activity: Activity, colorMode: Int) {
        activity.window.setColorMode(colorMode)
    }

    /**
     * Fossify Gallery is intentionally forced to use the standard Android color mode.
     *
     * This build disables both HDR and wide-color-gamut window rendering. The previous
     * implementation could switch the Activity window to WIDE_COLOR_GAMUT after a
     * bitmap was loaded, which is undesirable for testing the reported delayed
     * saturation/color shift when swiping between images.
     *
     * Keep the original parameters for source/API compatibility with the editor, but
     * deliberately ignore them here.
     */
    fun setColorModeForImage(activity: Activity, bitmap: Bitmap?, ultraHdr: Boolean = true) {
        activity.window.setColorMode(ActivityInfo.COLOR_MODE_DEFAULT)
    }

    fun resetColorMode(activity: Activity?) {
        activity?.window?.setColorMode(ActivityInfo.COLOR_MODE_DEFAULT)
    }
}
