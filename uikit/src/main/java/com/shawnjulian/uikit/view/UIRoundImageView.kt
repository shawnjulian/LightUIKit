package com.shawnjulian.uikit.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.createBitmap
import com.shawnjulian.uikit.R
import kotlin.math.max
import kotlin.math.min

/**
 * @ClassName UIRoundImageView
 * @Author ShawnJulian
 * @Date 2026/6/16
 * @Description Circular and rounded corner images
 */
class UIRoundImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    /**
     * Image type: circular or rounded corner
     */
    private var type: Int

    /**
     * Border color and width
     */
    private var mBorderColor: Int
    private var mBorderWidth: Float

    /**
     * Corner radius size
     */
    private var mCornerRadius: Float

    // Top-left corner radius
    private var mLeftTopCornerRadius: Float

    // Top-right corner radius
    private var mRightTopCornerRadius: Float

    // Bottom-left corner radius
    private var mLeftBottomCornerRadius: Float

    // Bottom-right corner radius
    private var mRightBottomCornerRadius: Float

    /**
     * Drawing paint
     */
    private var mBitmapPaint = Paint()
    private var mBorderPaint = Paint()

    /**
     * Corner radius value
     */
    private var mRadius = 0f

    /**
     * 3x3 matrix, mainly used for scaling
     */
    private var mMatrix: Matrix? = null

    /**
     * Render image, use image to shade drawing shapes
     */
    private var mBitmapShader: BitmapShader? = null

    /**
     * View width
     */
    private var mWidth = 0

    /**
     * Rounded corner image area
     */
    private var mRoundRect: RectF = RectF()

    private var mRoundPath: Path = Path()


    init {

        mMatrix = Matrix()

        mBitmapPaint = Paint().apply {
            isAntiAlias = true
        }

        mBorderPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
        }

        val a = context.obtainStyledAttributes(attrs, R.styleable.RoundImageView, defStyleAttr, 0)

        type = a.getInt(
            R.styleable.RoundImageView_type,
            TYPE_OVAL
        )
        mBorderColor = a.getColor(R.styleable.RoundImageView_border_color, Color.TRANSPARENT)
        mBorderWidth = a.getDimension(R.styleable.RoundImageView_border_width, 0f)
        mCornerRadius =
            a.getDimension(R.styleable.RoundImageView_corner_radius, dp2px(10).toFloat())
        mLeftTopCornerRadius = a.getDimension(R.styleable.RoundImageView_leftTop_corner_radius, 0f)
        mLeftBottomCornerRadius =
            a.getDimension(R.styleable.RoundImageView_leftBottom_corner_radius, 0f)
        mRightTopCornerRadius =
            a.getDimension(R.styleable.RoundImageView_rightTop_corner_radius, 0f)
        mRightBottomCornerRadius =
            a.getDimension(R.styleable.RoundImageView_rightBottom_corner_radius, 0f)

        a.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        /**
         * If the type is circular, force the view width and height to be consistent, with the smaller value as the standard
         */
        if (type == TYPE_CIRCLE) {
            mWidth = min(
                MeasureSpec.getSize(widthMeasureSpec),
                MeasureSpec.getSize(heightMeasureSpec)
            )
            mRadius = mWidth / 2 - mBorderWidth / 2
            setMeasuredDimension(mWidth, mWidth)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Rounded corner image range
        if (type == TYPE_ROUND || type == TYPE_OVAL) {
            mRoundRect = RectF(
                mBorderWidth / 2,
                mBorderWidth / 2,
                w - mBorderWidth / 2,
                h - mBorderWidth / 2
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        mBorderPaint.setColor(mBorderColor)
        mBorderPaint.strokeWidth = mBorderWidth

        if (getDrawable() == null) {
            return
        }

        setUpShader()

        when (type) {
            TYPE_ROUND -> {
                setRoundPath()

                canvas.drawPath(mRoundPath, mBitmapPaint)

                // Draw border
                canvas.drawPath(mRoundPath, mBorderPaint)
            }

            TYPE_CIRCLE -> {
                canvas.drawCircle(
                    mRadius + mBorderWidth / 2,
                    mRadius + mBorderWidth / 2,
                    mRadius,
                    mBitmapPaint
                )

                // Draw border
                canvas.drawCircle(
                    mRadius + mBorderWidth / 2,
                    mRadius + mBorderWidth / 2,
                    mRadius,
                    mBorderPaint
                )
            }

            else -> {
                canvas.drawOval(mRoundRect, mBitmapPaint)
                canvas.drawOval(mRoundRect, mBorderPaint)
            }
        }
    }


    private fun setRoundPath() {
        mRoundPath.reset()

        /**
         * If all four corner radii are the default value 0,
         * set the four corner radii to the value of mCornerRadius
         */
        if (mLeftTopCornerRadius == 0f && mLeftBottomCornerRadius == 0f && mRightTopCornerRadius == 0f && mRightBottomCornerRadius == 0f) {
            mRoundPath.addRoundRect(
                mRoundRect,
                floatArrayOf(
                    mCornerRadius, mCornerRadius,
                    mCornerRadius, mCornerRadius,
                    mCornerRadius, mCornerRadius,
                    mCornerRadius, mCornerRadius
                ),
                Path.Direction.CW
            )
        } else {
            mRoundPath.addRoundRect(
                mRoundRect,
                floatArrayOf(
                    mLeftTopCornerRadius, mLeftTopCornerRadius,
                    mRightTopCornerRadius, mRightTopCornerRadius,
                    mRightBottomCornerRadius, mRightBottomCornerRadius,
                    mLeftBottomCornerRadius, mLeftBottomCornerRadius
                ),
                Path.Direction.CW
            )
        }
    }


    /**
     * Initialize BitmapShader
     */
    private fun setUpShader() {
        val drawable: Drawable = getDrawable() ?: return

        val bmp: Bitmap = drawableToBitmap(drawable)
        // Use bmp as a shader, which means drawing bmp in a specified area
        mBitmapShader = BitmapShader(bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        var scale = 1.0f
        if (type == TYPE_CIRCLE) {
            // Get the smaller value of bitmap width or height
            val bSize = min(bmp.getWidth(), bmp.getHeight())
            scale = mWidth * 1.0f / bSize
            // Center the scaled image
            val dx: Float = (bmp.getWidth() * scale - mWidth) / 2
            val dy: Float = (bmp.getHeight() * scale - mWidth) / 2
            mMatrix?.setTranslate(-dx, -dy)
        } else if (type == TYPE_ROUND || type == TYPE_OVAL) {
            if (!(bmp.getWidth() == width && bmp.getHeight() == height)) {
                // If the image width or height does not match the view width or height, calculate the scaling ratio needed; the width and height of the scaled image must be greater than our view width and height; so we take the larger value here
                scale = max(
                    width * 1.0f / bmp.getWidth(),
                    height * 1.0f / bmp.getHeight()
                )
                // Center the scaled image
                val dx: Float = (scale * bmp.getWidth() - width) / 2
                val dy: Float = (scale * bmp.getHeight() - height) / 2
                mMatrix?.setTranslate(-dx, -dy)
            }
        }

        // Transformation matrix of the shader, mainly used for enlargement or reduction
        mMatrix?.preScale(scale, scale)

        mBitmapShader?.setLocalMatrix(mMatrix)

        // Set transformation matrix
        mBitmapShader?.setLocalMatrix(mMatrix)
        // Set shader
        mBitmapPaint.setShader(mBitmapShader)
    }


    /**
     * Convert drawable to bitmap
     */
    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            val bd = drawable
            return bd.bitmap
        }
        val w = drawable.intrinsicWidth
        val h = drawable.intrinsicHeight
        val bitmap = createBitmap(w, h)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, w, h)
        drawable.draw(canvas)
        return bitmap
    }


    /**
     * Set image type:
     * imageType=0 circular image
     * imageType=1 rounded corner image
     * Default is circular image
     */
    fun setType(imageType: Int): UIRoundImageView {
        if (this.type != imageType) {
            this.type = imageType
            if (this.type != TYPE_ROUND && this.type != TYPE_CIRCLE && this.type != TYPE_OVAL) {
                this.type = TYPE_OVAL
            }
            requestLayout()
        }
        return this
    }


    /**
     * Set the corner radius of the rounded corner image
     */
    fun setCornerRadius(cornerRadius: Int): UIRoundImageView {
        var cornerRadius = cornerRadius
        cornerRadius = dp2px(cornerRadius)
        if (mCornerRadius != cornerRadius.toFloat()) {
            mCornerRadius = cornerRadius.toFloat()
            invalidate()
        }
        return this
    }

    /**
     * Set the top-left corner radius of the rounded corner image
     */
    fun setLeftTopCornerRadius(cornerRadius: Int): UIRoundImageView {
        var cornerRadius = cornerRadius
        cornerRadius = dp2px(cornerRadius)
        if (mLeftTopCornerRadius != cornerRadius.toFloat()) {
            mLeftTopCornerRadius = cornerRadius.toFloat()
            invalidate()
        }
        return this
    }

    /**
     * Set the top-right corner radius of the rounded corner image
     */
    fun setRightTopCornerRadius(cornerRadius: Int): UIRoundImageView {
        var cornerRadius = cornerRadius
        cornerRadius = dp2px(cornerRadius)
        if (mRightTopCornerRadius != cornerRadius.toFloat()) {
            mRightTopCornerRadius = cornerRadius.toFloat()
            invalidate()
        }
        return this
    }

    /**
     * Set the bottom-left corner radius of the rounded corner image
     */
    fun setLeftBottomCornerRadius(cornerRadius: Int): UIRoundImageView {
        var cornerRadius = cornerRadius
        cornerRadius = dp2px(cornerRadius)
        if (mLeftBottomCornerRadius != cornerRadius.toFloat()) {
            mLeftBottomCornerRadius = cornerRadius.toFloat()
            invalidate()
        }
        return this
    }

    /**
     * Set the bottom-right corner radius of the rounded corner image
     */
    fun setRightBottomCornerRadius(cornerRadius: Int): UIRoundImageView {
        var cornerRadius = cornerRadius
        cornerRadius = dp2px(cornerRadius)
        if (mRightBottomCornerRadius != cornerRadius.toFloat()) {
            mRightBottomCornerRadius = cornerRadius.toFloat()
            invalidate()
        }
        return this
    }


    /**
     * Set border width
     */
    fun setBorderWidth(borderWidth: Int): UIRoundImageView {
        var borderWidth = borderWidth
        borderWidth = dp2px(borderWidth)
        if (mBorderWidth != borderWidth.toFloat()) {
            mBorderWidth = borderWidth.toFloat()
            invalidate()
        }
        return this
    }

    /**
     * Set border color
     */
    fun setBorderColor(borderColor: Int): UIRoundImageView {
        if (mBorderColor != borderColor) {
            mBorderColor = borderColor
            invalidate()
        }
        return this
    }

    private fun dp2px(dpVal: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dpVal.toFloat(), resources.displayMetrics
        ).toInt()
    }

    companion object {
        const val TYPE_CIRCLE: Int = 0
        const val TYPE_ROUND: Int = 1
        const val TYPE_OVAL: Int = 2
    }
}