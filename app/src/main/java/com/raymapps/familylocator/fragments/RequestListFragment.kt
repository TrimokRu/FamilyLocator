package com.raymapps.familylocator.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.raymapps.familylocator.R
import com.raymapps.familylocator.adapters.RequestListAdapter
import com.raymapps.familylocator.data.Person

import kotlinx.android.synthetic.main.fragment_request_list.*

class RequestListFragment : Fragment() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mCurrentUser: FirebaseUser
    private lateinit var mAuthUsers: DatabaseReference
    private lateinit var mAuthRequests: DatabaseReference
    private val mAdapter = RequestListAdapter()
    private val TAG = "RequestListFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_request_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mAuth = FirebaseAuth.getInstance()
        mCurrentUser = mAuth.currentUser!!
        mAuthUsers = FirebaseDatabase.getInstance().reference.child("Users")
        mAuthRequests = FirebaseDatabase.getInstance().reference.child("Requests")
        rvRequestList.layoutManager = LinearLayoutManager(requireContext())
        rvRequestList.adapter = mAdapter
        getRequests()

    }

    private fun getRequests() {
        mAuthRequests.orderByChild(mCurrentUser.uid).addValueEventListener(
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (ds in snapshot.children) {
                        if (ds.child(mCurrentUser.uid).child("status").value == "pending") {
                            mAuthUsers.child(ds.key!!).get().addOnSuccessListener {
                                val person = Person(
                                    it.child("uid").value.toString(),
                                    it.child("imgUrl").value.toString(),
                                    it.child("email").value.toString(),
                                    it.child("fullName").value.toString(),
                                    it.child("latitude").value.toString(),
                                    it.child("longitude").value.toString()
                                )

                                mAdapter.addItem(person)
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
    }
}