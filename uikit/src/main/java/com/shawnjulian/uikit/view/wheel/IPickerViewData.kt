package com.shawnjulian.uikit.view.wheel

/**
 * Data model contract for items displayed in a wheel/picker view.
 *
 * Implement this interface on any class whose instances should be shown
 * in a picker. The picker will display the string returned by
 * [pickerViewText]. Returning null will show an empty entry.
 *
 * Example:
 * class City(val name: String) : IPickerViewData {
 *     override val pickerViewText: String? = name
 * }
 *
 * @author ShawnJulian
 * @date 2016-07-13
 * @since 1.0
 */
interface IPickerViewData {
    /**
     * Text to display for this item in the picker. May be null to indicate
     * an empty/placeholder entry.
     */
    val pickerViewText: String
}
