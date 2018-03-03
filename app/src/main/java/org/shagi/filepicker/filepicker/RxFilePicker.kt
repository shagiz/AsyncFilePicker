package org.shagi.filepicker.filepicker

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
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.TimeUnit

class RxFilePicker : FilePickerDialog.OnFilePickedListener, Disposable {

    private lateinit var mDialog: FilePickerDialog

    private lateinit var mCacheController: CacheController

    private val compositeDisposable = CompositeDisposable()

    private var context: Context? = null

    private var loadingListener: OnLoadingListener? = null

    private var width = 1024;
    private var height = 1024;

    fun from(_context: Context): RxFilePicker {
        context = _context
        mCacheController = CacheController(_context)
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
        Timber.d("DebugTag, disposed $context")
    }

    fun attach(_loadingListener: OnLoadingListener): RxFilePicker {
        loadingListener = _loadingListener
        return this
    }

    override fun onFilePicked(uri: Uri, fileType: FileType, fromCamera: Boolean) {
        val key = System.currentTimeMillis()

        loadingListener?.onLoadingStart(key)

        context?.let {
            compositeDisposable.add(Observable.fromCallable { saveToFile(it, uri, fileType, fromCamera) }
                    .subscribeOn(Schedulers.io())
                    .delay(30, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnDispose { Timber.d("DebugTag, rxDisposed $context") }
                    .subscribe({
                        Timber.d("DebugTag, rxSucces $this - $context - $loadingListener")
                        loadingListener?.onLoadingSuccess(key, it)
                    }, {
                        Timber.d("DebugTag, rxFail $context")
                        loadingListener?.onLoadingFailure(key)
                    }, {
                        Timber.d("DebugTag, onComplete $context")
                    }))
        }
    }

    override fun onFilePickFailed() {
        Timber.d("DebugTag, onFilePickFailed $context")
        loadingListener?.onLoadingFailure(FILE_NOT_UPLOADED)
    }

    //---------------------------------------------------------------------------------------------

    private fun saveToFile(context: Context, uri: Uri, fileType: FileType, isFromCamera: Boolean): File {
        return if (fileType == FileType.FILE) {
            saveFile(uri)
        } else {
            savePhoto(context, uri, isFromCamera)
        }
    }

    private fun saveFile(uri: Uri): File {
        return mCacheController.saveFile(uri)
    }

    private fun savePhoto(context: Context, uri: Uri, isFromCamera: Boolean): File {
        val bitmap = if (width - height == 0) {
            scaleDown(context, uri)
        } else {
            resize(context, uri)
        }

        return mCacheController.saveBitmap(rotateIfNeeded(context, bitmap, uri, isFromCamera))
    }


    private fun rotateIfNeeded(context: Context, bitmap: Bitmap, uri: Uri, isFromCamera: Boolean): Bitmap {

        val path = if (isFromCamera) {
            uri.path
        } else {
            getPath(context, uri)
        }
        Timber.d("AA $uri path: $path")

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

        Timber.d("rotate $rotate")
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
        fun onLoadingFailure(key: Long)
    }

    companion object {
        const val FILE_NOT_UPLOADED = -1L
    }
}
