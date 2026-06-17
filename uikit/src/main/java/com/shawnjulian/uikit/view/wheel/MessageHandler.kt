package com.shawnjulian.uikit.view.wheel

import android.os.Handler
import android.os.Message

internal class MessageHandler(val loopview: WheelView) : Handler() {
    override fun handleMessage(msg: Message) {
        when (msg.what) {
            WHAT_INVALIDATE_LOOP_VIEW -> loopview.invalidate()
            WHAT_SMOOTH_SCROLL -> loopview.smoothScroll(WheelView.ACTION.FLING)
            WHAT_ITEM_SELECTED -> loopview.onItemSelected()
        }
    }

    companion object {
        const val WHAT_INVALIDATE_LOOP_VIEW: Int = 1000
        const val WHAT_SMOOTH_SCROLL: Int = 2000
        const val WHAT_ITEM_SELECTED: Int = 3000
    }
}
