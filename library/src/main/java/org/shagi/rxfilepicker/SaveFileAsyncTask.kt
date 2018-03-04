package org.shagi.rxfilepicker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.AsyncTask
import android.support.media.ExifInterface
import android.util.Log

import java.io.File
import java.io.FileNotFoundException
import java.lang.ref.WeakReference

class SaveFileAsyncTask internal constructor(context: Context,
                                             cacheController: CacheController?,
                                             private val fileType: FileType,
                                             private val isFromCamera: Boolean) : AsyncTask<Uri, Void, File>() {

    private var key: Long = 0
    private var throwable: Throwable? = null

    private val weakContext = WeakReference(context)
    private var weakListener: WeakReference<FilePicker.OnLoadingListener>? = null
    private var weakCacheController: WeakReference<CacheController>? = null

    private var width = 1024
    private var height = 1024

    init {
        cacheController?.let { weakCacheController = WeakReference(cacheController) }
    }

    internal fun setLoadingListener(loadingListener: FilePicker.OnLoadingListener?) {
        weakListener = WeakReference<FilePicker.OnLoadingListener>(loadingListener)
    }

    override fun onPreExecute() {
        key = System.currentTimeMillis()
        val loadingListener = weakListener?.get()
        loadingListener?.onLoadingStart(key) ?: cancel(false)
        Log.d("DEBUG", "onPreExecute $key, $loadingListener")
    }

    override fun doInBackground(vararg uris: Uri): File? {
        if (isCancelled) return null
        Log.d("DEBUG", "doInBackground $key, $uris, ${uris.size} , ${uris[0]}")

        weakContext.get()?.let {
             var r = saveToFile(it, uris[0], fileType, isFromCamera)
            Thread.sleep(200000)
            return r
        }

        return null;
    }

    override fun onPostExecute(file: File?) {
        val loadingListener = weakListener?.get()

        loadingListener?.let {
            throwable?.let {
                loadingListener.onLoadingFailure(key, it)
                return
            }
            if (file != null) {
                Log.d("DEBUG", "onPostExecute $key, $file, $loadingListener, ${weakContext.get()}")
                loadingListener.onLoadingSuccess(key, file)
            }
        }
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
        weakCacheController?.get()?.let {
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

        weakCacheController?.get()?.let {
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

}
