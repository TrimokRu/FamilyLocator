package com.raymapps.familylocator.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.raymapps.familylocator.R
import com.raymapps.familylocator.data.Person
import com.raymapps.familylocator.databinding.RequestListItemBinding

class RequestListAdapter : RecyclerView.Adapter<RequestListAdapter.RequestListHolder>() {
    private val mRequestMutableList = mutableListOf<Person>()
    private val mAuth = FirebaseAuth.getInstance()
    private val mAuthRequests = FirebaseDatabase.getInstance().reference.child("Requests")

    inner class RequestListHolder(item: View) : RecyclerView.ViewHolder(item) {
        private val binding = RequestListItemBinding.bind(item)
        fun bind(person: Person) = with(binding) {
            Glide.with(ivProfile.context).load(person.imgUrl).into(ivProfile)
            tvFullName.text = person.fullName
            btnReject.setOnClickListener {
                mAuthRequests.child(person.uid).child(mAuth.currentUser?.uid!!).removeValue()
                deleteItem(person)
            }
            btnAccept.setOnClickListener {
                mAuthRequests.child(person.uid).child(mAuth.currentUser?.uid!!).child("status")
                    .setValue("accept")
                mAuthRequests.child(mAuth.currentUser?.uid!!).child(person.uid).child("status")
                    .setValue("accept")
                deleteItem(person)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestListHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.request_list_item, parent, false)
        return RequestListHolder(view)
    }

    override fun onBindViewHolder(holder: RequestListHolder, position: Int) {
        holder.bind(mRequestMutableList[position])
    }

    override fun getItemCount(): Int {
        return mRequestMutableList.size
    }

    fun contains(person: Person): Boolean {
        return mRequestMutableList.contains(person)
    }

    fun deleteItem(person: Person) {
        if(mRequestMutableList.contains(person)) {
            mRequestMutableList.remove(person)
            notifyDataSetChanged()
        }
    }

    fun addItem(person: Person) {
        if (!mRequestMutableList.contains(person)) {
            mRequestMutableList.add(person)
            notifyDataSetChanged()
        }
    }
}