@file:OptIn(ExperimentalLayoutApi::class)

package fr.husi.ui.configuration

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ernestoyaquello.dragdropswipelazycolumn.OrderedItem
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.ernestoyaquello.dragdropswipelazycolumn.DragDropSwipeLazyColumn
import com.ernestoyaquello.dragdropswipelazycolumn.DraggableSwipeableItem
import com.ernestoyaquello.dragdropswipelazycolumn.DraggableSwipeableItemScope
import com.ernestoyaquello.dragdropswipelazycolumn.config.DraggableSwipeableItemColors
import com.ernestoyaquello.dragdropswipelazycolumn.state.rememberDragDropSwipeLazyColumnState
import fr.husi.GroupType
import fr.husi.Key
import fr.husi.compose.BoxedVerticalScrollbar
import fr.husi.compose.SheetActionRow
import fr.husi.compose.SheetSectionTitle
import fr.husi.compose.SimpleIconButton
import fr.husi.compose.TextButton
import fr.husi.compose.colorForUrlTestDelay
import fr.husi.compose.fadingEdge
import fr.husi.compose.material3.Icon
import fr.husi.compose.material3.IconButton
import fr.husi.compose.material3.Text
import fr.husi.compose.rememberScrollHideState
import fr.husi.compose.setPlainText
import fr.husi.database.DataStore
import fr.husi.database.ProxyEntity
import fr.husi.database.displayType
import fr.husi.fmt.ValidateResult
import fr.husi.fmt.config.ConfigBean
import fr.husi.fmt.toUniversalLink
import fr.husi.ktx.Logs
import fr.husi.ktx.blankAsNull
import fr.husi.ktx.onMainDispatcher
import fr.husi.ktx.readableMessage
import fr.husi.ktx.readableUrlTestError
import fr.husi.libcore.Libcore
import fr.husi.resources.Res
import fr.husi.resources.action_export_clipboard
import fr.husi.resources.action_export_file
import fr.husi.resources.action_export_msg
import fr.husi.resources.arrow_outward
import fr.husi.resources.available
import fr.husi.resources.connection_test_unreachable
import fr.husi.resources.content_copy
import fr.husi.resources.copy_all
import fr.husi.resources.delete
import fr.husi.resources.deprecated
import fr.husi.resources.drag_indicator
import fr.husi.resources.edit
import fr.husi.resources.error
import fr.husi.resources.error_title
import fr.husi.resources.file_export
import fr.husi.resources.fingerprint
import fr.husi.resources.insecure
import fr.husi.resources.internal_link
import fr.husi.resources.link
import fr.husi.resources.menu_configuration
import fr.husi.resources.more_vert
import fr.husi.resources.ok
import fr.husi.resources.outbound
import fr.husi.resources.qr_code
import fr.husi.resources.send
import fr.husi.resources.settings
import fr.husi.resources.share
import fr.husi.resources.share_qr_nfc
import fr.husi.resources.standard
import fr.husi.resources.traffic
import fr.husi.resources.unavailable
import fr.husi.resources.warning
import fr.husi.results.LocalResultEventBus
import fr.husi.results.ResultEffect
import fr.husi.ui.NavRoutes
import fr.husi.ui.StringOrRes
import io.github.oikvpqya.compose.fastscroller.material3.defaultMaterialScrollbarStyle
import io.github.oikvpqya.compose.fastscroller.rememberScrollbarAdapter
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.write
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

private data class PendingProfileEdit(
    val resultKey: String,
    val profileId: Long,
)

