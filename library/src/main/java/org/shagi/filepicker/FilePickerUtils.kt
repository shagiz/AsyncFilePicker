package org.shagi.filepicker

import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

const val TEMP_FILE = "temp_file"

fun getPath(context: Context, uri: Uri): String? {
    if ("content".equals(uri.scheme, ignoreCase = true)) {
        return getDataColumn(context, uri)
    } else if ("file".equals(uri.scheme, ignoreCase = true)) {
        return uri.path
    }

    return null
}

fun getExtension(file: File): String {
    return file.extension
}

fun getMimeType(file: File): String? {
    var mimeType: String? = null

    val extension = MimeTypeMap.getFileExtensionFromUrl(file.path)

    if (MimeTypeMap.getSingleton().hasExtension(extension)) {
        mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }
    return mimeType
}

fun getDataColumn(context: Context, uri: Uri): String? {

    var cursor: Cursor? = null
    val column = "_data"
    val projection = arrayOf(column)

    try {
        cursor = context.contentResolver.query(uri, projection, null, null, null)
        if (cursor != null && cursor.moveToFirst()) {
            val columnIndex = cursor.getColumnIndexOrThrow(column)
            return cursor.getString(columnIndex)
        }
    } finally {
        if (cursor != null)
            cursor.close()
    }
    return null
}

fun saveTempFileUri(context: Context, uri: Uri): File {
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.moveToFirst()

    val inputStream = context.contentResolver.openInputStream(uri)
    val file = File(context.filesDir, TEMP_FILE)
    val outputStream = FileOutputStream(file)

    val buffer = ByteArray(inputStream.available())
    inputStream.read(buffer)
    outputStream.write(buffer)

    outputStream.flush()

    cursor.close()

    return file
}

fun saveTempBitmap(context: Context, bitmap: Bitmap): File {

    val imageFile = File(context.filesDir, TEMP_FILE)

    ByteArrayOutputStream().use {
        val byteOutput = it;
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteOutput)
        it.flush()

        FileOutputStream(imageFile).use {
            byteOutput.writeTo(it)
        }
    }

    return imageFile
}