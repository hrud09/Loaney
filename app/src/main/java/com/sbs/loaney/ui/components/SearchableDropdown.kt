package com.sbs.loaney.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableDropdown(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector,
    options: List<String>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var filterQuery by remember { mutableStateOf(value) }

    LaunchedEffect(value) {
        if (value.isBlank()) {
            filterQuery = ""
        } else if (value != filterQuery) {
            kotlinx.coroutines.delay(2000L)
            filterQuery = value
        }
    }

    // Filter options based on the debounced filterQuery
    val filteredOptions = if (filterQuery.isBlank()) {
        options
    } else {
        options.filter { it.contains(filterQuery, ignoreCase = true) }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth()
    ) {
        CustomLightTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            label = label,
            leadingIcon = leadingIcon,
            modifier = Modifier.menuAnchor()
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 250.dp) // Limit height so it scrolls if too long
        ) {
            if (filteredOptions.isNotEmpty()) {
                filteredOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueChange(option)
                            filterQuery = option
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            } else {
                DropdownMenuItem(
                    text = { Text("Use custom: \"$value\"") },
                    onClick = { 
                        filterQuery = value
                        expanded = false 
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}
