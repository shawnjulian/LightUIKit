package com.shawnjulian.uikit.view.wheel

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Handler
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import androidx.core.content.ContextCompat
import com.shawnjulian.uikit.R
import com.shawnjulian.uikit.util.DisplayUtils
import com.shawnjulian.uikit.view.wheel.adapter.WheelAdapter
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.math.acos
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

/**
 * @ClassName WheelView
 * @Author ShawnJulian
 * @Date 2026/6/17
 * @Description 3d滚轮控件
 */
open class WheelView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    View(context, attrs) {
    enum class ACTION {
        // 点击，滑翔(滑到尽头)，拖拽事件
        CLICK, FLING, DAGGLE
    }

    var context: Context? = null

    var handler: Handler? = null
    private var gestureDetector: GestureDetector? = null
    var onItemSelectedListener: AdapterView.OnItemSelectedListener? = null

    // Timer mTimer;
    var mExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private var mFuture: ScheduledFuture<*>? = null

    var paintOuterText: Paint? = null
    var paintCenterText: Paint? = null
    var paintIndicator: Paint? = null

    var adapter: WheelAdapter<Any?>? = null

    private var label: String? = null //附加单位
    var outerTextSize: Float //选项的文字大小
    var customTextSize: Boolean //自定义文字大小，为true则用于使setTextSize函数无效，只能通过xml修改
    var maxTextWidth: Int = 0
    var maxTextHeight: Int = 0
    var itemHeight: Float = 0f //每行高度

    var textColorOut: Int
    var textColorCenter: Int
    var dividerColor: Int

    var isLoop: Boolean = false

    // 第一条线Y坐标值
    var firstLineY: Float = 0f

    //第二条线Y坐标
    var secondLineY: Float = 0f

    //中间Y坐标
    var centerY: Float = 0f

    //滚动总高度y值
    var totalScrollY: Int = 0

    //初始化默认选中第几个
    var initPosition: Int = 0

    //选中的Item是第几个
    private var selectedItem = 0
    var preCurrentIndex: Int = 0

    //滚动偏移值,用于记录滚动了多少个item
    var change: Int = 0

    // 显示几个条目
    var itemsVisible: Int = 11

    var measuredHeight: Int = 0
    var measuredWidth: Int = 0

    // 半圆周长
    var halfCircumference: Int = 0

    // 半径
    var radius: Int = 0

    private var mOffset = 0
    private var previousY = 0f
    var startTime: Long = 0

    var widthMeasureSpec: Int = 0

    private var mGravity = Gravity.CENTER
    private var drawCenterContentStart = 0 //中间选中文字开始绘制位置
    private var drawOutContentStart = 0 //非中间文字开始绘制位置

