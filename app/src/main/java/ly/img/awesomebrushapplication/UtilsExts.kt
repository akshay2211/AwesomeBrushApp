package ly.img.awesomebrushapplication

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.view.Window
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import ly.img.awesomebrushapplication.data.permissions
import java.io.File

/**
 * Created by akshay on 20/11/21
 * https://ak1.io
 */

//Bitmap convertor with support of API below 28
internal fun Context.getBitmap(imageUri: Uri) =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, imageUri))
    } else {
        MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
    }

//To retrieved Bounds to clip brushes on screen
internal fun ImageView.getImageBounds(): RectF? {
    return RectF().apply {
        if (drawable != null) {
            imageMatrix.mapRect(this, RectF(drawable.bounds))
        } else {
            return null
        }
    }
}

//writing files to storage via scope and normal manner acc. to Api level
internal fun Context.saveImage(bitmap: Bitmap): Uri? {
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.apply {
                    clear()
                    put(MediaStore.Audio.Media.IS_PENDING, 0)
                }
                contentResolver.update(uri, values, null, null)
            }
        }
        return uri
    } catch (e: java.lang.Exception) {
        if (uri != null) {
            // Don't leave an orphan entry in the MediaStore
            contentResolver.delete(uri, null, null)
        }
        throw e
    }
}

/**
 * merging 2 bitmaps
 * step 1 -> created a blank bitmap of size of image
 * step 2 -> cropped the brush stroke bitmap acc to image bounds
 * step 3 -> scaled the cropped bitmap to match image bitmap in size
 * step 4 -> finally drew all bitmap on single canvas with blank bitmap instance (on step 1)
 * */
internal suspend fun mergeBitmap(back: Bitmap, front: Bitmap?, bounds: RectF?): Bitmap? {
    if (front == null) throw Exception("Editor Bitmap is Empty")
    if (bounds == null) throw Exception("Bounds are null")
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

//visibility extension for any view
fun View.show() {
    visibility = View.VISIBLE
}

//visibility extension for any view
fun View.hide() {
    visibility = View.GONE
}

internal fun activityChooser(uri: Uri?) = Intent.createChooser(Intent().apply {
    type = "image/*"
    action = Intent.ACTION_VIEW
    data = uri
}, "Select Gallery App")


// scaled the actual large image bitmap acc to screen size
internal fun Bitmap.downScaledToScreenSize(window: Window): Bitmap {
    val display = window.windowManager.defaultDisplay
    val size = Point()
    display.getSize(size)

    var finalWidth = size.x
    var finalHeight = (this.height * size.x) / this.width // with width

    if (finalHeight > size.y) {
        finalWidth = (this.width * size.y) / this.height // with height
        finalHeight = size.y
    }

    return Bitmap.createScaledBitmap(this, finalWidth, finalHeight, false)
}

// permission helper to support scope storage API belo 29
internal fun AppCompatActivity.checkAndAskPermission(continueNext: () -> Unit) {
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
        if (ContextCompat.checkSelfPermission(this,
                permissions[0]) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, permissions, MainActivity.PERMISSION_CODE)
        } else {
            continueNext()
        }
    } else {
        continueNext()
    }
}