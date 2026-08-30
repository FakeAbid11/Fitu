package com.fitu.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fitu.ui.theme.OrangePrimary

@Composable
fun FituSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier.padding(bottom = 100.dp),
        snackbar = { data ->
            Snackbar(
                shape = RoundedCornerShape(16.dp),
                containerColor = Color(0xFF2A2A2F),
                contentColor = Color.White,
                action = {
                    data.visuals.actionLabel?.let { actionLabel ->
                        TextButton(onClick = { data.dismiss() }) {
                            Text(actionLabel, color = OrangePrimary)
                        }
                    }
                }
            ) {
                Text(data.visuals.message)
            }
        }
    )
}
