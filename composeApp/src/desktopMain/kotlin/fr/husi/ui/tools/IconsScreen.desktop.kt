package fr.husi.ui.tools

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.husi.compose.material3.Text
import fr.husi.resources.Res
import fr.husi.resources.custom_icons_android_only
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun IconsScreen(
    modifier: Modifier,
    bottomPadding: Dp,
    onVisibleChange: (Boolean) -> Unit,
    showSnackbar: (message: String) -> Unit,
) {
    onVisibleChange(false)
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(Res.string.custom_icons_android_only))
    }
}
