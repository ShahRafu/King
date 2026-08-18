*** Begin Patch
*** Update File: app/src/main/java/com/shahrafuking/kingassistant/ui/screens/SettingsDrawer.kt
@@
-        Spacer(modifier = Modifier.height(12.dp))
-        Row {
-            Button(onClick = {
-                BatteryHelper.requestIgnoreBatteryOptimization(ctx as android.app.Activity)
-            }) {
-                Text("Request Battery Exemption")
-            }
-            Spacer(modifier = Modifier.width(8.dp))
-            Button(onClick = { onClose() }) {
-                Text("Close")
-            }
-        }
+        Spacer(modifier = Modifier.height(12.dp))
+        Row {
+            Button(onClick = {
+                BatteryHelper.requestIgnoreBatteryOptimization(ctx as android.app.Activity)
+            }) {
+                Text("Request Battery Exemption")
+            }
+            Spacer(modifier = Modifier.width(8.dp))
+            Button(onClick = { com.shahrafuking.kingassistant.backup.BackupTrigger.runImmediateBackup(ctx) }) {
+                Text("Run Backup Now")
+            }
+            Spacer(modifier = Modifier.width(8.dp))
+            Button(onClick = { onClose() }) {
+                Text("Close")
+            }
+        }
*** End Patch
