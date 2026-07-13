package com.sbs.loaney.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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

    // Filter options based on the current input value
    val filteredOptions = if (value.isBlank()) {
        options
    } else {
        options.filter { it.contains(value, ignoreCase = true) }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        CustomLightTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            label = label,
            leadingIcon = leadingIcon,
            modifier = Modifier.onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    expanded = true
                }
            }
        )
        
        if (expanded) {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (filteredOptions.isNotEmpty()) {
                        filteredOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option, fontWeight = FontWeight.Medium) },
                                onClick = {
                                    onValueChange(option)
                                    expanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    } else {
                        DropdownMenuItem(
                            text = { Text("Use custom: \"$value\"", fontWeight = FontWeight.Medium) },
                            onClick = { expanded = false },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }
        }
    }
}
