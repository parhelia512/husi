package fr.husi.ui.tools

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
expect fun IconsScreen(
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp,
    onVisibleChange: (Boolean) -> Unit,
    showSnackbar: (message: String) -> Unit,
)