@Composable
internal fun GroupHolderScreen(
    modifier: Modifier = Modifier,
    viewModel: GroupProfilesHolderViewModel,
    bottomPadding: Dp,
    showActions: Boolean = true,
    onProfileSelect: (Long) -> Unit,
    onOpenProfileEditor: ((NavRoutes.ProfileEditor) -> Unit)? = null,
    needReload: () -> Unit,
    showQR: (name: String, url: String) -> Unit,
    onCopySuccess: () -> Unit,
    showSnackbar: (message: StringOrRes) -> Unit,
    showUndoSnackbar: (count: Int, onUndo: () -> Unit) -> Unit,
    onScrollHideChange: (Boolean) -> Unit = {},
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val resultBus = onOpenProfileEditor?.let { LocalResultEventBus.current }
    val pendingProfileEdits = remember { mutableStateListOf<PendingProfileEdit>() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.hiddenProfiles) {
        if (uiState.hiddenProfiles > 0) {
            showUndoSnackbar(uiState.hiddenProfiles) {
                viewModel.undo()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.commit()
        }
    }
    val showAddress by viewModel.alwaysShowAddress.collectAsStateWithLifecycle(false)
    val blurAddress by viewModel.blurredAddress.collectAsStateWithLifecycle(false)
    val trafficStatistics by viewModel.trafficStatistics.collectAsStateWithLifecycle(true)
    val securityAdvisory by viewModel.securityAdvisory.collectAsStateWithLifecycle(true)
    val layoutColumns by DataStore.configurationStore
        .intFlow(Key.PROFILE_LAYOUT_COLUMNS, 1)
        .collectAsStateWithLifecycle(1)

    val dragDropListState = rememberDragDropSwipeLazyColumnState()
    val focusRequester = remember { FocusRequester() }

    val scrollHideVisible by rememberScrollHideState(dragDropListState.lazyListState)
    LaunchedEffect(scrollHideVisible) {
        onScrollHideChange(scrollHideVisible)
    }

    LaunchedEffect(uiState.scrollIndex) {
        uiState.scrollIndex?.let { index ->
            dragDropListState.lazyListState.animateScrollToItem(index)
            viewModel.consumeScrollIndex()
        }
    }

    LaunchedEffect(uiState.shouldRequestFocus) {
        if (uiState.shouldRequestFocus) {
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {
                // non-TV environments
            }
            viewModel.consumeFocusRequest()
        }
    }

    resultBus?.let { bus ->
        for (pending in pendingProfileEdits.toList()) {
            ResultEffect<Boolean>(
                resultEventBus = bus,
                resultKey = pending.resultKey,
            ) { updated ->
                if (updated && pending.profileId == DataStore.selectedProxy) {
                    needReload()
                }
                pendingProfileEdits.remove(pending)
            }
        }
    }

    fun openProfileEditor(profile: ProxyEntity) {
        val resultKey = "profile-editor-${profile.id}"
        pendingProfileEdits.removeAll { it.resultKey == resultKey }
        pendingProfileEdits += PendingProfileEdit(
            resultKey = resultKey,
            profileId = profile.id,
        )
        onOpenProfileEditor?.invoke(
            NavRoutes.ProfileEditor(
                type = profile.type,
                id = profile.id,
                subscription = viewModel.group.type == GroupType.SUBSCRIPTION,
                resultKey = resultKey,
            ),
        )
    }

    var exportConfig by remember { mutableStateOf("") }
    val exportFileLauncher = rememberFileSaverLauncher(
        dialogSettings = FileKitDialogSettings.createDefault(),
    ) { file ->
        if (file != null) lifecycleOwner.lifecycleScope.launch {
            try {
                file.write(exportConfig.encodeToByteArray())
                onMainDispatcher {
                    showSnackbar(StringOrRes.Res(Res.string.action_export_msg))
                }
            } catch (e: Exception) {
                Logs.w(e)
                onMainDispatcher {
                    showSnackbar(StringOrRes.Direct(e.readableMessage))
                }
            }
        }
        exportConfig = ""
    }

    var showErrorAlert by remember { mutableStateOf<String?>(null) }

    if (layoutColumns > 1) {
        ProfileGrid(
            modifier = modifier,
            profiles = uiState.profiles,
            bottomPadding = bottomPadding,
            showActions = showActions,
            onProfileSelect = onProfileSelect,
            onReorder = { viewModel.submitReordered(it) },
            onScrollHideChange = onScrollHideChange,
            edit = { openProfileEditor(it.profile) },
            delete = { viewModel.undoableRemove(it.profile.id) },
            showQR = { profile, url -> showQR(profile.profile.displayName(), url) },
            exportToFile = { name, config ->
                exportConfig = config
                exportFileLauncher.launch(suggestedName = name, defaultExtension = "json")
            },
            showErrorAlert = { showErrorAlert = it },
            onCopySuccess = onCopySuccess,
            showAddress = showAddress,
            blurAddress = blurAddress,
            trafficStatistic = trafficStatistics,
            securityAdvice = securityAdvisory,
        )
    } else {
        Row(
            modifier = modifier.fillMaxSize(),
        ) {
            DragDropSwipeLazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .focusRequester(focusRequester)
                    .fadingEdge(dragDropListState.lazyListState),
                state = dragDropListState,
                items = uiState.profiles.toImmutableList(),
                key = { it.profile.id },
                contentType = { 0 },
                contentPadding = PaddingValues(
                    bottom = bottomPadding,
                ),
                userScrollEnabled = true,
                onIndicesChangedViaDragAndDrop = { viewModel.submitReordered(it) },
            ) { index, item ->
                DraggableSwipeableItem(
                    modifier = Modifier
                        .padding(4.dp)
                        .animateDraggableSwipeableItem(),
                    colors = DraggableSwipeableItemColors.createRemembered(
                        containerBackgroundColor = Color.Transparent,
                        containerBackgroundColorWhileDragged = Color.Transparent,
                        clickIndicationColor = Color.Transparent,
                        behindSwipeContainerBackgroundColor = Color.Transparent,
                        behindSwipeIconColor = Color.Transparent,
                    ),
                ) {
                    ProxyCard(
                        profile = item,
                        select = { onProfileSelect(item.profile.id) },
                        edit = {
                            openProfileEditor(item.profile)
                        },
                        delete = { viewModel.undoableRemove(item.profile.id) },
                        showQR = { url ->
                            showQR(item.profile.displayName(), url)
                        },
                        exportToFile = { name, config ->
                            exportConfig = config
                            exportFileLauncher.launch(
                                suggestedName = name,
                                defaultExtension = "json",
                            )
                        },
                        showErrorAlert = { showErrorAlert = it },
                        onCopySuccess = onCopySuccess,
                        showAddress = showAddress,
                        blurAddress = blurAddress,
                        trafficStatistic = trafficStatistics,
                        securityAdvice = securityAdvisory,
                        showActions = showActions,
                    )
                }
            }

            BoxedVerticalScrollbar(
                modifier = Modifier.fillMaxHeight(),
                adapter = rememberScrollbarAdapter(scrollState = dragDropListState.lazyListState),
                style = defaultMaterialScrollbarStyle().copy(
                    thickness = 12.dp,
                ),
            )
        }
    }

    if (showErrorAlert != null) AlertDialog(
        onDismissRequest = { showErrorAlert = null },
        confirmButton = {
            TextButton(stringResource(Res.string.ok)) {
                showErrorAlert = null
            }
        },
        icon = {
            Icon(vectorResource(Res.drawable.error), null)
        },
        title = { Text(stringResource(Res.string.error_title)) },
        text = { Text(showErrorAlert!!) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DraggableSwipeableItemScope<ProfileItem>.ProxyCard(
    modifier: Modifier = Modifier,
    profile: ProfileItem,
    select: () -> Unit,
    edit: () -> Unit,
    delete: () -> Unit,
    showQR: (url: String) -> Unit,
    onCopySuccess: () -> Unit,
    exportToFile: (name: String, config: String) -> Unit,
    showErrorAlert: (String) -> Unit,
    showAddress: Boolean,
    blurAddress: Boolean,
    trafficStatistic: Boolean,
    securityAdvice: Boolean,
    showActions: Boolean = true,
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    val entity = profile.profile
    val bean = entity.requireBean()

    val (name, address) = when {
        blurAddress && bean.name.isBlank() -> bean.displayAddress().blur() to null
        blurAddress && showAddress -> bean.displayName() to bean.displayAddress().blur()
        showAddress -> bean.displayName() to bean.displayAddress()
        else -> bean.displayName() to null
    }

    val hasTraffic = entity.tx + entity.rx > 0L
    val trafficText = hasTraffic.takeIf { trafficStatistic }?.let {
        stringResource(
            Res.string.traffic,
            Libcore.formatBytes(entity.tx),
            Libcore.formatBytes(entity.rx),
        )
    }

    val (statusText, statusColor) = when (entity.status) {
        in Int.MIN_VALUE..ProxyEntity.STATUS_INITIAL -> {
            trafficText.orEmpty() to MaterialTheme.colorScheme.onSurfaceVariant
        }

        ProxyEntity.STATUS_AVAILABLE -> {
            stringResource(
                Res.string.available,
                entity.ping,
            ) to colorForUrlTestDelay(entity.ping)
        }

        ProxyEntity.STATUS_UNAVAILABLE -> {
            val text = readableUrlTestError(entity.error)?.let { stringResource(it) }
                ?: stringResource(Res.string.unavailable)
            text to Color.Red
        }

        ProxyEntity.STATUS_UNREACHABLE -> {
            val text = readableUrlTestError(entity.error)?.let { stringResource(it) }
                ?: stringResource(Res.string.connection_test_unreachable)
            text to Color.Red
        }

        else -> "" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    val showMiddleRow =
        address != null || (hasTraffic && entity.status > ProxyEntity.STATUS_INITIAL)

    var showShareSheet by remember { mutableStateOf(false) }
    var showSecurityAlert by remember { mutableStateOf(false) }
    val shareSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val validateResult = if (showActions && securityAdvice) {
        bean.isInsecure()
    } else {
        ValidateResult.Secure.Continue
    }

    OutlinedCard(
        onClick = select,
        modifier = modifier,
        elevation = CardDefaults.elevatedCardElevation(),
        border = if (profile.isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            CardDefaults.outlinedCardBorder()
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = vectorResource(Res.drawable.drag_indicator),
                contentDescription = "Drag to reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(40.dp)
                    .padding(8.dp)
                    .dragDropModifier(),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 0.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )

                    if (showActions) {
                        SimpleIconButton(
                            imageVector = vectorResource(Res.drawable.edit),
                            contentDescription = stringResource(Res.string.edit),
                            modifier = Modifier.size(40.dp),
                            onClick = edit,
                        )

                        val shareIcon: DrawableResource
                        val shareBackground: Color
                        val shareTint: Color
                        when (validateResult) {
                            is ValidateResult.Insecure -> {
                                shareIcon = Res.drawable.warning
                                shareBackground = Color.Red
                                shareTint = Color.White
                            }

                            is ValidateResult.Deprecated -> {
                                shareIcon = Res.drawable.warning
                                shareBackground = Color.Yellow
                                shareTint = Color.Gray
                            }

                            is ValidateResult.Secure -> {
                                shareIcon = Res.drawable.share
                                shareBackground = Color.Transparent
                                shareTint = MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        }

                        Box {
                            val shareTooltipText = when (validateResult) {
                                is ValidateResult.Insecure -> stringResource(Res.string.insecure)
                                is ValidateResult.Deprecated -> stringResource(Res.string.deprecated)
                                is ValidateResult.Secure -> stringResource(Res.string.share)
                            }
                            val shareTooltipState = rememberTooltipState()

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(shareBackground, shape = CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                        positioning = TooltipAnchorPosition.Below,
                                    ),
                                    tooltip = {
                                        PlainTooltip {
                                            Text(shareTooltipText)
                                        }
                                    },
                                    state = shareTooltipState,
                                ) {
                                    IconButton(
                                        onClick = {
                                            when (validateResult) {
                                                is ValidateResult.Insecure, is ValidateResult.Deprecated -> {
                                                    showSecurityAlert = true
                                                }

                                                is ValidateResult.Secure -> {
                                                    showShareSheet = true
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(40.dp),
                                    ) {
                                        Icon(
                                            imageVector = vectorResource(shareIcon),
                                            contentDescription = shareTooltipText,
                                            tint = shareTint,
                                        )
                                    }
                                }
                            }

                            if (showShareSheet) {
                                val canNotShareOutbound = entity.type == ProxyEntity.TYPE_CHAIN ||
                                        entity.type == ProxyEntity.TYPE_PROXY_SET ||
                                        entity.mustUsePlugin() ||
                                        (bean as? ConfigBean)?.type == ConfigBean.TYPE_CONFIG

                                ModalBottomSheet(
                                    onDismissRequest = { showShareSheet = false },
                                    sheetState = shareSheetState,
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        if (entity.haveLink()) {
                                            SheetSectionTitle(
                                                text = stringResource(Res.string.share_qr_nfc),
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = vectorResource(Res.drawable.qr_code),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                },
                                            )
                                            if (entity.haveStandardLink()) {
                                                SheetActionRow(
                                                    text = stringResource(Res.string.standard),
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = vectorResource(
                                                                Res.drawable.send,
                                                            ),
                                                            contentDescription = null,
                                                        )
                                                    },
                                                    onClick = {
                                                        showQR(entity.toStdLink())
                                                        showShareSheet = false
                                                    },
                                                )
                                            }
                                            SheetActionRow(
                                                text = stringResource(Res.string.internal_link),
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = vectorResource(Res.drawable.link),
                                                        contentDescription = null,
                                                    )
                                                },
                                                onClick = {
                                                    showQR(bean.toUniversalLink())
                                                    showShareSheet = false
                                                },
                                            )
                                            HorizontalDivider()
                                            SheetSectionTitle(
                                                text = stringResource(Res.string.action_export_clipboard),
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = vectorResource(Res.drawable.share),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                },
                                            )
                                            if (entity.haveStandardLink()) {
                                                SheetActionRow(
                                                    text = stringResource(Res.string.standard),
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = vectorResource(
                                                                Res.drawable.content_copy,
                                                            ),
                                                            contentDescription = null,
                                                        )
                                                    },
                                                    onClick = {
                                                        scope.launch {
                                                            clipboard.setPlainText(entity.toStdLink())
                                                            onCopySuccess()
                                                        }
                                                        showShareSheet = false
                                                    },
                                                )
                                            }
                                            SheetActionRow(
                                                text = stringResource(Res.string.internal_link),
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = vectorResource(Res.drawable.fingerprint),
                                                        contentDescription = null,
                                                    )
                                                },
                                                onClick = {
                                                    scope.launch {
                                                        clipboard.setPlainText(bean.toUniversalLink())
                                                        onCopySuccess()
                                                    }
                                                    showShareSheet = false
                                                },
                                            )
                                        }
                                        HorizontalDivider()
                                        SheetSectionTitle(
                                            text = stringResource(Res.string.menu_configuration),
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = vectorResource(Res.drawable.settings),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            },
                                        )
                                        SheetActionRow(
                                            text = stringResource(Res.string.action_export_clipboard),
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = vectorResource(Res.drawable.copy_all),
                                                    contentDescription = null,
                                                )
                                            },
                                            onClick = {
                                                scope.launch {
                                                    runCatching {
                                                        clipboard.setPlainText(entity.exportConfig().first)
                                                    }.onSuccess {
                                                        onCopySuccess()
                                                    }.onFailure { e ->
                                                        showErrorAlert(e.readableMessage)
                                                    }
                                                }
                                                showShareSheet = false
                                            },
                                        )
                                        SheetActionRow(
                                            text = stringResource(Res.string.action_export_file),
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = vectorResource(Res.drawable.file_export),
                                                    contentDescription = null,
                                                )
                                            },
                                            onClick = {
                                                runCatching {
                                                    val data = entity.exportConfig()
                                                    exportToFile(data.second, data.first)
                                                }.onFailure { e ->
                                                    showErrorAlert(e.readableMessage)
                                                }
                                                showShareSheet = false
                                            },
                                        )

                                        if (!canNotShareOutbound) {
                                            HorizontalDivider()
                                            SheetSectionTitle(
                                                text = stringResource(Res.string.outbound),
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = vectorResource(Res.drawable.arrow_outward),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                },
                                            )
                                            SheetActionRow(
                                                text = stringResource(Res.string.action_export_clipboard),
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = vectorResource(Res.drawable.copy_all),
                                                        contentDescription = null,
                                                    )
                                                },
                                                onClick = {
                                                    scope.launch {
                                                        clipboard.setPlainText(entity.exportOutbound().first)
                                                        onCopySuccess()
                                                    }
                                                    showShareSheet = false
                                                },
                                            )
                                            SheetActionRow(
                                                text = stringResource(Res.string.action_export_file),
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = vectorResource(Res.drawable.file_export),
                                                        contentDescription = null,
                                                    )
                                                },
                                                onClick = {
                                                    val data = entity.exportOutbound()
                                                    exportToFile(data.second, data.first)
                                                    showShareSheet = false
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        SimpleIconButton(
                            imageVector = vectorResource(Res.drawable.delete),
                            contentDescription = stringResource(Res.string.delete),
                            modifier = Modifier.size(40.dp),
                            onClick = delete,
                        )
                    }
                }

                if (showMiddleRow) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 0.dp, end = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        address?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        if (hasTraffic && entity.status > ProxyEntity.STATUS_INITIAL) {
                            trafficText?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 0.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = entity.displayType(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.weight(1f),
                    )

                    if (statusText.isNotEmpty()) {
                        val errorText = entity.error?.blankAsNull()
                        Text(
                            text = statusText,
                            modifier = Modifier.clickable {
                                errorText?.let(showErrorAlert)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = statusColor,
                        )
                    }
                }
            }
        }
    }

    if (showActions && showSecurityAlert) AlertDialog(
        onDismissRequest = {
            showSecurityAlert = false
            showShareSheet = true
        },
        icon = {
            Icon(vectorResource(Res.drawable.warning), null)
        },
        title = {
            Text(
                stringResource(
                    when (validateResult) {
                        is ValidateResult.Insecure -> Res.string.insecure
                        is ValidateResult.Deprecated -> Res.string.deprecated
                        else -> error("impossible")
                    },
                ),
            )
        },
        text = {
            val textRes = when (validateResult) {
                is ValidateResult.Insecure -> validateResult.textRes
                is ValidateResult.Deprecated -> validateResult.textRes
                else -> error("impossible")
            }
            Text(stringResource(textRes))
        },
        confirmButton = {
            TextButton(stringResource(Res.string.ok)) {
                showSecurityAlert = false
                showShareSheet = true
            }
        },
    )
}

