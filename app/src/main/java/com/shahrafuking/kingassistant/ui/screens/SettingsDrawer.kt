*** Begin Patch
*** Update File: app/src/main/java/com/shahrafuking/kingassistant/ui/screens/SettingsDrawer.kt
@@
-        OutlinedButton(onClick = { showPlugins = true }) {
-            Text("Manage Plugins")
-        }
-        Spacer(modifier = Modifier.height(8.dp))
-        OutlinedButton(onClick = { showSecrets = true }) {
-            Text("Manage API Keys")
-        }
-        Spacer(modifier = Modifier.height(8.dp))
-        OutlinedButton(onClick = { showPermissions = true }) {
-            Text("Permission Check")
-        }
-        Spacer(modifier = Modifier.height(8.dp))
-        OutlinedButton(onClick = { showLogoPicker = true }) {
-            Text("App Logo & Theme")
-        }
+        OutlinedButton(onClick = { showPlugins = true }) {
+            Text("Manage Plugins")
+        }
+        Spacer(modifier = Modifier.height(8.dp))
+        OutlinedButton(onClick = { showSecrets = true }) {
+            Text("Manage API Keys")
+        }
+        Spacer(modifier = Modifier.height(8.dp))
+        OutlinedButton(onClick = { showPermissions = true }) {
+            Text("Permission Check")
+        }
+        Spacer(modifier = Modifier.height(8.dp))
+        OutlinedButton(onClick = { showLogoPicker = true }) {
+            Text("App Logo & Theme")
+        }
+        Spacer(modifier = Modifier.height(8.dp))
+        OutlinedButton(onClick = { /* open media backup dialog */ }) {
+            Text("Media Backup")
+        }
+        Spacer(modifier = Modifier.height(8.dp))
+        OutlinedButton(onClick = { /* open trade notepad */ }) {
+            Text("Trade Notepad")
+        }
+        Spacer(modifier = Modifier.height(8.dp))
+        OutlinedButton(onClick = { /* open voice calibration */ }) {
+            Text("Voice Calibration")
+        }
+        Spacer(modifier = Modifier.height(8.dp))
+        OutlinedButton(onClick = { /* cloud recovery */ }) {
+            Text("Data Recovery")
+        }
*** End Patch
