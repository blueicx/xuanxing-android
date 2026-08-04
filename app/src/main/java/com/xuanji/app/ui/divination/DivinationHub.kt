package com.xuanji.app.ui.divination

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xuanji.app.ui.components.AfricaIcon
import com.xuanji.app.ui.components.AmericaIcon
import com.xuanji.app.ui.components.AsiaIcon
import com.xuanji.app.ui.components.ChinaIcon
import com.xuanji.app.ui.components.CommonDivinationIcon
import com.xuanji.app.ui.components.DivinationWheelIcon
import com.xuanji.app.ui.components.EuropeIcon
import com.xuanji.app.ui.components.JapanIcon
import com.xuanji.app.ui.components.MiddleEastIcon
import com.xuanji.app.ui.components.ModernIcon
import com.xuanji.app.ui.components.REGION_COLORS
import com.xuanji.app.ui.components.RegionBadge
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SouthAsiaIcon
import com.xuanji.app.ui.components.SoutheastAsiaIcon

@Composable
fun DivinationHub(onNavigate: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val trimmed = query.trim()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "全球玄学图谱",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "按文明梳理的世界占卜体系。点击地区进入，再逐级下钻到子地区与具体体系；蓝色可点者为已实现工具，标注「规划中」者后续补全。所有内容仅供文化娱乐参考。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        // 快速搜索：按地区 / 分区 / 体系名过滤
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("搜索地区 / 分区 / 体系，如：塔罗、紫微、东南亚…") },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            singleLine = true
        )

        if (trimmed.isEmpty()) {
            DIVINATION_REGIONS.forEach { region ->
                RegionCard(region, onNavigate)
            }
        } else {
            val hits = searchDivination(trimmed)
            if (hits.isEmpty()) {
                Text(
                    "未找到「$trimmed」相关的内容。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                hits.forEach { hit ->
                    SearchHitCard(hit, onNavigate)
                }
            }
        }
    }
}

/** 一次搜索命中的体系：记录其所属地区与分区，便于展示路径 */
data class DivSearchHit(
    val system: DivSys,
    val regionName: String,
    val subregionName: String
)

/** 在地区名、分区名、体系名里做不区分大小写包含匹配，返回命中的体系列表（含亚洲的三级子分区） */
fun searchDivination(keyword: String): List<DivSearchHit> {
    val k = keyword.trim()
    if (k.isEmpty()) return emptyList()
    val hits = mutableListOf<DivSearchHit>()
    DIVINATION_REGIONS.forEach { region ->
        region.subregions.forEach { sub ->
            // 若 sub 还有子分区（如 亚洲→东亚→中国），先递归子分区，再匹配自身
            sub.children.forEach { child ->
                child.systems.forEach { sys ->
                    if (
                        region.name.contains(k) || sub.name.contains(k) || child.name.contains(k) ||
                        sys.name.contains(k) || sys.desc.contains(k)
                    ) {
                        hits.add(DivSearchHit(sys, "${region.name} · ${sub.name}", child.name))
                    }
                }
            }
            sub.systems.forEach { sys ->
                if (
                    region.name.contains(k) || sub.name.contains(k) || sys.name.contains(k) ||
                    sys.desc.contains(k)
                ) {
                    hits.add(DivSearchHit(sys, region.name, sub.name))
                }
            }
        }
    }
    return hits
}

