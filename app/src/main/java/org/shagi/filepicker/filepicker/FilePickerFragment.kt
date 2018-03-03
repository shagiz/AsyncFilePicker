package org.shagi.filepicker.filepicker

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.widget.Toast
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber
import java.io.File

class FilePickerFragment : Fragment(), RxFilePicker.OnLoadingListener {

    private var filePickerDialog: FilePickerDialog? = null
    private lateinit var picker: RxFilePicker
    private val compositeDisposable = CompositeDisposable()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Timber.d("DebugTag, onAttach ${activity} $context")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true

        picker = RxFilePicker().from(context.applicationContext)
    }

    override fun onDetach() {
        Timber.d("DebugTag, onDetach ${activity.isFinishing} $context")
        if (activity.isFinishing) {
            picker.dispose()
        } else {
            picker.detach()
        }
        super.onDetach()
    }

    fun use(filePickerDialog: FilePickerDialog) {
        this.filePickerDialog = filePickerDialog
    }

    fun show() {
        picker.with(filePickerDialog ?: FilePickerDialog.newInstance())
        compositeDisposable.add(picker.subscribe(fragmentManager))
    }

    fun setOnLoadingListener(_loadingListener: RxFilePicker.OnLoadingListener) {
        picker.setLoadingListener(_loadingListener)
    }

    override fun onLoadingStart(key: Long) {
        Timber.d("DebugTag, fragment onLoadingStart $context")
        Toast.makeText(context, "Loading start $context", Toast.LENGTH_SHORT).show()
    }

    override fun onLoadingSuccess(key: Long, file: File) {
        Timber.d("DebugTag, fragment onLoadingSuccess $context")
        Toast.makeText(context, "Loading success $context", Toast.LENGTH_SHORT).show()
    }

    override fun onLoadingFailure(key: Long) {
        Timber.d("DebugTag, fragment onLoadingFailure $context")
        Toast.makeText(context, "Loading fail $context", Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val FRAGMENT_TAG = "FilePickerFragment"

        fun getFragment(fragmentManager: FragmentManager): FilePickerFragment {
            var fragment = fragmentManager.findFragmentByTag(FRAGMENT_TAG) as? FilePickerFragment

            if (fragment == null) {
                fragment = FilePickerFragment()
                fragmentManager.beginTransaction().add(fragment, FRAGMENT_TAG).commitNowAllowingStateLoss()
            }

            return fragment
        }
    }

}
