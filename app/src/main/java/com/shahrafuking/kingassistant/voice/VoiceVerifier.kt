companion object {
    // Centralized default threshold for verification (tune on real device)
    const val DEFAULT_THRESHOLD = 0.78
}

fun saveTemplate(vector: DoubleArray): Boolean {
    try {
        val obj = JSONObject()
        val arr = vector.joinToString(separator = ",")
        obj.put("v", arr)
        obj.put("t", System.currentTimeMillis())
        return KeystoreHelper.encryptString(context, obj.toString(), storageKey)
    } catch (t: Throwable) {
        Log.w(TAG, "saveTemplate error", t)
        return false
    }
}

fun loadTemplate(): DoubleArray? {
    try {
        val json = KeystoreHelper.decryptString(context, storageKey) ?: return null
        val obj = JSONObject(json)
        val arrStr = obj.optString("v", "")
        if (arrStr.isBlank()) return null
        val parts = arrStr.split(",")
        val vec = DoubleArray(parts.size)
        for (i in parts.indices) vec[i] = parts[i].toDoubleOrNull() ?: 0.0
        return vec
    } catch (t: Throwable) {
        Log.w(TAG, "loadTemplate error", t)
        return null
    }
}

fun clearTemplate() { KeystoreHelper.clear(context, storageKey) }

fun verify(sampleVector: DoubleArray, threshold: Double = DEFAULT_THRESHOLD): Boolean {
    val template = loadTemplate() ?: return false
    val sim = cosineSimilarity(template, sampleVector)
    Log.i(TAG, "verify similarity=$sim threshold=$threshold")
    return sim >= threshold
}

private fun cosineSimilarity(a: DoubleArray, b: DoubleArray): Double {
    if (a.size != b.size) {
        val n = maxOf(a.size, b.size)
        val aa = DoubleArray(n); val bb = DoubleArray(n)
        for (i in 0 until n) {
            aa[i] = if (i < a.size) a[i] else 0.0
            bb[i] = if (i < b.size) b[i] else 0.0
        }
        return cosineSimilarity(aa, bb)
    }
    var dot = 0.0
    var na = 0.0
    var nb = 0.0
    for (i in a.indices) {
        dot += a[i] * b[i]
        na += a[i] * a[i]
        nb += b[i] * b[i]
    }
    if (na == 0.0 || nb == 0.0) return 0.0
    return dot / (sqrt(na) * sqrt(nb))
}
