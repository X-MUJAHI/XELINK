package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY lastTimestamp DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE peerId = :peerId LIMIT 1")
    suspend fun getConversation(peerId: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConversation(conversation: ConversationEntity)

    @Query("UPDATE conversations SET isOnline = :isOnline WHERE peerId = :peerId")
    suspend fun updateOnlineStatus(peerId: String, isOnline: Boolean)

    @Query("UPDATE conversations SET isOnline = 0")
    suspend fun markAllOffline()

    @Query("UPDATE conversations SET unreadCount = 0 WHERE peerId = :peerId")
    suspend fun resetUnreadCount(peerId: String)

    @Query("DELETE FROM conversations WHERE peerId = :peerId")
    suspend fun deleteConversation(peerId: String)
}
