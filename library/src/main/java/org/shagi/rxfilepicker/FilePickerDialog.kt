package org.shagi.rxfilepicker

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.file_picker_dialog_layout.*

open class FilePickerDialog : BottomSheetDialogFragment() {

    private lateinit var resolver: IntentResolver

    var filePickedListener: OnFilePickedListener? = null
    private var isCameraStarting = false
    private val customActions = ArrayList<CustomActionItem>()

    var showCamera = true
    var showGallery = true
    var showFileSystem = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        resolver = IntentResolver(activity)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.file_picker_dialog_layout, container, false)

    final override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        if (!showCamera && !showGallery && !showFileSystem && customActions.isEmpty()) {
            dismissAllowingStateLoss()
        }

        if (showCamera) {
            file_picker_camera.setOnClickListener {
                isCameraStarting = true
                resolver.launchCamera(this)
            }
        } else {
            file_picker_camera.visibility = View.GONE
        }

        if (showGallery) {
            file_picker_gallery.setOnClickListener {
                isCameraStarting = false
                resolver.launchGallery(this)
            }
        } else {
            file_picker_gallery.visibility = View.GONE
        }

        if (showFileSystem) {

            file_picker_files.setOnClickListener { onFilesOpenClick() }
        } else {
            file_picker_files.visibility = View.GONE
        }

        customActions.forEach {
            (view as LinearLayout).addView(it.generateView(context, view))
        }
    }

    private fun onFilesOpenClick() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        startActivityForResult(intent, IntentResolver.REQUESTER)
    }

    fun addCustomAction(customActionItem: CustomActionItem) {
        customActions.add(customActionItem)
    }

    override fun onDestroyView() {
        if (dialog != null && retainInstance) {
            dialog.setDismissMessage(null)
        }
        super.onDestroyView()
    }

    final override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IntentResolver.REQUESTER && resultCode == Activity.RESULT_OK) {
            val isFromCamera = resolver.fromCamera(data)
            val fileType = getFileType(data)
            val uri = getUri(data)

            uri?.let {
                filePickedListener?.onFilePicked(uri, fileType, isFromCamera)
            }
        }

        dismissAllowingStateLoss()
    }

    final override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == IntentResolver.REQUESTER) {
            var granted = true

            for (i in grantResults) {
                granted = granted && i == PackageManager.PERMISSION_GRANTED
            }

            if (granted) {
                if (isCameraStarting) {
                    resolver.launchCamera(this)
                } else {
                    resolver.launchGallery(this)
                }
            } else {
                dismissAllowingStateLoss()
            }
        }
    }

    //-----------------------------------------------------------------------------------------------

    private fun getFileType(data: Intent?): FileType {
        if (resolver.fromCamera(data)) return FileType.IMAGE

        val uri = data?.data
        val isImage = uri?.let {
            context.contentResolver.getType(it)
        }?.contains("image/") == true

        if (isImage) return FileType.IMAGE

        return FileType.FILE
    }

    private fun getUri(data: Intent?) = if (resolver.fromCamera(data)) {
        resolver.cameraUri()
    } else {
        data?.data
    }

    //-----------------------------------------------------------------------------------------------

    interface OnFilePickedListener {
        fun onFilePicked(uri: Uri, fileType: FileType, fromCamera: Boolean)
        fun onFilePickFailed()
    }

    companion object {
        const val TAG = "FilePickerDialog"

        @JvmStatic
        fun newInstance(): FilePickerDialog {
            val fragment = FilePickerDialog()

            return fragment
        }
    }
}