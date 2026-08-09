package com.shahrafuking.kingassistant.memory

import android.content.Context
import androidx.room.*
import android.util.Base64
import java.nio.ByteBuffer
import java.nio.ByteOrder

@Entity(tableName = "memory_entries")
data class MemoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val embeddingBase64: String? = null, // FloatArray stored as Base64 bytes
    val metadataJson: String? = null
)

@Dao
interface MemoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: MemoryEntry): Long

    @Query("SELECT * FROM memory_entries WHERE id = :id")
    suspend fun getById(id: Long): MemoryEntry?

    @Query("SELECT * FROM memory_entries ORDER BY timestamp DESC")
    suspend fun getAll(): List<MemoryEntry>

    @Query("DELETE FROM memory_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM memory_entries")
    suspend fun clearAll()
}

@Database(entities = [MemoryEntry::class], version = 1, exportSchema = false)
abstract class MemoryDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao

    companion object {
        @Volatile
        private var INSTANCE: MemoryDatabase? = null

        fun getInstance(context: Context): MemoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MemoryDatabase::class.java,
                    "king_memory.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }

        // Helper converters (used by repository when storing/loading embeddings)
        fun floatArrayToBase64(arr: FloatArray): String {
            val byteBuffer = ByteBuffer.allocate(arr.size * 4).order(ByteOrder.LITTLE_ENDIAN)
            arr.forEach { byteBuffer.putFloat(it) }
            return Base64.encodeToString(byteBuffer.array(), Base64.NO_WRAP)
        }

        fun base64ToFloatArray(s: String?): FloatArray? {
            if (s == null) return null
            val bytes = Base64.decode(s, Base64.NO_WRAP)
            val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val fa = FloatArray(bytes.size / 4)
            for (i in fa.indices) fa[i] = bb.getFloat()
            return fa
        }
    }
}
