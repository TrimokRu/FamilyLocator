package com.raymapps.familylocator.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.raymapps.familylocator.R
import com.raymapps.familylocator.data.Person
import com.skydoves.powermenu.CustomPowerMenu
import com.skydoves.powermenu.MenuAnimation
import com.skydoves.powermenu.MenuBaseAdapter
import com.skydoves.powermenu.OnMenuItemClickListener
import kotlinx.android.synthetic.main.fragment_maps.*
import java.io.IOException
import java.lang.Exception
import java.lang.NullPointerException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import kotlin.math.*


class MapsFragment : Fragment() {

    private lateinit var mGoogleMap: GoogleMap
    private var mGoogleMapType = GoogleMap.MAP_TYPE_NORMAL
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mAuthRequests: DatabaseReference
    private lateinit var mAuthUsers: DatabaseReference
    private var mFriends = HashMap<Person, Marker>()
    private lateinit var mCurrentUser: FirebaseUser
    private lateinit var mUserProf: DatabaseReference
    private val TAG = "MapsFragment"
    private var mLastKnownLocation: Location? = null
    private lateinit var mSharedPreferences: SharedPreferences

    private lateinit var mHandlerUserInActivity: Handler
    private lateinit var mRunnableUserInActivity: Runnable

    private var GPS_ENABLED: Boolean = false

    @SuppressLint("MissingPermission")
    private val callback = OnMapReadyCallback { googleMap ->
        mGoogleMap = googleMap
        googleMap.isMyLocationEnabled = true
        googleMap.uiSettings.isMyLocationButtonEnabled = false
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        getDeviceLocation()
        getFriends(requireView())
        val locationRequest = LocationRequest.create().apply {
            interval = 60000
            fastestInterval = 30000
            smallestDisplacement = 10f
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        mFusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback(), Looper.getMainLooper())

        userInActivity()
        mGoogleMap.setOnCameraMoveListener {
            stopUserInActivity()
            startUserInActivity()
        }

        if(!mSharedPreferences.getBoolean("autoPos", false)) mBtnAutoPos.iconTint = ColorStateList.valueOf(context?.getColor(R.color.red)!!)

        mBtnAutoPos.setOnClickListener {
            if(!mSharedPreferences.getBoolean("autoPos", false)) {
                mSharedPreferences.edit().putBoolean("autoPos", true).apply()
                mBtnAutoPos.iconTint = ColorStateList.valueOf(context?.getColor(R.color.green)!!)
                gotoLocation(mLastKnownLocation?.latitude, mLastKnownLocation?.longitude)
            }
            else {
                mSharedPreferences.edit().putBoolean("autoPos", false).apply()
                mBtnAutoPos.iconTint = ColorStateList.valueOf(context?.getColor(R.color.red)!!)
            }
        }

        mBtnMyPos.setOnClickListener {
            gotoLocation(mLastKnownLocation?.latitude, mLastKnownLocation?.longitude)
        }

        mBtnMapType.setOnClickListener {
            if(mGoogleMapType == GoogleMap.MAP_TYPE_NORMAL){
                mGoogleMapType = GoogleMap.MAP_TYPE_SATELLITE
                mGoogleMap.mapType = mGoogleMapType
            }else{
                mGoogleMapType = GoogleMap.MAP_TYPE_NORMAL
                mGoogleMap.mapType = mGoogleMapType
            }
        }
        mBtnSOS.setOnClickListener {
            showAlertDialog(getString(R.string.app_name), getString(R.string.sos_message),
                getString(R.string.btn_yes), null,
                getString(R.string.btn_no), null)
        }
    }
    private val mMarkerUsers = mutableMapOf<String, Marker>()
    private fun getFriends(view: View) {
        mAuthRequests.child(mCurrentUser.uid).orderByChild("status").equalTo("accept")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val tempFriend = HashMap<Person, Marker>()
                    for (ds in snapshot.children) {
                        mAuthUsers.child(ds.key!!).addValueEventListener(object: ValueEventListener{
                            @SuppressLint("SimpleDateFormat")
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val person = Person(
                                    snapshot.child("uid").value.toString(),
                                    snapshot.child("imgUrl").value.toString(),
                                    snapshot.child("email").value.toString(),
                                    snapshot.child("fullName").value.toString(),
                                    snapshot.child("latitude").value.toString(),
                                    snapshot.child("longitude").value.toString()
                                )
                                val lastUpdate = Date(snapshot.child("last_update").value.toString().toLong())
                                val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm").format(lastUpdate)
                                val markerOptions = MarkerOptions()
                                markerOptions.position(LatLng(person.latitude.toDouble(), person.longitude.toDouble()))
                                    .title(person.fullName + " - " + dateFormat.toString())
                                if(mMarkerUsers.containsKey(person.uid)) {
                                    mMarkerUsers[person.uid]?.position = LatLng(
                                        person.latitude.toDouble(),
                                        person.longitude.toDouble()
                                    )
                                    mMarkerUsers[person.uid]?.title = person.fullName + " - " + dateFormat.toString()
                                    mFriends.map {
                                        if(it.key.uid == person.uid) {
                                            mFriends[person] = it.value
                                            mFriends.minusAssign(it.key)
                                        }
                                    }
                                }
                                else {
                                    val marker = mGoogleMap.addMarker(markerOptions)
                                    mMarkerUsers[person.uid] = marker!!

                                    val executor = Executors.newSingleThreadExecutor()
                                    val handler = Handler(Looper.getMainLooper())

                                    executor.execute {
                                        val url = URL(person.imgUrl)
                                        var bmp: Bitmap? = null
                                        try {
                                            bmp = BitmapFactory.decodeStream(
                                                url.openConnection().getInputStream()
                                            )
                                            markerOptions.icon(
                                                BitmapDescriptorFactory.fromBitmap(
                                                    bmp
                                                )
                                            )
                                        } catch (e: IOException) {
                                            Log.e(TAG, e.message.toString())
                                        }
                                        handler.post {
                                            bmp?.let {
                                                marker.setIcon(
                                                    BitmapDescriptorFactory.fromBitmap(
                                                        bmp
                                                    )
                                                )
                                            }
                                        }
                                    }
                                    tempFriend[person] = marker
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.d(TAG, error.message)
                            }

                        })
                    }
                    mFriends.minus(tempFriend)
                    mFriends.map {
                        it.value.remove()
                    }
                    mFriends = tempFriend
                }