@Composable
private fun ProfileGrid(
    modifier: Modifier = Modifier,
    profiles: List<ProfileItem>,
    bottomPadding: Dp,
    showActions: Boolean,
    onProfileSelect: (Long) -> Unit,
    onReorder: (List<OrderedItem<ProfileItem>>) -> Unit,
    onScrollHideChange: (Boolean) -> Unit,
    edit: (ProfileItem) -> Unit,
    delete: (ProfileItem) -> Unit,
    showQR: (ProfileItem, String) -> Unit,
    exportToFile: (String, String) -> Unit,
    showErrorAlert: (String) -> Unit,
    onCopySuccess: () -> Unit,
    showAddress: Boolean,
    blurAddress: Boolean,
    trafficStatistic: Boolean,
    securityAdvice: Boolean,
) {
    val gridState = rememberLazyGridState()
    val density = LocalDensity.current

    // Track scroll direction for FAB visibility
    var lastFirstVisible by remember { mutableStateOf(0) }
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.firstVisibleItemIndex }
            .collect { current ->
                val visible = current <= lastFirstVisible
                lastFirstVisible = current
                onScrollHideChange(visible)
            }
    }

    // Drag-and-drop state
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val itemHeightPx = with(density) { 100.dp.toPx() }

    val currentProfiles = remember(profiles, draggedIndex, dragOffsetY) {
        if (draggedIndex == null || dragOffsetY == 0f) {
            profiles
        } else {
            val from = draggedIndex!!
            val rows = (dragOffsetY / itemHeightPx).let { offset ->
                val sign = if (offset > 0) 1 else -1
                val abs = kotlin.math.abs(offset)
                sign * (abs + 0.3f).toInt() // 30% threshold
            }
            val to = (from + rows * 2).coerceIn(0, profiles.size - 1)
            if (from == to) {
                profiles
            } else {
                val mutable = profiles.toMutableList()
                val item = mutable.removeAt(from)
                mutable.add(to, item)
                mutable
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().clipToBounds()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(4.dp, 4.dp, 4.dp, bottomPadding + 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            itemsIndexed(
                items = currentProfiles,
                key = { _, item -> item.profile.id },
            ) { index, item ->
                val isDragged = draggedIndex == index
                CompactProxyCard(
                    modifier = Modifier
                        .height(100.dp)
                        .then(
                            if (isDragged) {
                                Modifier
                                    .shadow(8.dp, shape = MaterialTheme.shapes.medium)
                                    .graphicsLayer {
                                        scaleX = 1.05f
                                        scaleY = 1.05f
                                        alpha = 0.9f
                                    }
                            } else {
                                Modifier
                            }
                        )
                        .pointerInput(profiles) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { _ ->
                                    draggedIndex = index
                                    dragOffsetY = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetY += dragAmount.y
                                    val from = draggedIndex ?: return@detectDragGesturesAfterLongPress
                                    val rows =
                                        (dragOffsetY / itemHeightPx).let { offset ->
                                            val sign = if (offset > 0) 1 else -1
                                            val abs = kotlin.math.abs(offset)
                                            sign * (abs + 0.3f).toInt()
                                        }
                                    val to = (from + rows * 2).coerceIn(0, profiles.size - 1)
                                    if (from != to && to in profiles.indices) {
                                        val changes = mutableListOf<OrderedItem<ProfileItem>>()
                                        val step = if (from < to) 1 else -1
                                        var i = from
                                        while (i != to) {
                                            val next = i + step
                                            changes.add(
                                                OrderedItem(
                                                    value = profiles[i],
                                                    initialIndex = i,
                                                    newIndex = next,
                                                ),
                                            )
                                            i = next
                                        }
                                        changes.add(
                                            OrderedItem(
                                                value = profiles[from],
                                                initialIndex = from,
                                                newIndex = to,
                                            ),
                                        )
                                        onReorder(changes)
                                        draggedIndex = to
                                        dragOffsetY = 0f
                                    }
                                },
                                onDragEnd = {
                                    draggedIndex = null
                                    dragOffsetY = 0f
                                },
                                onDragCancel = {
                                    draggedIndex = null
                                    dragOffsetY = 0f
                                },
                            )
                        },
                    profile = item,
                    select = { onProfileSelect(item.profile.id) },
                    edit = { edit(item) },
                    delete = { delete(item) },
                    showQR = { url -> showQR(item, url) },
                    exportToFile = exportToFile,
                    showErrorAlert = showErrorAlert,
                    onCopySuccess = onCopySuccess,
                    showAddress = showAddress,
                    blurAddress = blurAddress,
                    trafficStatistic = trafficStatistic,
                    securityAdvice = securityAdvice,
                    showActions = showActions,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactProxyCard(
    modifier: Modifier = Modifier,
    profile: ProfileItem,
    select: () -> Unit,
    edit: () -> Unit,
    delete: () -> Unit,
    showQR: (url: String) -> Unit,
    onCopySuccess: () -> Unit,
    exportToFile: (name: String, config: String) -> Unit,
    showErrorAlert: (String) -> Unit,
    showAddress: Boolean,
    blurAddress: Boolean,
    trafficStatistic: Boolean,
    securityAdvice: Boolean,
    showActions: Boolean = true,
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    val entity = profile.profile
    val bean = entity.requireBean()

    val name = when {
        blurAddress && bean.name.isBlank() -> bean.displayAddress().blur()
        else -> bean.displayName()
    }

    val hasTraffic = entity.tx + entity.rx > 0L
    val trafficText = hasTraffic.takeIf { trafficStatistic }?.let {
        stringResource(
            Res.string.traffic,
            Libcore.formatBytes(entity.tx),
            Libcore.formatBytes(entity.rx),
        )
    }

    val (statusText, statusColor) = when (entity.status) {
        in Int.MIN_VALUE..ProxyEntity.STATUS_INITIAL -> {
            trafficText.orEmpty() to MaterialTheme.colorScheme.onSurfaceVariant
        }

        ProxyEntity.STATUS_AVAILABLE -> {
            stringResource(
                Res.string.available,
                entity.ping,
            ) to colorForUrlTestDelay(entity.ping)
        }

        ProxyEntity.STATUS_UNAVAILABLE -> {
            val text = readableUrlTestError(entity.error)?.let { stringResource(it) }
                ?: stringResource(Res.string.unavailable)
            text to Color.Red
        }

        ProxyEntity.STATUS_UNREACHABLE -> {
            val text = readableUrlTestError(entity.error)?.let { stringResource(it) }
                ?: stringResource(Res.string.connection_test_unreachable)
            text to Color.Red
        }

        else -> "" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    var showShareSheet by remember { mutableStateOf(false) }
    var showSecurityAlert by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    val shareSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val validateResult = if (showActions && securityAdvice) {
        bean.isInsecure()
    } else {
        ValidateResult.Secure.Continue
    }

    val shareMenuLabel: String = when (validateResult) {
        is ValidateResult.Insecure -> stringResource(Res.string.insecure)
        is ValidateResult.Deprecated -> stringResource(Res.string.deprecated)
        is ValidateResult.Secure -> stringResource(Res.string.share)
    }

    OutlinedCard(
        onClick = select,
        modifier = modifier,
        elevation = CardDefaults.elevatedCardElevation(),
        border = if (profile.isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            CardDefaults.outlinedCardBorder()
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
        ) {
            // Title row: drag handle + name + overflow menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = vectorResource(Res.drawable.drag_indicator),
                    contentDescription = "Drag to reorder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )

                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )

                if (showActions) {
                    Box {
                        SimpleIconButton(
                            imageVector = vectorResource(Res.drawable.more_vert),
                            contentDescription = stringResource(Res.string.menu),
                            modifier = Modifier.size(28.dp),
                            onClick = { showOverflowMenu = true },
                        )
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.edit)) },
                                onClick = {
                                    showOverflowMenu = false
                                    edit()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(shareMenuLabel) },
                                onClick = {
                                    showOverflowMenu = false
                                    when (validateResult) {
                                        is ValidateResult.Insecure,
                                        is ValidateResult.Deprecated -> showSecurityAlert = true
                                        is ValidateResult.Secure -> showShareSheet = true
                                    }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.delete)) },
                                onClick = {
                                    showOverflowMenu = false
                                    delete()
                                },
                            )
                        }
                    }
                }
            }

            Text(
                text = entity.displayType(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
            )

            // Traffic row (only when traffic statistics enabled and there is traffic)
            if (hasTraffic && trafficStatistic && entity.status > ProxyEntity.STATUS_INITIAL) {
                trafficText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }

            if (statusText.isNotEmpty()) {
                val errorText = entity.error?.blankAsNull()
                Text(
                    text = statusText,
                    modifier = Modifier.clickable {
                        errorText?.let(showErrorAlert)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor,
                    maxLines = 1,
                )
            }
        }
    }

    if (showActions && showShareSheet) {
        val canNotShareOutbound = entity.type == ProxyEntity.TYPE_CHAIN ||
            entity.type == ProxyEntity.TYPE_PROXY_SET ||
            entity.mustUsePlugin() ||
            (bean as? ConfigBean)?.type == ConfigBean.TYPE_CONFIG

        ModalBottomSheet(
            onDismissRequest = { showShareSheet = false },
            sheetState = shareSheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (entity.haveLink()) {
                    SheetSectionTitle(
                        text = stringResource(Res.string.share_qr_nfc),
                        leadingIcon = {
                            Icon(
                                imageVector = vectorResource(Res.drawable.qr_code),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                    if (entity.haveStandardLink()) {
                        SheetActionRow(
                            text = stringResource(Res.string.standard),
                            leadingIcon = {
                                Icon(
                                    imageVector = vectorResource(Res.drawable.send),
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                showQR(entity.toStdLink())
                                showShareSheet = false
                            },
                        )
                    }
                    SheetActionRow(
                        text = stringResource(Res.string.internal_link),
                        leadingIcon = {
                            Icon(
                                imageVector = vectorResource(Res.drawable.link),
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            showQR(bean.toUniversalLink())
                            showShareSheet = false
                        },
                    )
                    HorizontalDivider()
                    SheetSectionTitle(
                        text = stringResource(Res.string.action_export_clipboard),
                        leadingIcon = {
                            Icon(
                                imageVector = vectorResource(Res.drawable.share),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                    if (entity.haveStandardLink()) {
                        SheetActionRow(
                            text = stringResource(Res.string.standard),
                            leadingIcon = {
                                Icon(
                                    imageVector = vectorResource(Res.drawable.content_copy),
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                scope.launch {
                                    clipboard.setPlainText(entity.toStdLink())
                                    onCopySuccess()
                                }
                                showShareSheet = false
                            },
                        )
                    }
                    SheetActionRow(
                        text = stringResource(Res.string.internal_link),
                        leadingIcon = {
                            Icon(
                                imageVector = vectorResource(Res.drawable.fingerprint),
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            scope.launch {
                                clipboard.setPlainText(bean.toUniversalLink())
                                onCopySuccess()
                            }
                            showShareSheet = false
                        },
                    )
                }
                HorizontalDivider()
                SheetSectionTitle(
                    text = stringResource(Res.string.menu_configuration),
                    leadingIcon = {
                        Icon(
                            imageVector = vectorResource(Res.drawable.settings),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
                SheetActionRow(
                    text = stringResource(Res.string.action_export_clipboard),
                    leadingIcon = {
                        Icon(
                            imageVector = vectorResource(Res.drawable.copy_all),
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        scope.launch {
                            runCatching {
                                clipboard.setPlainText(entity.exportConfig().first)
                            }.onSuccess {
                                onCopySuccess()
                            }.onFailure { e ->
                                showErrorAlert(e.readableMessage)
                            }
                        }
                        showShareSheet = false
                    },
                )
                SheetActionRow(
                    text = stringResource(Res.string.action_export_file),
                    leadingIcon = {
                        Icon(
                            imageVector = vectorResource(Res.drawable.file_export),
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        runCatching {
                            val data = entity.exportConfig()
                            exportToFile(data.second, data.first)
                        }.onFailure { e ->
                            showErrorAlert(e.readableMessage)
                        }
                        showShareSheet = false
                    },
                )

                if (!canNotShareOutbound) {
                    HorizontalDivider()
                    SheetSectionTitle(
                        text = stringResource(Res.string.outbound),
                        leadingIcon = {
                            Icon(
                                imageVector = vectorResource(Res.drawable.arrow_outward),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                    SheetActionRow(
                        text = stringResource(Res.string.action_export_clipboard),
                        leadingIcon = {
                            Icon(
                                imageVector = vectorResource(Res.drawable.copy_all),
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            scope.launch {
                                clipboard.setPlainText(entity.exportOutbound().first)
                                onCopySuccess()
                            }
                            showShareSheet = false
                        },
                    )
                    SheetActionRow(
                        text = stringResource(Res.string.action_export_file),
                        leadingIcon = {
                            Icon(
                                imageVector = vectorResource(Res.drawable.file_export),
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            val data = entity.exportOutbound()
                            exportToFile(data.second, data.first)
                            showShareSheet = false
                        },
                    )
                }
            }
        }
    }

    if (showActions && showSecurityAlert) AlertDialog(
        onDismissRequest = {
            showSecurityAlert = false
            showShareSheet = true
        },
        icon = {
            Icon(vectorResource(Res.drawable.warning), null)
        },
        title = {
            Text(
                stringResource(
                    when (validateResult) {
                        is ValidateResult.Insecure -> Res.string.insecure
                        is ValidateResult.Deprecated -> Res.string.deprecated
                        else -> error("impossible")
                    },
                ),
            )
        },
        text = {
            val textRes = when (validateResult) {
                is ValidateResult.Insecure -> validateResult.textRes
                is ValidateResult.Deprecated -> validateResult.textRes
                else -> error("impossible")
            }
            Text(stringResource(textRes))
        },
        confirmButton = {
            TextButton(stringResource(Res.string.ok)) {
                showSecurityAlert = false
                showShareSheet = true
            }
        },
    )
}

/** Make server address blurred. */
private fun String.blur(): String = when (length) {
    in 0 until 20 -> {
        val halfLength = length / 2
        substring(0, halfLength) + "*".repeat(length - halfLength)
    }

    in 20..30 -> substring(0, 15) + "*".repeat(length - 15)

    else -> substring(0, 15) + "*".repeat(15)
}
