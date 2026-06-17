package com.shawnjulian.uikit.view.wheel.adapter

/**
 * The simple Array wheel adapter
 * @param <T> the element type
</T> */
class ArrayWheelAdapter<T> @JvmOverloads constructor(// items
    private val items: ArrayList<T?>, // length
    private var length: Int = DEFAULT_LENGTH
) : WheelAdapter<Any?> {

    /**
     * Contructor
     * @param items the items
     */
    init {
        this.length = length
    }

    override fun getItem(index: Int): Any? {
        if (index >= 0 && index < items.size) {
            return items.get(index)
        }
        return ""
    }

    override fun getItemsCount(): Int {
        return items.size
    }

    override fun indexOf(o: Any?): Int {
        return items.indexOf(o)
    }

    companion object {
        /** The default items length  */
        const val DEFAULT_LENGTH: Int = 4
    }
}
