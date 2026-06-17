package com.shawnjulian.uikit.util

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.util.TypedValue
import androidx.fragment.app.FragmentActivity

/**
 * @ClassName DisplayUtils
 * @Author ShawnJulian
 * @Date 2026/6/17
 * @Description Utility functions for converting between dp/px/sp units and
 * retrieving device screen metrics (width, height, density).
 */
object DisplayUtils {

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    fun dip2px(context: Context?, dpValue: Float): Int {
        val scale = context?.resources?.displayMetrics?.density ?: 1F
        return (dpValue * scale + 0.5f).toInt()
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    fun px2dip(context: Context?, pxValue: Float): Int {
        val scale = context?.resources?.displayMetrics?.density ?: 1F
        return (pxValue / scale + 0.5f).toInt()
    }

    /**
     * 35.     * 将px值转换为sp值，保证文字大小不变
     * 36.     *
     * 37.     * @param pxValue
     * 38.     * @param fontScale
     * 39.     *            （DisplayMetrics类中属性scaledDensity）
     * 40.     * @return
     * 41.
     */
    fun px2sp(context: Context, pxValue: Float): Int {
        val fontScale = context.resources.displayMetrics.scaledDensity
        return (pxValue / fontScale + 0.5f).toInt()
    }

    /**
     * 将sp值转换为px值，保证文字大小不变
     *
     * @param spValue
     * @param fontScale
     *（DisplayMetrics类中属性scaledDensity）
     * @return
     *
     */
    fun sp2px(context: Context, spValue: Float): Int {
        // original code
//        val fontScale = context.resources.displayMetrics.scaledDensity
//        return (spValue * fontScale + 0.5f).toInt()

        return if (Build.VERSION.SDK_INT >= 34) {
            TypedValue.deriveDimension(
                TypedValue.COMPLEX_UNIT_SP,
                spValue,
                context.resources.displayMetrics
            ).toInt()
        } else {
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                spValue,
                context.resources.displayMetrics
            ).toInt()
        }
    }

    fun getPhoneWidth(activity: FragmentActivity?): Int {
        if (activity == null || activity.isDestroyed) {
            return 0
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Use WindowMetrics on API 30+ to avoid deprecated getDefaultDisplay()/getMetrics()
            val bounds = activity.windowManager.currentWindowMetrics.bounds
            bounds.width()
        } else {
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
            displayMetrics.widthPixels
        }
    }

    fun getPhoneHeight(activity: FragmentActivity?): Int {
        if (activity == null || activity.isDestroyed) {
            return 0
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Use WindowMetrics on API 30+ to avoid deprecated getDefaultDisplay()/getMetrics()
            val bounds = activity.windowManager.currentWindowMetrics.bounds
            bounds.height()
        } else {
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
            displayMetrics.heightPixels
        }
    }


    private fun checkContext(context: Context?): Boolean {
        return context != null
    }

    /**
     * @param context
     * @return
     */
    fun getDensity(context: Context?): Float {
        if (!checkContext(context)) {
            return 0f
        }
        return context!!.resources.displayMetrics.density
    }
}
