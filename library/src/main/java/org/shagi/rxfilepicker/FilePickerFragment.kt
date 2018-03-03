package org.shagi.rxfilepicker

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import io.reactivex.disposables.CompositeDisposable

class FilePickerFragment : Fragment() {

    private var filePickerDialog: FilePickerDialog? = null
    private lateinit var picker: RxFilePicker
    private val compositeDisposable = CompositeDisposable()
    private var filePickerSettings: FilePickerSettings? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true

        val useCache = arguments?.getBoolean(ARG_USE_CACHE) == true
        picker = RxFilePicker(context.applicationContext, useCache)
    }

    override fun onDetach() {
        if (activity.isFinishing) {
            picker.dispose()
        } else {
            picker.detach()
        }
        super.onDetach()
    }

    fun setOnLoadingListener(_loadingListener: RxFilePicker.OnLoadingListener) {
        picker.setLoadingListener(_loadingListener)
    }

    fun use(filePickerDialog: FilePickerDialog) {
        this.filePickerDialog = filePickerDialog
    }

    fun setup(settings: FilePickerSettings) {
        filePickerSettings = settings
    }

    fun show() {
        filePickerSettings?.let {
            picker.setup(it)
        }
        picker.with(filePickerDialog ?: FilePickerDialog.newInstance())
        compositeDisposable.add(picker.subscribe(fragmentManager))
    }

    companion object {
        private const val FRAGMENT_TAG = "FilePickerFragment"
        private const val ARG_USE_CACHE = "use_cache"

        fun getFragment(fragmentManager: FragmentManager, useCache: Boolean = false): FilePickerFragment {
            var fragment = fragmentManager.findFragmentByTag(FRAGMENT_TAG) as? FilePickerFragment

            if (fragment == null) {
                fragment = FilePickerFragment().apply {
                    val bundle = Bundle()
                    bundle.putBoolean(ARG_USE_CACHE, useCache)
                    arguments = bundle
                }

                fragmentManager.beginTransaction().add(fragment, FRAGMENT_TAG).commitNowAllowingStateLoss()
            }

            return fragment
        }
    }
}
