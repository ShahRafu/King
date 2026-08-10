*** Begin Patch
*** Update File: app/src/main/java/com/shahrafuking/kingassistant/ui/screens/SettingsDrawer.kt
@@
 import com.shahrafuking.kingassistant.core.BudgetManager
+import com.shahrafuking.kingassistant.memory.MemoryDatabase
+import com.shahrafuking.kingassistant.memory.MemoryRepository
+import androidx.compose.foundation.lazy.LazyColumn
+import androidx.compose.foundation.lazy.items
+import androidx.compose.runtime.LaunchedEffect
+import androidx.compose.runtime.rememberCoroutineScope
+import kotlinx.coroutines.launch
+import java.text.SimpleDateFormat
+import java.util.Date
+import java.util.Locale
@@
         Divider(modifier = Modifier.padding(vertical = 8.dp))
 
-        // Panic controls
-        PanicRow()
+        // Budget history (recent simulated trades / attempts)
+        BudgetHistory()
+
+        Divider(modifier = Modifier.padding(vertical = 8.dp))
+
+        // Panic controls
+        PanicRow()
@@
 }
+
+@Composable
+fun BudgetHistory() {
+    val ctx = LocalContext.current
+    val repo = MemoryRepository(MemoryDatabase.getInstance(ctx).memoryDao())
+    val scope = rememberCoroutineScope()
+    var entries by remember { mutableStateOf<List<com.shahrafuking.kingassistant.memory.MemoryEntry>>(emptyList()) }
+
+    LaunchedEffect(Unit) {
+        scope.launch {
+            try {
+                // look for simulated_trade and trade_attempt types
+                val recent = repo.getRecentByTypes(listOf("simulated_trade", "trade_attempt"), 20)
+                entries = recent
+            } catch (_: Throwable) { }
+        }
+    }
+
+    Column(modifier = Modifier.fillMaxWidth()) {
+        Text("Budget / Trade History", style = MaterialTheme.typography.subtitle1)
+        Spacer(modifier = Modifier.padding(4.dp))
+        if (entries.isEmpty()) {
+            Text("No recent simulated trades recorded.")
+        } else {
+            LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
+                items(entries) { e ->
+                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
+                    val ts = try { sdf.format(Date(e.timestamp)) } catch (_: Throwable) { e.timestamp.toString() }
+                    Column(modifier = Modifier.fillMaxWidth().padding(6.dp)) {
+                        Text(text = "${ts} — ${e.text}")
+                        e.metadataJson?.let { md -> Text(text = md, style = MaterialTheme.typography.caption) }
+                        Divider(modifier = Modifier.padding(vertical = 4.dp))
+                    }
+                }
+            }
+        }
+    }
+}
*** End Patch
