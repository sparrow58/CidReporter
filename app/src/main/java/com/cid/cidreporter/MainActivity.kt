package com.cid.cidreporter

import ResultHeaderComponent
import SearchComponent
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.cid.cidreporter.ui.theme.CidReporterTheme
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
@AndroidEntryPoint

class MainActivity : ComponentActivity() {
    private val manageExternalStoragePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // Check if the permission was granted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
                // Permission granted, proceed with file access
                // You can call your function to access files here if needed
            } else {
                // Permission denied, show a message to the user
                Toast.makeText(this, "Permission denied. Unable to access external storage.", Toast.LENGTH_SHORT).show()
            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CidReporterTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    val viewModel = hiltViewModel<MainViewModel>()
                    val searchResult by viewModel.searchResult.collectAsState()

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // SearchComponent
                            item {
                                SearchComponent(
                                    searchQuery = "",
                                    searchType = SearchType.NAME,
                                    onSearch = { query, type -> viewModel.onSearch(query, type) }
                                )
                            }


                            // Displaying the card twice for testing purpose
                            items(2) {
                                searchResult?.let { result ->
                                    if (result.data.isNotEmpty()) {
                                        ResultHeaderComponent(
                                            filename = result.filename,
                                            lineNumber = result.lineNumber,
                                            timeTaken = result.timeTaken,
                                            data = result.data
                                        )
                                    } else {
                                        Text(
                                            text = "لم يتم العثور على نتائج",
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    // The method to request the MANAGE_EXTERNAL_STORAGE permission
    private fun requestManageExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:${applicationContext.packageName}")
                manageExternalStoragePermissionLauncher.launch(intent)
            }
        }
    }
}



