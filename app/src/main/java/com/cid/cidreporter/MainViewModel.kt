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
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import kotlin.system.measureTimeMillis


@HiltViewModel
class MainViewModel @Inject constructor(
    private val mainRepository: IMainRepository,
    private val context: Application
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchType = MutableStateFlow(SearchType.NAME)
    val searchType: StateFlow<SearchType> = _searchType

    private val _searchResult = MutableStateFlow<SearchResult?>(null)
    val searchResult: StateFlow<SearchResult?> = _searchResult

    fun onSearch(query: String, type: SearchType) {
        //requestManageExternalStoragePermission()
        viewModelScope.launch {
            try {
                val file = getExcelFile()

                if (file != null && !file.exists()) throw Exception("file not found")
                println("sabsab file " + file?.name)

                val workbook = WorkbookFactory.create(file)
                val sheet = workbook.getSheetAt(0)
                val result = searchInExcel(sheet,query, type)
                _searchResult.value = result ?: SearchResult("Not found", emptyList(), 0L, -1)
            }
            catch (ex: Exception){
                println("sabsab error $ex")
            }
        }
    }

    private fun getExcelFile(): File? {
        try {
            val documentsDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val cidDir = File(documentsDir, "cid")
            val excelFile = File(cidDir, "miniصنعاء قاعدة البيانات الخاصة بالعائدين كاملة.xls")

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
    private  fun getHeaderRow(sheet: Sheet): Row? {
        for (value in sheet) {
            when {
                value.count() > 3 -> {
                   return value
                }
            }
        }
        return null
    }
    private suspend fun searchInExcel(sheet: Sheet, query: String, type: SearchType): SearchResult? {


        var nameColumnIndex: Int? = null
        var phoneColumnIndex: Int? = null

        val headerRow  = getHeaderRow(sheet) ?: return null

        println("sabsab header row $headerRow")
        for (cell in headerRow) {
            val cellValue = getCellContent(cell)
            when {
                cellValue.contains("الاسم", ignoreCase = true) -> nameColumnIndex = cell.columnIndex
                cellValue.contains("رقم الهاتف", ignoreCase = true) ||
                        cellValue.contains("رقم التلفون", ignoreCase = true) -> phoneColumnIndex = cell.columnIndex
            }
        }

        val searchColumnIndex = when (type) {
            SearchType.NAME -> nameColumnIndex
            SearchType.PHONE -> phoneColumnIndex
        }

        if (searchColumnIndex == null) return null

        var foundRowNumber = -1
        val resultList = mutableListOf<Pair<String, String>>()

        val timeTaken = measureTimeMillis {
            for (row in sheet) {
                if (row.rowNum == 0) continue
                val cellValue = getCellContent( row.getCell(searchColumnIndex))
                if (cellValue.contains(query)) {
                    foundRowNumber = row.rowNum + 1  // Row numbers are zero-indexed
                    for (cell in row) {
                        val columnHeader = headerRow.getCell(cell.columnIndex)?.stringCellValue ?: ""

                        // Check the cell type and format accordingly
                        val cellContent = getCellContent(cell)

                        resultList.add(columnHeader to cellContent)
                    }
                    break
                }
            }
        }
        return if (foundRowNumber != -1) {
            SearchResult(
                filename = sheet.sheetName ?: "no file name",
                data = resultList,
                timeTaken = timeTaken,
                lineNumber = foundRowNumber
            )
        } else {
            null
        }
    }

    private fun getCellContent(cell: Cell): String {
        val cellContent = when (cell.cellType) {
            CellType.NUMERIC -> {
                // Format numeric value to avoid scientific notation
                if (DateUtil.isCellDateFormatted(cell)) {
                    cell.dateCellValue.toString()
                } else {
                    cell.numericCellValue.toLong().toString() // Convert to long to remove decimals
                }
            }

            CellType.STRING -> cell.stringCellValue
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            CellType.FORMULA -> cell.cellFormula
            else -> cell.stringCellValue
        }
        return cellContent
    }
}
data class SearchResult(
    val filename: String,
    val data: List<Pair<String, String>>,
    val timeTaken: Long,
    val lineNumber: Int
)

