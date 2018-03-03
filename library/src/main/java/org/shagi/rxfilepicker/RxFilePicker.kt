package org.shagi.rxfilepicker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.support.media.ExifInterface
import android.support.v4.app.FragmentManager
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.FileNotFoundException

class RxFilePicker(var context: Context?, private val useCache: Boolean = false) : FilePickerDialog.OnFilePickedListener, Disposable {

    private lateinit var mDialog: FilePickerDialog

    private var mCacheController: CacheController? = null

    private val compositeDisposable = CompositeDisposable()

    private var loadingListener: OnLoadingListener? = null

    private var width = 1024
    private var height = 1024

    init {
        context?.let {
            if (useCache) {
                mCacheController = CacheController(it)
            }
        }
    }

    fun setup(settings: FilePickerSettings): RxFilePicker {
        width = settings.maxWidth
        height = settings.maxHeight
        mCacheController?.setup(settings.maxCacheSize, settings.maxFileSize)
        return this
    }

    fun with(filePickerDialog: FilePickerDialog): RxFilePicker {
        mDialog = filePickerDialog
        return this
    }

    fun subscribe(supportFragmentManager: FragmentManager): Disposable {
        mDialog.filePickedListener = this
        mDialog.show(supportFragmentManager, FilePickerDialog.TAG)
        return this
    }

    override fun isDisposed(): Boolean {
        return compositeDisposable.isDisposed
    }

    fun detach() {
        loadingListener = null
    }

    fun setLoadingListener(_loadingListener: OnLoadingListener): RxFilePicker {
        loadingListener = _loadingListener
        return this
    }

    override fun dispose() {
        detach()
        context = null
        compositeDisposable.dispose()
    }

    override fun onFilePicked(uri: Uri, fileType: FileType, fromCamera: Boolean) {
        val key = System.currentTimeMillis()

        loadingListener?.onLoadingStart(key)

        context?.let {
            compositeDisposable.add(Observable.fromCallable { saveToFile(it, uri, fileType, fromCamera) }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            { loadingListener?.onLoadingSuccess(key, it) },
                            { loadingListener?.onLoadingFailure(key, it) }
                    )
            )
        }
    }

    override fun onFilePickFailed() {
        loadingListener?.onLoadingFailure(FILE_NOT_UPLOADED, IllegalStateException("File not uploaded"))
    }

    //---------------------------------------------------------------------------------------------

    private fun saveToFile(context: Context, uri: Uri, fileType: FileType, isFromCamera: Boolean): File {
        return if (fileType == FileType.FILE) {
            saveFile(context, uri)
        } else {
            savePhoto(context, uri, isFromCamera)
        }
    }

    private fun saveFile(context: Context, uri: Uri): File {
        mCacheController?.let {
            return it.saveFile(uri)
        }

        return saveTempFileUri(context, uri)
    }

    private fun savePhoto(context: Context, uri: Uri, isFromCamera: Boolean): File {
        val bitmap = if (width - height == 0) {
            scaleDown(context, uri)
        } else {
            resize(context, uri)
        }

        mCacheController?.let {
            return it.saveBitmap(rotateIfNeeded(context, bitmap, uri, isFromCamera))
        }

        return saveTempBitmap(context, rotateIfNeeded(context, bitmap, uri, isFromCamera))
    }


    private fun rotateIfNeeded(context: Context, bitmap: Bitmap, uri: Uri, isFromCamera: Boolean): Bitmap {

        val path = if (isFromCamera) {
            uri.path
        } else {
            getPath(context, uri)
        }

        val exif = if (path != null) {
            ExifInterface(path)
        } else {
            ExifInterface(context.contentResolver.openInputStream(uri))
        }

        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        val rotate = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            else -> 0
        }

        return rotate(bitmap, rotate)
    }

    private fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees != 0) {
            val matrix = Matrix()
            matrix.postRotate(degrees.toFloat())
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }

        return bitmap
    }

    @Throws(FileNotFoundException::class)
    private fun getOptions(context: Context, uri: Uri): BitmapFactory.Options {
        var options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri), null, options)

        var w = options.outWidth
        var h = options.outHeight
        var scale = 1
        while (true) {
            if (w / 2 < width || h / 2 < height)
                break

            w /= 2
            h /= 2
            scale *= 2
        }

        options = BitmapFactory.Options()
        options.inSampleSize = scale
        return options
    }


    @Throws(FileNotFoundException::class)
    private fun scaleDown(context: Context, uri: Uri): Bitmap {
        return BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri), null, getOptions(context, uri))
    }

    @Throws(FileNotFoundException::class)
    private fun resize(context: Context, uri: Uri): Bitmap {
        return Bitmap.createScaledBitmap(scaleDown(context, uri), width, height, false)
    }

    //----------------------------------------------------------------------------------------------

    interface OnLoadingListener {
        fun onLoadingStart(key: Long)
        fun onLoadingSuccess(key: Long, file: File)
        fun onLoadingFailure(key: Long, throwable: Throwable)
    }

    companion object {
        const val FILE_NOT_UPLOADED = -1L
    }
}
