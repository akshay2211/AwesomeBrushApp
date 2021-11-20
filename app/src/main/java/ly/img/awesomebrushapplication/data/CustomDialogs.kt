package ly.img.awesomebrushapplication.data

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.SeekBar
import ly.img.awesomebrushapplication.databinding.SeekbarRowBinding
import petrov.kristiyan.colorpicker.ColorPicker

/**
 * Created by akshay on 20/11/21
 * https://ak1.io
 */

fun Context.showStrokeSizeDialog(
    progress: Int,
    layoutInflater: LayoutInflater,
    sizeChange: (Float) -> Unit,
) {
    AlertDialog.Builder(this).setTitle("Brush Stroke size")
        .setView(SeekbarRowBinding.inflate(layoutInflater).apply {
            seekbar.progress = progress
            this@apply.progress.text = "$progress"
            seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean,
                ) {
                    this@apply.progress.text = "$progress"
                    sizeChange(progress.toFloat())
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {

                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                }
            })
        }.root).setPositiveButton("Ok", null).show()
}

fun Activity.showCustomColorDialog(onChangeColor: (Int) -> Unit) {
    ColorPicker(this).apply {
        setOnChooseColorListener(object : ColorPicker.OnChooseColorListener {
            override fun onChooseColor(position: Int, color: Int) {
                onChangeColor(color)
            }

            override fun onCancel() {
                // put code
            }
        })
    }.show()
}