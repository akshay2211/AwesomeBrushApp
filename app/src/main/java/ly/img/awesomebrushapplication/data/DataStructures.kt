package ly.img.awesomebrushapplication.data

import android.graphics.Path

/**
 * Created by akshay on 20/11/21
 * https://ak1.io
 */

internal data class Dot(var x: Float = 0.0f, var y: Float = 0.0f)

internal val dots = ArrayList<Dot>()

class PathGroup {
    val path = Path()
    val childPath = Path()
}