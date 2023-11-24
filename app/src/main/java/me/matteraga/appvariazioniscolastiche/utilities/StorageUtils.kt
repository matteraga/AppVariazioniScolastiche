package me.matteraga.appvariazioniscolastiche.utilities

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream

class StorageUtils(private val context: Context) {

    private val sharedPref = context.getSharedPreferences("files", Context.MODE_PRIVATE)

    private val resolver = context.contentResolver

    private inline fun <T> sdk29AndUp(onSdk29: () -> T): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            onSdk29()
        } else null
    }

    // Salva il file in memoria e aggiorna le shared preferences
    fun save(bytes: ByteArray, fileName: String, key: String): Uri? {
        return try {
            val uri = sdk29AndUp {
                save29AndUp(bytes, fileName)
            } ?: save28(bytes, fileName)

            with(sharedPref.edit()) {
                putString("${key}-uri", uri.toString())
                apply()
            }

            uri
        } catch (th: Throwable) {
            Log.e("StorageUtils", "Saving file error", th)
            null
        }
    }

    // Salva il file nella cartella Variazioni
    private fun save28(bytes: ByteArray, fileName: String): Uri? {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val dir = File(
                Environment.getExternalStorageDirectory()
                    .toString() + File.separator + "Variazioni"
            )
            if (!dir.exists()) {
                dir.mkdir()
            }
            val target = File(
                Environment.getExternalStorageDirectory()
                    .toString() + File.separator + "Variazioni",
                fileName
            )
            FileOutputStream(target).use { output ->
                output.write(bytes)
            }
            return FileProvider.getUriForFile(
                context,
                context.applicationContext.packageName + ".provider",
                target
            )
        }

        return null
    }

    // Salva il file nella cartella Download
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun save29AndUp(bytes: ByteArray, fileName: String): Uri? {
        delete29AndUp(fileName)

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        resolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            contentValues
        )?.also { outputUri ->
            resolver.openOutputStream(outputUri).use { output ->
                output!!.write(bytes)
                return outputUri
            }
        }

        return null
    }

    // Elimina il file dalla cartella Download
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun delete29AndUp(fileName: String) {
        resolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME
            ),
            "${MediaStore.MediaColumns.DISPLAY_NAME} == ?",
            arrayOf(fileName),
            null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)

            if (cursor.moveToFirst()) {
                val id = cursor.getLong(idColumn)
                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    id
                )
                resolver.delete(contentUri, null, null)
            }
        }
    }

    // Elimina tutti i file delle variazioni
    fun bulkDelete(): Boolean {
        return try {
            val files = sharedPref.all.values.map { it.toString().toUri() }

            sdk29AndUp {
                bulkDelete29AndUp(files)
            } ?: bulkDelete28(files.map { it.pathSegments.last() })

            // Rimuove i percorsi dei file dalle shared preferences
            with(sharedPref.edit()) {
                clear()
                apply()
            }

            true
        } catch (th: Throwable) {
            Log.e("StorageUtils", "Bulk delete error", th)
            false
        }
    }

    // Elimina dalla cartella Variazioni i file passati
    private fun bulkDelete28(files: List<String>) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            File(
                Environment.getExternalStorageDirectory()
                    .toString() + File.separator + "Variazioni"
            ).listFiles()?.forEach { file ->
                if (file.isFile && files.contains(file.name)) {
                    file.delete()
                }
            }
        }
    }

    // Elimina dalla cartella Download i file passati
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun bulkDelete29AndUp(files: List<Uri>) {
        resolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.MediaColumns._ID
            ),
            null,
            null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    id
                )
                if (files.contains(contentUri)) {
                    resolver.delete(contentUri, null, null)
                }
            }
        }
    }

    // Controlla se il file esiste
    fun check(uri: Uri): Boolean {
        return try {
            DocumentFile.fromSingleUri(context, uri)?.exists() ?: false
        } catch (e: SecurityException) {
            false
        } catch (th: Throwable) {
            Log.e("StorageUtils", "File check error", th)
            false
        }
    }
}