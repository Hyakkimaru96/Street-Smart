package com.example.streetsmart

import android.os.Bundle
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ReportDetailActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var descriptionText: TextView
    private lateinit var locationText: TextView
    private lateinit var voteText: TextView
    private lateinit var upvoteBtn: Button
    private lateinit var downvoteBtn: Button

    private val firestore = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser

    private lateinit var reportId: String
    private var userVoteStatus: Int = 0 // 1 = upvoted, -1 = downvoted, 0 = none

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_detail)

        // Set up custom toolbar with back button
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            title = "Report Detail"
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }


        imageView = findViewById(R.id.detailImage)
        descriptionText = findViewById(R.id.detailDescription)
        locationText = findViewById(R.id.detailLocation)
        voteText = findViewById(R.id.detailVotes)
        upvoteBtn = findViewById(R.id.detailUpvote)
        downvoteBtn = findViewById(R.id.detailDownvote)

        // Get data from intent
        val report = intent.getSerializableExtra("report") as? Report
        reportId = intent.getStringExtra("reportId") ?: ""

        if (report != null) {
            descriptionText.text = report.description
            locationText.text = "📍 ${report.location}"
            voteText.text = "👍 ${report.upvotes} | 👎 ${report.downvotes}"

            Glide.with(this)
                .load(report.imageUrl)
                .placeholder(R.drawable.ic_placeholder)
                .into(imageView)

            userVoteStatus = report.userVoteStatus
            updateVoteButtons()
        }

        upvoteBtn.setOnClickListener { handleVote(1) }
        downvoteBtn.setOnClickListener { handleVote(-1) }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateVoteButtons() {
        upvoteBtn.isEnabled = userVoteStatus != 1
        downvoteBtn.isEnabled = userVoteStatus != -1
    }

    private fun handleVote(vote: Int) {
        val userId = currentUser?.uid ?: return
        val reportRef = firestore.collection("reports").document(reportId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(reportRef)

            val currentVotes = snapshot.get("userVotes") as? Map<*, *> ?: emptyMap<String, Long>()
            val mutableVotes = currentVotes.toMutableMap()
            val previousVote = (mutableVotes[userId] as? Long)?.toInt() ?: 0

            if (previousVote == vote) return@runTransaction  // Already voted the same way

            mutableVotes[userId] = vote

            val upvotes = mutableVotes.values.count { it == 1L }
            val downvotes = mutableVotes.values.count { it == -1L }

            transaction.update(reportRef, mapOf(
                "userVotes" to mutableVotes,
                "upvotes" to upvotes,
                "downvotes" to downvotes
            ))
        }.addOnSuccessListener {
            userVoteStatus = vote
            updateVoteButtons()
            voteText.text = if (vote == 1) "👍 (you voted)" else "👎 (you voted)"
        }
    }
}
