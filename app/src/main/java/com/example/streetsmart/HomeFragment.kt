package com.example.streetsmart

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

    private lateinit var reportAdapter: ReportAdapter
    private lateinit var reportsRecyclerView: RecyclerView
    private val firestore = FirebaseFirestore.getInstance()
    private val reports = mutableListOf<Report>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val reportButton = view.findViewById<Button>(R.id.btnReportIssue)
        reportsRecyclerView = view.findViewById(R.id.reportsRecyclerView)

        reportAdapter = ReportAdapter(reports)
        reportsRecyclerView.layoutManager = LinearLayoutManager(context)
        reportsRecyclerView.adapter = reportAdapter

        reportButton.setOnClickListener {
            val intent = Intent(requireContext(), ReportActivity::class.java)
            startActivity(intent)
        }

        fetchReportsFromOtherUsers()

        return view
    }

    private fun fetchReportsFromOtherUsers() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("reports")
            .whereNotEqualTo("userId", currentUserId)
            .get()
            .addOnSuccessListener { snapshot ->
                reports.clear()

                val docs = snapshot.documents
                if (docs.isEmpty()) {
                    reportAdapter.setReports(emptyList())
                    return@addOnSuccessListener
                }

                var loaded = 0

                for (doc in docs) {
                    val report = doc.toObject(Report::class.java)
                    if (report != null) {
                        report.id = doc.id

                        firestore.collection("reports")
                            .document(doc.id)
                            .collection("votes")
                            .document(currentUserId)
                            .get()
                            .addOnSuccessListener { voteSnap ->
                                report.userVoteStatus = voteSnap.getLong("vote")?.toInt() ?: 0
                            }
                            .addOnCompleteListener {
                                reports.add(report)
                                loaded++
                                if (loaded == docs.size) {
                                    // Optional: Sort by timestamp or votes here
                                    reportAdapter.setReports(reports)
                                }
                            }
                    } else {
                        loaded++
                        if (loaded == docs.size) {
                            reportAdapter.setReports(reports)
                        }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to load reports", Toast.LENGTH_SHORT).show()
            }
    }
}
