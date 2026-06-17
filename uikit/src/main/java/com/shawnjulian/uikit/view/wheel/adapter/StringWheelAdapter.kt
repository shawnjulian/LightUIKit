package com.shawnjulian.uikit.view.wheel.adapter

/**
 * @ClassName StringWheelAdapter
 * @Author ShawnJulian
 * @Date 2026/6/17
 * @Description
 */
class StringWheelAdapter(private val list: MutableList<String?>) : WheelAdapter<Any?> {
    override fun getItemsCount(): Int {
        return list.size
    }

    override fun getItem(index: Int): Any? {
        return list[index]
    }

    override fun indexOf(o: Any?): Int {
        return list.indexOf(o)
    }
}
