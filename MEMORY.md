# Permanent Adaptive Memory (Memory module)

This module provides a simple, local "memory" implementation backed by Room and a tiny
in-memory vector index for nearest-neighbor (k-NN) searches by embedding.

Files added
- app/src/main/java/com/shahrafuking/kingassistant/memory/MemoryDatabase.kt
- app/src/main/java/com/shahrafuking/kingassistant/memory/MemoryRepository.kt
- app/src/main/java/com/shahrafuking/kingassistant/memory/SimpleVectorIndex.kt

Design notes
- Embeddings are stored in the Room table as Base64-encoded float arrays (embeddingBase64).
- MemoryRepository exposes convenience methods to add, list, delete and search memories.
- SimpleVectorIndex is a tiny in-memory cosine-similarity index. It is intended for small
  datasets and quick experimentation.

Security & future improvements
- The current Room database is not encrypted. For sensitive personal data consider:
  - using SQLCipher for Android (encrypted Room) or
  - encrypting embeddings/text before storage using Android Keystore.
- For large-scale similarity search replace SimpleVectorIndex with an ANN library or
  an external vector database (FAISS, HNSWlib, Milvus, Pinecone, etc.).

Usage example (Kotlin coroutine)

    val db = MemoryDatabase.getInstance(context)
    val repo = MemoryRepository(db.memoryDao())

    // add a memory (embedding computed elsewhere)
    val id = repo.addMemory("Met with Alice about project", embedding = floatArrayOf(0.1f, 0.2f, ...))

    // search
    val results = repo.searchSimilar(queryEmbedding, topK = 5)

Notes
- Embedding generation is out-of-scope for this module — compute embeddings using your
  preferred model or service and pass the FloatArray into addMemory / searchSimilar.

