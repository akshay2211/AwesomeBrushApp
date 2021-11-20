package ly.img.awesomebrushapplication.components

import android.graphics.*
import android.util.Log
import ly.img.awesomebrushapplication.data.PathGroup
import java.util.*

/**
 * Created by akshay on 20/11/21
 * https://ak1.io
 */

interface BrushLifecycle {
    fun unDo(function: (Stack<Path>) -> Unit)
    fun reDo(function: (Path) -> Unit)
    fun generateBitmap(measuredWidth: Int, measuredHeight: Int)
    fun bitmapRecycle()
    fun draw(canvas: Canvas?, pathGroup: PathGroup)
    fun pushStack(childPath: Path)
}

internal class BrushLifecycleImpl : BrushLifecycle {

    private val undoList = Stack<Path>()
    private val redoList = Stack<Path>()

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


    override fun unDo(function: (Stack<Path>) -> Unit) {
        Log.e("undo list", "size ${undoList.size}")
        if (undoList.isNotEmpty()) {
            redoList.push(undoList.lastElement())
            undoList.pop()
            paintBitmap?.eraseColor(Color.TRANSPARENT)
            function(undoList)
        }
    }

    override fun reDo(function: (Path) -> Unit) {
        if (redoList.isNotEmpty()) {
            undoList.push(redoList.lastElement())
            redoList.pop()
            paintBitmap?.eraseColor(Color.TRANSPARENT)
            function(undoList.lastElement())
        }
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

    override fun pushStack(childPath: Path) {
        if (redoList.isNotEmpty()) redoList.clear()
        undoList.add(childPath)
    }

}