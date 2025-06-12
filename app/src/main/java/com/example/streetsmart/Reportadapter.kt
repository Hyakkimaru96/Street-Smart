package com.example.streetsmart

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView

class ReportAdapter(private val reports: List<Report>) :
    RecyclerView.Adapter<ReportAdapter.ReportViewHolder>() {

    class ReportViewHolder(view: View) : RecyclerView.ViewHolder(view) {
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
        holder.descText.text = report.description
        holder.locText.text = "📍 ${report.location}"
        holder.voteCount.text = "👍 ${report.upVotes} | 👎 ${report.downVotes}"

        holder.upBtn.setOnClickListener {
            report.upVotes++
            notifyItemChanged(position)
        }

        holder.downBtn.setOnClickListener {
            report.downVotes++
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = reports.size
}
