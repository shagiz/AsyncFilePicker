package org.shagi.filepicker

import android.net.Uri
import java.io.File

data class ExtFile(val file: File,
                   val baseUri: Uri? = null,
                   val name: String? = null,
                   val extension: String? = null,
                   val mimType: String? = null) {

    override fun toString(): String {
        return """
            File: $file
            Uri: $baseUri
            Name: $name
            Extension: $extension
            MimeType: $mimType
        """.trimIndent()
    }
}
