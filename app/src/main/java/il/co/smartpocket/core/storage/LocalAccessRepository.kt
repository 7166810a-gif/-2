package il.co.smartpocket.core.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.provider.DocumentsContract

data class LocalFileEntry(
    val name: String,
    val mimeType: String?,
    val sizeBytes: Long,
    val isDirectory: Boolean,
    val uri: Uri
)

/**
 * Stores only user-selected SAF access. No broad storage permission is requested.
 * The app never uploads or transmits the selected content.
 */
object LocalAccessRepository {
    private const val PREFS_NAME = "mega_pocket_x_access"

    fun persistReadAccess(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun persistTreeAccess(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            true
        } catch (_: Exception) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    fun saveUri(context: Context, key: String, uri: Uri) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(key, uri.toString()).apply()
    }

    fun readUri(context: Context, key: String): Uri? {
        val value = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(key, null) ?: return null
        return runCatching { Uri.parse(value) }.getOrNull()
    }

    fun listChildren(context: Context, treeUri: Uri): List<LocalFileEntry> {
        return runCatching {
            val documentId = DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE
            )
            val results = mutableListOf<LocalFileEntry>()
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                while (cursor.moveToNext()) {
                    val childId = cursor.getString(idColumn) ?: continue
                    val name = if (nameColumn >= 0) cursor.getString(nameColumn) else null
                    val mime = if (mimeColumn >= 0) cursor.getString(mimeColumn) else null
                    val size = if (sizeColumn >= 0 && !cursor.isNull(sizeColumn)) cursor.getLong(sizeColumn) else 0L
                    val childUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childId)
                    results += LocalFileEntry(
                        name = name ?: "ללא שם",
                        mimeType = mime,
                        sizeBytes = size,
                        isDirectory = mime == DocumentsContract.Document.MIME_TYPE_DIR,
                        uri = childUri
                    )
                }
            }
            results.sortedWith(compareBy<LocalFileEntry> { !it.isDirectory }.thenBy { it.name.lowercase() })
        }.getOrElse { emptyList() }
    }

    fun displayName(context: Context, uri: Uri): String {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) cursor.getString(index) else null
                } else null
            }
        }.getOrNull() ?: uri.lastPathSegment ?: "קובץ מקומי"
    }
}
