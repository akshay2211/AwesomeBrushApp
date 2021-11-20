package ly.img.awesomebrushapplication.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class BrushCanvas @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {
    init {
        setWillNotDraw(false)
    }

    private val brushStrokePaint = Paint().also {
        it.style = Paint.Style.STROKE
    }
    private val path = Path()

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return super.onTouchEvent(event)
        // TODO: Implement touch.
    }

    fun updatePath() {
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
        data class DummyPosType(
            var x:Float = 0.0f,
            var y:Float = 0.0f
        )

        val isFirstPoint: Boolean = TODO()
        if (isFirstPoint) {
            val point: DummyPosType = TODO("Get the first point of your stroke")
            path.moveTo(point.x, point.y)
        } else {
            val point: DummyPosType = TODO("Get the current point to draw a cubic Bezier curve to. In the above scenario this would be P3.")
            val lastPoint: DummyPosType = TODO("The point that you received prior to `point`. In the above scenario this would be P2.")
            val nextPoint: DummyPosType = TODO("The next point that you're going to draw (P4 in the above scenario or the last you get from onTouchEvent) or null if this is the last point.")
            val beforeLastPoint: DummyPosType = TODO("The point you received prior to `lastPoint` or `lastPoint` if you didn't receive a point prior to it. In the above scenario this would be P1.")

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
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        // TODO: This is only an example code how drawing works in general.

        val w = width.toFloat()
        val h = height.toFloat()

        brushStrokePaint.color = Color.BLACK
        brushStrokePaint.strokeWidth = 10.0f

        path.reset()
        path.moveTo(w * 0.1f, h * 0.1f)
        path.lineTo(w * 0.9f, h * 0.25f)
        path.lineTo(w * 0.1f, h * 0.5f)
        path.lineTo(w * 0.9f, h * 0.75f)
        path.lineTo(w * 0.1f, h * 0.9f)

        // TODO: If there is time left, try to implement a cache and draw only the last line instead of everything.
        canvas?.drawPath(path, brushStrokePaint)

    }

    companion object {
        private const val SMOOTH_VAL = 3
    }
}