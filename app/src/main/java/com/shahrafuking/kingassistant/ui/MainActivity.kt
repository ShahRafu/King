*** Begin Patch
*** Update File: app/src/main/java/com/shahrafuking/kingassistant/ui/MainActivity.kt
@@
             AppTheme(darkTheme = darkPref.value) {
                 MaterialTheme {
@@
                     Scaffold(
@@
                     ) { padding ->
                         com.shahrafuking.kingassistant.ui.screens.HomeScreen(
@@
                             onOpenSettings = {
                                 scope.launch { scaffoldState.drawerState.open() }
                             }
                         )
                     }
                 }
             }
+            // schedule weekly backup by default if enabled
+            LaunchedEffect(Unit) {
+                val enabled = getSharedPreferences("king_prefs", Context.MODE_PRIVATE).getBoolean("backup_enabled", true)
+                if (enabled) {
+                    com.shahrafuking.kingassistant.backup.BackupScheduler.scheduleWeeklyBackup(this@MainActivity)
+                } else {
+                    com.shahrafuking.kingassistant.backup.BackupScheduler.cancelWeeklyBackup(this@MainActivity)
+                }
+            }
         }
     }
 }
*** End Patch
