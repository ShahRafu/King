*** Begin Patch
*** Update File: app/src/main/java/com/shahrafuking/kingassistant/ui/screens/SettingsDrawer.kt
@@
-    var showSecrets by remember { mutableStateOf(false) }
-    var showPermissions by remember { mutableStateOf(false) }
-    var showPlugins by remember { mutableStateOf(false) }
-    var showLogoPicker by remember { mutableStateOf(false) }
+    var showSecrets by remember { mutableStateOf(false) }
+    var showPermissions by remember { mutableStateOf(false) }
+    var showPlugins by remember { mutableStateOf(false) }
+    var showLogoPicker by remember { mutableStateOf(false) }
+    var showMediaBackup by remember { mutableStateOf(false) }
+    var showTradeNotepad by remember { mutableStateOf(false) }
+    var showVoiceCal by remember { mutableStateOf(false) }
+    var showCloudRecovery by remember { mutableStateOf(false) }
+    var showPluginUpdate by remember { mutableStateOf(false) }
@@
-        OutlinedButton(onClick = { showPlugins = true }) {
+        OutlinedButton(onClick = { showPlugins = true }) {
             Text("Manage Plugins")
         }
@@
-        OutlinedButton(onClick = { showLogoPicker = true }) {
-            Text("App Logo & Theme")
-        }
+        OutlinedButton(onClick = { showLogoPicker = true }) {
+            Text("App Logo & Theme")
+        }
+        Spacer(modifier = Modifier.height(8.dp))
+        OutlinedButton(onClick = { showMediaBackup = true }) {
+            Text("Media Backup")
+        }
+        Spacer(modifier = Modifier.height(8.dp))
+        OutlinedButton(onClick = { showTradeNotepad = true }) {
+            Text("Trade Notepad")
+        }
+        Spacer(modifier = Modifier.height(8.dp))
+        OutlinedButton(onClick = { showVoiceCal = true }) {
+            Text("Voice Calibration")
+        }
+        Spacer(modifier = Modifier.height(8.dp))
+        OutlinedButton(onClick = { showCloudRecovery = true }) {
+            Text("Data Recovery")
+        }
+        Spacer(modifier = Modifier.height(8.dp))
+        OutlinedButton(onClick = { showPluginUpdate = true }) {
+            Text("Check Plugin Updates")
+        }
@@
-    if (showSecrets) {
+    if (showSecrets) {
         AlertDialog(
@@
     }
@@
-    if (showPermissions) {
+    if (showPermissions) {
         AlertDialog(
@@
     }
+
+    if (showMediaBackup) {
+        AlertDialog(
+            onDismissRequest = { showMediaBackup = false },
+            title = { Text("Media Backup") },
+            text = {
+                Column { MediaBackupScreen(LocalContext.current, onClose = { showMediaBackup = false }) }
+            },
+            confirmButton = {
+                Button(onClick = { showMediaBackup = false }) { Text("Done") }
+            }
+        )
+    }
+
+    if (showTradeNotepad) {
+        AlertDialog(
+            onDismissRequest = { showTradeNotepad = false },
+            title = { Text("Trade Notepad") },
+            text = {
+                Column { TradeNotepadScreen(LocalContext.current, onClose = { showTradeNotepad = false }) }
+            },
+            confirmButton = {
+                Button(onClick = { showTradeNotepad = false }) { Text("Done") }
+            }
+        )
+    }
+
+    if (showVoiceCal) {
+        AlertDialog(
+            onDismissRequest = { showVoiceCal = false },
+            title = { Text("Voice Calibration") },
+            text = {
+                Column { VoiceCalibrationScreen(LocalContext.current, onClose = { showVoiceCal = false }) }
+            },
+            confirmButton = {
+                Button(onClick = { showVoiceCal = false }) { Text("Done") }
+            }
+        )
+    }
+
+    if (showCloudRecovery) {
+        AlertDialog(
+            onDismissRequest = { showCloudRecovery = false },
+            title = { Text("Data Recovery & Sync") },
+            text = {
+                Column { CloudRecoveryScreen(onClose = { showCloudRecovery = false }) }
+            },
+            confirmButton = {
+                Button(onClick = { showCloudRecovery = false }) { Text("Done") }
+            }
+        )
+    }
+
+    if (showPluginUpdate) {
+        AlertDialog(
+            onDismissRequest = { showPluginUpdate = false },
+            title = { Text("Plugin Update Checker") },
+            text = {
+                Column {
+                    // minimal: call plugin manager and show list; update check can be kicked from here
+                    val pm = com.shahrafuking.kingassistant.plugin.PluginManager(LocalContext.current)
+                    val list = pm.listPlugins()
+                    if (list.isEmpty()) {
+                        Text("No plugins installed.")
+                    } else {
+                        for (p in list) {
+                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
+                                Text(p.id, modifier = Modifier.weight(1f))
+                                Text(if (p.enabled) "ENABLED" else "DISABLED")
+                            }
+                        }
+                    }
+                    Spacer(modifier = Modifier.height(8.dp))
+                    Text("Use PluginUpdateChecker in your backend to verify available updates.")
+                }
+            },
+            confirmButton = {
+                Button(onClick = { showPluginUpdate = false }) { Text("Close") }
+            }
+        )
+    }
*** End Patch
