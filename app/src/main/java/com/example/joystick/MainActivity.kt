package com.example.joystick

import android.content.Context
import android.hardware.*
import android.hardware.SensorManager.getOrientation
import android.hardware.SensorManager.getRotationMatrix
import android.os.Bundle
import android.util.Log
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.core.graphics.rotationMatrix
import kotlinx.android.synthetic.main.fragment_first.*
import java.lang.System.currentTimeMillis
import kotlin.system.measureTimeMillis

import com.example.joystick.mqtt.MqttClientHelper
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.util.*
import kotlin.concurrent.schedule

class MainActivity : AppCompatActivity(), SensorEventListener {

    private var sensorManager: SensorManager? = null
    var gmf = FloatArray(3)

    var azimuth = 0.0f
    var roll = 0.0f
    var pitch = 0.0f

    var alpha = 0.15f

    var azimuth_old = 0.0f
    var roll_old = 0.0f
    var pitch_old = 0.0f

    var azimuth_adj = 0.0f
    var roll_adj = 0.0f
    var pitch_adj = 0.0f

    private val mqttClient by lazy {
        MqttClientHelper(this)
    }

    override fun onAccuracyChanged(s: Sensor?, i: Int) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event!!.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            getAccelerometer(event)
        }
        if (event!!.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            getMagField(event)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            zeroAxis()
        }

        setMqttCallBack()
        Timer("CheckMqttConnection", false).schedule(3000) {
            if (!mqttClient.isConnected()) {
                Snackbar.make(findViewById(android.R.id.content),"Failed to connect to: '$SOLACE_MQTT_HOST' within 3 seconds", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Action", null).show()
            }
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    private fun setMqttCallBack() {
        mqttClient.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(b: Boolean, s: String) {
                val snackbarMsg = "Connected to host:\n'$SOLACE_MQTT_HOST'."
                Log.w("Debug", snackbarMsg)
                Snackbar.make(findViewById(android.R.id.content), snackbarMsg, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                Log.w("Debug", "Message arrived")
            }

            override fun connectionLost(throwable: Throwable) {
                val snackbarMsg = "Connection to host lost:\n'$SOLACE_MQTT_HOST'"
                Log.w("Debug", snackbarMsg)
                Snackbar.make(findViewById(android.R.id.content), snackbarMsg, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            }

            override fun deliveryComplete(iMqttDeliveryToken: IMqttDeliveryToken) {
                Log.w("Debug", "Message published to host '$SOLACE_MQTT_HOST'")
            }
        })
    }

    private fun zeroAxis() {
        azimuth_adj = azimuth
        pitch_adj = pitch
        roll_adj = roll
        Toast.makeText(this, "Zeroed axes", Toast.LENGTH_SHORT).show()
    }

    private fun getMagField(event: SensorEvent) {
        gmf = event.values
//        Toast.makeText(this, "gmf!", Toast.LENGTH_SHORT).show()
    }

    private fun getAccelerometer(event: SensorEvent) {
        // Movement
        val xVal = event.values[0]
        val yVal = event.values[1]
        val zVal = event.values[2]

        val ori = FloatArray(3)
        val rMat = FloatArray(9)

        getRotationMatrix(rMat, null, event.values, gmf)
        getOrientation(rMat, ori)

        azimuth = ori[0]
        roll = ori[1]
        pitch = ori[2]

        azimuth = (azimuth * 180/Math.PI).toFloat()
        pitch = (pitch * 180/Math.PI).toFloat()
        roll = (roll * 180/Math.PI).toFloat()

        roll = (-1 * roll).toFloat()
        pitch = (pitch + 90).toFloat()

        azimuth = azimuth - azimuth_adj
        roll = roll - roll_adj
        pitch = pitch - pitch_adj

        if (azimuth >= 90)
        {
            azimuth = 89.99f
        }
        else if (azimuth <= -90) {
            azimuth = -89.99f
        }

        if (roll >= 90)
        {
            roll = 89.99f
        }
        else if (roll <= -90) {
            roll = -89.99f
        }

        if (pitch >= 90)
        {
            pitch = 89.99f
        }
        else if (pitch <= -90) {
            pitch = 89.99f
        }

//        azimuth = azimuth + alpha * (azimuth_old - azimuth)
//        roll = roll + alpha * (roll_old - roll)
//        pitch = pitch + alpha * (pitch_old - pitch)

//        azimuth_old = azimuth
//        roll_old = roll
//        pitch_old = pitch



        xAxis.text = "X Value: ".plus(roll.toString())
        yAxis.text = "Y Value: ".plus(pitch.toString())
        zAxis.text = "Z Value: ".plus(azimuth.toString())

        var msg_string = String.format("%.2f", roll) + " " + String.format("%.2f", pitch) + " " + String.format("%.2f", azimuth)

        try {
            mqttClient.publish("joystick_axis", msg_string)
            "Published to topic 'joystick_axis'"
        } catch (ex: MqttException) {
            "Error publishing to topic: joystick_axis"
        }

//        val accelerationSquareRoot = (xVal * xVal + yVal * yVal + zVal * zVal) / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH)
//
//        if (accelerationSquareRoot >= 3) {
//            Toast.makeText(this, "Device was shuffled", Toast.LENGTH_SHORT).show()
//        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
    override fun onResume() {
        super.onResume()
        sensorManager!!.registerListener(this, sensorManager!!.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager!!.registerListener(this, sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        sensorManager!!.unregisterListener(this)
    }
}