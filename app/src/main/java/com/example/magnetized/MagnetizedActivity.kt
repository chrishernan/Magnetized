package com.example.magnetized

import android.content.Context
import android.content.Intent
import android.hardware.*
import android.hardware.SensorManager.SENSOR_DELAY_NORMAL
import android.hardware.SensorManager.SENSOR_DELAY_GAME
import android.hardware.SensorManager.SENSOR_DELAY_UI
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_magnetized.*
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Exception


class MagnetizedActivity : AppCompatActivity() , SensorEventListener{

    //Sensor variables
    private lateinit var sensorManager: SensorManager
    private var magneticSensor : Sensor? = null
    private val accelerometerReading = FloatArray(3)
    private var magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    // Variables to help determine magnetism
    private var isInitialReadingDone : Boolean = false
    private var isInitialReadingRunning : Boolean = false
    private var isFlat : Boolean = false
    private var isMagnetized : Boolean = false
    private var initialReadingList : MutableList<Float> = mutableListOf()
    private var initialReading : Float = 0.0F
    private var count : Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_magnetized)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        button_check_again.setOnClickListener {
            isFlat = false
            isInitialReadingDone = false
            isInitialReadingRunning = false
            isMagnetized = false
            initialReading = 0.0F
            initialReadingList.clear()
            count = 0
            textView_reading.text = ""
            button_check_again.visibility = View.INVISIBLE
            textView_app_title_mag_reader.visibility = View.INVISIBLE
        }
    }

    override fun onResume() {
        // Get updates from the accelerometer and magnetometer at a constant rate.
        // To make batch operations more efficient and reduce power consumption,
        // provide support for delaying updates to the application.
        //
        // In this example, the sensor reporting delay is small enough such that
        // the application receives an update before the system checks the sensor
        // readings again.
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
            sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_UI,
                SensorManager.SENSOR_DELAY_UI
            )
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also { magneticField ->
            sensorManager.registerListener(
                this,
                magneticField,
                SensorManager.SENSOR_DELAY_UI,
                SensorManager.SENSOR_DELAY_UI
            )
        }
        super.onResume()
    }

    override fun onPause() {
        // Don't receive any more updates from either sensor.
        sensorManager.unregisterListener(this)
        isFlat = false
        isInitialReadingDone = false
        isInitialReadingRunning = false
        isMagnetized = false
        count = 0
        initialReading = 0.0F
        initialReadingList.clear()
        super.onPause()
        if(textView_app_title_mag_reader.visibility == View.VISIBLE) textView_app_title_mag_reader.visibility = View.INVISIBLE
    }

    /**
     *
     */
    override fun onSensorChanged(event: SensorEvent?) {

        if(!isFlat){
            if (event!!.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
                checkIfDeviceIsFlat()
            }

        } else if(isFlat) {
            if (event!!.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
                checkIfDeviceIsFlat()
            }
            if (event!!.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)

               if (isInitialReadingDone and !isInitialReadingRunning) {
                   if(!isMagnetized) checkMagnetism(magnetometerReading[2])
                   else if(isMagnetized){
                       //display button to check magnetism again and set pertinent
                       //variables to their starting state
                       button_check_again.visibility = View.VISIBLE
                   }
                } else if(!isInitialReadingDone and !isInitialReadingRunning){
                    saveInitialSensorReading(magnetometerReading[2])
                }
                //if isInitialReadingRunning is true, then basically don't do anything
                //and wait for it to be done
            }
        }
    }

    //not really used so it's empty
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    private fun displayMagneticReadings(reading : String) {
        try{
            var readingConcat : String = "$reading mT"
            textView_reading.text = readingConcat
        } catch (e: Exception) {
            Log.e("Reading Error", e.toString())
        }

    }

    private fun saveInitialSensorReading(reading: Float ){
        textView_reading.text = "Scan Over this when you see readings"
        if(count < 20) {
            count++
            initialReadingList.add(reading)
            isInitialReadingRunning = true
        }
        else if (count >= 20){
            isInitialReadingDone = true
            for(r in initialReadingList) {
                initialReading += r
            }
            initialReading = initialReading/((initialReadingList.size).toFloat())
        }
        isInitialReadingRunning = false
    }

    private fun checkIfDeviceIsFlat() {
        if (accelerometerReading[1] >= -1.2 && accelerometerReading[1] <= 1.2) {
            if (accelerometerReading[2] <= 9.8 && accelerometerReading[2] >= 8.0) {
                if (accelerometerReading[0] >= -1.2 && accelerometerReading[0] <= 1.2) {
                    isFlat = true
                    textView_is_phone_face_up.text = "PHONE IS FACE UP"
                }
            }

        }  else {
            if(isMagnetized) {
                textView_is_phone_face_up.text = "PHONE IS NOT FACE UP"
            }else {
                textView_is_phone_face_up.text = "PHONE IS NOT FACE UP"
                textView_reading.text = ""
                isInitialReadingDone = false
                isInitialReadingRunning = false
                count = 0
                isFlat = false
                initialReading = 0.0F
                initialReadingList.clear()
                if(textView_app_title_mag_reader.visibility == View.VISIBLE) textView_app_title_mag_reader.visibility = View.INVISIBLE
            }
        }
    }

    private fun checkMagnetism(reading : Float) {
        if(reading >= (initialReading +10.0F) || reading <= (initialReading-10.0F)){
            textView_reading.text = "$reading mt"
            textView_app_title_mag_reader.visibility = View.VISIBLE
            isMagnetized = true
        } else {
            displayMagneticReadings(reading.toString())
        }
    }


}