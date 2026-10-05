package com.knot.app.ui.activities

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.knot.app.R
import com.knot.app.model.Group
import com.knot.app.ui.theme.KnotDarkBrown
import com.knot.app.ui.theme.KnotCream
import com.knot.app.ui.theme.KnotInk

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateActivityScreen(
    viewModel: CreateActivityViewModel,
    onBackClick: () -> Unit,
) {
    val uiState = viewModel.uiState

    var selectedGroup by remember { mutableStateOf<Group?>(null) }
    var expanded by remember { mutableStateOf(value = false) }

    var activityTitle by remember { mutableStateOf("") }
    var activityDescription by remember { mutableStateOf("") }

    // Default to the first loaded group once groups come in, so the picker
    // isn't left on "Select Group" when there's really only one to choose.
    LaunchedEffect(uiState.groups) {
        if (selectedGroup == null) {
            selectedGroup = uiState.groups.firstOrNull()
        }
    }

    // Once the activity is created, hand control back to whoever pushed this screen.
    LaunchedEffect(uiState.created) {
        if (uiState.created) {
            onBackClick()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Create new Activity",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.left_arrow),
                            contentDescription = "Back",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Group Selector
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(KnotDarkBrown.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                        .clickable(enabled = uiState.groups.isNotEmpty()) { expanded = true }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = when {
                                uiState.isLoadingGroups -> "Loading groups…"
                                uiState.groups.isEmpty() -> "No groups yet"
                                else -> selectedGroup?.name ?: "Select Group"
                            },
                            color = Color.White
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                    }
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    uiState.groups.forEach { group ->
                        DropdownMenuItem(
                            text = { Text(group.name) },
                            onClick = {
                                selectedGroup = group
                                expanded = false
                            }
                        )
                    }
                }
            }

            // Inputs
            OutlinedTextField(
                value = activityTitle,
                onValueChange = { activityTitle = it },
                placeholder = { Text("Enter Activity Title") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = KnotCream,
                    focusedContainerColor = KnotCream
                )
            )

            OutlinedTextField(
                value = activityDescription,
                onValueChange = { activityDescription = it },
                placeholder = { Text("Enter Activity Description") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = KnotCream,
                    focusedContainerColor = KnotCream
                )
            )

            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Footer Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onBackClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, KnotInk)
                ) {
                    Text("CANCEL", color = KnotInk)
                }
                Button(
                    onClick = {
                        viewModel.createActivity(selectedGroup, activityTitle, activityDescription)
                    },
                    enabled = !uiState.isSubmitting,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KnotDarkBrown)
                ) {
                    Text(if (uiState.isSubmitting) "CREATING…" else "CREATE", color = Color.White)
                }
            }
        }
    }
}
