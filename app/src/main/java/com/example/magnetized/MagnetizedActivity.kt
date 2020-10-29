package com.example.magnetized

import android.content.Context
import android.content.Intent
import android.hardware.*
import android.hardware.SensorManager.SENSOR_DELAY_NORMAL
import android.hardware.SensorManager.SENSOR_DELAY_UI
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_magnetized.*
import java.lang.Exception


class MagnetizedActivity : AppCompatActivity() , SensorEventListener{


    private lateinit var sensorManager: SensorManager
    private var magneticSensor : Sensor? = null
    private val accelerometerReading = FloatArray(3)
    private var magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)


    private var isInitialReadingDone : Boolean = false
    private var isInitialReadingRunning : Boolean = false
    private var isFlat : Boolean = false
    private var initialReadingList : MutableList<Float> = mutableListOf()
    private var initialReading : Float = 0.0F

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_magnetized)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
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
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_UI
            )
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also { magneticField ->
            sensorManager.registerListener(
                this,
                magneticField,
                SensorManager.SENSOR_DELAY_NORMAL,
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
        super.onPause()
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
                    displayMagneticReadings(magnetometerReading[2].toString())
                } else if(!isInitialReadingRunning){
                    saveInitialSensorReading(magnetometerReading[2],isInitialReadingDone)
                }
                //if isInitialReadingRunning is true, then basically don't do anything
            }
        }
    }

    //not really used
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Toast.makeText(
            this,
            "Sensor accuracy has changed to ${accuracy.toString()}",
            Toast.LENGTH_SHORT).show()
    }

    private fun displayMagneticReadings(reading : String) {
        try{
            var readingConcat : String = reading + " mT - y \n"
            textView_reading.text = readingConcat
        } catch (e: Exception) {
            Log.e("Reading Error", e.toString())
        }

    }

    private fun saveInitialSensorReading(reading: Float , calculateAverage: Boolean){
        if(!calculateAverage) initialReadingList.add(reading)
        else {
            for(r in initialReadingList) {
                initialReading += r
            }
            initialReading = initialReading/((initialReadingList.size).toFloat())
        }
    }

    private fun checkIfDeviceIsFlat() {
        if (accelerometerReading[1] >= -1.0 && accelerometerReading[1] <= 1.0) {
            if (accelerometerReading[2] <= 9.8 && accelerometerReading[2] >= 8.0) {
                if (accelerometerReading[0] >= -1.0 && accelerometerReading[0] <= 1.0) {
                    isFlat = true
                    textView_instructions1.text = "PHONE IS FACE I["
                }
            }
        } else {
                textView_instructions1.text = "PHONE IS NOT FACE UP"
                isFlat = false
        }
        var readingConcat: String = accelerometerReading[0].toString() + " - 1\n" +
                accelerometerReading[1].toString() + " - 2\n" +
                accelerometerReading[2].toString() + " - 3\n"
        textView_reading.text = readingConcat

    }


}