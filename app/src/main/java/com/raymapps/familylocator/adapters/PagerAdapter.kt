package com.raymapps.familylocator.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.raymapps.familylocator.fragments.FamilyListFragment
import com.raymapps.familylocator.fragments.RequestListFragment

class PagerAdapter(fm: FragmentManager, lifecycle: Lifecycle): FragmentStateAdapter(fm, lifecycle) {

    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        return when(position){
            0-> FamilyListFragment()
            1-> RequestListFragment()
            else-> FamilyListFragment()
        }
    }
}