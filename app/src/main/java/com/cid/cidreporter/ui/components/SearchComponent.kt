import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchComponent(
    searchQuery: String,
    searchType: SearchType,
    onSearch: (String, SearchType) -> Unit
) {
    var searchText by remember { mutableStateOf(searchQuery) }
    var selectedSearchType by remember { mutableStateOf(searchType) }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = "Search by:",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selectedSearchType == SearchType.NAME,
                    onClick = { selectedSearchType = SearchType.NAME }
                )
                Text(text = "Name")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selectedSearchType == SearchType.PHONE,
                    onClick = { selectedSearchType = SearchType.PHONE }
                )
                Text(text = "Phone Number")
            }
        }

        TextField(
            value = searchText,
            onValueChange = { searchText = it },
            label = { Text("Enter ${if (selectedSearchType == SearchType.NAME) "Name" else "Phone Number"}") },
            keyboardOptions = KeyboardOptions(
                keyboardType = if (selectedSearchType == SearchType.PHONE) KeyboardType.Phone else KeyboardType.Text
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { onSearch(searchText, selectedSearchType) },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Search")
        }
    }
}

enum class SearchType {
    NAME, PHONE
}
