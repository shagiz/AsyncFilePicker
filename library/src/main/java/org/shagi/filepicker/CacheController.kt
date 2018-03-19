package org.shagi.filepicker

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

private const val MAX_CACHE_SIZE = 52_428_800L //50 MB
private const val MAX_FILE_SIZE = 10_485_760L //10MB

class CacheController constructor(val context: Context) {

    var maxCacheSize = MAX_CACHE_SIZE
    var maxFileSize = MAX_FILE_SIZE

    private val cacheDir = File(context.cacheDir, "files_cache")

    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }

    fun setup(maxCacheSize: Long, maxFileSize: Long) {
        if (maxFileSize > maxCacheSize) {
            throw IllegalStateException("max file size couldn't be greater then max cache size")
        }

        this.maxCacheSize = maxCacheSize
        this.maxFileSize = maxFileSize
    }

    @Throws(MaxCacheFileSizeException::class)
    fun saveBitmap(bitmap: Bitmap): File {

        val currentSize = getDirSize(cacheDir)
        val imageFile = File(cacheDir, "image_" + System.currentTimeMillis() + ".jpg")

        ByteArrayOutputStream().use {
            val byteOutput = it;
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteOutput)
            it.flush()

            val size = it.size()

            if (size > maxFileSize) {
                throw MaxCacheFileSizeException()
            }

            if (currentSize + size > maxCacheSize) {
                cleanSpace(cacheDir, (currentSize + size) - maxCacheSize)
            }

            FileOutputStream(imageFile).use {
                byteOutput.writeTo(it)
            }
        }
        return imageFile
    }

    @Throws(MaxCacheFileSizeException::class)
    fun saveFile(uri: Uri): File {

        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.moveToFirst()

        val name = cursor?.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                ?: uri.lastPathSegment

        val size = cursor?.getLong(cursor.getColumnIndex(OpenableColumns.SIZE))

        if (size != null && size > maxFileSize) {
            throw MaxCacheFileSizeException()
        }

        val inputStream = context.contentResolver.openInputStream(uri)

        if (size == null && inputStream.available() > maxFileSize) {
            throw MaxCacheFileSizeException()
        }

        val currentSize = getDirSize(cacheDir)
        val newSize = currentSize + inputStream.available()

        if (newSize > maxCacheSize) {
            cleanSpace(cacheDir, newSize - maxCacheSize)
        }

        val file = File(cacheDir, name)

        val outputStream = FileOutputStream(file)

        val buffer = ByteArray(inputStream.available())
        inputStream.read(buffer)
        outputStream.write(buffer)

        outputStream.flush()
        cursor?.close()

        return file
    }

    private fun cleanSpace(dir: File, bytes: Long) {
        var bytesDeleted = 0L
        dir.listFiles()
                .filter { it.isFile }
                .sortedBy { it.lastModified() }
                .forEach {
                    bytesDeleted += it.length()
                    it.delete()
                    if (bytesDeleted > bytes) {
                        return
                    }
                }
    }

    private fun getDirSize(dir: File): Long {
        var size = 0L
        dir.listFiles().forEach { size += it.length() }
        return size
    }

    class MaxCacheFileSizeException : IllegalArgumentException("File size limit exceeded")
}