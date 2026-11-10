package dev.reelbox.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import dev.reelbox.BuildConfig
import dev.reelbox.data.UpdateChecker
import dev.reelbox.data.UpdateInfo
import dev.reelbox.ui.theme.Accent
import dev.reelbox.ui.theme.InkElevated
import dev.reelbox.ui.theme.TextPrimary
import dev.reelbox.ui.theme.TextSecondary

/**
 * Checks the website for a newer release once per launch and offers it.
 * "Later" hides the prompt until the app is next started.
 */
@Composable
fun UpdatePrompt() {
    val context = LocalContext.current
    var update by remember { mutableStateOf<UpdateInfo?>(null) }
    var dismissed by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!dismissed) update = UpdateChecker.check()
    }

    val info = update ?: return
    if (dismissed) return

    AlertDialog(
        onDismissRequest = { dismissed = true },
        title = { Text("Update available", color = TextPrimary) },
        text = {
            val notes = info.notes.trim()
            Text(
                buildString {
                    append("Reelbox ${info.versionName} is ready. You have ${BuildConfig.VERSION_NAME}.")
                    if (notes.isNotEmpty()) append("\n\n$notes")
                    append("\n\nYour browser will download the update. Open it to install; your watchlist is kept.")
                },
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    dismissed = true
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(info.apkUrl))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                },
            ) { Text("Update", color = Accent) }
        },
        dismissButton = {
            TextButton(onClick = { dismissed = true }) { Text("Later", color = TextSecondary) }
        },
        containerColor = InkElevated,
        textContentColor = TextSecondary,
    )
}
