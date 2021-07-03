package com.sh.firebasecrud

import android.R
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.sh.firebasecrud.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var myAdapter: MainAdapter
    private lateinit var databaseRef: DatabaseReference
    var listUser = ArrayList<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        databaseRef = FirebaseDatabase.getInstance().getReference("Users")

        myAdapter = MainAdapter()
        myAdapter.onItemClick = { selectedData ->
            Intent(this, FormActivity::class.java).apply {
                putExtra(FormActivity.EXTRA_USER, selectedData)
                startActivity(this)
            }
        }
        getData()
        with(binding.rvUser) {
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
            adapter = myAdapter
        }
        binding.addFab.setOnClickListener {
            Intent(this, FormActivity::class.java).apply {
                startActivity(this)
            }
        }
    }

    private fun getData() {
        val dbListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                listUser.clear()
                dataSnapshot.children.forEach {
                    val user = it.value as HashMap<String, User>
                    listUser.add(
                        User(
                            it.key.toString(),
                            user["username"].toString(),
                            user["email"].toString(),
                            user["password"].toString(),
                            user["avatarPath"].toString()
                        )
                    )
                }
                myAdapter.setData(listUser)
                myAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w("TAG", "loadPost:onCancelled", databaseError.toException())
            }
        }
        databaseRef.addValueEventListener(dbListener)
    }
}