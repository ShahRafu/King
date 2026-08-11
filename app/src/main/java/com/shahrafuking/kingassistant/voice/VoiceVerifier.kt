*** Begin Patch
*** Update File: app/src/main/java/com/shahrafuking/kingassistant/voice/VoiceVerifier.kt
@@
-                val floatEmb = doubleArrayToFloatArray(vector)
+                val floatEmb = doubleArrayToFloatArray(vector)
@@
-            val floatEmb = doubleArrayToFloatArray(sampleVector)
+            val floatEmb = doubleArrayToFloatArray(sampleVector)
@@
-    private fun doubleArrayToFloatArray(d: DoubleArray): FloatArray {
-        val f = FloatArray(d.size)
-        for (i in d.indices) f[i] = d[i].toFloat()
-        return f
-    }
+    // public helper used across the module
+    fun doubleArrayToFloatArray(d: DoubleArray): FloatArray {
+        val f = FloatArray(d.size)
+        for (i in d.indices) f[i] = d[i].toFloat()
+        return f
+    }
*** End Patch
