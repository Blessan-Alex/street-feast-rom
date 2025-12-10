package com.streatfeast.app.storage

import androidx.room.withTransaction
import com.streatfeast.app.models.MenuItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MenuLocalDataSource(
    private val db: StreetFeastDatabase
) {
    private val menuDao = db.menuDao()

    fun observeCategories(storeId: String): Flow<List<CategoryEntity>> {
        return menuDao.getAllCategories(storeId)
    }

    fun observeItems(storeId: String): Flow<List<ItemEntity>> {
        return menuDao.getAllItems(storeId)
    }

    fun observeItemsByCategory(categoryId: String): Flow<List<ItemEntity>> {
        return menuDao.getItemsByCategory(categoryId)
    }

    fun observeFrequentItems(storeId: String): Flow<List<FrequentItemEntity>> {
        return menuDao.getFrequentItems(storeId)
    }

    suspend fun getCategories(storeId: String): List<CategoryEntity> {
        return menuDao.getAllCategoriesSync(storeId)
    }

    suspend fun getItems(storeId: String): List<ItemEntity> {
        return menuDao.getAllItemsSync(storeId)
    }

    suspend fun getItemsByCategory(categoryId: String): List<ItemEntity> {
        return menuDao.getItemsByCategorySync(categoryId)
    }

    suspend fun getFrequentItems(storeId: String): List<FrequentItemEntity> {
        return menuDao.getFrequentItemsSync(storeId)
    }

    suspend fun replaceMenuData(
        storeId: String,
        categories: List<CategoryEntity>,
        items: List<ItemEntity>,
        frequentItems: List<FrequentItemEntity>
    ) {
        db.withTransaction {
            menuDao.clearCategoriesForStore(storeId)
            menuDao.clearItemsForStore(storeId)
            menuDao.clearFrequentItemsForStore(storeId)
            
            if (categories.isNotEmpty()) {
                menuDao.upsertCategories(categories)
            }
            if (items.isNotEmpty()) {
                menuDao.upsertItems(items)
            }
            if (frequentItems.isNotEmpty()) {
                menuDao.upsertFrequentItems(frequentItems)
            }
        }
    }
    
    // Targeted upserts/deletes for incremental updates
    suspend fun upsertCategory(category: CategoryEntity) = menuDao.upsertCategory(category)
    suspend fun deleteCategory(id: String, storeId: String) = menuDao.deleteCategory(id, storeId)
    
    suspend fun upsertItem(item: ItemEntity) = menuDao.upsertItem(item)
    suspend fun deleteItem(id: String, storeId: String) = menuDao.deleteItem(id, storeId)
    
    suspend fun upsertFrequentItem(entity: FrequentItemEntity) = menuDao.upsertFrequentItem(entity)
    suspend fun deleteFrequentItem(itemId: String, storeId: String) = menuDao.deleteFrequentItem(itemId, storeId)
    
    suspend fun getMaxUpdatedAtForItems(storeId: String): Long? = menuDao.getMaxUpdatedAtForItems(storeId)
    suspend fun getMaxUpdatedAtForCategories(storeId: String): Long? = menuDao.getMaxUpdatedAtForCategories(storeId)
    
    suspend fun upsertMetadata(metadata: MenuMetadataEntity) = menuDao.upsertMetadata(metadata)
    suspend fun getMetadata(storeId: String): MenuMetadataEntity? = menuDao.getMetadata(storeId)
}

// Extension functions to convert entities to domain models
fun CategoryEntity.toCategory(): com.streatfeast.app.models.Category {
    return com.streatfeast.app.models.Category(
        id = id,
        name = name,
        isActive = isActive
    )
}

fun ItemEntity.toMenuItem(): MenuItem {
    val sizesList = RoomConverters.fromStringList(sizes) ?: emptyList()
    return MenuItem(
        id = id,
        name = name,
        description = "", // Not stored in ItemEntity
        sizes = sizesList,
        vegFlag = vegFlag,
        categoryId = categoryId
    )
}

fun FrequentItemEntity.toItemId(): String {
    return itemId
}


