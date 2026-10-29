package io.github.hdghg.sevenhandshakes.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.hdghg.sevenhandshakes.data.DefaultReviewRepository
import io.github.hdghg.sevenhandshakes.data.ReviewRepository
import io.github.hdghg.sevenhandshakes.data.source.gratitude.ReviewDao
import io.github.hdghg.sevenhandshakes.data.source.gratitude.ReviewDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Singleton
    @Binds
    abstract fun bindGratitudeRepository(repository: DefaultReviewRepository): ReviewRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Singleton
    @Provides
    fun provideDataBase(
        @ApplicationContext context: Context,
    ): ReviewDatabase = Room
        .databaseBuilder(
            context.applicationContext,
            ReviewDatabase::class.java,
            "Gratitudes.db",
        ).build()

    @Provides
    fun provideGratitudesDao(database: ReviewDatabase): ReviewDao = database.reviewDao()
}
