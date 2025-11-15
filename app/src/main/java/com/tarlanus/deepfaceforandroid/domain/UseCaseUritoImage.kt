package com.tarlanus.deepfaceforandroid.domain

import javax.inject.Inject

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.content.ContextCompat
class UseCaseUritoImage @Inject constructor() {
    fun getPath(context: Context, uri: Uri): String? {
        return when {
            DocumentsContract.isDocumentUri(context, uri) -> {
                when {
                    isExternalStorageDocument(uri) -> {
                        val docId = DocumentsContract.getDocumentId(uri)
                        val split = docId.split(":")
                        val type = split[0]
                        if (type.equals("primary", ignoreCase = true)) {
                            val externalFilesDirs = ContextCompat.getExternalFilesDirs(context, null)
                            "${externalFilesDirs[0]}/${split[1]}"
                        } else {
                            null
                        }
                    }

                    isDownloadsDocument(uri) -> {
                        val id = DocumentsContract.getDocumentId(uri)
                        val contentUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"),
                            id.toLong()
                        )
                        getDataColumn(context, contentUri, null, null)
                    }

                    isMediaDocument(uri) -> {
                        val docId = DocumentsContract.getDocumentId(uri)
                        val split = docId.split(":")
                        val type = split[0]

                        val contentUri = when (type) {
                            "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                            else -> null
                        }

                        val selection = "_id=?"
                        val selectionArgs = arrayOf(split[1])
                        getDataColumn(context, contentUri, selection, selectionArgs)
                    }

                    else -> null
                }
            }

            "content".equals(uri.scheme, ignoreCase = true) -> {
                getDataColumn(context, uri, null, null)
            }

            "file".equals(uri.scheme, ignoreCase = true) -> {
                uri.path
            }

            else -> null
        }
    }

    private fun getDataColumn(
        context: Context,
        uri: Uri?,
        selection: String?,
        selectionArgs: Array<String>?
    ): String? {
        val column = "_data"
        val projection = arrayOf(column)
        context.contentResolver.query(uri ?: return null, projection, selection, selectionArgs, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndexOrThrow(column)
                    return cursor.getString(index)
                }
            }
        return null
    }

    private fun isExternalStorageDocument(uri: Uri): Boolean =
        uri.authority == "com.android.externalstorage.documents"

    private fun isDownloadsDocument(uri: Uri): Boolean =
        uri.authority == "com.android.providers.downloads.documents"

    private fun isMediaDocument(uri: Uri): Boolean =
        uri.authority == "com.android.providers.media.documents"
}