    init {
        textColorOut = ContextCompat.getColor(context, R.color.text_color_secondary_content)
        textColorCenter = ContextCompat.getColor(context, R.color.text_color_secondary_title)
        dividerColor = ContextCompat.getColor(context, R.color.line_color)
        //配合customTextSize使用，customTextSize为true才会发挥效果
        outerTextSize = resources.getDimensionPixelSize(R.dimen.text_size_extra).toFloat()
        customTextSize = resources.getBoolean(R.bool.pickerview_customTextSize)
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.WheelView, 0, 0)
            mGravity = a.getInt(R.styleable.WheelView_pickerview_gravity, Gravity.CENTER)
            textColorOut = a.getColor(R.styleable.WheelView_wheelView_textColorOut, textColorOut)
            textColorCenter =
                a.getColor(R.styleable.WheelView_wheelView_textColorCenter, textColorCenter)
            dividerColor = a.getColor(R.styleable.WheelView_wheelView_dividerColor, dividerColor)
            outerTextSize =
                a.getDimensionPixelOffset(
                    R.styleable.WheelView_wheelView_textSize,
                    outerTextSize.toInt()
                ).toFloat()
        }
        initLoopView(context)
    }

    private fun initLoopView(context: Context) {
        this.context = context
        handler = MessageHandler(this)
        gestureDetector = GestureDetector(context, LoopViewGestureListener(this)).apply {
            setIsLongpressEnabled(false)
        }

        isLoop = true

        totalScrollY = 0
        initPosition = -1

        initPaints()
    }

    private fun initPaints() {

        val density: Float = DisplayUtils.getDensity(context)

        outerTextSize = (context!!.resources
            .getDimensionPixelSize(R.dimen.text_size_secondary_content) / density)

        paintOuterText = Paint().apply {
            setColor(textColorOut)
            isAntiAlias = true
            textSize = outerTextSize
        }

        paintCenterText = Paint().apply {
            setColor(textColorCenter)
            isAntiAlias = true
            textSize = outerTextSize
        }

        paintIndicator = Paint().apply {
            setColor(dividerColor)
            isAntiAlias = true
        }
    }

    private fun remeasure() {
        if (adapter == null) {
            return
        }

        measureTextWidthHeight()

        //最大Text的高度乘间距倍数得到 可见文字实际的总高度，半圆的周长
        halfCircumference = (itemHeight * (itemsVisible - 1)).toInt()
        //整个圆的周长除以PI得到直径，这个直径用作控件的总高度
        measuredHeight = ((halfCircumference * 2) / Math.PI).toInt()
        //求出半径
        radius = (halfCircumference / Math.PI).toInt()
        //控件宽度，这里支持weight
        measuredWidth = MeasureSpec.getSize(widthMeasureSpec)
        //计算两条横线和控件中间点的Y位置
        firstLineY = (measuredHeight - itemHeight) / 2.0f
        secondLineY = (measuredHeight + itemHeight) / 2.0f
        centerY = (measuredHeight + maxTextHeight) / 2.0f - CENTERCONTENTOFFSET
        //初始化显示的item的position，根据是否loop
        if (initPosition == -1) {
            if (isLoop) {
                initPosition = (adapter!!.getItemsCount() + 1) / 2
            } else {
                initPosition = 0
            }
        }

        preCurrentIndex = initPosition
    }

    /**
     * 计算最大len的Text的宽高度
     */
    private fun measureTextWidthHeight() {
        val rect = Rect()
        for (i in 0 until adapter!!.getItemsCount()) {
            val s1 = getContentText(adapter!!.getItem(i))
            paintCenterText!!.getTextBounds(s1, 0, s1.length, rect)
            val textWidth = rect.width()
            if (textWidth > maxTextWidth) {
                maxTextWidth = textWidth
            }
            paintCenterText!!.getTextBounds("\u661F\u671F", 0, 2, rect) // 星期
            val textHeight = rect.height()
            if (textHeight > maxTextHeight) {
                maxTextHeight = textHeight
            }
        }
        itemHeight = lineSpacingMultiplier * maxTextHeight
    }

    fun smoothScroll(action: ACTION?) {
        cancelFuture()
        if (action == ACTION.FLING || action == ACTION.DAGGLE) {
            mOffset = ((totalScrollY % itemHeight + itemHeight) % itemHeight).toInt()
            if (mOffset.toFloat() > itemHeight / 2.0f) {
                mOffset = (itemHeight - mOffset.toFloat()).toInt()
            } else {
                mOffset = -mOffset
            }
        }
        //停止的时候，位置有偏移，不是全部都能正确停止到中间位置的，这里把文字位置挪回中间去
        mFuture = mExecutor.scheduleWithFixedDelay(
            SmoothScrollTimerTask(this, mOffset),
            0,
            10,
            TimeUnit.MILLISECONDS
        )
    }

    protected fun scrollBy(velocityY: Float) {
        cancelFuture()

        mFuture = mExecutor.scheduleWithFixedDelay(
            InertiaTimerTask(this, velocityY),
            0,
            VELOCITYFLING.toLong(),
            TimeUnit.MILLISECONDS
        )
    }

    fun cancelFuture() {
        if (mFuture != null && !mFuture!!.isCancelled()) {
            mFuture!!.cancel(true)
            mFuture = null
        }
    }

    /**
     * 设置是否循环滚动
     *
     * @param cyclic 是否循环
     */
    fun setCyclic(cyclic: Boolean) {
        isLoop = cyclic
    }

    fun setTextSize(size: Float) {
        if (size > 0.0f && !customTextSize) {
            outerTextSize = (context!!.resources.displayMetrics.density * size)
            paintOuterText?.textSize = outerTextSize
            paintCenterText?.textSize = outerTextSize
        }
    }

    fun setOnItemSelectedListener(onItemSelectedListener: AdapterView.OnItemSelectedListener?) {
        this.onItemSelectedListener = onItemSelectedListener
    }

    fun setAdapter(adapter: WheelAdapter<Any?>?) {
        this.adapter = adapter
        remeasure()
        invalidate()
    }

    fun getAdapter(): WheelAdapter<Any?>? {
        return adapter
    }

    var currentItem: Int
        get() = selectedItem
        set(currentItem) {
            this.initPosition = currentItem
            totalScrollY = 0 //回归顶部，不然重设setCurrentItem的话位置会偏移的，就会显示出不对位置的数据
            invalidate()
        }

    protected fun onItemSelected() {
        if (onItemSelectedListener != null) {
            postDelayed(OnItemSelectedRunnable(this), 200L)
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (adapter == null) {
            return
        }
        //可见的item数组
        val visibles: Array<Any?>? = arrayOfNulls<Any>(itemsVisible)
        //滚动的Y值高度除去每行Item的高度，得到滚动了多少个item，即change数
        change = (totalScrollY / itemHeight).toInt()
        try {
            //滚动中实际的预选中的item(即经过了中间位置的item) ＝ 滑动前的位置 ＋ 滑动相对位置
            preCurrentIndex = initPosition + change % adapter!!.getItemsCount()
        } catch (e: ArithmeticException) {
            //System.out.println("出错了！adapter.getItemsCount() == 0，联动数据不匹配");
        }
        if (!isLoop) { //不循环的情况
            if (preCurrentIndex < 0) {
                preCurrentIndex = 0
            }
            if (preCurrentIndex > adapter!!.getItemsCount() - 1) {
                preCurrentIndex = adapter!!.getItemsCount() - 1
            }
        } else { //循环

            if (preCurrentIndex < 0) { //举个例子：如果总数是5，preCurrentIndex ＝ －1，那么preCurrentIndex按循环来说，其实是0的上面，也就是4的位置
                preCurrentIndex = adapter!!.getItemsCount() + preCurrentIndex
            }

            if (preCurrentIndex > adapter!!.getItemsCount() - 1) { //同理上面,自己脑补一下
                preCurrentIndex = preCurrentIndex - adapter!!.getItemsCount()
            }
        }

        //跟滚动流畅度有关，总滑动距离与每个item高度取余，即并不是一格格的滚动，每个item不一定滚到对应Rect里的，这个item对应格子的偏移值
        val itemHeightOffset = (totalScrollY % itemHeight).toInt()
        // 设置数组中每个元素的值
        var counter = 0
        while (counter < itemsVisible) {
            var index =
                preCurrentIndex - (itemsVisible / 2 - counter) //索引值，即当前在控件中间的item看作数据源的中间，计算出相对源数据源的index值

            //判断是否循环，如果是循环数据源也使用相对循环的position获取对应的item值，如果不是循环则超出数据源范围使用""空白字符串填充，在界面上形成空白无数据的item项
            if (isLoop) {
                index = getLoopMappingIndex(index)
                visibles!![counter] = adapter!!.getItem(index)
            } else if (index < 0) {
                visibles!![counter] = ""
            } else if (index > adapter!!.getItemsCount() - 1) {
                visibles!![counter] = ""
            } else {
                visibles!![counter] = adapter!!.getItem(index)
            }
            counter++
        }

        //中间两条横线
        canvas.drawLine(0.0f, firstLineY, measuredWidth.toFloat(), firstLineY, paintIndicator!!)
        canvas.drawLine(0.0f, secondLineY, measuredWidth.toFloat(), secondLineY, paintIndicator!!)
        //单位的Label
        if (label != null) {
            val drawRightContentStart = measuredWidth - getTextWidth(paintCenterText!!, label)
            //靠右并留出空隙
            canvas.drawText(
                label!!,
                drawRightContentStart - CENTERCONTENTOFFSET,
                centerY,
                paintCenterText!!
            )
        }
        counter = 0
        while (counter < itemsVisible) {
            canvas.save()
            // L(弧长)=α（弧度）* r(半径) （弧度制）
            // 求弧度--> (L * π ) / (π * r)   (弧长X派/半圆周长)
            val itemHeight = maxTextHeight * lineSpacingMultiplier
            val radian = ((itemHeight * counter - itemHeightOffset) * Math.PI) / halfCircumference
            // 弧度转换成角度(把半圆以Y轴为轴心向右转90度，使其处于第一象限及第四象限
            val angle = (90.0 - (radian / Math.PI) * 180.0).toFloat()
            // 九十度以上的不绘制
            if (angle >= 90f || angle <= -90f) {
                canvas.restore()
            } else {
                val contentText = getContentText(visibles!![counter])

                //计算开始绘制的位置
                measuredCenterContentStart(contentText)
                measuredOutContentStart(contentText)
                val translateY =
                    (radius - cos(radian) * radius - (sin(radian) * maxTextHeight) / 2.0).toFloat()
                //根据Math.sin(radian)来更改canvas坐标系原点，然后缩放画布，使得文字高度进行缩放，形成弧形3d视觉差
                canvas.translate(0.0f, translateY)
                canvas.scale(1.0f, sin(radian).toFloat())
                if (translateY <= firstLineY && maxTextHeight + translateY >= firstLineY) {
                    // 条目经过第一条线
                    canvas.save()
                    canvas.clipRect(0f, 0f, measuredWidth.toFloat(), firstLineY - translateY)
                    canvas.scale(1.0f, sin(radian).toFloat() * SCALECONTENT)
                    canvas.drawText(
                        contentText,
                        drawOutContentStart.toFloat(),
                        maxTextHeight.toFloat(),
                        paintOuterText!!
                    )
                    canvas.restore()
                    canvas.save()
                    canvas.clipRect(
                        0f,
                        firstLineY - translateY,
                        measuredWidth.toFloat(),
                        (itemHeight).toInt().toFloat()
                    )
                    canvas.scale(1.0f, sin(radian).toFloat() * 1f)
                    canvas.drawText(
                        contentText,
                        drawCenterContentStart.toFloat(),
                        maxTextHeight - CENTERCONTENTOFFSET,
                        paintCenterText!!
                    )
                    canvas.restore()
                } else if (translateY <= secondLineY && maxTextHeight + translateY >= secondLineY) {
                    // 条目经过第二条线
                    canvas.save()
                    canvas.clipRect(0f, 0f, measuredWidth.toFloat(), secondLineY - translateY)
                    canvas.scale(1.0f, sin(radian).toFloat() * 1.0f)
                    canvas.drawText(
                        contentText,
                        drawCenterContentStart.toFloat(),
                        maxTextHeight - CENTERCONTENTOFFSET,
                        paintCenterText!!
                    )
                    canvas.restore()
                    canvas.save()
                    canvas.clipRect(
                        0f,
                        secondLineY - translateY,
                        measuredWidth.toFloat(),
                        (itemHeight).toInt().toFloat()
                    )
                    canvas.scale(1.0f, sin(radian).toFloat() * SCALECONTENT)
                    canvas.drawText(
                        contentText,
                        drawOutContentStart.toFloat(),
                        maxTextHeight.toFloat(),
                        paintOuterText!!
                    )
                    canvas.restore()
                } else if (translateY >= firstLineY && maxTextHeight + translateY <= secondLineY) {
                    // 中间条目
                    canvas.clipRect(0, 0, measuredWidth, (itemHeight).toInt())
                    canvas.drawText(
                        contentText,
                        drawCenterContentStart.toFloat(),
                        maxTextHeight - CENTERCONTENTOFFSET,
                        paintCenterText!!
                    )
                    val preSelectedItem: Int = adapter!!.indexOf(visibles[counter])
                    if (preSelectedItem != -1) {
                        selectedItem = preSelectedItem
                    }
                } else {
                    // 其他条目
                    canvas.save()
                    canvas.clipRect(0, 0, measuredWidth, (itemHeight).toInt())
                    canvas.scale(1.0f, sin(radian).toFloat() * SCALECONTENT)
                    canvas.drawText(
                        contentText,
                        drawOutContentStart.toFloat(),
                        maxTextHeight.toFloat(),
                        paintOuterText!!
                    )
                    canvas.restore()
                }
                canvas.restore()
            }
            counter++
        }
    }

    //递归计算出对应的index
    private fun getLoopMappingIndex(index: Int): Int {
        var index = index
        if (index < 0) {
            index = index + adapter!!.getItemsCount()
            index = getLoopMappingIndex(index)
        } else if (index > adapter!!.getItemsCount() - 1) {
            index = index - adapter!!.getItemsCount()
            index = getLoopMappingIndex(index)
        }
        return index
    }

    /**
     * 根据传进来的对象获取getPickerViewText()方法，来获取需要显示的值
     *
     * @param item 数据源的item
     * @return 对应显示的字符串
     */
    private fun getContentText(item: Any?): String {
        if (item == null) {
            return ""
        } else if (item is IPickerViewData) {
            return item.pickerViewText
        }
        return item.toString()
    }

    private fun measuredCenterContentStart(content: String) {
        val rect = Rect()
        paintCenterText!!.getTextBounds(content, 0, content.length, rect)
        when (mGravity) {
            Gravity.CENTER -> drawCenterContentStart =
                ((measuredWidth - rect.width()) * 0.5).toInt()

            Gravity.LEFT -> drawCenterContentStart = 0
            Gravity.RIGHT -> drawCenterContentStart = measuredWidth - rect.width()
        }
    }

    private fun measuredOutContentStart(content: String) {
        val rect = Rect()
        paintOuterText!!.getTextBounds(content, 0, content.length, rect)
        when (mGravity) {
            Gravity.CENTER -> drawOutContentStart = ((measuredWidth - rect.width()) * 0.5).toInt()
            Gravity.LEFT -> drawOutContentStart = 0
            Gravity.RIGHT -> drawOutContentStart = measuredWidth - rect.width()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        this.widthMeasureSpec = widthMeasureSpec
        remeasure()
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val eventConsumed = gestureDetector!!.onTouchEvent(event)
        when (event.getAction()) {
            MotionEvent.ACTION_DOWN -> {
                startTime = System.currentTimeMillis()
                cancelFuture()
                previousY = event.getRawY()
            }

            MotionEvent.ACTION_MOVE -> {
                val dy = previousY - event.getRawY()
                previousY = event.getRawY()
                totalScrollY = (totalScrollY + dy).toInt()

                // 边界处理。
                if (!isLoop) {
                    var top = -initPosition * itemHeight
                    val count = adapter?.getItemsCount() ?: 0
                    var bottom: Float = (count - 1 - initPosition) * itemHeight
                    if (totalScrollY - itemHeight * 0.3 < top) {
                        top = totalScrollY - dy
                    } else if (totalScrollY + itemHeight * 0.3 > bottom) {
                        bottom = totalScrollY - dy
                    }

                    if (totalScrollY < top) {
                        totalScrollY = top.toInt()
                    } else if (totalScrollY > bottom) {
                        totalScrollY = bottom.toInt()
                    }
                }
            }

            MotionEvent.ACTION_UP -> if (!eventConsumed) {
                val y = event.getY()
                val l = acos(((radius - y) / radius).toDouble()) * radius
                val circlePosition = ((l + itemHeight / 2) / itemHeight).toInt()

                val extraOffset = (totalScrollY % itemHeight + itemHeight) % itemHeight
                mOffset = ((circlePosition - itemsVisible / 2) * itemHeight - extraOffset).toInt()

                if ((System.currentTimeMillis() - startTime) > 120) {
                    // 处��拖拽事件
                    smoothScroll(ACTION.DAGGLE)
                } else {
                    // 处理条目点击事件
                    smoothScroll(ACTION.CLICK)
                }
            }

            else -> if (!eventConsumed) {
                val y = event.getY()
                val l = acos(((radius - y) / radius).toDouble()) * radius
                val circlePosition = ((l + itemHeight / 2) / itemHeight).toInt()

                val extraOffset = (totalScrollY % itemHeight + itemHeight) % itemHeight
                mOffset = ((circlePosition - itemsVisible / 2) * itemHeight - extraOffset).toInt()

                if ((System.currentTimeMillis() - startTime) > 120) {
                    smoothScroll(ACTION.DAGGLE)
                } else {
                    smoothScroll(ACTION.CLICK)
                }
            }
        }
        invalidate()

        return true
    }

    val itemsCount: Int
        /**
         * 获取Item个数
         *
         * @return item个数
         */
        get() = adapter?.getItemsCount() ?: 0

    /**
     * 附加在右边的单位字符串
     *
     * @param label 单位
     */
    fun setLabel(label: String?) {
        this.label = label
    }

    fun setGravity(gravity: Int) {
        this.mGravity = gravity
    }

    fun getTextWidth(paint: Paint, str: String?): Int {
        var iRet = 0
        if (str != null && str.length > 0) {
            val len = str.length
            val widths = FloatArray(len)
            paint.getTextWidths(str, widths)
            for (j in 0 until len) {
                iRet += ceil(widths[j].toDouble()).toInt()
            }
        }
        return iRet
    }

    companion object {
        // 条目间距倍数
        const val lineSpacingMultiplier: Float = 2.0f

        // 修改这个值可以改变滑行速度
        private const val VELOCITYFLING = 5
        private const val SCALECONTENT = 0.8f //非中间文字则用此控制高度，压扁形成3d错觉
        private const val CENTERCONTENTOFFSET = 6f //中间文字文字居中需要此偏移值
    }
}