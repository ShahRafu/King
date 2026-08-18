@@
     Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
         Text("Trade Notepad", style = MaterialTheme.typography.h6)
         Spacer(modifier = Modifier.height(8.dp))
         // existing UI omitted for brevity (assumes earlier version exists)
+        Spacer(modifier = Modifier.height(8.dp))
+        // Import notes from a JSON file
+        val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
+            if (uri != null) {
+                try {
+                    val content = ctx.contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() } ?: "[]"
+                    // validate JSON
+                    val arr = JSONArray(content)
+                    prefs.edit().putString("trade_notepad", arr.toString()).apply()
+                    // update local variable
+                    notes = arr.toString()
+                } catch (e: Throwable) {
+                    // ignore invalid import
+                }
+            }
+        }
+        Button(onClick = { importLauncher.launch("application/json") }) { Text("Import from JSON") }
+        Spacer(modifier = Modifier.height(8.dp))
*** End Patch
