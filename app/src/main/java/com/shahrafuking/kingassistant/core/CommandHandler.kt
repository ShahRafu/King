*** Begin Patch
*** Update File: app/src/main/java/com/shahrafuking/kingassistant/core/CommandHandler.kt
@@
-                // try to extract budget (simple number extraction)
-                // supports formats like "20", "২০"(Bengali digits not fully supported here), "20.5", and optional words 'ডলার' or '$' or 'usd'
-                val numRegex = Regex("(\\d+[\\.,]?\\d*)")
-                val m = numRegex.find(lowered)
-                var budget: Double? = m?.value?.replace(',', '.')?.toDoubleOrNull()
+                // try to extract budget (robust parser supporting Bengali digits, English digits, or spelled-out words)
+                var budget: Double? = com.shahrafuking.kingassistant.util.NumberParser.parseNumber(text)
*** End Patch
