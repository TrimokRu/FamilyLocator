package com.raymapps.familylocator.fragments

import android.app.Dialog
import android.os.Bundle
import android.text.TextUtils
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.raymapps.familylocator.R
import com.raymapps.familylocator.adapters.PagerAdapter
import kotlinx.android.synthetic.main.add_friend_dialog.*
import kotlinx.android.synthetic.main.fragment_family.*

class FamilyFragment : Fragment() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mCurrentUser: FirebaseUser
    private lateinit var mAuthUsers: DatabaseReference
    private lateinit var mAuthRequests: DatabaseReference
    private val TAG = "FamilyFragment"
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_family, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mAuth = FirebaseAuth.getInstance()
        mCurrentUser = mAuth.currentUser!!
        mAuthUsers = FirebaseDatabase.getInstance().reference.child("Users")
        mAuthRequests = FirebaseDatabase.getInstance().reference.child("Requests")
        val pagerAdapter = activity?.supportFragmentManager?.let { PagerAdapter(it, lifecycle) }
        viewpager2.adapter = pagerAdapter
        TabLayoutMediator(tabLayout, viewpager2) { tab, position ->
            when (position) {
                0 -> tab.text = "Семья"
                1 -> tab.text = "Заявки"
            }
        }.attach()

        fabAddFriend.setOnClickListener {
            showCustomDialog()
        }
    }

    private fun showCustomDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.add_friend_dialog)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
        with(dialog) {
            btnSendInvite.setOnClickListener {
                if (etEmail.text.toString().isEmpty()) {
                    Snackbar.make(
                        requireView(),
                        "Email не может быть пустым!",
                        Snackbar.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                } else if (!etEmail.text.toString().isEmailValid()) {
                    Snackbar.make(
                        requireView(),
                        "Email имеет неверный формат!",
                        Snackbar.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                } else if (etEmail.text.toString() == mCurrentUser.email) {
                    Snackbar.make(
                        requireView(),
                        "Вы не можете отправить приглашение самому себе!",
                        Snackbar.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                val query = mAuthUsers.orderByChild("email").equalTo(etEmail.text.toString())

                query.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.childrenCount > 0) {
                            val hashMap = HashMap<String, Any>()
                            hashMap["status"] = "pending"
                            mAuthRequests.child(mCurrentUser.uid)
                                .child(snapshot.children.first().key.toString())
                                .updateChildren(hashMap).addOnCompleteListener {
                                    if (it.isSuccessful) {
                                        dialog.hide()
                                        Snackbar.make(
                                            requireView(),
                                            "Приглашение успешно отправлено!",
                                            Snackbar.LENGTH_SHORT
                                        ).show()
                                    } else Snackbar.make(
                                        requireView(),
                                        "Что-то пошло не так!",
                                        Snackbar.LENGTH_SHORT
                                    ).show()
                                }
                        } else {
                            Snackbar.make(
                                requireView(),
                                "Пользователь не найден",
                                Snackbar.LENGTH_SHORT
                            ).show()
                            return
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Snackbar.make(requireView(), error.message, Snackbar.LENGTH_SHORT)
                            .show()
                    }

                })
            }
        }
    }

    private fun String.isEmailValid(): Boolean {
        return !TextUtils.isEmpty(this) && android.util.Patterns.EMAIL_ADDRESS.matcher(this)
            .matches()
    }
}