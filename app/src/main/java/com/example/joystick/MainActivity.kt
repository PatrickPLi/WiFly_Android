package com.example.joystick

import android.content.Context
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.SensorManager.getOrientation
import android.hardware.SensorManager.getRotationMatrix
import android.os.Bundle
import android.util.Log
import android.view.Display
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.rotationMatrix
import com.example.joystick.mqtt.MqttClientHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_first.*
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

    // System display. Need this for determining rotation.
    private var mDisplay: Display? = null

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
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
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

        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        mDisplay = wm.defaultDisplay
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
        val rotationMatrixAdjusted = FloatArray(9)
        SensorManager.remapCoordinateSystem(rMat,
            SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X,
            rotationMatrixAdjusted);
        getOrientation(rMat, ori)

        azimuth = ori[0]
        roll = ori[1]
        pitch = ori[2]

        azimuth = (azimuth * 180/Math.PI).toFloat()
        pitch = (pitch * 180/Math.PI).toFloat()
        roll = (roll * 180/Math.PI).toFloat()

        var adjusted_azimuth = azimuth - azimuth_adj
        var adjusted_roll = roll - roll_adj
        var adjusted_pitch = pitch - pitch_adj

        adjusted_roll = (-1 * roll).toFloat()
//        adjusted_pitch = (pitch + 90).toFloat()


        if (adjusted_azimuth >= 45)
        {
            adjusted_azimuth = 44.99f
        }
        else if (adjusted_azimuth <= -45) {
            adjusted_azimuth = -44.99f
        }

        if (adjusted_roll >= 45)
        {
            adjusted_roll = 44.99f
        }
        else if (adjusted_roll <= -45) {
            adjusted_roll = -44.99f
        }

        if (adjusted_pitch >= 45)
        {
            adjusted_pitch = 44.99f
        }
        else if (adjusted_pitch <= -45) {
            adjusted_pitch = -44.99f
        }

//        azimuth = azimuth + alpha * (azimuth_old - azimuth)
//        roll = roll + alpha * (roll_old - roll)
//        pitch = pitch + alpha * (pitch_old - pitch)

//        azimuth_old = azimuth
//        roll_old = roll
//        pitch_old = pitch



        xAxis.text = "X Value: ".plus(adjusted_roll.toString())
        yAxis.text = "Y Value: ".plus(adjusted_pitch.toString())
        zAxis.text = "Z Value: ".plus(adjusted_azimuth.toString())

        var msg_string = String.format("%.2f", adjusted_roll) + " " + String.format("%.2f", adjusted_pitch) + " " + String.format("%.2f", adjusted_azimuth)

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