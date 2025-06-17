package com.example.streetsmart

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ReportAdapter(private var reports: List<Report>) :
    RecyclerView.Adapter<ReportAdapter.ReportViewHolder>() {

    private val firestore = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    class ReportViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val reportImage: ImageView = view.findViewById(R.id.reportImageView)
        val descText: TextView = view.findViewById(R.id.textDescription)
        val locText: TextView = view.findViewById(R.id.textLocation)
        val upBtn: Button = view.findViewById(R.id.btnUpvote)
        val downBtn: Button = view.findViewById(R.id.btnDownvote)
        val voteCount: TextView = view.findViewById(R.id.voteCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_report, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val report = reports[position]

        Glide.with(holder.itemView.context)
            .load(report.imageUrl)
            .into(holder.reportImage)

        holder.descText.text = report.description
        holder.locText.text = "📍 ${report.location}"
        holder.voteCount.text = "👍 ${report.upvotes} | 👎 ${report.downvotes}"

        fun updateUI() {
            holder.voteCount.text = "👍 ${report.upvotes} | 👎 ${report.downvotes}"
        }

        holder.upBtn.setOnClickListener {
            if (currentUserId == null || report.id.isNullOrEmpty()) return@setOnClickListener
            val voteRef = firestore.collection("reports")
                .document(report.id)
                .collection("votes")
                .document(currentUserId)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(voteRef)
                val previousVote = snapshot.getLong("vote")?.toInt() ?: 0

                if (previousVote == 1) {
                    report.upvotes--
                    transaction.delete(voteRef)
                    report.userVoteStatus = 0
                } else {
                    if (previousVote == -1) report.downvotes--
                    if (previousVote != 1) report.upvotes++

                    transaction.set(voteRef, mapOf("vote" to 1))
                    report.userVoteStatus = 1
                }

                transaction.update(
                    firestore.collection("reports").document(report.id),
                    mapOf("upvotes" to report.upvotes, "downvotes" to report.downvotes)
                )
            }.addOnSuccessListener {
                updateUI()
                notifyItemChanged(position)
            }
        }

        holder.downBtn.setOnClickListener {
            if (currentUserId == null || report.id.isNullOrEmpty()) return@setOnClickListener
            val voteRef = firestore.collection("reports")
                .document(report.id)
                .collection("votes")
                .document(currentUserId)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(voteRef)
                val previousVote = snapshot.getLong("vote")?.toInt() ?: 0

                if (previousVote == -1) {
                    report.downvotes--
                    transaction.delete(voteRef)
                    report.userVoteStatus = 0
                } else {
                    if (previousVote == 1) report.upvotes--
                    if (previousVote != -1) report.downvotes++

                    transaction.set(voteRef, mapOf("vote" to -1))
                    report.userVoteStatus = -1
                }

                transaction.update(
                    firestore.collection("reports").document(report.id),
                    mapOf("upvotes" to report.upvotes, "downvotes" to report.downvotes)
                )
            }.addOnSuccessListener {
                updateUI()
                notifyItemChanged(position)
            }
        }

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, ReportDetailActivity::class.java)
            intent.putExtra("report", report)
            intent.putExtra("reportId", report.id)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = reports.size

    fun setReports(newReports: List<Report>) {
        reports = newReports
        notifyDataSetChanged()
    }
}
