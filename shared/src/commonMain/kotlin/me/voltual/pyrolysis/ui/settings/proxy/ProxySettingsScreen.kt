package me.voltual.pyrolysis.ui.settings.proxy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.voltual.pyrolysis.DefaultApiBaseUrl
import me.voltual.pyrolysis.DefaultWanyueyunUploadApiBaseUrl
import me.voltual.pyrolysis.data.ProxySettingsDataStore
import me.voltual.pyrolysis.getPlatformId
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProxySettingsScreen(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState,
    viewModel: ProxySettingsViewModel = koinViewModel()
) {
    val platformId = remember { getPlatformId() }

    if (platformId == "web-wasm") {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Web 平台无需配置代理",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "应用将自动使用部署站点的相对路径进行通信。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "网络代理与区域限制",
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = "若应用在当前网络环境下连接异常，您可以开启代理或配置自定义的反代服务器基址。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "启用自定义代理基址",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "关闭后将直接连接官方服务器",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.useCustomProxy,
                        onCheckedChange = { viewModel.setUseCustomProxy(it) }
                    )
                }

                if (uiState.useCustomProxy) {
                    OutlinedTextField(
                        value = uiState.customProxyUrl,
                        onValueChange = { viewModel.setCustomProxyUrl(it) },
                        label = { Text("API 代理基址") },
                        placeholder = { Text(ProxySettingsDataStore.DEFAULT_PROXY) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = uiState.customWanyueyunUrl,
                        onValueChange = { viewModel.setCustomWanyueyunUrl(it) },
                        label = { Text("挽悦云上传代理基址") },
                        placeholder = { Text(DefaultWanyueyunUploadApiBaseUrl) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                } else {
                    Text(
                        text = "当前直连目标:\nAPI: $DefaultApiBaseUrl\n上传: $DefaultWanyueyunUploadApiBaseUrl",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        OutlinedButton(
            onClick = { viewModel.resetDefaults() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("恢复默认")
        }
    }
}