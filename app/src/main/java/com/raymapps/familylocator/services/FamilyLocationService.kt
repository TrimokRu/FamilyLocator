package com.raymapps.familylocator.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.raymapps.familylocator.R

class FamilyLocationService : Service() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mAuthUser: DatabaseReference
    private lateinit var mCurrentUser: FirebaseUser
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient


    private val CHANNEL_ID = "channel1"

    private val mLocationRequest = LocationRequest.create().apply {
        interval = 60000
        fastestInterval = 30000
        smallestDisplacement = 10f
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) notificationBuilder()
        return START_NOT_STICKY
    }

    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()
        mAuth = FirebaseAuth.getInstance()
        mCurrentUser = mAuth.currentUser!!
        mAuthUser = FirebaseDatabase.getInstance().reference.child(mCurrentUser.uid)
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, locationCallback(), Looper.getMainLooper())
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun notificationBuilder() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val nc = NotificationChannel(CHANNEL_ID, "Family Locator", NotificationManager.IMPORTANCE_NONE)
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.service_title))
            .setContentText(getString(R.string.service_text))
            .setSmallIcon(R.drawable.ic_baseline_location_on_24)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.logo))
            .setOnlyAlertOnce(true)
            .setChannelId(CHANNEL_ID)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .build()
        nm.createNotificationChannel(nc)
        nm.notify(13, notification)
        startForeground(13, notification)
    }

    private fun locationCallback() = object: LocationCallback(){
        override fun onLocationResult(location: LocationResult) {
            super.onLocationResult(location)
            val hashMap = HashMap<String, Any>()
            hashMap["latitude"] = location.lastLocation.latitude
            hashMap["longitude"] = location.lastLocation.longitude
            hashMap["last_update"] = ServerValue.TIMESTAMP
            mAuthUser.updateChildren(hashMap)
        }
    }
}