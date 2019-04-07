package com.shripal17.karma20

import android.content.Intent
import android.os.Bundle
import android.os.Handler
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

  private var horiFlaps = 0
  private var horiDir = 0
  private var propDir = false
  private var propState = false
  private var propSpeed = 0
  private var pumpState = false
  private var pumpSpeed = 0
  private var subPump = false

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
        horiDir = 0
      }
    })

    // update TextView value
    horizontal_motion.setOnThumbValueChangeListener { _, _, _, value ->
      horizontal_motion_value.text = value.toString()
      horiDir = value
    }

    // update TextView value
    horizontal_flaps.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        horizontal_flaps_value.text = ((progress - 50) * 2).toString()
        horiFlaps = (progress - 50) * 2
      }

      override fun onStartTrackingTouch(seekBar: SeekBar?) {

      }

      override fun onStopTrackingTouch(seekBar: SeekBar?) {

      }
    })

    propeller_state.addSwitchObserver { _, isChecked ->
      propState = isChecked
    }

    propeller_direction.addSwitchObserver { _, isChecked ->
      propDir = isChecked
    }

    propeller_speed.setOnProgressChangedListener {
      propSpeed = if (propState) {
        if (propDir) {
          it
        } else {
          -it
        }
      } else {
        0
      }
    }

    pump_state.addSwitchObserver { _, isChecked ->
      pumpState = isChecked
    }

    pump_speed.setOnProgressChangedListener {
      pumpSpeed = if (pumpState) {
        it
      } else {
        0
      }
    }

    submersible_pump_state.addSwitchObserver { _, isChecked ->
      subPump = isChecked
    }

    Handler().postDelayed(
      {
        BluetoothSelectorActivity.openBluetoothPicker(this, rc = BLUETOOTH_DEVICE_PICKER_RC)
      }, 500
    )
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == BLUETOOTH_DEVICE_PICKER_RC) {
      val device = BluetoothSelectorActivity.parseResult(resultCode, data)

      if (device != null) {
        communicator = BluetoothCommunicator(
          this,
          device.address,
          { status, e ->
            if (status) {
              toast("Connected to ${device.name} successfully")
              timer.stop()
              timer = Timer()
              timer.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                  CommandSender.run()
                }
              }, 0, 100)
            } else {
              toast("Could not connect to ${device.name}")
            }
          },
          {
            Log.d("recvd", it)
            val parts = it.split(",")
            val azimuth = parts[0].toDouble()
            val pitch = parts[1].toDouble()
            val roll = parts[2].toDouble()
            val sensorData = StringBuilder().apply {
              append(String.format("Azimuth: %.4f\n", azimuth))
              append(String.format("Pitch: %.4f\n", pitch))
              append(String.format("Roll: %.4f", roll))
            }
            sensor_data.text = sensorData.toString()
          },
          {
            toast("Disconnected from ${device.name}")
            timer.stop()
          })
      }
      Handler().postDelayed(
        {
          communicator?.start()
        }, 2000
      )
    }
  }

  private var CommandSender = Runnable {
    if (communicator == null) {
      return@Runnable
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
    Log.d("data", sb.toString())
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
