package ly.img.awesomebrushapplication.components

import android.graphics.*
import android.util.Log
import androidx.annotation.ColorInt
import ly.img.awesomebrushapplication.data.CustomPath
import java.util.*

/**
 * Created by akshay on 20/11/21
 * https://ak1.io
 */

interface BrushLifecycle {
    fun unDo(function: (Stack<CustomPath>) -> Unit)
    fun reDo(function: (CustomPath) -> Unit)
    fun generateBitmap(measuredWidth: Int, measuredHeight: Int)
    fun bitmapRecycle()
    fun draw(canvas: Canvas?, path: Path)
    fun pushStack(childPath: Path)
    fun setStrokeSize(progress: Float)
    fun setStrokeColor(@ColorInt color: Int)
    fun reset()
}

internal class BrushLifecycleImpl : BrushLifecycle {

    private val undoList = Stack<CustomPath>()
    private val redoList = Stack<CustomPath>()
    var bounds: RectF? = null
    var paintBitmap: Bitmap? = null
        private set
    private var mPaintCanvas: Canvas? = null


    private val brushStrokePaint = Paint().also {
        it.style = Paint.Style.STROKE
        it.isAntiAlias = true
        it.color = Color.BLACK
        it.strokeJoin = Paint.Join.ROUND
        it.strokeCap = Paint.Cap.ROUND
        it.strokeWidth = 10.0f
    }


    override fun unDo(function: (Stack<CustomPath>) -> Unit) {
        Log.e("undo list", "size ${undoList.size}")
        if (undoList.isNotEmpty()) {
            redoList.push(undoList.lastElement())
            undoList.pop()
            paintBitmap?.eraseColor(Color.TRANSPARENT)
            function(undoList)
        }
    }

    override fun reDo(function: (CustomPath) -> Unit) {
        if (redoList.isNotEmpty()) {
            undoList.push(redoList.lastElement())
            redoList.pop()
            paintBitmap?.eraseColor(Color.TRANSPARENT)
            function(undoList.lastElement())
        }
    }

    override fun generateBitmap(measuredWidth: Int, measuredHeight: Int) {
        paintBitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888)
        bounds?.let {
            mPaintCanvas = Canvas(paintBitmap!!).apply {
                this.clipRect(it)
            }
        }
    }

    override fun bitmapRecycle() {
        if (paintBitmap != null && !paintBitmap!!.isRecycled) {
            paintBitmap!!.recycle()
        }
    }

    override fun draw(canvas: Canvas?, path: Path) {
        if (paintBitmap != null) {
            for (customPath in undoList) {
                mPaintCanvas?.drawPath(customPath.path, Paint().apply {
                    this.style = Paint.Style.STROKE
                    this.isAntiAlias = true
                    this.color = customPath.strokeColor
                    this.strokeJoin = Paint.Join.ROUND
                    this.strokeCap = Paint.Cap.ROUND
                    this.strokeWidth = customPath.strokeWidth
                })
            }

            mPaintCanvas?.drawPath(path, brushStrokePaint)
            canvas?.drawBitmap(paintBitmap!!, 0f, 0f, null)
        }
    }

    override fun pushStack(childPath: Path) {
        if (redoList.isNotEmpty()) redoList.clear()
        undoList.add(CustomPath(childPath, brushStrokePaint.strokeWidth, brushStrokePaint.color))
    }

    override fun setStrokeSize(progress: Float) {
        brushStrokePaint.apply {
            this.strokeWidth = progress.toFloat()
        }
    }

    override fun setStrokeColor(color: Int) {
        brushStrokePaint.apply {
            this.color = color
        }
    }

    override fun reset() {
        undoList.clear()
        redoList.clear()
    }

}