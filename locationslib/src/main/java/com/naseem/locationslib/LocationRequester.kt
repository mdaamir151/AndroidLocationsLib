package com.naseem.locationslib

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import java.util.*


class LocationRequester private constructor(
    private val mContext: Context,
    private val mLocationRequest: LocationRequest
) : Observer {
    private var mListener: Listener? = null
    private var mPermissionListener: PermissionListener? = null
    private var mState: State =
        State.ONE_SHOT
    private val mLocationProvidersChangedListener = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (LocationManager.PROVIDERS_CHANGED_ACTION == intent?.action) {
                //TODO
            }
        }
    }

    private val mCallback = object : LocationCallback() {
        override fun onLocationResult(res: LocationResult?) {
            if (res != null && res.lastLocation != null) {
                mListener?.onLocationUpdate(res.lastLocation)
            }
        }
    }

    /*
     * Entry Point
     */
    fun checkPermission(permissionListener: PermissionListener) {
        mPermissionListener = permissionListener
        PermissionObservable.addObserver(this)
        val intent = Intent(mContext, PermissionActivity::class.java)
        intent.putExtra("data", mLocationRequest)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        mContext.startActivity(intent)
    }

    fun setListener(listener: Listener) {
        mListener = listener
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(oneShot: Boolean = false) {
        mState = if (oneShot) State.ONE_SHOT
        else State.UPDATING
        FusedLocationProviderClient(mContext).requestLocationUpdates(
            mLocationRequest,
            mCallback,
            Looper.myLooper()
        )
    }

    fun pause() {
        if (mState != State.UPDATING) return
        FusedLocationProviderClient(mContext).removeLocationUpdates(mCallback)
        mState = State.PAUSED
    }

    fun resume() {
        if (mState != State.PAUSED) return
        startLocationUpdates()
    }

    fun fetchFreshLocation() {
        startLocationUpdates(true)
    }

    @SuppressLint("MissingPermission")
    fun fetchLastLocation() {
        val lastLocation = FusedLocationProviderClient(mContext).lastLocation
        lastLocation.addOnSuccessListener {
            if (it != null) {
                mListener?.onLocationUpdate(it)
            } else {
                startLocationUpdates(true)
            }
        }.addOnFailureListener {
            mListener?.onFailure(it)
        }
    }

    enum class LocationPriority { HIGH_ACCURACY, BALANCED_POWER, LOW_POWER, NO_POWER }

    enum class State { ONE_SHOT, UPDATING, PAUSED }

    enum class PermissionResult {
        CAN_PROCEED, PLAY_SERVICES_UNAVAILABLE, LOCATION_PERMISSION_DENIED,
        LOCATION_UNAVAILABLE, UNKNOWN_ERROR, LOCATION_SETTINGS_DISABLED
    }

    interface PermissionListener {
        fun status(code: PermissionResult)
    }

    interface Listener {
        fun onLocationUpdate(location: Location)
        fun onFailure(exception: Exception)
    }

    class Builder(context: Context) {
        private var mContext = context
        private var mFastestInterval = 2_000L //2 sec
        private var mUpdateInterval = 15_000L //15 sec
        private var mPriority =
            LocationPriority.HIGH_ACCURACY

        fun fastestInterval(interval: Long): Builder {
            mFastestInterval = interval
            return this
        }

        fun updateInterval(interval: Long): Builder {
            mUpdateInterval = interval
            return this
        }

        fun priority(priority: LocationPriority): Builder {
            mPriority = priority
            return this
        }

        fun build(): LocationRequester {
            val locationRequest = LocationRequest()
            locationRequest.fastestInterval = mFastestInterval
            locationRequest.interval = mUpdateInterval
            locationRequest.priority = when (mPriority) {
                LocationPriority.HIGH_ACCURACY -> LocationRequest.PRIORITY_HIGH_ACCURACY
                LocationPriority.BALANCED_POWER -> LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
                LocationPriority.LOW_POWER -> LocationRequest.PRIORITY_LOW_POWER
                LocationPriority.NO_POWER -> LocationRequest.PRIORITY_NO_POWER
            }
            return LocationRequester(
                mContext,
                locationRequest
            )
        }
    }

    override fun update(obs: Observable?, arg: Any?) {
        val res = arg as PermissionResult
        mPermissionListener?.status(res)
        PermissionObservable.deleteObserver(this)
    }
}