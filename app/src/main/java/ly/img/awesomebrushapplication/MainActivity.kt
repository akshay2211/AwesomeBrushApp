package ly.img.awesomebrushapplication

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
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
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var bitmapCopy: Bitmap? = null
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
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode === PERMISSION_CODE) {
                if (permissions[0] == Manifest.permission.WRITE_EXTERNAL_STORAGE) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        onPressSave()
                    } else {
                        ActivityCompat.requestPermissions(this, permissions, PERMISSION_CODE)
                    }
                }
            }
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
        bitmapCopy = getBitmap(uri).copy(Bitmap.Config.RGB_565, false)
        binding.imageView.apply {
            setImageBitmap(bitmapCopy?.downScaledToScreenSize(window))
        }.post {
            binding.imageView.getImageBounds()?.let {
                binding.canvas.bounds = it
            }
        }
    }

    @MainThread
    private fun onPressSave() {
        checkAndAskPermission {
            if (bitmapCopy == null) {
                Toast.makeText(this@MainActivity,
                    "Add a image from Gallery First",
                    Toast.LENGTH_SHORT).show()
                return@checkAndAskPermission
            }
            binding.progressScreen.show()
            CoroutineScope(Dispatchers.Default).launch {
                saveBrushToGallery()
            }
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
        try {
            mergeBitmap(bitmapCopy!!,
                binding.canvas.getResultBitmap(),
                binding.canvas.bounds)?.also {
                withContext(Dispatchers.IO) {
                    val uri = saveImage(it)
                    withContext(Dispatchers.Main) {
                        binding.progressScreen.hide()
                        startActivity(activityChooser(uri))
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                binding.progressScreen.hide()
                Toast.makeText(this@MainActivity, e.localizedMessage, Toast.LENGTH_SHORT).show()
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
        const val PERMISSION_CODE = 10
    }
}


