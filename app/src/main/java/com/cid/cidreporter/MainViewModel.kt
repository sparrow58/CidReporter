package com.cid.cidreporter

import SearchType
import android.app.Application
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cid.cidreporter.domain.repository.IMainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(
    private val mainRepository: IMainRepository,
    private val context: Application
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchType = MutableStateFlow(SearchType.NAME)
    val searchType: StateFlow<SearchType> = _searchType

    private val _searchResult = MutableStateFlow<List<String>>(emptyList())
    val searchResult: StateFlow<List<String>> = _searchResult

    fun onSearch(query: String, type: SearchType) {
        //requestManageExternalStoragePermission()
        viewModelScope.launch {
            try {
                val result = searchInExcel(query, type)
                _searchResult.value = result ?: listOf("Not found")
            }
            catch (ex: Exception){
                println("sabsab error $ex")
            }
        }
    }

    private suspend fun performSearch(query: String, type: SearchType): List<String> {
        // Replace this with actual search logic
        return listOf("Result 1", "Result 2", "Result 3")
    }

    private fun getExcelFile(): File? {
        try {
            val documentsDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val cidDir = File(documentsDir, "cid")
            val excelFile = File(cidDir, "miniقاعدة بيانات حصر الساكنين.xlsx")

            return if (excelFile.exists()) {
                excelFile
            } else {
                println("Error file not found")
                null
            }
        } catch (e: Exception) {
            println("Error " + e.message)
        }
        return null
    }

    private suspend fun searchInExcel(query: String, type: SearchType): List<String>? {
        val file = getExcelFile()

        if (file != null && !file.exists()) return null
        println("sabsab file " + file?.name)
        val fileInputStream = withContext(Dispatchers.IO) {
            FileInputStream(file)
        };

        val workbook = XSSFWorkbook(fileInputStream)

        //val workbook = WorkbookFactory.create(file)
        val sheet = workbook.getSheetAt(0)

        var nameColumnIndex: Int? = null
        var phoneColumnIndex: Int? = null

        // Identify the correct columns
        val headerRow = sheet.getRow(0)
        for (cell in headerRow) {
            val cellValue = cell.stringCellValue.trim()
            when {
                cellValue.contains("الاسم", ignoreCase = true) -> nameColumnIndex = cell.columnIndex
                cellValue.contains("رقم الهاتف", ignoreCase = true) ||
                        cellValue.contains("رقم التلفون", ignoreCase = true) -> phoneColumnIndex =
                    cell.columnIndex
            }
        }

        val searchColumnIndex = when (type) {
            SearchType.NAME -> nameColumnIndex
            SearchType.PHONE -> phoneColumnIndex
        }

        if (searchColumnIndex == null) return null

        // Search for the query in the detected column
        for (row in sheet) {
            if (row.rowNum == 0) continue // Skip header row
            val cellValue = row.getCell(searchColumnIndex)?.stringCellValue?.trim()
            if (cellValue?.contains(query) == true) {
                // Return the entire row as a list of strings
                return row.map { it.toString() }
            }
        }

        return null
    }
}

