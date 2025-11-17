package com.streatfeast.app.di

import android.content.Context
import com.streatfeast.app.repositories.AuthRepository
import com.streatfeast.app.repositories.SupabaseOrderRepository
import com.streatfeast.app.storage.OrderLocalDataSource
import com.streatfeast.app.storage.StreetFeastDatabase

object ServiceLocator {

    @Volatile
    private var orderRepository: SupabaseOrderRepository? = null

    @Volatile
    private var authRepository: AuthRepository? = null

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

    suspend fun clear() {
        orderRepository?.stopRealtime()
        orderRepository = null
        authRepository = null
        SupabaseModule.clear()
    }
}



