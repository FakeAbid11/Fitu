package com.fitu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fitu.data.local.entity.FoodCacheEntity

@Dao
interface FoodCacheDao {
    @Query("SELECT * FROM food_cache WHERE query = :query LIMIT 1")
    suspend fun getCache(query: String): FoodCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(cache: FoodCacheEntity)

    @Query("DELETE FROM food_cache WHERE timestamp < :beforeTimestamp")
    suspend fun clearOldCache(beforeTimestamp: Long)

    @Query("DELETE FROM food_cache")
    suspend fun clearAllCache()
}
