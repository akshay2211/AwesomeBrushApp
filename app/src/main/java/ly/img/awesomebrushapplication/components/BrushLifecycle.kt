package ly.img.awesomebrushapplication.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import ly.img.awesomebrushapplication.data.PathGroup

/**
 * Created by akshay on 20/11/21
 * https://ak1.io
 */

interface BrushLifecycle {
    fun unDo()
    fun reDo()
    fun generateBitmap(measuredWidth: Int, measuredHeight: Int)
    fun bitmapRecycle()
    fun draw(canvas: Canvas?, pathGroup: PathGroup)
}

internal class BrushLifecycleImpl : BrushLifecycle {
    var paintBitmap: Bitmap? = null
        private set
    var mPaintCanvas: Canvas? = null
        private set

    private val brushStrokePaint = Paint().also {
        it.style = Paint.Style.STROKE
        it.isAntiAlias = true
        it.color = Color.BLACK
        it.strokeJoin = Paint.Join.ROUND
        it.strokeCap = Paint.Cap.ROUND
        it.strokeWidth = 10.0f
    }


    override fun unDo() {
        TODO("Not yet implemented")
    }

    override fun reDo() {
        TODO("Not yet implemented")
    }

    override fun generateBitmap(measuredWidth: Int, measuredHeight: Int) {
        paintBitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888)
        mPaintCanvas = Canvas(paintBitmap!!)
    }

    override fun bitmapRecycle() {
        if (paintBitmap != null && !paintBitmap!!.isRecycled) {
            paintBitmap!!.recycle()
        }
    }

    override fun draw(canvas: Canvas?, pathGroup: PathGroup) {
        if (paintBitmap != null) {
            mPaintCanvas?.drawPath(pathGroup.path, brushStrokePaint)
            mPaintCanvas?.drawPath(pathGroup.childPath, brushStrokePaint)
            canvas?.drawBitmap(paintBitmap!!, 0f, 0f, null)
        }
    }

}