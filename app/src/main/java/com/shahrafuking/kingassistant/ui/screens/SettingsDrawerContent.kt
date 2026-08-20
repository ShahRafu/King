*** Begin Patch
*** Update File: app/src/main/java/com/shahrafuking/kingassistant/ui/screens/SettingsDrawerContent.kt
@@
 import kotlinx.coroutines.launch
+import androidx.compose.foundation.text.KeyboardOptions
+import androidx.compose.ui.text.input.ImeAction
+import com.shahrafuking.kingassistant.settings.SecurePrefs
@@
     item {
             Spacer(modifier = Modifier.height(8.dp))
             Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                 Button(onClick = { Toast.makeText(ctx, "Settings saved", Toast.LENGTH_SHORT).show() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                     Text("Save & Close")
                 }
                 Spacer(modifier = Modifier.height(24.dp))
             }
         }
+        // Backend proxy settings
+        item {
+            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
+                Text("Backend Proxy", style = MaterialTheme.typography.subtitle1)
+                Spacer(modifier = Modifier.height(8.dp))
+                val backendUrl by settingsRepo.backendUrlFlow.collectAsState(initial = "")
+                val secure = remember { SecurePrefs(ctx) }
+                val backendTokenFlow = secure.tokenFlow()
+                val backendToken by backendTokenFlow.collectAsState(initial = "")
+
+                OutlinedTextField(
+                    value = backendUrl,
+                    onValueChange = { v -> scope.launch { settingsRepo.setBackendUrl(v) } },
+                    label = { Text("Backend URL") },
+                    placeholder = { Text("http://192.168.0.5:8080") },
+                    modifier = Modifier.fillMaxWidth(),
+                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
+                )
+                Spacer(modifier = Modifier.height(8.dp))
+                OutlinedTextField(
+                    value = backendToken,
+                    onValueChange = { v -> scope.launch { secure.setToken(v) } },
+                    label = { Text("Backend Token") },
+                    placeholder = { Text("set secure token") },
+                    modifier = Modifier.fillMaxWidth(),
+                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
+                )
+                Spacer(modifier = Modifier.height(12.dp))
+            }
+        }
*** End Patch
