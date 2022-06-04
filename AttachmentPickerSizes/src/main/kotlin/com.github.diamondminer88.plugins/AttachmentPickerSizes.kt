package com.github.diamondminer88.plugins

import android.content.Context
import android.graphics.Color
import android.icu.text.DecimalFormat
import android.view.*
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT
import androidx.recyclerview.widget.RecyclerView
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.after
import com.aliucord.utils.DimenUtils.dp
import com.lytefast.flexinput.R
import com.lytefast.flexinput.adapters.AttachmentPreviewAdapter
import com.lytefast.flexinput.model.Media
import kotlin.math.*

typealias AttachmentPreviewAdapterViewHolder<T> = AttachmentPreviewAdapter<T>.b
typealias MediaCursorAdapter = b.b.a.d.h
typealias MediaCursorAdapterViewHolder = b.b.a.d.h.a

@Suppress("unused")
@AliucordPlugin
class AttachmentPickerSizes : Plugin() {
	private val attachmentItemId = Utils.getResId("attachment_item", "id")
	private val transparentBlack = Color.argb(210, 0, 0, 0)
	private val labelId = View.generateViewId()

	override fun start(context: Context) {
		patcher.after<AttachmentPreviewAdapter<*>>(
			"onBindViewHolder",
			RecyclerView.ViewHolder::class.java,
			Integer.TYPE,
		) {
			val holder = it.args[0] as AttachmentPreviewAdapterViewHolder<*>

			val item = this.a.get(it.args[1] as Int)
			if (item !is Media)
				return@after

			val layout = (holder.itemView as ViewGroup)
				.findViewById<View>(attachmentItemId)
				.parent as ViewGroup

			bindSizeTag(layout, item)
		}

		patcher.after<MediaCursorAdapter>(
			"onBindViewHolder",
			RecyclerView.ViewHolder::class.java,
			Integer.TYPE,
		) {
			val holder = it.args[0] as MediaCursorAdapterViewHolder
			val data = holder.p as Media

			bindSizeTag(holder.itemView as ViewGroup, data)
		}
	}

	override fun stop(context: Context) {
		patcher.unpatchAll()
	}

	private fun bindSizeTag(layout: ViewGroup, media: Media) {
		if (layout.findViewById<View>(labelId) != null)
			return

		val size = Utils.appActivity.contentResolver
			.openFileDescriptor(media.uri, "r")
			?.statSize
			?.let { size -> getReadableSize(size) }


		layout.addView(CardView(layout.context).apply {
			id = labelId

			if (size == null) {
				visibility = View.GONE
				return@apply
			}

			layoutParams = ConstraintLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
				startToStart = 0
				topToTop = 0
				setMargins(4.dp, 4.dp, 4.dp, 4.dp)
			}
			setCardBackgroundColor(transparentBlack)

			addView(TextView(layout.context, null, 0, R.i.UiKit_TextAppearance_MaterialEditText_Label).apply {
				text = size
				setTextColor(Color.WHITE)
				setPadding(5.dp, 2.dp, 5.dp, 2.dp)
			})
		})
	}

	private fun getReadableSize(sizeBytes: Long): String? {
		if (sizeBytes <= 0) return "0"
		val units = arrayOf("B", "KB", "MB", "GB", "TB")
		val digitGroups = (log10(sizeBytes.toDouble()) / log10(1024.0)).toInt()
		return DecimalFormat("#,##0.#").format(sizeBytes / 1024.0.pow(digitGroups.toDouble())).toString() + " " + units[digitGroups]
	}
}
