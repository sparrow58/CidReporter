package com.cid.cidreporter

import ResultCardComponent
import SearchComponent
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
                Toast.makeText(
                    this,
                    "Permission denied. Unable to access external storage.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestManageExternalStoragePermission()
        setContent {
            CidReporterTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    val viewModel = hiltViewModel<MainViewModel>()
                    val searchResults by viewModel.searchResults.collectAsState()
                    val listState = rememberLazyListState()

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        // Automatically scroll to the last added item
                        LaunchedEffect(searchResults.size) {
                            if (searchResults.isNotEmpty()) {
                                listState.animateScrollToItem(searchResults.size )
                            }
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            state = listState
                        ) {
                            // SearchComponent
                            item {
                                SearchComponent(searchQuery = "",
                                    searchType = SearchType.NAME,
                                    onSearch = { query, type -> viewModel.onSearch(query, type) })
                            }

                            items(searchResults) { result ->
                                ResultCardComponent(
                                    filename = result.filename,
                                    lineNumber = result.lineNumber,
                                    timeTaken = result.timeTaken,
                                    data = result.data
                                )
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
