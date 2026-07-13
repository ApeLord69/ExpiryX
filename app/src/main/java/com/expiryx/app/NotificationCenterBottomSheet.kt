package com.expiryx.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.expiryx.app.databinding.BottomsheetNotificationLogBinding
import com.expiryx.app.databinding.ItemNotificationLogBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationCenterBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomsheetNotificationLogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomsheetNotificationLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        WindowInsetsHelper.setupBottomSheetEdgeToEdge(this, binding.root)

        val dao = (requireActivity().application as ProductApplication).database.notificationLogDao()
        val adapter = NotificationLogAdapter()
        
        binding.recyclerNotificationLogs.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerNotificationLogs.adapter = adapter

        dao.getRecentLogs().observe(viewLifecycleOwner) { logs ->
            adapter.submitList(logs)
            binding.txtNoLogs.visibility = if (logs.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.btnClearLogs.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                dao.clearAll()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class NotificationLogAdapter : ListAdapter<NotificationLog, NotificationLogAdapter.ViewHolder>(DiffCallback()) {
        class ViewHolder(val binding: ItemNotificationLogBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ItemNotificationLogBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val log = getItem(position)
            holder.binding.txtLogTitle.text = log.title
            holder.binding.txtLogMessage.text = log.message
            holder.binding.txtLogTime.text = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(log.timestamp))
            holder.binding.urgencyIndicator.visibility = if (log.urgency == 1) View.VISIBLE else View.INVISIBLE
        }

        class DiffCallback : DiffUtil.ItemCallback<NotificationLog>() {
            override fun areItemsTheSame(oldItem: NotificationLog, newItem: NotificationLog) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: NotificationLog, newItem: NotificationLog) = oldItem == newItem
        }
    }
}
