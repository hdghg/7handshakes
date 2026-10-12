package io.github.hdghg.sevenhandshakes.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.hdghg.sevenhandshakes.data.DefaultGratitudeRepository
import io.github.hdghg.sevenhandshakes.data.DefaultTaskRepository
import io.github.hdghg.sevenhandshakes.data.GratitudeRepository
import io.github.hdghg.sevenhandshakes.data.TaskRepository
import io.github.hdghg.sevenhandshakes.data.source.gratitude.GratitudeDao
import io.github.hdghg.sevenhandshakes.data.source.gratitude.GratitudeDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Singleton
    @Binds
    abstract fun bindTaskRepository(repository: DefaultTaskRepository): TaskRepository

    @Singleton
    @Binds
    abstract fun bindGratitudeRepository(repository: DefaultGratitudeRepository): GratitudeRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Singleton
    @Provides
    fun provideDataBase(
        @ApplicationContext context: Context,
    ): GratitudeDatabase = Room
        .databaseBuilder(
            context.applicationContext,
            GratitudeDatabase::class.java,
            "Gratitudes.db",
        ).build()

    @Provides
    fun provideGratitudesDao(database: GratitudeDatabase): GratitudeDao = database.gratitudeDao()
}
