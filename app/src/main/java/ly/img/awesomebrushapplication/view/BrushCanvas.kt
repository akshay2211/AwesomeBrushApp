package ly.img.awesomebrushapplication.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import ly.img.awesomebrushapplication.components.BrushLifecycleImpl
import ly.img.awesomebrushapplication.data.Dot
import ly.img.awesomebrushapplication.data.PathGroup
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
    private val pathGroup = PathGroup()

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return event?.let {
            return@let updatePath(event)
        } ?: false
    }

    private fun updatePath(event: MotionEvent): Boolean {
        var result = super.onTouchEvent(event)
        // To get a very smooth path we do not simply want to draw lines between two consecutive points,
        // but rather draw a cubic Bezier curve between two consecutive points through two calculated control
        // points. The control points are calculated based on the previous point and the next point, which
        // means that you always have to draw one point in the past.
        //
        // Imagine the user is drawing on screen and as the user drags his finger around on the screen, you receive
        // multiple points. The last point that you receive is point P4. The point that you received prior to that 
        // is point P3 and so on. Now in order to get a smooth drawing, you'll want to draw a cubic Bezier curve between
        // P2 and P3 through control points that are calculated using P1 and P4.
        // 
        // This also means that in order to actually reach the last point that you've received (P4 in the above scenario),
        // you'll have to draw once more **after** the user's finger has already left the screen.
        //
        // If the user only taps on the screen instead of dragging their finger around, you should draw a point.

        // The algorithm below implements the described behavior from above. You only need to fetch the appropriate
        // points from your custom data structure.

        // Note: this should also be replaced by your custom data structure that stores points.

        var isFirstPoint = true
        var point = Dot(event.x, event.y)
        var lastPoint = point
        var nextPoint = point
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
            pathGroup.childPath.apply {
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

            pathGroup.childPath.cubicTo(
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
                result = false
                pathGroup.path.addPath(pathGroup.childPath)
                brushLifecycle.pushStack(Path(pathGroup.childPath))
                pathGroup.childPath.reset()
            }
        }
        this.postInvalidate()
        return result
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        brushLifecycle.draw(canvas, pathGroup)
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
        brushLifecycle.unDo { list ->
            pathGroup.path.reset()
            list.forEachIndexed { index, path ->
                pathGroup.path.addPath(path)
            }
            this.postInvalidate()
        }
    }

    fun reDo() {
        brushLifecycle.reDo {
            pathGroup.path.addPath(it)
            this.postInvalidate()
        }
    }

    fun reset() {
        brushLifecycle.bitmapRecycle()
        brushLifecycle.generateBitmap(measuredWidth, measuredHeight)
    }

    fun getResultBitmap():Bitmap? = brushLifecycle.paintBitmap

    companion object {
        private const val SMOOTH_VAL = 3
    }
}