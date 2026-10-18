/*
 * SPDX-FileCopyrightText: 2026 The Halcyon Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard

import android.app.Dialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.internal.app.LocaleHelper
import com.android.internal.app.LocalePicker
import com.android.internal.app.LocaleStore
import java.util.Collections
import java.util.Locale
import java.util.concurrent.Executors

class LanguagePickerDialogFragment : DialogFragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LanguagePickerAdapter
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_language_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.cancel_button).setOnClickListener {
            dismiss()
        }

        recyclerView = view.findViewById(R.id.language_list)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = LanguagePickerAdapter(
            onLocaleSelected = { locale ->
                onLanguageSelected(locale)
            }
        )
        recyclerView.adapter = adapter

        loadLanguages()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setBackgroundDrawableResource(android.R.color.transparent)
            val displayMetrics = resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.90).toInt().coerceAtMost(
                (420 * displayMetrics.density).toInt()
            )
            val height = (displayMetrics.heightPixels * 0.75).toInt()
            window.setLayout(width, height)
            window.setDimAmount(0.6f)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        executor.shutdownNow()
    }

    private fun loadLanguages() {
        val context = context?.applicationContext ?: return
        executor.execute {
            try {
                val isInDeveloperMode = Settings.Global.getInt(
                    context.contentResolver,
                    Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                    0
                ) != 0

                val assetLocales = LocalePicker.getAllAssetLocales(context, isInDeveloperMode)

                LocaleStore.fillCache(context)
                val levelLocales = LocaleStore.getLevelLocales(context, Collections.emptySet(), null, true)
                val currentLocale = Locale.getDefault()
                val comparator = LocaleHelper.LocaleInfoComparator(currentLocale, false)

                val suggestedStoreList = levelLocales.filter { it.isSuggested }.sortedWith(comparator)

                val items = mutableListOf<LanguageItem>()
                if (suggestedStoreList.isNotEmpty()) {
                    items.add(LanguageItem.Header(getString(R.string.suggested_languages)))
                    suggestedStoreList.forEach { info ->
                        items.add(
                            LanguageItem.LocaleEntry(
                                id = "suggested_${info.locale.toLanguageTag()}",
                                label = info.fullNameNative,
                                locale = info.locale
                            )
                        )
                    }
                }

                if (assetLocales.isNotEmpty()) {
                    items.add(LanguageItem.Header(getString(R.string.all_languages)))
                    assetLocales.forEach { info ->
                        items.add(
                            LanguageItem.LocaleEntry(
                                id = "all_${info.locale.toLanguageTag()}",
                                label = info.label,
                                locale = info.locale
                            )
                        )
                    }
                }

                activity?.runOnUiThread {
                    if (isAdded) {
                        adapter.submitList(items)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading languages", e)
            }
        }
    }

    private fun onLanguageSelected(locale: Locale) {
        val app = activity?.application as? SetupWizardApp
        app?.ignoreSimLocale = true
        LocalePicker.updateLocale(locale)
        dismissAllowingStateLoss()
    }

    sealed class LanguageItem {
        data class Header(val title: String) : LanguageItem()
        data class LocaleEntry(
            val id: String,
            val label: String,
            val locale: Locale
        ) : LanguageItem()
    }

    private class LanguagePickerAdapter(
        private val onLocaleSelected: (Locale) -> Unit
    ) : ListAdapter<LanguageItem, RecyclerView.ViewHolder>(DiffCallback) {

        companion object {
            private const val TYPE_HEADER = 0
            private const val TYPE_LOCALE = 1

            private val DiffCallback = object : DiffUtil.ItemCallback<LanguageItem>() {
                override fun areItemsTheSame(oldItem: LanguageItem, newItem: LanguageItem): Boolean {
                    return when {
                        oldItem is LanguageItem.Header && newItem is LanguageItem.Header ->
                            oldItem.title == newItem.title
                        oldItem is LanguageItem.LocaleEntry && newItem is LanguageItem.LocaleEntry ->
                            oldItem.id == newItem.id
                        else -> false
                    }
                }

                override fun areContentsTheSame(oldItem: LanguageItem, newItem: LanguageItem): Boolean {
                    return oldItem == newItem
                }
            }
        }

        override fun getItemViewType(position: Int): Int {
            return when (getItem(position)) {
                is LanguageItem.Header -> TYPE_HEADER
                is LanguageItem.LocaleEntry -> TYPE_LOCALE
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return if (viewType == TYPE_HEADER) {
                val view = inflater.inflate(R.layout.language_picker_header, parent, false)
                HeaderViewHolder(view)
            } else {
                val view = inflater.inflate(R.layout.language_picker_item, parent, false)
                LocaleViewHolder(view, onLocaleSelected)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val item = getItem(position)) {
                is LanguageItem.Header -> (holder as HeaderViewHolder).bind(item)
                is LanguageItem.LocaleEntry -> (holder as LocaleViewHolder).bind(item)
            }
        }

        class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val titleView: TextView = itemView.findViewById(R.id.header_title)

            fun bind(header: LanguageItem.Header) {
                titleView.text = header.title
            }
        }

        class LocaleViewHolder(
            itemView: View,
            private val onLocaleSelected: (Locale) -> Unit
        ) : RecyclerView.ViewHolder(itemView) {
            private val nameView: TextView = itemView.findViewById(R.id.locale_name)
            private val selectedIndicator: ImageView = itemView.findViewById(R.id.selected_indicator)
            private val primaryColor: Int
            private val accentColor: Int

            init {
                val ta = itemView.context.obtainStyledAttributes(
                    intArrayOf(android.R.attr.textColorPrimary, android.R.attr.colorAccent)
                )
                primaryColor = ta.getColor(0, 0xFFFFFFFF.toInt())
                accentColor = ta.getColor(1, primaryColor)
                ta.recycle()
            }

            fun bind(entry: LanguageItem.LocaleEntry) {
                nameView.text = entry.label
                nameView.textLocale = entry.locale
                nameView.setTextColor(primaryColor)

                val currentDefault = Locale.getDefault()
                val isSelected = isMatchingLocale(entry.locale, currentDefault)

                if (isSelected) {
                    selectedIndicator.visibility = View.VISIBLE
                    selectedIndicator.imageTintList = ColorStateList.valueOf(accentColor)
                } else {
                    selectedIndicator.visibility = View.GONE
                }

                itemView.setOnClickListener {
                    onLocaleSelected(entry.locale)
                }
            }

            private fun isMatchingLocale(candidate: Locale, current: Locale): Boolean {
                if (candidate == current) return true
                if (candidate.toLanguageTag().equals(current.toLanguageTag(), ignoreCase = true)) return true
                if (candidate.language != current.language) return false
                if (candidate.country.isNotEmpty() && current.country.isNotEmpty()) {
                    return candidate.country.equals(current.country, ignoreCase = true)
                }
                if (candidate.script.isNotEmpty() && current.script.isNotEmpty()) {
                    return candidate.script.equals(current.script, ignoreCase = true)
                }
                return candidate.country.isEmpty() || current.country.isEmpty()
            }
        }
    }

    companion object {
        const val TAG = "LanguagePickerDialog"

        fun newInstance(): LanguagePickerDialogFragment {
            return LanguagePickerDialogFragment()
        }
    }
}

