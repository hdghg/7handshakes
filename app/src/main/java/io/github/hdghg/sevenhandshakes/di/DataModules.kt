package io.github.hdghg.sevenhandshakes.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import io.github.hdghg.sevenhandshakes.data.ContactRepository
import io.github.hdghg.sevenhandshakes.data.DefaultContactRepository
import io.github.hdghg.sevenhandshakes.data.DefaultHandshakeDepthRepository
import io.github.hdghg.sevenhandshakes.data.DefaultIdentityRepository
import io.github.hdghg.sevenhandshakes.data.DefaultReviewRepository
import io.github.hdghg.sevenhandshakes.data.DefaultSelectedIdentityRepository
import io.github.hdghg.sevenhandshakes.data.HandshakeDepthRepository
import io.github.hdghg.sevenhandshakes.data.IdentityRepository
import io.github.hdghg.sevenhandshakes.data.ReviewRepository
import io.github.hdghg.sevenhandshakes.data.SelectedIdentityRepository
import io.github.hdghg.sevenhandshakes.data.share.ContactLinkImporter
import io.github.hdghg.sevenhandshakes.data.share.ReviewLinkImporter
import io.github.hdghg.sevenhandshakes.data.share.RoomShareImportStore
import io.github.hdghg.sevenhandshakes.data.share.ShareImporter
import io.github.hdghg.sevenhandshakes.data.source.gratitude.ReviewDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val repositoryModule = module {
    singleOf(::DefaultContactRepository) { bind<ContactRepository>() }
    singleOf(::DefaultIdentityRepository) { bind<IdentityRepository>() }
    singleOf(::DefaultSelectedIdentityRepository) { bind<SelectedIdentityRepository>() }
    singleOf(::DefaultHandshakeDepthRepository) { bind<HandshakeDepthRepository>() }
    single<ReviewRepository> {
        DefaultReviewRepository(
            reviewDao = get(),
            contactDao = get(),
            dispatcher = get(DefaultDispatcher),
            scope = get(ApplicationScope),
        )
    }
}

val shareModule = module {
    factoryOf(::ContactLinkImporter)
    factoryOf(::ReviewLinkImporter)
    factoryOf(::RoomShareImportStore)
    factory { ShareImporter(get<RoomShareImportStore>()) }
}

val dataStoreModule = module {
    // Only one DataStore instance may exist per file, hence the singleton.
    single<DataStore<Preferences>> {
        PreferenceDataStoreFactory.create { androidContext().preferencesDataStoreFile("settings") }
    }
}

val databaseModule = module {
    single<ReviewDatabase> {
        Room.databaseBuilder(androidContext(), ReviewDatabase::class.java, "Gratitudes.db").build()
    }
    factory { get<ReviewDatabase>().reviewDao() }
    factory { get<ReviewDatabase>().contactDao() }
    factory { get<ReviewDatabase>().identityDao() }
}
