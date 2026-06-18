package com.shawnjulian.uikit.view.wheel.adapter


/**
 * Numeric Wheel adapter.
 */
class NumericWheelAdapter @JvmOverloads constructor(// Values
    private val minValue: Int = DEFAULT_MIN_VALUE, private val maxValue: Int = DEFAULT_MAX_VALUE
) : WheelAdapter<Any?> {

    /**
     * Constructor
     * @param minValue the wheel min value
     * @param maxValue the wheel max value
     */
    override fun getItem(index: Int): Any {
        if (index >= 0 && index < getItemsCount()) {
            val value = minValue + index
            return value
        }
        return 0
    }

    override fun getItemsCount(): Int {
        return maxValue - minValue + 1
    }

    override fun indexOf(o: Any?): Int {
        return o as Int - minValue
    }

    companion object {

        /** The default min value  */
        const val DEFAULT_MAX_VALUE: Int = 9

        /** The default max value  */
        private const val DEFAULT_MIN_VALUE = 0
    }
}