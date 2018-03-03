package org.shagi.filepicker.filepicker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.shagi.filepicker.R
import org.shagi.rxfilepicker.FilePickerDialog

class MyFilePicker : FilePickerDialog() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.aaa, container, false)

}
