package com.example.streetsmart

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ReportsFragment : Fragment() {

    private lateinit var reportsRecyclerView: RecyclerView
    private lateinit var reportAdapter: ReportAdapter

    private val firestore = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_reports, container, false)
        reportsRecyclerView = view.findViewById(R.id.reportsRecyclerView)
        reportsRecyclerView.layoutManager = LinearLayoutManager(context)
        reportAdapter = ReportAdapter(emptyList())
        reportsRecyclerView.adapter = reportAdapter

        loadReports()

        return view
    }

    private fun loadReports() {
        val userId = currentUser?.uid ?: return

        firestore.collection("reports")
            .get()
            .addOnSuccessListener { result ->
                val reportList = result.documents.mapNotNull { doc ->
                    val report = doc.toObject(Report::class.java)
                    report?.copy(id = doc.id)
                }.filter { it.userId == userId }

                reportAdapter = ReportAdapter(reportList)
                reportsRecyclerView.adapter = reportAdapter
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to load reports", Toast.LENGTH_SHORT).show()
            }
    }
}
