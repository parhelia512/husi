package fr.husi.ui.tools

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

data class ImportResult(
    val importedCount: Int,
    val errors: List<String>,
)

object IconPackManager {

    private const val ICON_PACK_DIR = "icon_pack"

    private val RECOGNIZED_FILES = setOf(
        "ic_service_active.png",
        "ic_service_rest.png",
        "ic_shortcut_toggle.png",
        "ic_shortcut_scan.png",
        "ic_launcher_foreground.png",
    )

    fun iconPackDir(context: Context): File {
        return File(context.filesDir, ICON_PACK_DIR).also { it.mkdirs() }
    }

    fun hasCustomIcons(context: Context): Boolean {
        val dir = iconPackDir(context)
        return dir.listFiles()?.any { it.name in RECOGNIZED_FILES } == true
    }

    fun loadIconBitmap(context: Context, name: String): Bitmap? {
        val file = File(iconPackDir(context), name)
        if (!file.exists()) return null
        return BitmapFactory.decodeFile(file.absolutePath)
    }

    fun importFromZip(context: Context, zipUri: Uri): Result<ImportResult> {
        return runCatching {
            val dir = iconPackDir(context)
            var importedCount = 0
            val errors = mutableListOf<String>()

            context.contentResolver.openInputStream(zipUri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val entryName = entry.name
                            val baseName = entryName.substringAfterLast('/')
                            // Match by filename to handle nested paths (e.g., "drawable/ic_service_active.png")
                            val targetName = RECOGNIZED_FILES.find { it.equals(baseName, ignoreCase = true) }
                            if (targetName != null) {
                                try {
                                    val bitmap = BitmapFactory.decodeStream(zip)
                                    if (bitmap != null) {
                                        val outFile = File(dir, targetName)
                                        FileOutputStream(outFile).use { fos ->
                                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                                        }
                                        bitmap.recycle()
                                        importedCount++
                                    } else {
                                        errors.add("$targetName: invalid image")
                                    }
                                } catch (e: Exception) {
                                    errors.add("$targetName: ${e.message}")
                                }
                            }
                        }
                        entry = zip.nextEntry
                    }
                }
            } ?: errors.add("Cannot open ZIP file")

            if (importedCount == 0 && errors.isEmpty()) {
                errors.add("No recognized icon files found in ZIP")
            }

            ImportResult(importedCount, errors)
        }
    }

    fun resetIcons(context: Context) {
        val dir = iconPackDir(context)
        dir.listFiles()?.forEach { it.delete() }
    }

    fun iconTargets(context: Context): List<Pair<String, Boolean>> {
        return RECOGNIZED_FILES.map { name ->
            name to (loadIconBitmap(context, name) != null)
        }
    }
}
