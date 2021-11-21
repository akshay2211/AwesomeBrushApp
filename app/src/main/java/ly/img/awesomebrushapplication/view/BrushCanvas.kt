package ly.img.awesomebrushapplication.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import ly.img.awesomebrushapplication.components.BrushLifecycleImpl
import ly.img.awesomebrushapplication.data.Dot
import ly.img.awesomebrushapplication.data.dots

class BrushCanvas @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    init {
        setWillNotDraw(false)
    }

    var bounds: RectF?
        get() = brushLifecycle.bounds
        set(value) {
            brushLifecycle.bounds = value
            reset()
        }


    private val brushLifecycle = BrushLifecycleImpl()
    private val path = Path()

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return event?.let {
            return@let updatePath(event)
        } ?: false
    }

    private fun updatePath(event: MotionEvent): Boolean {
        var result = super.onTouchEvent(event)
        var isFirstPoint = true
        var point = Dot(event.x, event.y)
        var lastPoint = point
        var nextPoint : Dot?= point
        var beforeLastPoint = Dot(0.0f, 0.0f)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isFirstPoint = true
                result = true
                dots.clear()
                dots.add(Dot(event.x, event.y))
            }
            MotionEvent.ACTION_MOVE -> {
                result = true
                isFirstPoint = false
                dots.add(Dot(event.x, event.y))
                nextPoint = dots[dots.size - 1]
                point = dots[dots.size - 2]
                lastPoint = point
                when (dots.size) {
                    1 -> beforeLastPoint = point
                    2 -> beforeLastPoint = lastPoint
                    3 -> {
                        lastPoint = dots[dots.size - 3]
                        beforeLastPoint = lastPoint
                    }
                    else -> {
                        lastPoint = dots[dots.size - 3]
                        beforeLastPoint = dots[dots.size - 4]
                    }
                }

            }
        }

        if (isFirstPoint) {
            path.apply {
                moveTo(point.x, point.y)
                addCircle(point.x, point.y, 0f, Path.Direction.CW)
            }
        } else {
            val pointDx: Float
            val pointDy: Float
            if (nextPoint == null) {
                pointDx = (point.x - lastPoint.x) / SMOOTH_VAL
                pointDy = (point.y - lastPoint.y) / SMOOTH_VAL
            } else {
                pointDx = (nextPoint.x - lastPoint.x) / SMOOTH_VAL
                pointDy = (nextPoint.y - lastPoint.y) / SMOOTH_VAL
            }

            val lastPointDx = (point.x - beforeLastPoint.x) / SMOOTH_VAL
            val lastPointDy = (point.y - beforeLastPoint.y) / SMOOTH_VAL

            path.cubicTo(
                lastPoint.x + lastPointDx,
                lastPoint.y + lastPointDy,
                point.x - pointDx,
                point.y - pointDy,
                point.x,
                point.y
            )
        }
        when (event.action) {
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                nextPoint = null
                result = false
                brushLifecycle.pushStack(Path(path))
                path.reset()
            }
        }
        this.postInvalidate()
        return result
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        brushLifecycle.draw(canvas, path)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        brushLifecycle.bitmapRecycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (brushLifecycle.paintBitmap == null) {
            brushLifecycle.generateBitmap(measuredWidth, measuredHeight)
        }
    }

    fun unDo() {
        brushLifecycle.unDo { _ ->
            this.postInvalidate()
        }
    }

    fun reDo() {
        brushLifecycle.reDo {
            this.postInvalidate()
        }
    }

    fun reset() {
        path.reset()
        brushLifecycle.reset()
        brushLifecycle.bitmapRecycle()
        brushLifecycle.generateBitmap(measuredWidth, measuredHeight)
        this.postInvalidate()
    }

    fun getResultBitmap(): Bitmap? = brushLifecycle.paintBitmap
    fun setStrokeSize(progress: Float) {
        brushLifecycle.setStrokeSize(progress)
        this.postInvalidate()
    }

    companion object {
        private const val SMOOTH_VAL = 3
    }

    fun setStrokeColor(@ColorInt color: Int) {
        brushLifecycle.setStrokeColor(color)
    }
}