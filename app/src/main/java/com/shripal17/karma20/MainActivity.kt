package com.shripal17.karma20

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.library.bluetooth.BluetoothCommunicator
import com.library.bluetooth.BluetoothSelectorActivity
import io.apptik.widget.MultiSlider
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.toast
import java.util.*

class MainActivity : AppCompatActivity() {

  private val BLUETOOTH_DEVICE_PICKER_RC = 896

  private var communicator: BluetoothCommunicator? = null

  private var timer = Timer()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    // initial value is 0
    horizontal_motion.getThumb(0).value = 0

    horizontal_motion.setOnTrackingChangeListener(object : MultiSlider.OnTrackingChangeListener {
      override fun onStartTrackingTouch(multiSlider: MultiSlider?, thumb: MultiSlider.Thumb?, value: Int) {
      }

      // reset to zero when user stops touching the slider
      override fun onStopTrackingTouch(multiSlider: MultiSlider?, thumb: MultiSlider.Thumb?, value: Int) {
        horizontal_motion_value.text = "0"
        horizontal_motion.getThumb(0).value = 0
      }
    })

    // update TextView value
    horizontal_motion.setOnThumbValueChangeListener { _, _, _, value ->
      horizontal_motion_value.text = value.toString()
    }

    // update TextView value
    horizontal_flaps.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        horizontal_flaps_value.text = ((progress - 50) * 2).toString()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar?) {

      }

      override fun onStopTrackingTouch(seekBar: SeekBar?) {

      }
    })

    BluetoothSelectorActivity.openBluetoothPicker(this, deviceNameToAutoConnect = "LCUSM", rc = BLUETOOTH_DEVICE_PICKER_RC)
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == BLUETOOTH_DEVICE_PICKER_RC) {
      val device = BluetoothSelectorActivity.parseResult(requestCode, resultCode, data)

      if (device != null) {
        communicator = BluetoothCommunicator(
          this,
          device.address,
          { status, e ->
            if (status) {
              toast("Connected to ${device.name} successfully")
              timer = Timer()
              timer.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                  sendCommands()
                }
              }, 0, 200)
            } else {
              toast("Could not connect to ${device.name}")
            }
          },
          {
            Log.d("recvd", it)
          },
          {
            toast("Disconnected from ${device.name}")
            timer.stop()
          })
      }

      communicator?.run()
    }
  }

  private fun sendCommands() {
    if (communicator == null) {
      return
    } else if (!communicator!!.isAlive) {
      return
    }
    val sb = StringBuilder()
    sb.apply {
      append(horizontal_flaps_value.text.toString())
      append(",")

      append(horizontal_motion_value.text)
      append(",")

      if (propeller_state.isChecked) {
        if (!propeller_direction.isChecked) {
          append("-")
        }
        append(propeller_speed.progress)
      } else {
        append("0")
      }
      append(",")

      if (pump_state.isChecked) {
        append(pump_speed.progress)
      } else {
        append("0")
      }
      append(",")

      if (submersible_pump_state.isChecked) {
        append("1")
      } else {
        append("0")
      }
    }
    communicator?.write(sb.toString())
  }

  override fun onDestroy() {
    super.onDestroy()
    timer.stop()
    communicator?.release()
  }

  private fun Timer.stop() {
    try {
      cancel()
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }
}
