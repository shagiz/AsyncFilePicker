package org.shagi.filepicker

import android.content.Context
import android.net.Uri
import android.os.AsyncTask
import android.support.v4.app.FragmentManager

class FilePicker(var context: Context?, private val useCache: Boolean = false) : FilePickerDialog.OnFilePickedListener {

    private lateinit var mDialog: FilePickerDialog

    private var mCacheController: CacheController? = null
    private var loadingListener: OnLoadingListener? = null

    private var width = 1024
    private var height = 1024

    private var tasks = ArrayList<SaveFileAsyncTask>()

    init {
        context?.let {
            if (useCache) {
                mCacheController = CacheController(it)
            }
        }
    }

    fun setup(settings: FilePickerSettings) {
        width = settings.maxWidth
        height = settings.maxHeight
        mCacheController?.setup(settings.maxCacheSize, settings.maxFileSize)
    }

    fun with(filePickerDialog: FilePickerDialog) {
        mDialog = filePickerDialog
    }

    fun subscribe(supportFragmentManager: FragmentManager) {
        mDialog.filePickedListener = this
        mDialog.show(supportFragmentManager, FilePickerDialog.TAG)
    }

    fun detach() {
        loadingListener = null
        tasks.forEach { it.setLoadingListener(null) }
    }

    fun setLoadingListener(_loadingListener: OnLoadingListener) {
        loadingListener = _loadingListener
        tasks.forEach { it.setLoadingListener(_loadingListener) }
    }

    fun dispose() {
        detach()
        tasks.clear()
        context = null
    }

    override fun onFilePicked(uri: Uri, fileType: FileType, fromCamera: Boolean) {
        context?.let {
            tasks.add(SaveFileAsyncTask(it, mCacheController, fileType, fromCamera).apply {
                loadingListener?.let {
                    setLoadingListener(it)
                    executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, uri)
                }
            })
        }
    }

    override fun onFilePickFailed() {
        loadingListener?.onLoadingFailure(FILE_NOT_UPLOADED, IllegalStateException("File not uploaded"))
    }

    interface OnLoadingListener {
        /**
         * Invokes when file start loading. When you are trying to get photos/files
         * from remote source (Google drive, Dropbox etc.) it can take long time so you may want to show progress.
         * @param key - key for current loading file.
         */
        fun onLoadingStart(key: Long)
        fun onLoadingSuccess(key: Long, file: ExtFile)
        fun onLoadingFailure(key: Long, throwable: Throwable)
    }

    companion object {
        const val FILE_NOT_UPLOADED = -1L
    }
}
