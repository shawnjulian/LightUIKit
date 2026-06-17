package com.shawnjulian.uikit.view.wheel

import java.util.TimerTask
import kotlin.math.abs

class SmoothScrollTimerTask(val loopView: WheelView, var offset: Int) : TimerTask() {
    var realTotalOffset: Int
    var realOffset: Int = 0

    init {
        realTotalOffset = Int.MAX_VALUE
    }

    override fun run() {
        if (realTotalOffset == Int.MAX_VALUE) {
            realTotalOffset = offset
        }

        // Divide the scrolling range into ten parts and redraw by each small part unit
        realOffset = (realTotalOffset.toFloat() * 0.1f).toInt()

        if (realOffset == 0) {
            realOffset = if (realTotalOffset < 0) {
                -1
            } else {
                1
            }
        }

        if (abs(realTotalOffset) <= 1) {
            loopView.cancelFuture()
            loopView.handler!!.sendEmptyMessage(MessageHandler.WHAT_ITEM_SELECTED)
        } else {
            loopView.totalScrollY += realOffset

            // If not in loop mode, scrolling back is required when clicking on blank areas,
            // otherwise it will result in selecting item at index -1
            if (!loopView.isLoop) {
                val itemHeight = loopView.itemHeight
                val top = (-loopView.initPosition).toFloat() * itemHeight
                val bottom =
                    (loopView.itemsCount - 1 - loopView.initPosition).toFloat() * itemHeight
                if (loopView.totalScrollY <= top || loopView.totalScrollY >= bottom) {
                    loopView.totalScrollY -= realOffset
                    loopView.cancelFuture()
                    loopView.handler!!.sendEmptyMessage(MessageHandler.WHAT_ITEM_SELECTED)
                    return
                }
            }
            loopView.handler!!.sendEmptyMessage(MessageHandler.WHAT_INVALIDATE_LOOP_VIEW)
            realTotalOffset -= realOffset
        }
    }
}
