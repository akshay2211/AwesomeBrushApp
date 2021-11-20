package ly.img.awesomebrushapplication

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ly.img.awesomebrushapplication.data.showCustomColorDialog
import ly.img.awesomebrushapplication.data.showStrokeSizeDialog
import ly.img.awesomebrushapplication.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var bitmapCopy: Bitmap? = null
    private lateinit var bounds: RectF
    private var defaultStrikeWidth = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater).also { setContentView(it.root) }

        binding.undoButton.setOnClickListener { binding.canvas.unDo() }
        binding.redoButton.setOnClickListener { binding.canvas.reDo() }
        binding.addButton.setOnClickListener { onPressLoadImage() }
        binding.saveButton.setOnClickListener { onPressSave() }
        binding.sizeButton.setOnClickListener { showSizeDialog() }
        binding.colorButton.setOnClickListener { showColorDialog() }
        binding.resetButton.setOnClickListener { binding.canvas.reset() }

        /*

          == Layout ==

            We require a very simple layout here and you can use an XML layout or code to create it:
              * Load Image -> Load an image from the gallery and display it on screen.
              * Save Image -> Save the final image to the gallery.
              * Color -> Let the user select a color from a list of colors.
              * Size -> Let the user specify the radius of a stroke via a slider.
              * Clear all -> Let the user remove all strokes from the image to start over.

          ----------------------------------------------------------------------
         | HINT: The layout doesn't have to look good, but it should be usable. |
          ----------------------------------------------------------------------

          == Requirements ==
              * Your drawing must be applied to the original image, not the downscaled preview. That means that 
                your brush must work in image coordinates instead of view coordinates and the saved image must have 
                the same resolution as the originally loaded image.
              * You can ignore OutOfMemory issues. If you run into memory issues just use a smaller image for testing.

          == Things to consider ==
            These features would be nice to have. Structure your program in such a way, that it could be added afterwards 
            easily. If you have time left, feel free to implement it already.

              * The user can make mistakes, so a history (undo/redo) would be nice.
              * The image usually doesn't change while brushing, but can be replaced with a higher resolution variant. A 
                common scenario would be a small preview but a high-resolution final rendering. Keep this in mind when 
                creating your data structures.
         */


    }

    private fun showColorDialog() {
        showCustomColorDialog {
            onChangeColor(it)
        }
    }

    private fun showSizeDialog() {
        showStrokeSizeDialog(defaultStrikeWidth, layoutInflater) {
            onSizeChanged(it)
        }
    }

    private fun onPressLoadImage() {
        val intent = Intent(Intent.ACTION_PICK)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            intent.type = "image/*"
        } else {
            intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
        }
        startActivityForResult(intent, GALLERY_INTENT_RESULT)
    }


    private fun handleGalleryImage(uri: Uri) {
        // Adjust size of the drawable area, after loading the image.
        bitmapCopy = getBitmap(uri).copy(Bitmap.Config.RGB_565, false)
        binding.imageView.apply {
            setImageBitmap(bitmapCopy)
        }.post {
            binding.imageView.getImageBounds()?.let {
                bounds = it
                binding.canvas.bounds = it
            }
        }

    }

    @MainThread
    private fun onPressSave() {
        if (bitmapCopy == null) {
            Toast.makeText(this@MainActivity,
                "Add a image from Gallery First",
                Toast.LENGTH_SHORT).show()
            return
        }
        binding.progressScreen.show()
        CoroutineScope(Dispatchers.Default).launch {
            saveBrushToGallery()
        }
    }

    private fun onChangeColor(@ColorInt color: Int) {
        binding.canvas.setStrokeColor(color)
        binding.colorButton.setColorFilter(color)
    }

    private fun onSizeChanged(size: Float) {
        defaultStrikeWidth = size.toInt()
        binding.canvas.setStrokeSize(size)
    }

    @WorkerThread
    private suspend fun saveBrushToGallery() {
        // Do not worry about memory here.
        // ... instead just use only test images that fit into the available memory.

        // Because it can take some time to create the brush, it would be nice to indicate progress here, but only if you have time left.
        val bitmap =
            mergeBitmap(bitmapCopy!!,
                binding.canvas.getResultBitmap(),
                binding.canvas.bounds)?.also {
                withContext(Dispatchers.IO) {
                    val uri = saveImage(it)
                    withContext(Dispatchers.Main) {
                        binding.progressScreen.hide()
                        startActivity(Intent.createChooser(Intent().apply {
                            type = "image/*"
                            action = Intent.ACTION_VIEW
                            data = uri
                        }, "Select Gallery App"))
                    }
                }
            }
        if (bitmap == null) {
            withContext(Dispatchers.Main) {
                binding.progressScreen.hide()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (data != null && resultCode == Activity.RESULT_OK && requestCode == GALLERY_INTENT_RESULT) {
            val uri = data.data
            if (uri != null) {
                handleGalleryImage(uri)
            }
        }

    }

    companion object {
        const val GALLERY_INTENT_RESULT = 0
    }
}


