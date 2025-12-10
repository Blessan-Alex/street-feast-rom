package com.streatfeast.app.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuDao {
    
    // Categories - return all categories regardless of isActive status
    @Query("SELECT * FROM categories WHERE storeId = :storeId ORDER BY name")
    fun getAllCategories(storeId: String): Flow<List<CategoryEntity>>
    
    @Query("SELECT * FROM categories WHERE storeId = :storeId ORDER BY name")
    suspend fun getAllCategoriesSync(storeId: String): List<CategoryEntity>
    
    // Items - return all items regardless of isActive status
    @Query("SELECT * FROM items WHERE storeId = :storeId ORDER BY name")
    fun getAllItems(storeId: String): Flow<List<ItemEntity>>
    
    @Query("SELECT * FROM items WHERE storeId = :storeId ORDER BY name")
    suspend fun getAllItemsSync(storeId: String): List<ItemEntity>
    
    @Query("SELECT * FROM items WHERE categoryId = :categoryId ORDER BY name")
    fun getItemsByCategory(categoryId: String): Flow<List<ItemEntity>>
    
    @Query("SELECT * FROM items WHERE categoryId = :categoryId ORDER BY name")
    suspend fun getItemsByCategorySync(categoryId: String): List<ItemEntity>
    
    // Frequent items
    @Query("SELECT * FROM frequent_items WHERE storeId = :storeId ORDER BY orderIndex")
    fun getFrequentItems(storeId: String): Flow<List<FrequentItemEntity>>
    
    @Query("SELECT * FROM frequent_items WHERE storeId = :storeId ORDER BY orderIndex")
    suspend fun getFrequentItemsSync(storeId: String): List<FrequentItemEntity>
    
    // Upsert operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategories(categories: List<CategoryEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategory(category: CategoryEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItems(items: List<ItemEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItem(item: ItemEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFrequentItems(items: List<FrequentItemEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFrequentItem(item: FrequentItemEntity)
    
    // Clear operations
    @Query("DELETE FROM categories WHERE storeId = :storeId")
    suspend fun clearCategories(storeId: String)
    
    @Query("DELETE FROM items WHERE storeId = :storeId")
    suspend fun clearItems(storeId: String)
    
    @Query("DELETE FROM frequent_items WHERE storeId = :storeId")
    suspend fun clearFrequentItems(storeId: String)
    
    // Combined clear
    @Query("DELETE FROM categories WHERE storeId = :storeId")
    suspend fun clearCategoriesForStore(storeId: String)
    
    @Query("DELETE FROM items WHERE storeId = :storeId")
    suspend fun clearItemsForStore(storeId: String)
    
    @Query("DELETE FROM frequent_items WHERE storeId = :storeId")
    suspend fun clearFrequentItemsForStore(storeId: String)
    
    // Targeted deletes
    @Query("DELETE FROM categories WHERE id = :id AND storeId = :storeId")
    suspend fun deleteCategory(id: String, storeId: String)
    
    @Query("DELETE FROM items WHERE id = :id AND storeId = :storeId")
    suspend fun deleteItem(id: String, storeId: String)
    
    @Query("DELETE FROM frequent_items WHERE itemId = :itemId AND storeId = :storeId")
    suspend fun deleteFrequentItem(itemId: String, storeId: String)
    
    // Freshness helpers
    @Query("SELECT MAX(updatedAt) FROM items WHERE storeId = :storeId")
    suspend fun getMaxUpdatedAtForItems(storeId: String): Long?
    
    @Query("SELECT MAX(updatedAt) FROM categories WHERE storeId = :storeId")
    suspend fun getMaxUpdatedAtForCategories(storeId: String): Long?
    
    // Metadata
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMetadata(metadata: MenuMetadataEntity)
    
    @Query("SELECT * FROM menu_metadata WHERE storeId = :storeId LIMIT 1")
    suspend fun getMetadata(storeId: String): MenuMetadataEntity?
}


