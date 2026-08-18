package com.shahrafuking.kingassistant.memory

import android.content.Context
import androidx.room.*
import com.shahrafuking.kingassistant.security.KeystoreHelper
import net.sqlcipher.database.SupportFactory
import net.sqlcipher.database.SQLiteDatabase
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import android.util.Base64

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
    fun insert(entry: MemoryEntry): Long

    @Query("SELECT * FROM memory_entries WHERE id = :id")
    fun getById(id: Long): MemoryEntry?

    @Query("SELECT * FROM memory_entries ORDER BY timestamp DESC")
    fun getAll(): List<MemoryEntry>

    @Query("DELETE FROM memory_entries WHERE id = :id")
    fun deleteById(id: Long): Int

    @Query("DELETE FROM memory_entries")
    fun clearAll(): Int
}

@Database(entities = [MemoryEntry::class], version = 1, exportSchema = false)
abstract class MemoryDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao

    companion object {
        @Volatile
        private var INSTANCE: MemoryDatabase? = null

        fun getInstance(context: Context): MemoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = buildDatabase(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }

        private fun buildDatabase(appContext: Context): MemoryDatabase {
            try {
                // Ensure SQLCipher native libs are loaded
                SQLiteDatabase.loadLibs(appContext)

                // Retrieve or create a DB passphrase stored securely via KeystoreHelper
                val keyName = "db_passphrase_v1"
                var passphrase = KeystoreHelper.decryptString(appContext, keyName)
                if (passphrase.isNullOrBlank()) {
                    // generate a random 32-byte base64 string
                    val rnd = java.security.SecureRandom()
                    val bytes = ByteArray(32)
                    rnd.nextBytes(bytes)
                    passphrase = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    KeystoreHelper.encryptString(appContext, passphrase, keyName)
                }

                val pass = passphrase!!.toByteArray(StandardCharsets.UTF_8)
                val factory = SupportFactory(pass)

                return Room.databaseBuilder(appContext, MemoryDatabase::class.java, "king_memory.db")
                    .openHelperFactory(factory)
                    .build()
            } catch (t: Throwable) {
                // If any error occurs (e.g., SQLCipher not available), fall back to plain Room
                android.util.Log.w("MemoryDatabase", "encrypted db init failed, falling back to plain Room", t)
                return Room.databaseBuilder(appContext, MemoryDatabase::class.java, "king_memory.db").build()
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
