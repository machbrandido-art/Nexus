package com.nexus.app.di

import com.nexus.app.data.ConversationRepository
import com.nexus.app.data.ProviderRepository
import com.nexus.app.data.crypto.CryptoManager
import com.nexus.app.data.crypto.SecretCipher
import com.nexus.app.data.datastore.SettingsRepository
import com.nexus.app.data.db.ConversationDao
import com.nexus.app.data.db.KanbanDao
import com.nexus.app.data.db.MessageDao
import com.nexus.app.data.db.NexusDatabase
import com.nexus.app.data.db.ProviderDao
import com.nexus.app.features.chat.ChatViewModel
import com.nexus.app.features.kanban.KanbanViewModel
import com.nexus.app.features.providers.ProvidersViewModel
import com.nexus.app.features.settings.SettingsViewModel
import com.nexus.app.modes.ModeRepository
import com.nexus.app.providers.ProviderRegistry
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

val appModule = module {

    single {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    single { NexusDatabase.build(androidContext()) }
    single<SecretCipher> { CryptoManager() }

    single<ProviderDao> { get<NexusDatabase>().providerDao() }
    single<ConversationDao> { get<NexusDatabase>().conversationDao() }
    single<MessageDao> { get<NexusDatabase>().messageDao() }
    single<KanbanDao> { get<NexusDatabase>().kanbanDao() }

    single { SettingsRepository(androidContext()) }
    single { ProviderRegistry(get()) }
    single { ProviderRepository(get(), get(), get()) }
    single { ConversationRepository(get(), get()) }
    single { ModeRepository() }

    viewModel { ChatViewModel(get(), get(), get(), get()) }
    viewModel { ProvidersViewModel(get(), get()) }
    viewModel { KanbanViewModel(get()) }
    viewModel { SettingsViewModel(get()) }
}