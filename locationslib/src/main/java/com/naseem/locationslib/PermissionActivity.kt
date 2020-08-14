package com.naseem.locationslib

import android.app.AlertDialog
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.naseem.locationslib.LocationRequester.PermissionResult

class PermissionActivity : AppCompatActivity() {

    private lateinit var mLocationRequest: LocationRequest
    private var mPermissionAsked = false

    override fun onStart() {
        super.onStart()
        val data: LocationRequest? = intent.getParcelableExtra("data")
        mLocationRequest = data ?: LocationRequest()
        if (data == null) {
            finish()
        } else {
            checkPermissionsAskIfNeeded()
        }
    }

    private val mPermissions = arrayOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private fun checkPermissionsAskIfNeeded() {
        val allGranted = mPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) checkGPlayServicesAvailability()
        else if (!mPermissionAsked) requestPermissions()
        else setResult(PermissionResult.LOCATION_PERMISSION_DENIED)
    }

    private fun requestPermissions() {
        mPermissionAsked = true
        ActivityCompat.requestPermissions(
            this, mPermissions,
            REQUEST_LOCATION_PERMISSION
        )
    }

    private fun checkGPlayServicesAvailability() {
        val code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
        if (code != ConnectionResult.SUCCESS) {
            val play = GoogleApiAvailability.getInstance()
            val dialog =
                play.getErrorDialog(this, code,
                    REQUEST_ADDRESS_GOOGLE_SERVICES
                ) {
                    setResult(PermissionResult.PLAY_SERVICES_UNAVAILABLE)
                }
            dialog.show()
        } else {
            checkIfSettingsSatisfied()
        }
    }

    private fun checkIfSettingsSatisfied() {
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(mLocationRequest)
        val locationSettingsRequest = builder.build()
        val settingsClient = LocationServices.getSettingsClient(this)
        val task = settingsClient.checkLocationSettings(locationSettingsRequest)
        task.addOnCompleteListener {
            try {
                it.getResult(ApiException::class.java)
                setResult(PermissionResult.CAN_PROCEED)
            } catch (exception: ApiException) {
                when (exception.statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        try {
                            val resolvable = exception as ResolvableApiException
                            resolvable.startResolutionForResult(
                                this,
                                REQUEST_CHECK_SETTINGS
                            )
                        } catch (e: IntentSender.SendIntentException) {
                            setResult(PermissionResult.UNKNOWN_ERROR)
                        } catch (e: ClassCastException) {
                            setResult(PermissionResult.UNKNOWN_ERROR)
                        }
                    }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        setResult(PermissionResult.LOCATION_UNAVAILABLE)
                    }
                }
            }

        }
    }

    private fun openSettingsDialog() {
        val message = "Please grant all location permissions to continue"
        val builder = AlertDialog.Builder(this)
        builder.setMessage(message)
        builder.setPositiveButton("OPEN SETTINGS") { _, _ ->
            openSettingsPage()
        }.setNegativeButton("CANCEL") { _, _ ->
            setResult(PermissionResult.LOCATION_PERMISSION_DENIED)
        }
        builder.show()
    }

    private fun openSettingsPage() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.fromParts("package", packageName, null)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            when (resultCode) {
                RESULT_OK -> checkPermissionsAskIfNeeded()
                RESULT_CANCELED -> setResult(PermissionResult.LOCATION_SETTINGS_DISABLED)
            }
        } else if (requestCode == REQUEST_ADDRESS_GOOGLE_SERVICES) {
            if (resultCode == RESULT_OK) checkGPlayServicesAvailability()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            when {
                grantResults.size == mPermissions.size && grantResults.all { it == PackageManager.PERMISSION_GRANTED } -> checkGPlayServicesAvailability()
                mPermissions.any {
                    ActivityCompat.shouldShowRequestPermissionRationale(this, it)
                } -> openSettingsDialog()
                else -> setResult(PermissionResult.LOCATION_PERMISSION_DENIED)
            }
        }
    }

    private fun setResult(result: PermissionResult) {
        PermissionObservable.notifyResult(result)
        finish()
    }

    companion object {
        const val REQUEST_CHECK_SETTINGS: Int = 131
        const val REQUEST_ADDRESS_GOOGLE_SERVICES: Int = 132
        const val REQUEST_LOCATION_PERMISSION: Int = 133
    }
}