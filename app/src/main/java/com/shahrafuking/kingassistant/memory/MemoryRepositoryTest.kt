package com.shahrafuking.kingassistant.memory

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import androidx.room.Room
import android.content.Context

class MemoryRepositoryTest {
    private lateinit var db: MemoryDatabase
    private lateinit var repo: MemoryRepository

    @Before
    fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, MemoryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = MemoryRepository(db.memoryDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testAddAndSearch() = runBlocking {
        val emb1 = floatArrayOf(0.1f, 0.2f, 0.3f)
        val emb2 = floatArrayOf(0.9f, 0.1f, 0.2f)

        val id1 = repo.addMemory("first", emb1, mapOf("tag" to "t1"))
        val id2 = repo.addMemory("second", emb2, mapOf("tag" to "t2"))

        val results = repo.searchSimilar(floatArrayOf(0.1f, 0.2f, 0.3f), topK = 2)
        assertTrue(results.isNotEmpty())
        // best match should be the first
        val (entry, score) = results[0]
        assertEquals("first", entry.text)
        assertTrue(score > 0.9f)
    }
}
