package il.co.smartpocket.core.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns

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
