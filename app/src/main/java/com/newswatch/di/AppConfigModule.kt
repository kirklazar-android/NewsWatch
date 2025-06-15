package com.newswatch.di

import com.newswatch.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object AppConfigModule {
    @Provides
    @Named("gnewsApiKey")
    fun gnewsApiKey(): String = BuildConfig.GNEWS_API_KEY
}