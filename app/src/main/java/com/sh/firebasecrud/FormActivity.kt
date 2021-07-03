package com.sh.firebasecrud

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.sh.firebasecrud.databinding.ActivityFormBinding

class FormActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFormBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var storage: FirebaseStorage
    private lateinit var storageReference: StorageReference

    var imgPath: Uri = Uri.EMPTY
    var imgDownloadUrl: Uri = Uri.EMPTY

    companion object {
        var EXTRA_USER = "user"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val user = intent.extras?.getParcelable<User>(EXTRA_USER)
        database = FirebaseDatabase.getInstance()
        databaseReference = database.getReference("Users")
        storage = Firebase.storage
        storageReference = storage.reference

        setInput(user)

        with(binding) {
            imgAvatar.setOnClickListener {
                Log.d("IMG_AVATAR", "clicked")
                ImagePicker.with(this@FormActivity)
                    .saveDir(getExternalFilesDir(Environment.DIRECTORY_DCIM)!!)
                    .crop()                    //Crop image(Optional), Check Customization for more option
                    .compress(1024)            //Final image size will be less than 1 MB(Optional)
                    .maxResultSize(
                        1080,
                        1080
                    )    //Final image resolution will be less than 1080 x 1080(Optional)
                    .start()
            }
            btnSubmit.setOnClickListener {
                saveImage()
            }
            btnDelete.setOnClickListener {
                deleteUserFirebase(user!!)
                deleteImageFirebase(user.avatarPath.toString(), false)
            }
        }
    }

    private fun saveUser() {
        with(binding) {
            val username = regIdtUsername.text
            val password = regIdtPassword.text
            val email = regIdtEmail.text
            val id = getRandomString(10)

            val userData = User(
                id,
                username.toString(),
                email.toString(),
                password.toString(),
                imgDownloadUrl.toString()
            )

            val user = intent.extras?.getParcelable<User>(EXTRA_USER)
            if (user != null) {
//                storeDataFirebase(userData)
                databaseReference.child(user.id.toString()).setValue(userData)
                    .addOnCompleteListener {
                        Toast.makeText(this@FormActivity, "Successs", Toast.LENGTH_SHORT).show()
                        regIdtUsername.setText("")
                        regIdtPassword.setText("")
                        regIdtEmail.setText("")
                    }.addOnFailureListener {
                        Log.d("FIREBASE_ERROR", it.message.toString())
                    }
            } else {
                databaseReference.push().setValue(userData).addOnCompleteListener {
                    Toast.makeText(this@FormActivity, "Successs", Toast.LENGTH_SHORT).show()
                    regIdtUsername.setText("")
                    regIdtPassword.setText("")
                    regIdtEmail.setText("")
                }.addOnFailureListener {
                    Log.d("FIREBASE_ERROR", it.message.toString())
                }
            }
        }
    }

    private fun storeDataFirebase(user: User, isUpdate: Boolean) {
        val dbref = databaseReference

        databaseReference.push().setValue(user).addOnCompleteListener {
            Toast.makeText(this@FormActivity, "Successs", Toast.LENGTH_SHORT).show()
            finish()
        }.addOnFailureListener {
            Log.d("FIREBASE_ERROR", it.message.toString())
        }
    }

    private fun uploadImageFirebase() {
        val fileRef = storageReference.child("images/${imgPath.lastPathSegment}")
        val uploadTask = fileRef.putFile(imgPath)
        uploadTask.addOnFailureListener {
            Log.d("UPLOAD_FAILED", it.message.toString())
        }.addOnCompleteListener { task ->
            fileRef.downloadUrl.addOnCompleteListener {
                imgDownloadUrl = it.result ?: Uri.EMPTY
                saveUser()
            }
        }
    }

    private fun deleteUserFirebase(user: User) {
        databaseReference.child(user.id.toString()).removeValue().addOnCompleteListener {
            Toast.makeText(this@FormActivity, "Successs", Toast.LENGTH_SHORT).show()
            finish()
        }.addOnFailureListener {
            Log.d("FIREBASE_ERROR", it.message.toString())
        }
    }

    private fun deleteImageFirebase(url: String, reupload: Boolean) {
        val fileDeleteRef = storage.getReferenceFromUrl(url)
        val deleteTask = fileDeleteRef.delete()
        deleteTask.addOnFailureListener {
            Log.d("UPLOAD_FAILED", it.message.toString())
        }.addOnCompleteListener {
            if(reupload) uploadImageFirebase()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            //Image Uri will not be null for RESULT_OK
            val file: Uri = data?.data!!
            imgPath = file
            // Use Uri object instead of File to avoid storage permissions
            binding.imgAvatar.setImageURI(file)

        } else if (resultCode == ImagePicker.RESULT_ERROR) {
            Toast.makeText(this, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Task Cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setInput(user: User?) {
        with(binding) {
            if(user?.id != null) {
                regIdtEmail.setText(user.email)
                regIdtUsername.setText(user.username)
                regIdtPassword.setText(user.password)
                imgAvatar.loadImage(user.avatarPath.toString())
                btnDelete.visibility = View.VISIBLE
                imgPath = user.avatarPath?.toUri() ?: Uri.EMPTY
            } else {
                regIdtEmail.setText("")
                regIdtUsername.setText("")
                regIdtPassword.setText("")
                imgAvatar.loadGIFDrawable(R.drawable.anim_default)
                btnDelete.visibility = View.GONE
            }
        }
    }

    private fun saveImage() {
        if (imgPath == Uri.EMPTY) {
            Toast.makeText(this, "Please select an Image first!", Toast.LENGTH_SHORT).show()
        } else {
            val storage = Firebase.storage
            val storageRef = storage.reference
            val user = intent.extras?.getParcelable<User>(EXTRA_USER)
            if (user != null) {
                if (imgPath.scheme != "https") {
                    val fileDeleteRef = storage.getReferenceFromUrl(user.avatarPath.toString())
                    val deleteTask = fileDeleteRef.delete()
                    deleteTask.addOnFailureListener {
                        Log.d("UPLOAD_FAILED", it.message.toString())
                    }.addOnCompleteListener {
                        val fileRef = storageRef.child("images/${imgPath.lastPathSegment}")
                        val uploadTask = fileRef.putFile(imgPath)
                        uploadTask.addOnFailureListener {
                            Log.d("UPLOAD_FAILED", it.message.toString())
                        }.addOnCompleteListener {
                            fileRef.downloadUrl.addOnCompleteListener {
                                imgDownloadUrl = it.result ?: Uri.EMPTY
                                saveUser()
                            }
                        }
                    }
                } else {
                    imgDownloadUrl = imgPath
                    saveUser()
                }
            } else {
                val fileRef = storageRef.child("images/${imgPath.lastPathSegment}")
                val uploadTask = fileRef.putFile(imgPath)
                uploadTask.addOnFailureListener {
                    Log.d("UPLOAD_FAILED", it.message.toString())
                }.addOnCompleteListener { task ->
                    fileRef.downloadUrl.addOnCompleteListener {
                        imgDownloadUrl = it.result ?: Uri.EMPTY
                        saveUser()
                    }
                }
            }
        }
    }

    private fun ImageView.loadImage(id: String) {
        Glide.with(this.context)
            .load(id)
            .into(this)
    }

    private fun ImageView.loadGIFDrawable(id: Int) {
        Glide.with(this.context)
            .asGif()
            .load(id)
            .into(this)
    }

    private fun getRandomString(length: Int): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }
}