                override fun onCancelled(error: DatabaseError) {
                    Snackbar.make(view, error.message, Snackbar.LENGTH_SHORT).show()
                }

            })
    }

    private fun showAlertDialog(title: String, 
                                message: String, 
                                positiveText: String?,
                                positiveListener: DialogInterface.OnClickListener?,
                                negativeText: String? = null, 
                                negativeListener: DialogInterface.OnClickListener? = null): androidx.appcompat.app.AlertDialog? {
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveText, positiveListener)
            .setNegativeButton(negativeText, negativeListener)
        return dialog.show()
    }


    override fun onResume() {
        checkGPS()
        super.onResume()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_maps, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mAuth = FirebaseAuth.getInstance()
        mAuthRequests = FirebaseDatabase.getInstance().reference.child("Requests")
        mAuthUsers = FirebaseDatabase.getInstance().reference.child("Users")
        mCurrentUser = mAuth.currentUser!!
        mUserProf = mAuthUsers.child(mCurrentUser.uid)
        mSharedPreferences = requireActivity().getSharedPreferences(requireActivity().packageName, MODE_PRIVATE)
        checkMyPermission()

        fabMap.setOnClickListener {
            if(mFriends.size < 1)
                Snackbar.make(requireView(), requireActivity().getString(R.string.not_found_friends), Snackbar.LENGTH_SHORT).show()
            else {
                Log.d("TEST", mFriends.keys.toString())
                val customMenu = CustomPowerMenu.Builder(requireContext(), FabMenuAdapter())
                    .addItemList(mFriends.keys.toList())
                    .setAnimation(MenuAnimation.SHOWUP_TOP_LEFT)
                    .setMenuRadius(10f)
                    .setMenuShadow(10f)
                    .setWidth(600)
                    .build() as CustomPowerMenu
                customMenu.onMenuItemClickListener =
                    OnMenuItemClickListener { _, item ->
                        item?.let {
                            gotoLocation(item.latitude.toDouble(), item.longitude.toDouble())
                            customMenu.dismiss()
                        }
                    }
                customMenu.showAtCenter(requireView())
            }
        }
    }

    private fun checkMyPermission() {
        Dexter.withContext(requireContext()).withPermissions(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.let {
                        if (report.areAllPermissionsGranted()) {
                            val mapFragment =
                                childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
                            mapFragment?.getMapAsync(callback)
                        } else {
                            val intent = Intent()
                            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            val uri = Uri.fromParts("package", requireActivity().packageName, "")
                            intent.data = uri
                            startActivity(intent)
                        }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: MutableList<PermissionRequest>?,
                    permissionToken: PermissionToken?
                ) {
                    permissionToken?.continuePermissionRequest()
                }

            }).check()
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceLocation() {
        try {
                val locationResult = mFusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {
                        task.result?.let {
                            mLastKnownLocation = task.result
                            mLastKnownLocation?.let {
                                val hashMap = HashMap<String, Any>()
                                hashMap["latitude"] = mLastKnownLocation!!.latitude
                                hashMap["longitude"] = mLastKnownLocation!!.longitude
                                mUserProf.updateChildren(hashMap)
                                gotoLocation(mLastKnownLocation?.latitude, mLastKnownLocation?.longitude, false) }
                            }
                    } else {
                        Log.d(TAG, "Current location is null. Using defaults.")
                        Log.e(TAG, "Exception: %s", task.exception)
                        val latitude = mAuthUsers.child(mCurrentUser.uid).child("latitude").get().result.toString().toDouble()
                        val longitude = mAuthUsers.child(mCurrentUser.uid).child("longitude").get().result.toString().toDouble()
                        gotoLocation(latitude, longitude, false)
                    }
                }
        } catch (e: Exception) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun gotoLocation(latitude: Double?, longitude: Double?, animate: Boolean = true) {
        if(latitude == null || longitude == null) return
        val latLng = LatLng(latitude, longitude)

        val cameraUpdate: CameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 18f)
        if(animate) mGoogleMap.animateCamera(cameraUpdate)
        else mGoogleMap.moveCamera(cameraUpdate)
    }

    private fun locationCallback() = object: LocationCallback(){
        override fun onLocationResult(location: LocationResult) {
            super.onLocationResult(location)
            val hashMap = HashMap<String, Any>()
            hashMap["latitude"] = location.lastLocation.latitude
            hashMap["longitude"] = location.lastLocation.longitude
            hashMap["last_update"] = ServerValue.TIMESTAMP
            mUserProf.updateChildren(hashMap)
            mLastKnownLocation = location.lastLocation
        }
    }
     private fun userInActivity(){
         mHandlerUserInActivity = Handler(Looper.getMainLooper())
         mRunnableUserInActivity = Runnable {
             if(mSharedPreferences.getBoolean("autoPos", false))
                 gotoLocation(mLastKnownLocation?.latitude, mLastKnownLocation?.longitude)
         }
     }

    private fun startUserInActivity() = mHandlerUserInActivity.postDelayed(mRunnableUserInActivity, 6000)
    private fun stopUserInActivity() = mHandlerUserInActivity.removeCallbacks(mRunnableUserInActivity)

    inner class FabMenuAdapter: MenuBaseAdapter<Person>() {
        @SuppressLint("SetTextI18n")
        override fun getView(index: Int, view: View?, viewGroup: ViewGroup?): View {
            val ctx = viewGroup?.context
            var vw = view
            if(vw == null){
                val inflater = ctx?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                vw = inflater.inflate(R.layout.item_map_menu, viewGroup, false)
            }
            val item = getItem(index) as Person
            vw?.let {
                Glide.with(it).load(item.imgUrl).into(it.findViewById(R.id.ivPersonImg))
                it.findViewById<TextView>(R.id.tvPersonName).text = "${item.fullName} ~ ${distance(item.longitude.toDouble(), item.latitude.toDouble(),
                mLastKnownLocation?.longitude, mLastKnownLocation?.latitude)} ${requireActivity().getString(R.string.meters)}"
            }
            return super.getView(index, vw, viewGroup)
        }
    }

    private fun distance(lon1: Double, lat1: Double, lon2: Double?, lat2: Double?): Int {
        if(lon2 == null || lat2 == null) return 0
        val radius = 6371
        val latDistance = Math.toRadians(lat2 - lat1)
        val lonDistance = Math.toRadians(lon2 - lon1)
        val a = (sin(latDistance / 2) * sin(latDistance / 2)
                + (cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2))
                * sin(lonDistance / 2) * sin(lonDistance / 2)))
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        var distance = radius * c * 1000
        distance = distance.pow(2.0)
        return round(sqrt(distance)).toInt()
    }

    private fun checkGPS(){
        val lm = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try{
            GPS_ENABLED = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        }catch (ex: Exception){ ex.printStackTrace() }

        if(!GPS_ENABLED) {
            showAlertDialog("GPS",
                requireActivity().getString(R.string.gps_off),
                "OK",
                { _, _ -> requireActivity().startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) })
        }
    }
}

