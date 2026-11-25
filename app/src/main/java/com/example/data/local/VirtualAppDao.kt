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

    @Query("SELECT * FROM virtual_apps WHERE userSpace = :userSpace ORDER BY importedAt DESC")
    fun observeAppsForSpace(userSpace: Int): Flow<List<VirtualAppEntity>>

    @Query("SELECT * FROM virtual_apps WHERE packageName = :packageName AND userSpace = :userSpace LIMIT 1")
    suspend fun getByPackage(packageName: String, userSpace: Int = 0): VirtualAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertApp(app: VirtualAppEntity)

    @Query("UPDATE virtual_apps SET virtualRootRequested = :enabled WHERE packageName = :packageName AND userSpace = :userSpace")
    suspend fun updateVirtualRootRequested(packageName: String, userSpace: Int, enabled: Boolean)

    @Query("UPDATE virtual_apps SET gmsBridgeRequested = :enabled WHERE packageName = :packageName AND userSpace = :userSpace")
    suspend fun updateGmsBridgeRequested(packageName: String, userSpace: Int, enabled: Boolean)

    @Query("DELETE FROM virtual_apps WHERE packageName = :packageName AND userSpace = :userSpace")
    suspend fun deleteByPackage(packageName: String, userSpace: Int = 0)
}
