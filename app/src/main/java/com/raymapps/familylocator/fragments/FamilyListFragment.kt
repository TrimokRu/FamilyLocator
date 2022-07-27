package com.raymapps.familylocator.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.raymapps.familylocator.R
import com.raymapps.familylocator.adapters.FamilyListAdapter
import com.raymapps.familylocator.data.Person
import kotlinx.android.synthetic.main.fragment_family_list.*

class FamilyListFragment : Fragment() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mCurrentUser: FirebaseUser
    private lateinit var mAuthUsers: DatabaseReference
    private lateinit var mAuthRequests: DatabaseReference
    private val mAdapter = FamilyListAdapter()
    private val TAG = "FamilyListFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_family_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mAuth = FirebaseAuth.getInstance()
        mCurrentUser = mAuth.currentUser!!
        mAuthUsers = FirebaseDatabase.getInstance().reference.child("Users")
        mAuthRequests = FirebaseDatabase.getInstance().reference.child("Requests")
        rvFamilyList.layoutManager = LinearLayoutManager(requireContext())
        rvFamilyList.adapter = mAdapter
        getFriends(view)
    }

    private var mFriends = mutableListOf<Person>()
    private fun getFriends(view: View) {
        mAuthRequests.child(mCurrentUser.uid).orderByChild("status").equalTo("accept")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val tempFriend = mutableListOf<Person>()
                    for (ds in snapshot.children) {
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
                            tempFriend.add(person)
                        }
                    }
                    mFriends.minusAssign(tempFriend)
                    mFriends.map{ mAdapter.deleteItem(it) }
                    mFriends = tempFriend
                }

                override fun onCancelled(error: DatabaseError) {
                    Snackbar.make(view, error.message, Snackbar.LENGTH_SHORT).show()
                }

            })
    }
}