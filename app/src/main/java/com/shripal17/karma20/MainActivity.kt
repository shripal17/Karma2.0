package com.shripal17.karma20

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    horizontal_motion.setOnSeekbarChangeListener {
      Log.d("changeValue", it.toInt().toString())
      horizontal_motion_value.text = it.toInt().toString()
    }

    horizontal_motion.setOnSeekbarFinalValueListener {
      Log.d("finalValue", it.toInt().toString())
      horizontal_motion.minStartValue = 0f
      horizontal_motion_value.text = "0"
    }
  }
}
