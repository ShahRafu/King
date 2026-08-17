package com.shahrafuking.kingassistant.embedding

interface EmbedderAdapter {
    fun embed(text: String): FloatArray
}

class ProductionEmbedderAdapter : EmbedderAdapter {
    override fun embed(text: String): FloatArray {
        // TODO: Replace with real embedder invocation
        return FloatArray(0)
    }
}
