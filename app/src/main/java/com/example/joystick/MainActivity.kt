package com.example.joystick

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.SensorManager.getOrientation
import android.hardware.SensorManager.getRotationMatrix
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

    var DEFAULT_DATA_RATE = 35000
    var DATA_RATE = DEFAULT_DATA_RATE

    val RELEASE = MotionEvent.ACTION_UP
    val PRESS = MotionEvent.ACTION_DOWN

    private var sensorManager: SensorManager? = null
    var gmf = FloatArray(3)

    var azimuth_raw = 0
    var azimuth = 0.0f
    var roll = 0.0f
    var pitch = 0.0f

    var buttons = 4 //initially 4 to represent gear down position 0000 0000 0000 0100
    var brake = 0
    var gear = 1

    var fu = 0
    var fd = 0
    var vt = 0

    var xAxis : TextView? = null
    var yAxis : TextView? = null
    var zAxis : TextView? = null
    var throttleAxis : TextView? = null
    var rudderSeek : SeekBar? = null
    var throttleSeek: SeekBar? = null

    var lookUp : Button? = null
    var lookDown : Button? = null
    var lookLeft : Button? = null
    var lookRight : Button? = null


    val vib_len = 100L
    var alpha = 0.15f

    var azimuth_old = 0.0f
    var roll_old = 0.0f
    var pitch_old = 0.0f

    var azimuth_adj = 0.0f
    var roll_adj = 0.0f
    var pitch_adj = 0.0f

    var msg_string = "0 0 0"

    var start = false

    //Button bit positions
    val BRAKE = 0
    val GEAR_UP = 1
    val GEAR_DOWN = 2
    val FLAPS_UP = 3
    val FLAPS_DOWN = 4
    val TOGGLE_VIEW = 5
    val LOOK_UP = 6
    val LOOK_LEFT = 7
    val LOOK_BACK = 8
    val LOOK_RIGHT = 9
    val RESET_VIEW = 10
    val RUDDER_LEFT = 11
    val RUDDER_RIGHT = 12

    // System display. Need this for determining rotation.
    private var mDisplay: Display? = null

    private val mqttClient by lazy {
        MqttClientHelper(this)
    }



    override fun onAccuracyChanged(s: Sensor?, i: Int) {
    }

    fun setBit(shift: Int)
    {
        buttons = buttons or (1 shl shift)
    }

    fun clearBit(shift: Int)
    {
        buttons = buttons and (1 shl shift).inv()
    }

    fun vibrate(view: View)
    {
        val v = (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
        v.vibrate(
            VibrationEffect.createOneShot(
                vib_len,
                VibrationEffect.DEFAULT_AMPLITUDE
            )
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onStart() {
        super.onStart()

        //FLAPS_UP
        findViewById<Button>(R.id.flapsUp).setOnTouchListener { v, event ->
            val action = event.action
            val bit = FLAPS_UP
            if(action==RELEASE) clearBit(bit)
            else if (action==PRESS) setBit(bit)
            true
        }

        //FLAPS_DOWN
        findViewById<Button>(R.id.flapsDown).setOnTouchListener { v, event ->
            val action = event.action
            val bit = FLAPS_DOWN
            if(action==RELEASE) clearBit(bit)
            else if (action==PRESS) setBit(bit)
            true
        }

        //TOGGLE_VIEW
        findViewById<Button>(R.id.toggleView).setOnTouchListener { v, event ->
            val action = event.action
            val bit = TOGGLE_VIEW
            if(action==RELEASE) clearBit(bit)
            else if (action==PRESS) setBit(bit)
            true
        }

        //RESET_VIEW
        findViewById<Button>(R.id.resetView).setOnTouchListener { v, event ->
            val action = event.action
            val bit = RESET_VIEW
            if(action==RELEASE) clearBit(bit)
            else if (action==PRESS) setBit(bit)
            true
        }

        //UP
        findViewById<Button>(R.id.lookUp).setOnTouchListener { v, event ->
            val action = event.action
            val bit = LOOK_UP
            if(action==RELEASE) clearBit(bit)
            else if (action==PRESS) setBit(bit)
            true
        }

        //DOWN
        findViewById<Button>(R.id.lookDown).setOnTouchListener { v, event ->
            val action = event.action
            val bit = LOOK_BACK
            if(action==RELEASE) clearBit(bit)
            else if (action==PRESS) setBit(bit)
            true
        }

        //LEFT
        findViewById<Button>(R.id.lookLeft).setOnTouchListener { v, event ->
            val action = event.action
            val bit = LOOK_LEFT
            if(action==RELEASE) clearBit(bit)
            else if (action==PRESS) setBit(bit)
            true
        }

        //RIGHT
        findViewById<Button>(R.id.lookRight).setOnTouchListener { v, event ->
            val action = event.action
            val bit = LOOK_RIGHT
            if(action==RELEASE) clearBit(bit)
            else if (action==PRESS) setBit(bit)
            true
        }

        //RUDDER_LEFT
        findViewById<Button>(R.id.rudderLeft).setOnTouchListener { v, event ->
            val action = event.action
            val bit = RUDDER_LEFT
            if(action==RELEASE) clearBit(bit)
            else if (action==PRESS) setBit(bit)
            true
        }
        //RUDDER_RIGHT
        findViewById<Button>(R.id.rudderRight).setOnTouchListener { v, event ->
            val action = event.action
            val bit = RUDDER_RIGHT
            if(action==RELEASE) clearBit(bit)
            else if (action==PRESS) setBit(bit)
            true
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            zeroAxis()
        }



        setMqttCallBack()
        Timer("CheckMqttConnection", false).schedule(3000) {
            if (!mqttClient.isConnected()) {
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "Failed to connect to: '$SOLACE_MQTT_HOST' within 3 seconds",
                    Snackbar.LENGTH_INDEFINITE
                )
                    .setAction("Action", null).show()
            }
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager!!.registerListener(
            this,
            sensorManager!!.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
            1000000
        )
        sensorManager!!.registerListener(
            this,
            sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            DATA_RATE
        )

        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        mDisplay = wm.defaultDisplay
    }


    override fun onSensorChanged(event: SensorEvent?) {
        if (event!!.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            if (start)
            {
                getAccelerometer(event)
            }
        }
        if (event!!.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            getMagField(event)
        }
    }

    private fun setMqttCallBack() {
        mqttClient.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(b: Boolean, s: String) {
                val snackbarMsg = "Connected to host:\n'$SOLACE_MQTT_HOST'."
                Log.w("Debug", snackbarMsg)
                Snackbar.make(
                    findViewById(android.R.id.content),
                    snackbarMsg,
                    Snackbar.LENGTH_SHORT
                )
                    .setAction("Action", null).show()
                start = true
                try {
//                    mqttClient.subscribe("btnRecv")
//                    "Subscribed to topic btnRecv"
                } catch (ex: MqttException) {
                    "Error subscribing to topic: btnRecv"
                }
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                Log.w("Debug", "Message arrived")
//                vt = 0
//                fu = 0
//                fd = 0
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

    fun rud_left(view: View) {
        val v = (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
//        v.vibrate(VibrationEffect.createOneShot(vib_len,
//            VibrationEffect.DEFAULT_AMPLITUDE))
        buttons = buttons xor (1 shl RUDDER_LEFT)
    }
    fun rud_right(view: View) {
        val v = (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
//        v.vibrate(VibrationEffect.createOneShot(vib_len,
//            VibrationEffect.DEFAULT_AMPLITUDE))
        buttons = buttons xor (1 shl RUDDER_RIGHT)
    }

    fun toggle_datarate(view: View) {
        if (DATA_RATE == DEFAULT_DATA_RATE)
        {
            DATA_RATE = 25000
            Toast.makeText(this, "Set data rate to fast", Toast.LENGTH_SHORT).show()
        }
        else
        {
            DATA_RATE = DEFAULT_DATA_RATE
            Toast.makeText(this, "Set data rate to slows", Toast.LENGTH_SHORT).show()
        }
        sensorManager!!.unregisterListener(
            this,
            sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        )
        sensorManager!!.registerListener(
            this,
            sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            DATA_RATE
        )
    }

    fun press_gear(view: View) {
        gear = gear xor 1
        val v = (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
        v.vibrate(
            VibrationEffect.createOneShot(
                vib_len,
                VibrationEffect.DEFAULT_AMPLITUDE
            )
        )
        if (gear == 1) //GEAR_DOWN
        {
            gear_btn.setBackgroundColor(Color.GREEN)
            buttons = buttons or (1 shl GEAR_DOWN) //set GEAR_DOWN bit to 1
            buttons = buttons and (1 shl GEAR_UP).inv() //set GEAR_UP bit to 0
        }
        else //GEAR_UP
        {
            gear_btn.setBackgroundColor(Color.RED)
            buttons = buttons or (1 shl GEAR_UP) //set GEAR_UP bit to 1
            buttons = buttons and (1 shl GEAR_DOWN).inv() //set GEAR_DOWN bit to 0
        }
    }

    fun flaps_up(view: View) {
        val v = (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
        v.vibrate(
            VibrationEffect.createOneShot(
                vib_len,
                VibrationEffect.DEFAULT_AMPLITUDE
            )
        )
        fu = fu xor 1
        buttons = buttons xor (1 shl FLAPS_UP)
    }

    fun flaps_down(view: View) {
        val v = (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
        v.vibrate(
            VibrationEffect.createOneShot(
                vib_len,
                VibrationEffect.DEFAULT_AMPLITUDE
            )
        )
        fd = fd xor 1
        buttons = buttons xor (1 shl FLAPS_DOWN)
    }

    fun toggle_view(view: View) {
        val v = (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
        v.vibrate(
            VibrationEffect.createOneShot(
                vib_len,
                VibrationEffect.DEFAULT_AMPLITUDE
            )
        )
        vt = vt xor 1
        buttons = buttons xor (1 shl TOGGLE_VIEW)
    }

    fun reset_view(view: View) {
        val v = (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
        v.vibrate(
            VibrationEffect.createOneShot(
                vib_len,
                VibrationEffect.DEFAULT_AMPLITUDE
            )
        )
        buttons = buttons xor (1 shl RESET_VIEW)
    }

    fun look_up(view: View) {
        buttons = buttons xor (1 shl LOOK_UP)
    }

    fun look_left(view: View) {
        buttons = buttons xor (1 shl LOOK_LEFT)
    }

    fun look_back(view: View) {
        buttons = buttons xor (1 shl LOOK_BACK)
    }

    fun look_right(view: View) {
        buttons = buttons xor (1 shl LOOK_RIGHT)
    }

    fun toggle_brake(view: View) {
        brake = brake xor 1
        if (brake == 1)
        {
            brake_btn.setBackgroundColor(Color.RED)
            buttons = buttons or (1 shl BRAKE)
        }
        else
        {
            brake_btn.setBackgroundColor(Color.WHITE)
            buttons = buttons and (1 shl BRAKE).inv()
        }
        val v = (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
        v.vibrate(
            VibrationEffect.createOneShot(
                vib_len,
                VibrationEffect.DEFAULT_AMPLITUDE
            )
        )
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

//        rudderSeek = findViewById<SeekBar>(R.id.rudderSeek)
//        var azimuth_raw = 0
        var throttle_raw = 0
//        if (rudderSeek != null)
//        {
//            azimuth_raw = rudderSeek!!.getProgress()
//        }

        throttleSeek = findViewById<SeekBar>(R.id.throttleSeek)
        if (throttleSeek != null)
        {
            throttle_raw = throttleSeek!!.getProgress()
        }

        val ori = FloatArray(3)
        val rMat = FloatArray(9)

        getRotationMatrix(rMat, null, event.values, gmf)
        val rotationMatrixAdjusted = FloatArray(9)
        SensorManager.remapCoordinateSystem(
            rMat,
            SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X,
            rotationMatrixAdjusted
        );
        getOrientation(rMat, ori)

//        azimuth = ori[0]
        val OldMin = 0
        val OldMax = 100
        val NewMin = -30f
        val NewMax = 30f

        val OldRange = (OldMax - OldMin)
        val NewRange = (NewMax - NewMin)
        azimuth = scale(azimuth_raw, 0, 100, -30f, 30f)
        roll = ori[1]
        pitch = ori[2]
        var adjusted_throttle = scale(throttle_raw, 0, 100, -44.99f, 44.99f)

//        azimuth = (azimuth * 180/Math.PI).toFloat()
        pitch = (pitch * 180/Math.PI).toFloat()
        roll = (roll * 180/Math.PI).toFloat()

        var adjusted_azimuth = azimuth - azimuth_adj
        var adjusted_roll = roll - roll_adj
        var adjusted_pitch = pitch - pitch_adj

        adjusted_roll = (-1 * adjusted_roll).toFloat()
//        adjusted_pitch = (pitch + 90).toFloat()

        val v = (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
        var hit_limit = false;
        if (adjusted_azimuth >= 45)
        {
            adjusted_azimuth = 44.99f
            hit_limit = true;
        }
        else if (adjusted_azimuth <= -45) {
            adjusted_azimuth = -44.99f
            hit_limit = true;
        }

        if (adjusted_roll >= 45)
        {
            adjusted_roll = 44.99f
            hit_limit = true;
        }
        else if (adjusted_roll <= -45) {
            adjusted_roll = -44.99f
            hit_limit = true;
        }

        if (adjusted_pitch >= 45)
        {
            adjusted_pitch = 44.99f
            hit_limit = true;
        }
        else if (adjusted_pitch <= -45) {
            adjusted_pitch = -44.99f
            hit_limit = true;
        }

        if (hit_limit) {
            v.vibrate(
                VibrationEffect.createOneShot(
                    400L,
                    VibrationEffect.EFFECT_HEAVY_CLICK
                )
            )
        }


//        azimuth = azimuth + alpha * (azimuth_old - azimuth)
//        roll = roll + alpha * (roll_old - roll)
//        pitch = pitch + alpha * (pitch_old - pitch)

//        azimuth_old = azimuth
//        roll_old = roll
//        pitch_old = pitch

        xAxis = findViewById(R.id.xAxis)
        yAxis = findViewById(R.id.yAxis)
//        zAxis = findViewById(R.id.zAxis)
        throttleAxis = findViewById(R.id.throttleAxis)

        xAxis?.text = "Roll: ".plus(String.format("%.0f", adjusted_roll))
        yAxis?.text = "Pitch: ".plus(String.format("%.0f", adjusted_pitch))
//        zAxis?.text = "Yaw: ".plus(String.format("%.0f", adjusted_azimuth))
        throttleAxis?.text = "Throttle: ".plus(throttle_raw)

        msg_string = String.format("%.2f", adjusted_roll) + " " + String.format(
            "%.2f",
            adjusted_pitch
        ) + " " + String.format("%.2f", adjusted_azimuth) + " " + String.format(
            "%.2f",
            adjusted_throttle
        ) + " " + String.format("%d", buttons)

        azimuth = 0f
        if (azimuth_raw > 50) {
            azimuth_raw -= 50
            azimuth_raw /= 1024
            azimuth_raw += 50
        }
        else {
            azimuth_raw = 50 - azimuth_raw
            azimuth_raw /= 1024
            azimuth_raw = 50 - azimuth_raw
        }
        rudderSeek?.setProgress(azimuth_raw)

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

    private fun scale(raw: Int, oldMin: Int, oldMax: Int, newMin: Float, newMax: Float): Float {
        val OldRange = (oldMax - oldMin)
        val NewRange = (newMax - newMin)
        return ((((raw - oldMin)*NewRange)/OldRange) + newMin).toFloat()
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
        sensorManager!!.registerListener(
            this,
            sensorManager!!.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
            1000000
        )
        sensorManager!!.registerListener(
            this,
            sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            DATA_RATE
        )
//        setContentView(R.layout.activity_main)
    }

    override fun onPause() {
        super.onPause()
        sensorManager!!.unregisterListener(this)
    }
}