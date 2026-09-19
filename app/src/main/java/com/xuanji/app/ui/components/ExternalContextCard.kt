package com.xuanji.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.xuanji.app.domain.external.CitySearchResult
import com.xuanji.app.domain.external.ExternalContextProvider
import com.xuanji.app.domain.external.ExternalResult
import com.xuanji.app.domain.external.OfflineContextProvider
import com.xuanji.app.domain.external.OpenMeteoContextProvider
import com.xuanji.app.domain.external.WeatherSnapshot
import kotlinx.coroutines.launch

/** Manual city search + optional weather; no GPS and no background requests. */
@Composable
fun ExternalContextCard(networkEnabled: Boolean, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val offline = remember { OfflineContextProvider() }
    val online = remember { OpenMeteoContextProvider() }
    var query by remember { mutableStateOf("") }
    var cities by remember { mutableStateOf<List<CitySearchResult>>(emptyList()) }
    var selected by remember { mutableStateOf<CitySearchResult?>(null) }
    var weather by remember { mutableStateOf<WeatherSnapshot?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    FortuneCard(modifier = modifier, title = "天气与地点（可选）") {
        SectionTitle("天气与地点")
        Text(
            if (networkEnabled) "已开启联网：只在你点击搜索/天气时请求，使用 Open-Meteo；未开启时不猜测实时天气。"
            else "当前为离线目录：可查看城市标签，但不会伪造实时天气。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it.take(40) },
            label = { Text("手动输入城市，例如上海") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    scope.launch {
                        val provider: ExternalContextProvider = if (networkEnabled) online else offline
                        when (val result = provider.searchCities(query)) {
                            is ExternalResult.Success -> {
                                cities = result.value
                                message = if (networkEnabled) "已从联网目录返回 ${result.value.size} 个候选" else "离线目录结果"
                            }
                            is ExternalResult.Failure -> {
                                val fallback = offline.searchCities(query)
                                if (fallback is ExternalResult.Success) {
                                    cities = fallback.value
                                    message = "联网不可用，已回退离线目录：${result.reason}"
                                } else {
                                    message = "城市搜索失败：${result.reason}"
                                }
                            }
                        }
                    }
                },
                enabled = query.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) { Text("搜索城市") }
            OutlinedButton(
                onClick = {
                    selected?.let { city ->
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:${city.latitude},${city.longitude}?q=${Uri.encode(city.name)}")))
                        }.onFailure {
                            message = "手机没有可用的地图应用"
                        }
                    }
                },
                enabled = selected != null,
                modifier = Modifier.weight(1f)
            ) { Text("打开地图") }
        }
        cities.take(5).forEach { city ->
            OutlinedButton(
                onClick = {
                    selected = city
                    weather = null
                    message = "已选择：${city.name}"
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("${city.name} · ${city.country}${city.admin1?.let { " · $it" }.orEmpty()}") }
        }
        selected?.let { city ->
            Spacer(Modifier.height(4.dp))
            Text("当前城市：${city.name}（${city.latitude}, ${city.longitude}）", style = MaterialTheme.typography.bodyMedium)
            Button(
                onClick = {
                    scope.launch {
                        if (!networkEnabled) {
                            message = "未开启联网，实时天气不可用。"
                            return@launch
                        }
                        when (val result = online.weather(city)) {
                            is ExternalResult.Success -> {
                                weather = result.value
                                message = "天气已更新"
                            }
                            is ExternalResult.Failure -> message = "天气请求失败：${result.reason}"
                        }
                    }
                },
                enabled = networkEnabled,
                modifier = Modifier.fillMaxWidth()
            ) { Text("查询实时天气") }
        }
        weather?.let { snapshot ->
            Text(
                "${snapshot.city.name}：${snapshot.temperatureC ?: "--"}°C，体感 ${snapshot.apparentTemperatureC ?: "--"}°C，降水概率 ${snapshot.precipitationProbability ?: "--"}% · ${snapshot.source}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        message?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Spacer(Modifier.height(4.dp))
        Text("地图由手机已安装的地图应用打开；请自行核对路线、天气预警和安全信息。", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
