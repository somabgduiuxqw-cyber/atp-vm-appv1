package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VirtualAppDao {
    @Query("SELECT * FROM virtual_apps ORDER BY importedAt DESC")
    fun observeAllApps(): Flow<List<VirtualAppEntity>>

    @Query("SELECT * FROM virtual_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getByPackage(packageName: String): VirtualAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertApp(app: VirtualAppEntity)

    @Query("UPDATE virtual_apps SET virtualRootRequested = :enabled WHERE packageName = :packageName")
    suspend fun updateVirtualRootRequested(packageName: String, enabled: Boolean)

    @Query("UPDATE virtual_apps SET gmsBridgeRequested = :enabled WHERE packageName = :packageName")
    suspend fun updateGmsBridgeRequested(packageName: String, enabled: Boolean)

    @Query("DELETE FROM virtual_apps WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)
}
