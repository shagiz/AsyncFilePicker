package org.shagi.filepicker.filepicker

import android.content.Context
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.file_picker_action_layout.view.*
import org.shagi.filepicker.R

class CustomActionItem(@DrawableRes private val icon: Int,
                       @StringRes private val title: Int,
                       private val listener: View.OnClickListener) {

    fun generateView(ctx: Context, parent: ViewGroup): View {
        val view = LayoutInflater.from(ctx).inflate(R.layout.file_picker_action_layout, parent, false)
        view.setOnClickListener(listener)
        view.file_picker_custom_action_icon.setImageResource(icon)
        view.file_picker_custom_action_title.setText(title)
        return view
    }
}
