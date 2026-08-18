*** Begin Patch
*** Update File: app/src/main/java/com/shahrafuking/kingassistant/ui/screens/SettingsDrawer.kt
@@
     Column(modifier = Modifier.padding(16.dp)) {
         Text("Settings", style = MaterialTheme.typography.h6)
         Spacer(modifier = Modifier.height(6.dp))
+
+        // Backup remote configuration
+        val ctx = LocalContext.current
+        val prefs = ctx.getSharedPreferences("king_prefs", android.content.Context.MODE_PRIVATE)
+        var remoteOwner by remember { mutableStateOf(prefs.getString("backup_remote_owner", "ShahRafu") ?: "ShahRafu") }
+        var remoteRepo by remember { mutableStateOf(prefs.getString("backup_remote_repo", "") ?: "") }
+        var remotePath by remember { mutableStateOf(prefs.getString("backup_remote_path", "backups") ?: "backups") }
+
+        OutlinedTextField(
+            value = remoteOwner,
+            onValueChange = { remoteOwner = it; prefs.edit().putString("backup_remote_owner", it).apply() },
+            label = { Text("Backup GitHub Owner") },
+            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
+        )
+        OutlinedTextField(
+            value = remoteRepo,
+            onValueChange = { remoteRepo = it; prefs.edit().putString("backup_remote_repo", it).apply() },
+            label = { Text("Backup GitHub Repo (name)") },
+            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
+        )
+        OutlinedTextField(
+            value = remotePath,
+            onValueChange = { remotePath = it; prefs.edit().putString("backup_remote_path", it).apply() },
+            label = { Text("Backup Path in Repo (e.g. backups)") },
+            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
+        )
+        Text("Note: store a GitHub personal access token in Settings → Manage API Keys under key 'GITHUB_BACKUP_TOKEN'. The app will upload weekly backups to the specified private repo.")
*** End Patch
