package org.shagi.filepicker.filepicker

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File


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
            val column_index = cursor.getColumnIndexOrThrow(column)
            return cursor.getString(column_index)
        }
    } finally {
        if (cursor != null)
            cursor.close()
    }
    return null
}