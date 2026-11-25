package org.waxmoon.ui.theme

import androidx.compose.runtime.Composable
import com.example.ui.theme.AtpVmTheme

@Composable
fun GithubMultiAppTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    AtpVmTheme(darkTheme = darkTheme, content = content)
}