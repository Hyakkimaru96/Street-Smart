package com.example.streetsmart

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HomeFragment : Fragment() {

    private lateinit var reportAdapter: ReportAdapter
    private lateinit var reportsRecyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val reportButton = view.findViewById<Button>(R.id.btnReportIssue)
        reportsRecyclerView = view.findViewById(R.id.reportsRecyclerView)

        // Dummy data
        val dummyReports = listOf(
            Report("Pothole near Main Street", "12.9716, 77.5946", 5, 1),
            Report("Water leakage on 2nd Ave", "13.0022, 77.5600", 3, 0),
        )

        reportAdapter = ReportAdapter(dummyReports)
        reportsRecyclerView.layoutManager = LinearLayoutManager(context)
        reportsRecyclerView.adapter = reportAdapter

        reportButton.setOnClickListener {
            val intent = Intent(requireContext(), ReportActivity::class.java)
            startActivity(intent)
        }

        return view
    }
}