@Composable
private fun SearchHitCard(hit: DivSearchHit, onNavigate: (String) -> Unit) {
    val sys = hit.system
    val enabled = sys.route != null
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable { onNavigate(sys.route!!) } else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (enabled) 4.dp else 0.dp)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                sys.icon,
                contentDescription = sys.name,
                tint = if (enabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        sys.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!enabled) {
                        Spacer(Modifier.width(8.dp))
                        Text("规划中", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "${hit.regionName} · ${hit.subregionName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (enabled) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "进入", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun RegionCard(region: DivRegion, onNavigate: (String) -> Unit) {
    val count = region.subregions.sumOf { sub ->
        if (sub.children.isNotEmpty()) sub.children.sumOf { it.systems.size } else sub.systems.size
    }
    // 只有一个子地区时，直接下钻到「体系列表」，省去再点一次同名子地区的多余跳转
    val target = if (region.subregions.size == 1) {
        "divination/subregion/${region.key}/${region.subregions[0].key}"
    } else {
        "divination/region/${region.key}"
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate(target) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RegionBadge(
                icon = regionIcon(region.key),
                color = REGION_COLORS[region.key] ?: MaterialTheme.colorScheme.secondary,
                sizeDp = 40.dp
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(region.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(2.dp))
                Text("${region.subregions.size} 个分区 · $count 个体系", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.AutoStories, contentDescription = "进入", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun RegionScreen(regionKey: String, onNavigate: (String) -> Unit, onBack: () -> Unit) {
    val region = regionByKey(regionKey)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BackRow(region?.name ?: "未找到", onBack)
        if (region == null) {
            Text("未找到该地区的资料。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            return@Column
        }
        region.subregions.forEach { sub ->
            SubregionCard(region.key, sub, onNavigate)
        }
    }
}

@Composable
private fun SubregionCard(regionKey: String, sub: DivSubregion, onNavigate: (String) -> Unit) {
    // 有子分区：仅一个子分区则直接进入其体系列表，多个才进分组页（如 亚洲→东亚→中国/日本）
    val target = when {
        sub.children.isEmpty() -> "divination/subregion/$regionKey/${sub.key}"
        sub.children.size == 1 -> "divination/subregion/$regionKey/${sub.children[0].key}"
        else -> "divination/group/$regionKey/${sub.key}"
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate(target) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RegionBadge(
                icon = subregionIcon(regionKey, sub.key),
                color = REGION_COLORS[regionKey] ?: MaterialTheme.colorScheme.secondary,
                sizeDp = 36.dp
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(sub.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(2.dp))
                val subCount = if (sub.children.isNotEmpty()) sub.children.size else sub.systems.size
                Text(
                    if (sub.children.isNotEmpty()) "${subCount} 个子分区" else "${subCount} 个体系",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Filled.AutoStories, contentDescription = "进入", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** 三级导航的分组页：展示某分区下的子分区（如 亚洲→东亚→中国/日本） */
@Composable
fun GroupScreen(regionKey: String, groupKey: String, onNavigate: (String) -> Unit, onBack: () -> Unit) {
    val group = subregionByKey(regionKey, groupKey)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BackRow(group?.name ?: "未找到", onBack)
        if (group == null) {
            Text("未找到该分区的资料。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            return@Column
        }
        Text(
            group.overview,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        group.children.forEach { child ->
            // 子分区若还有子分区则继续分组，否则进入体系列表
            val target = if (child.children.isNotEmpty()) {
                "divination/group/$regionKey/${child.key}"
            } else {
                "divination/subregion/$regionKey/${child.key}"
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(target) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    RegionBadge(
                        icon = subregionIcon(regionKey, child.key),
                        color = REGION_COLORS[regionKey] ?: MaterialTheme.colorScheme.secondary,
                        sizeDp = 36.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(child.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(2.dp))
                        Text("${child.systems.size} 个体系", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Filled.AutoStories, contentDescription = "进入", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun SubregionScreen(regionKey: String, subKey: String, onNavigate: (String) -> Unit, onBack: () -> Unit) {
    val sub = subregionByKey(regionKey, subKey)
    var showOverview by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BackRow(sub?.name ?: "未找到", onBack)
        if (sub == null) {
            Text("未找到该分区的资料。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            return@Column
        }
        // 直接进来就优先展示体系；总览收进可展开的「简介」里，避免遮挡体系列表
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { showOverview = !showOverview },
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionTitle("体系列表")
            Spacer(Modifier.weight(1f))
            Text(
                if (showOverview) "收起简介 ⌃" else "简介 ⌄",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (showOverview) {
            Text(
                sub.overview,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        sub.systems.forEach { sys ->
            SystemCard(sys, onNavigate)
        }
    }
}

@Composable
private fun SystemCard(sys: DivSys, onNavigate: (String) -> Unit) {
    val enabled = sys.route != null
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable { onNavigate(sys.route!!) } else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (enabled) 4.dp else 0.dp)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                sys.icon,
                contentDescription = sys.name,
                tint = if (enabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.fillMaxWidth()) {
                Text(
                    sys.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    sys.desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!enabled) {
                Spacer(Modifier.width(8.dp))
                Text("规划中", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun BackRow(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(4.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
    }
}

/** 各顶级板块的自定义轮廓图标 */
private fun regionIcon(regionKey: String): androidx.compose.ui.graphics.vector.ImageVector = when (regionKey) {
    "asia" -> AsiaIcon
    "africa" -> AfricaIcon
    "europe" -> EuropeIcon
    "america" -> AmericaIcon
    "modern" -> ModernIcon
    "common" -> CommonDivinationIcon
    else -> DivinationWheelIcon
}

/** 各二级/三级分区的自定义地图轮廓图标（如 东亚→中国/日本） */
private fun subregionIcon(regionKey: String, subKey: String): androidx.compose.ui.graphics.vector.ImageVector {
    // 优先按子分区 key
    return when (subKey) {
        "eastasia" -> AsiaIcon
        "southeastasia", "sea" -> SoutheastAsiaIcon
        "southasia", "india" -> SouthAsiaIcon
        "middleeast" -> MiddleEastIcon
        "china" -> ChinaIcon
        "japan" -> JapanIcon
        else -> regionIcon(regionKey)
    }
}
