package fr.husi.ui.tools

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import fr.husi.compose.TextButton
import fr.husi.compose.material3.Button
import fr.husi.compose.material3.Icon
import fr.husi.compose.material3.OutlinedButton
import fr.husi.compose.material3.Text
import fr.husi.ktx.Logs
import fr.husi.ktx.readableMessage
import fr.husi.resources.Res
import fr.husi.resources.cancel
import fr.husi.resources.delete
import fr.husi.resources.error
import fr.husi.resources.error_title
import fr.husi.resources.icon_pack_description
import fr.husi.resources.icon_pack_import
import fr.husi.resources.icon_pack_import_error
import fr.husi.resources.icon_pack_import_success
import fr.husi.resources.icon_pack_reset
import fr.husi.resources.icon_pack_reset_done
import fr.husi.resources.icon_target_launcher
import fr.husi.resources.icon_target_qs_active
import fr.husi.resources.icon_target_qs_rest
import fr.husi.resources.icon_target_shortcut_scan
import fr.husi.resources.icon_target_shortcut_toggle
import fr.husi.resources.ok
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import java.io.File

@Composable
actual fun IconsScreen(
    modifier: Modifier,
    bottomPadding: Dp,
    onVisibleChange: (Boolean) -> Unit,
    showSnackbar: (message: String) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var refreshKey by remember { mutableStateOf(0) }
    var errorDialog by remember { mutableStateOf<String?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }

    val importLauncher = rememberFilePickerLauncher(
        type = FileKitType.File(extensions = listOf("zip")),
    ) { file ->
        if (file == null) return@rememberFilePickerLauncher
        val filePath = file.path ?: return@rememberFilePickerLauncher
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val uri = Uri.fromFile(File(filePath))
                val result = IconPackManager.importFromZip(context, uri)
                result.fold(
                    onSuccess = { importResult ->
                        withContext(Dispatchers.Main) {
                            if (importResult.importedCount > 0) {
                                showSnackbar(
                                    getString(
                                        Res.string.icon_pack_import_success,
                                        importResult.importedCount,
                                    ),
                                )
                                refreshKey++
                            }
                            if (importResult.errors.isNotEmpty()) {
                                errorDialog = importResult.errors.joinToString("\n")
                            }
                        }
                    },
                    onFailure = { e ->
                        Logs.e(e)
                        withContext(Dispatchers.Main) {
                            showSnackbar(getString(Res.string.icon_pack_import_error))
                        }
                    },
                )
            } catch (e: Exception) {
                Logs.e(e)
                withContext(Dispatchers.Main) {
                    errorDialog = e.readableMessage
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        onVisibleChange(true)
    }

    val iconTargets = remember(refreshKey) {
        listOf(
            "ic_service_active" to Res.string.icon_target_qs_active,
            "ic_service_rest" to Res.string.icon_target_qs_rest,
            "ic_shortcut_toggle" to Res.string.icon_target_shortcut_toggle,
            "ic_shortcut_scan" to Res.string.icon_target_shortcut_scan,
            "ic_launcher_foreground" to Res.string.icon_target_launcher,
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // Description
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(Res.string.icon_pack_description),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = { importLauncher.launch() },
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = org.jetbrains.compose.resources.vectorResource(
                        fr.husi.resources.Res.drawable.file_export,
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(Res.string.icon_pack_import))
            }

            OutlinedButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = org.jetbrains.compose.resources.vectorResource(
                        fr.husi.resources.Res.drawable.delete,
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(Res.string.icon_pack_reset))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Icon preview grid
        Text(
            text = "Current Icons",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.height(300.dp),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(iconTargets, key = { it.first }) { (fileName, labelRes) ->
                val bitmap = remember(refreshKey) {
                    IconPackManager.loadIconBitmap(context, "$fileName.png")
                }
                IconPreviewCard(
                    label = stringResource(labelRes),
                    bitmap = bitmap,
                    isCustom = bitmap != null,
                )
            }
        }

        Spacer(modifier = Modifier.height(bottomPadding))
    }

    if (errorDialog != null) AlertDialog(
        onDismissRequest = { errorDialog = null },
        confirmButton = {
            TextButton(stringResource(Res.string.ok)) { errorDialog = null }
        },
        icon = { Icon(org.jetbrains.compose.resources.vectorResource(Res.drawable.error), null) },
        title = { Text(stringResource(Res.string.error_title)) },
        text = { Text(errorDialog!!) },
    )

    if (showResetDialog) AlertDialog(
        onDismissRequest = { showResetDialog = false },
        confirmButton = {
            TextButton(stringResource(Res.string.ok)) {
                showResetDialog = false
                IconPackManager.resetIcons(context)
                refreshKey++
                showSnackbar(getString(Res.string.icon_pack_reset_done))
            }
        },
        dismissButton = {
            TextButton(stringResource(Res.string.cancel)) {
                showResetDialog = false
            }
        },
        title = { Text(stringResource(Res.string.icon_pack_reset)) },
        text = { Text("Reset all custom icons to defaults?") },
    )
}

@Composable
private fun IconPreviewCard(
    label: String,
    bitmap: Bitmap?,
    isCustom: Boolean,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (bitmap != null) {
                Image(
                    painter = BitmapPainter(bitmap.asImageBitmap()),
                    contentDescription = label,
                    modifier = Modifier.size(48.dp),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "—",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isCustom) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
            )
        }
    }
}
