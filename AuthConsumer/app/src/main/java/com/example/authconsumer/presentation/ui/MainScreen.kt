package com.example.authconsumer.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.authconsumer.domain.model.AuthKey
import com.example.authconsumer.domain.model.FetchResult
import com.example.authconsumer.presentation.components.AuthKeyItem
import com.example.authconsumer.presentation.viewmodel.AuthKeyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: AuthKeyViewModel,
    modifier: Modifier = Modifier
) {
    val fetchResult by viewModel.fetchResult.collectAsState()
    val currentKeyResult by viewModel.currentKeyResult.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Auth Consumer") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.fetchCurrentValidKey() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Get Current Key")
                }
                OutlinedButton(
                    onClick = { viewModel.fetchAuthKeys() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Fetch All Keys")
                }
            }

            CurrentKeySection(currentKeyResult = currentKeyResult)

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            AllKeysSection(fetchResult = fetchResult)
        }
    }
}

@Composable
private fun CurrentKeySection(currentKeyResult: FetchResult<AuthKey?>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Current Valid Key",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        when (currentKeyResult) {
            is FetchResult.Idle -> {
                Text(
                    text = "Tap 'Get Current Key' to fetch",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            is FetchResult.Loading -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(16.dp)
                    )
                    Text(
                        text = "Fetching current key...",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            is FetchResult.Success -> {
                val key = currentKeyResult.data
                if (key != null) {
                    AuthKeyItem(authKey = key)
                } else {
                    Text(
                        text = "No valid key available",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            is FetchResult.Error -> {
                Text(
                    text = "Error: ${currentKeyResult.message}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun AllKeysSection(fetchResult: FetchResult<List<AuthKey>>) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "All Keys",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        when (fetchResult) {
            is FetchResult.Idle -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tap 'Fetch All Keys' to load all keys",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            is FetchResult.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is FetchResult.Success -> {
                if (fetchResult.data.isEmpty()) {
                    Text(
                        text = "No keys available from provider",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    Text(
                        text = "Found ${fetchResult.data.size} key(s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    LazyColumn {
                        items(fetchResult.data, key = { it.id }) { authKey ->
                            AuthKeyItem(authKey = authKey)
                        }
                    }
                }
            }

            is FetchResult.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Error: ${fetchResult.message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
