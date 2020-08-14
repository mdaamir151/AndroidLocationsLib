package com.naseem.locationlib

import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.naseem.locationslib.LocationRequester

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val btn = findViewById<Button>(R.id.btn)
        val textView = findViewById<TextView>(R.id.text_view)
        val listener = object : LocationRequester.Listener {
            override fun onLocationUpdate(location: Location) {
                textView.text = "(${location.latitude}, ${location.longitude})"
            }

            override fun onFailure(exception: Exception) {
                textView.text = "Exception: $exception"
            }
        }
        val locReq = LocationRequester.Builder(this).build()
        locReq.setListener(listener)
        btn.setOnClickListener {
            locReq.checkPermission(object : LocationRequester.PermissionListener {
                override fun status(code: LocationRequester.PermissionResult) {
                    val txt = when (code) {
                        LocationRequester.PermissionResult.CAN_PROCEED -> {
                            locReq.startLocationUpdates()
                            "CAN_PROCEED"
                        }
                        LocationRequester.PermissionResult.PLAY_SERVICES_UNAVAILABLE -> "PLAY_SERVICES_UNAVAILABLE"
                        LocationRequester.PermissionResult.LOCATION_PERMISSION_DENIED -> "LOCATION_PERMISSION_DENIED"
                        LocationRequester.PermissionResult.LOCATION_UNAVAILABLE -> "LOCATION_UNAVAILABLE"
                        LocationRequester.PermissionResult.UNKNOWN_ERROR -> "UNKNOWN_ERROR"
                        LocationRequester.PermissionResult.LOCATION_SETTINGS_DISABLED -> "LOCATION_SETTINGS_DISABLED"
                    }
                    Toast.makeText(this@MainActivity, txt, Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}