package com.shahrafuking.kingassistant.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Updated KingHomeScreen per UI refinements:
 * - Removed top black header/status area
 * - Replaced 3-dot menu with Settings icon (top-right)
 * - Disabled swipe-to-open for the drawer (open only via settings icon click)
 * - Redesigned bottom input box to a clean GitHub-like search bar (removed literal "Search" placeholder)
 */

@Composable
fun KingHomeScreen(onSettingsSelected: (() -> Unit)? = null) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val drawerState = rememberDrawerState(DrawerValue.Closed)

    // Settings repository & current mode (kept for compatibility)
    val settingsRepo = remember { SettingsRepository(ctx) }
    val currentMode by settingsRepo.raghuPreviewModeFlow.collectAsState(initial = RaghuPreviewMode.EXTERNAL)

    ModalDrawer(
        drawerState = drawerState,
        drawerContent = {
            SettingsDrawerContent(currentMode = currentMode, onModeSelected = { mode ->
                scope.launch {
                    settingsRepo.setRaghuPreviewMode(mode)
                    drawerState.close()
                }
            })
        },
        gesturesEnabled = false // disable edge swipe-to-open; only Settings icon opens it
    ) {
        // No TopAppBar; place a settings icon fixed at the top-right inside the content
        Box(modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)) {

            IconButton(
                onClick = {
                    onSettingsSelected?.invoke()
                    scope.launch { drawerState.open() }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Open settings",
                    tint = MaterialTheme.colors.onBackground
                )
            }

            // Main screen content
            Column(modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
                .padding(top = 56.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Spacer(modifier = Modifier.height(12.dp))

                // small example score card (replace with real content)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    backgroundColor = Color(0xFF1F8A70),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(modifier = Modifier
                        .padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Budget: --", color = Color.White, modifier = Modifier.weight(1f))
                        Text(text = "Prob: --%", color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Bottom input styled like GitHub search bar
                ChatSearchBar(
                    onSend = { msg ->
                        Toast.makeText(ctx, "Send: $msg", Toast.LENGTH_SHORT).show()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ChatSearchBar(onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = null, // removed literal "Search" placeholder per spec
            singleLine = true,
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color.Gray)
            },
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                backgroundColor = MaterialTheme.colors.surface,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = MaterialTheme.colors.onSurface
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(onClick = { onSend(text); text = "" }) {
            Icon(imageVector = Icons.Default.Send, contentDescription = "Send")
        }
    }
}
