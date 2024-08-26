package com.cid.cidreporter.di

import android.app.Application
import com.cid.cidreporter.data.repository.MainRepository
import com.cid.cidreporter.data.files.ExcelFileReader
import com.cid.cidreporter.domain.files.IExcelFileReader
import com.cid.cidreporter.domain.repository.IMainRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideExcelFileReader(): IExcelFileReader {
        return ExcelFileReader()
    }

    @Provides
    @Singleton
        fun provideMainRepository(excelFileReader: IExcelFileReader,app: Application): IMainRepository{
        return MainRepository(excelFileReader,app)
    }
}