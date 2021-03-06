package com.example.joystick

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */

var textbox : EditText? = null

class SecondFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_second, container, false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.button_second).setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }

        textbox = getView()!!.findViewById<EditText>(R.id.IPTextInput)
        view.findViewById<Button>(R.id.setIP).setOnClickListener {
            var ip = textbox?.text.toString()
            var mqtt_host = "tcp://" + ip + ":1883"
            val save = this.getActivity()!!.getSharedPreferences("preferences", 0)
            save.edit().putString("hostIP", mqtt_host).commit()
            Toast.makeText(activity, "Host set to $mqtt_host", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onStart() {
        super.onStart()

    }
}