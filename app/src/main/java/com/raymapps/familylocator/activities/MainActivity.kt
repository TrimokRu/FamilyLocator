package com.raymapps.familylocator.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.raymapps.familylocator.R
import com.raymapps.familylocator.fragments.FamilyFragment
import com.raymapps.familylocator.fragments.MapsFragment
import com.raymapps.familylocator.fragments.ProfileFragment
import com.raymapps.familylocator.services.FamilyLocationService
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportFragmentManager.beginTransaction().replace(R.id.container, MapsFragment()).commit()

        bottomNav.setOnItemSelectedListener {
            val fragment: Fragment = when(it.itemId){
                R.id.map -> MapsFragment()
                R.id.profile -> ProfileFragment()
                R.id.family -> FamilyFragment()
                    else -> MapsFragment()
                }
            supportFragmentManager.beginTransaction().replace(R.id.container, fragment).commit()
            return@setOnItemSelectedListener true
        }
        val intent = Intent(this, FamilyLocationService::class.java)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            startForegroundService(intent)
        }else startService(intent)
    }
}
