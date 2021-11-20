package ly.img.awesomebrushapplication.data

import android.graphics.Paint
import android.graphics.Path
import androidx.annotation.ColorInt

/**
 * Created by akshay on 20/11/21
 * https://ak1.io
 */

internal data class Dot(var x: Float = 0.0f, var y: Float = 0.0f)

internal val dots = ArrayList<Dot>()

data class CustomPath(val path:Path, val strokeWidth: Float, @ColorInt val strokeColor :Int)