package com.cid.cidreporter

import SearchType
import android.app.Application
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cid.cidreporter.domain.repository.IMainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.File
import javax.inject.Inject
import kotlin.system.measureTimeMillis


@HiltViewModel
class MainViewModel @Inject constructor(
    private val mainRepository: IMainRepository, private val context: Application
) : ViewModel() {

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults

    fun onSearch(query: String, type: SearchType) {
        viewModelScope.launch {
            _searchResults.value = emptyList()
            try {
                val excelFiles = getExcelFiles()

                // Process each Excel file in parallel
                val sheetResults = excelFiles.map { file ->
                    async {
                        val sheets = getSheet(file)
                        sheets.map { sheet ->
                            async {
                                val result = searchInExcel(sheet, query, type)
                                result?.let {
                                    // Update the UI with the result as it comes in
                                    withContext(Dispatchers.Main) {
                                        _searchResults.value += it
                                    }
                                }
                            }
                        }.awaitAll() // Wait for all sheet searches to complete
                        sheets
                    }
                }.awaitAll() // Wait for all file processing to complete

                // Additional processing if needed
            } catch (ex: Exception) {
                println("sabsab error $ex")
            }
        }
    }

    private suspend fun getSheet(file: File): List<Sheet> = withContext(Dispatchers.IO) {
        val workbook = WorkbookFactory.create(file)
        return@withContext (0 until workbook.numberOfSheets).map { workbook.getSheetAt(it) }
    }

    private fun getExcelFiles(): List<File> {
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val cidDir = File(documentsDir, "cid")

        return cidDir.listFiles { _, name ->
            name.endsWith(".xls") || name.endsWith(".xlsx")
        }?.toList() ?: emptyList()
    }


    private fun getHeaderRow(sheet: Sheet): Row? {
        for (value in sheet) {
            when {
                value.count() > 3 -> {
                    return value
                }
            }
        }
        return null
    }

    private fun searchInExcel(
        sheet: Sheet, query: String, type: SearchType
    ): SearchResult? {


        var nameColumnIndex: Int? = null
        var phoneColumnIndex: Int? = null

        val headerRow = getHeaderRow(sheet) ?: return null

        println("sabsab header row $headerRow")
        for (cell in headerRow) {
            val cellValue = getCellContent(cell)
            when {
                cellValue.contains("الاسم", ignoreCase = true) -> nameColumnIndex = cell.columnIndex
                cellValue.contains(
                    "رقم الهاتف",
                    ignoreCase = true
                ) || cellValue.contains("رقم التلفون", ignoreCase = true) -> phoneColumnIndex =
                    cell.columnIndex
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
                val cellValue = getCellContent(row.getCell(searchColumnIndex))
                if (cellValue.contains(query)) {
                    foundRowNumber = row.rowNum + 1  // Row numbers are zero-indexed
                    for (cell in row) {
                        val columnHeader =
                            headerRow.getCell(cell.columnIndex)?.stringCellValue ?: ""

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

