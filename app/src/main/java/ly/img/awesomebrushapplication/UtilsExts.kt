package ly.img.awesomebrushapplication

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import java.io.File

/**
 * Created by akshay on 20/11/21
 * https://ak1.io
 */

fun Context.getBitmap(imageUri: Uri) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, imageUri))
} else {
    MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
}


fun ImageView.getImageBounds(): RectF? {
    return RectF().apply {
        if (drawable != null) {
            imageMatrix.mapRect(this, RectF(drawable.bounds))
        } else {
            return null
        }
    }
}

fun Context.saveImage(bitmap: Bitmap): Uri? {
    var uri: Uri? = null
    try {
        val fileName = System.nanoTime().toString() + ".jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            } else {
                val directory =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                val file = File(directory, fileName)
                put(MediaStore.MediaColumns.DATA, file.absolutePath)
            }
        }

        uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        uri?.let {
            contentResolver.openOutputStream(it).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 0, output)
            }
            values.apply {
                clear()
                put(MediaStore.Audio.Media.IS_PENDING, 0)
            }
            contentResolver.update(uri, values, null, null)
        }
        return uri
    } catch (e: java.lang.Exception) {
        if (uri != null) {
            // Don't leave an orphan entry in the MediaStore
            contentResolver.delete(uri, null, null)
        }
        return null
    }
}

suspend fun mergeBitmap(back: Bitmap, front: Bitmap?, bounds: RectF?): Bitmap? {
    if (front == null) return null
    if (bounds == null) return null
    return Bitmap.createBitmap(back.width, back.height, Bitmap.Config.ARGB_8888).apply {
        val cropFront = Bitmap.createBitmap(
            front,
            bounds.left.toInt(),
            bounds.top.toInt(),
            bounds.width().toInt(),
            bounds.height().toInt()
        )
        val canvas = Canvas(this)
        canvas.drawBitmap(back, 0f, 0f, null)
        canvas.drawBitmap(
            Bitmap
                .createScaledBitmap(cropFront, back.width, back.height, false),
            0f, 0f, null
        )
    }
}

fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.GONE
}