package com.cid.cidreporter.data.repository

import android.app.Application
import com.cid.cidreporter.R
import com.cid.cidreporter.domain.files.IExcelFileReader
import com.cid.cidreporter.domain.repository.IMainRepository

class MainRepository(
    private val excelFileReader: IExcelFileReader,
    private val appContext: Application
) : IMainRepository {
    init {
        val appName = appContext.getString(R.string.app_name)
        println("Hello from my repository $appName")
    }
    override fun test() {
        TODO("Not yet implemented")
    }
}