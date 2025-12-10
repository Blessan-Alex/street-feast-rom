package com.streatfeast.app.di

import android.content.Context
import com.streatfeast.app.repositories.AuthRepository
import com.streatfeast.app.repositories.MenuRepository
import com.streatfeast.app.repositories.SupabaseOrderRepository
import com.streatfeast.app.storage.MenuLocalDataSource
import com.streatfeast.app.storage.OrderLocalDataSource
import com.streatfeast.app.storage.StreetFeastDatabase
import com.streatfeast.app.utils.Constants

object ServiceLocator {

    @Volatile
    private var orderRepository: SupabaseOrderRepository? = null

    @Volatile
    private var authRepository: AuthRepository? = null

    @Volatile
    private var menuRepository: MenuRepository? = null

    fun provideOrderRepository(context: Context): SupabaseOrderRepository {
        val existing = orderRepository
        if (existing != null) return existing

        val client = SupabaseModule.provideClient(context)
        val db = StreetFeastDatabase.getInstance(context)
        val local = OrderLocalDataSource(db)
        return SupabaseOrderRepository(client, local, context.applicationContext).also { orderRepository = it }
    }

    fun provideAuthRepository(context: Context): AuthRepository {
        val existing = authRepository
        if (existing != null) return existing

        val client = SupabaseModule.provideClient(context)
        return AuthRepository(client).also { authRepository = it }
    }

    fun provideMenuRepository(context: Context): MenuRepository {
        val existing = menuRepository
        if (existing != null) return existing

        val client = SupabaseModule.provideClient(context)
        val db = StreetFeastDatabase.getInstance(context)
        val local = MenuLocalDataSource(db)
        return MenuRepository(client, local, context.applicationContext, Constants.DEFAULT_STORE_ID).also { menuRepository = it }
    }

    suspend fun clear() {
        orderRepository?.stopRealtime()
        menuRepository?.stopRealtime()
        orderRepository = null
        authRepository = null
        menuRepository = null
        SupabaseModule.clear()
    }
}



