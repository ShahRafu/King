package com.shahrafuking.kingassistant.memory

import androidx.room.*

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val text: String,
    val embeddingCsv: String?, // nullable comma-separated embedding
    val createdAt: Long
)

@Dao
interface MemoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MemoryEntity)

    @Query("SELECT * FROM memories WHERE id = :id")
    suspend fun getById(id: String): MemoryEntity?

    @Query("SELECT * FROM memories ORDER BY createdAt DESC LIMIT :limit")
    suspend fun listRecent(limit: Int): List<MemoryEntity>

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteById(id: String)
}
