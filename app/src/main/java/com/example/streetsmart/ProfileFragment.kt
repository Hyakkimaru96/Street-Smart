package com.example.streetsmart

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private lateinit var editName: EditText
    private lateinit var editEmail: EditText
    private lateinit var editPassword: EditText
    private lateinit var editProfileButton: Button
    private lateinit var updateProfileButton: Button
    private lateinit var profilePic: ImageView

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        editName = view.findViewById(R.id.editName)
        editEmail = view.findViewById(R.id.editEmail)
        editPassword = view.findViewById(R.id.editPassword)
        editProfileButton = view.findViewById(R.id.editProfileButton)
        updateProfileButton = view.findViewById(R.id.updateProfileButton)
        profilePic = view.findViewById(R.id.imgProfilePic)

        loadUserData()

        editProfileButton.setOnClickListener {
            editName.isEnabled = true
            editEmail.isEnabled = true
            editPassword.visibility = View.VISIBLE
            updateProfileButton.visibility = View.VISIBLE
            editProfileButton.visibility = View.GONE
        }

        updateProfileButton.setOnClickListener {
            val newName = editName.text.toString().trim()
            val newEmail = editEmail.text.toString().trim()
            val newPassword = editPassword.text.toString().trim()

            val user = auth.currentUser
            val userId = user?.uid

            if (userId != null) {
                val updates = hashMapOf(
                    "name" to newName,
                    "email" to newEmail
                )

                db.collection("users").document(userId).set(updates)
                    .addOnSuccessListener {
                        user.updateEmail(newEmail).addOnCompleteListener { emailTask ->
                            if (emailTask.isSuccessful) {
                                if (newPassword.isNotEmpty()) {
                                    user.updatePassword(newPassword).addOnCompleteListener { passTask ->
                                        if (passTask.isSuccessful) {
                                            Toast.makeText(context, "Profile and password updated", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Password update failed", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                                }

                                // Reset editing state
                                editName.isEnabled = false
                                editEmail.isEnabled = false
                                editPassword.visibility = View.GONE
                                updateProfileButton.visibility = View.GONE
                                editProfileButton.visibility = View.VISIBLE

                                loadUserData()
                            } else {
                                Toast.makeText(context, "Email update failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Profile update failed", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        return view
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        editName.setText(document.getString("name") ?: "")
                        editEmail.setText(document.getString("email") ?: "")
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to load profile", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
