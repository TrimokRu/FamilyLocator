package com.raymapps.familylocator.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.raymapps.familylocator.R
import com.raymapps.familylocator.data.Person
import com.raymapps.familylocator.databinding.FamilyListItemBinding

class FamilyListAdapter : RecyclerView.Adapter<FamilyListAdapter.FamilyListHolder>() {
    private val mFamilyMutableList = mutableListOf<Person>()
    private val mAuth = FirebaseAuth.getInstance()
    private val mRequests = FirebaseDatabase.getInstance().reference.child("Requests")

    inner class FamilyListHolder(item: View) : RecyclerView.ViewHolder(item) {
        private val binding = FamilyListItemBinding.bind(item)
        fun bind(person: Person) = with(binding) {

            Glide.with(ivProfile.context).load(person.imgUrl).into(ivProfile)
            tvFullName.text = person.fullName

            btnDelete.setOnClickListener {
                mRequests.child(mAuth.currentUser?.uid!!).child(person.uid).removeValue()
                mRequests.child(person.uid).child(mAuth.currentUser?.uid!!).removeValue()
                deleteItem(person)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FamilyListHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.family_list_item, parent, false)
        return FamilyListHolder(view)
    }

    override fun onBindViewHolder(holder: FamilyListHolder, position: Int) {
        holder.bind(mFamilyMutableList[position])
    }

    override fun getItemCount(): Int {
        return mFamilyMutableList.size
    }

    fun addItem(person: Person) {
        if(!mFamilyMutableList.contains(person)) {
            mFamilyMutableList.add(person)
            notifyDataSetChanged()
        }
    }

    fun deleteItem(person: Person){
        if(mFamilyMutableList.contains(person)) {
            mFamilyMutableList.remove(person)
            notifyDataSetChanged()
        }
    }
}