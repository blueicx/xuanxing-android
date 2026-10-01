package com.xuanji.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.xuanji.app.ui.divination.ClassicalAstrologyScreen
import com.xuanji.app.ui.divination.ArabicAstrologyScreen
import com.xuanji.app.ui.divination.AztecAstrologyScreen
import com.xuanji.app.ui.divination.BabylonianAstrologyScreen
import com.xuanji.app.ui.divination.CelticTreeCalendarScreen
import com.xuanji.app.ui.divination.ChakraScreen
import com.xuanji.app.ui.divination.CrystalBallScreen
import com.xuanji.app.ui.divination.DivinationHub
import com.xuanji.app.ui.divination.FengShuiScreen
import com.xuanji.app.ui.divination.GroupScreen
import com.xuanji.app.ui.divination.HellenisticAstrologyScreen
import com.xuanji.app.ui.divination.HumanDesignScreen
import com.xuanji.app.ui.divination.HermesAlchemyScreen
import com.xuanji.app.ui.divination.IfaScreen
import com.xuanji.app.ui.divination.IChingScreen
import com.xuanji.app.ui.divination.KabbalahAstrologyScreen
import com.xuanji.app.ui.divination.KhmerAstrologyScreen
import com.xuanji.app.ui.divination.LawOfAttractionScreen
import com.xuanji.app.ui.divination.LenormandScreen
import com.xuanji.app.ui.divination.LotDrawScreen
import com.xuanji.app.ui.divination.LiuYaoScreen
import com.xuanji.app.ui.divination.LiuRenScreen
import com.xuanji.app.ui.divination.MahaboteScreen
import com.xuanji.app.ui.divination.MayaTzolkinScreen
import com.xuanji.app.ui.divination.MayaGalacticScreen
import com.xuanji.app.ui.divination.MedicineWheelScreen
import com.xuanji.app.ui.divination.MeiHuaScreen
import com.xuanji.app.ui.divination.NadiAstrologyScreen
import com.xuanji.app.ui.divination.NagaRainScreen
import com.xuanji.app.ui.divination.NineStarsScreen
import com.xuanji.app.ui.divination.NumerologyScreen
import com.xuanji.app.ui.divination.NameologyScreen
import com.xuanji.app.ui.divination.OnmyodoScreen
import com.xuanji.app.ui.divination.PalmistryScreen
import com.xuanji.app.ui.divination.PersianAstrologyScreen
import com.xuanji.app.ui.divination.PhysiognomyScreen
import com.xuanji.app.ui.divination.PrasnaScreen
import com.xuanji.app.ui.divination.QiMenScreen
import com.xuanji.app.ui.divination.QiZhengScreen
import com.xuanji.app.ui.divination.ReferenceScreen
import com.xuanji.app.ui.divination.RegionScreen
import com.xuanji.app.ui.divination.RuneScreen
import com.xuanji.app.ui.divination.SubregionScreen
import com.xuanji.app.ui.divination.TajulMulukScreen
import com.xuanji.app.ui.divination.TaiYiScreen
import com.xuanji.app.ui.divination.TarotScreen
import com.xuanji.app.ui.divination.TibetanAstrologyScreen
import com.xuanji.app.ui.divination.ThirteenMoonScreen
import com.xuanji.app.ui.divination.TodayOracleScreen
import com.xuanji.app.ui.divination.TwentyEightMansionsScreen
import com.xuanji.app.ui.divination.VastuScreen
import com.xuanji.app.ui.divination.YemeniAstrologyScreen
import com.xuanji.app.ui.divination.VedicScreen
import com.xuanji.app.ui.divination.ZiweiScreen
import com.xuanji.app.ui.eastern.EasternScreen
import com.xuanji.app.ui.history.HistoryScreen
import com.xuanji.app.ui.profile.ProfileScreen
import com.xuanji.app.ui.western.WesternScreen
import com.xuanji.app.ui.composite.CompositeFortuneScreen
import com.xuanji.app.ui.test.TestHubScreen
import com.xuanji.app.ui.components.LocalImmersiveStageOpen
import com.xuanji.app.ui.components.LocalMysticGuideVisible
import com.xuanji.app.ui.components.TodayFortuneMode
import com.xuanji.app.ui.components.CompanionGameCard
import com.xuanji.app.di.AppModule
import com.xuanji.app.domain.MysticCharacterId
import com.xuanji.app.domain.game.CompanionGameCatalog

sealed class Screen(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    data object Today : Screen("today", "今日", Icons.Filled.AutoAwesome)
    data object Charts : Screen("charts", "排盘", Icons.Filled.AutoStories)
    data object Explore : Screen("explore", "探索", Icons.Filled.Explore)
    data object Eastern : Screen("eastern", "东方", Icons.Filled.AutoStories)
    data object Western : Screen("western", "西方", Icons.Filled.Star)
    data object Divination : Screen("divination", "占卜", Icons.Filled.Casino)
    data object History : Screen("history", "记录", Icons.Filled.History)
    data object Profile : Screen("profile", "我的", Icons.Filled.Person)
    data object Composite : Screen("composite", "综合", Icons.Filled.AutoAwesome)
    data object Test : Screen("test", "测试", Icons.Filled.Quiz)

    // 占卜子页
    data object TodayOracle : Screen("divination/today", "今日算命", Icons.Filled.Casino)
    data object Tarot : Screen("divination/tarot", "塔罗牌", Icons.Filled.AutoStories)
    data object Numerology : Screen("divination/numerology", "生命数字", Icons.Filled.Star)
    data object Vedic : Screen("divination/vedic", "印度占星", Icons.Filled.Casino)
    data object Rune : Screen("divination/rune", "北欧符文", Icons.Filled.Casino)
    data object Ziwei : Screen("divination/ziwei", "紫微斗数", Icons.Filled.Star)
    data object LiuYao : Screen("divination/liuyao", "六爻", Icons.Filled.Casino)
    data object MeiHua : Screen("divination/meihua", "梅花易数", Icons.Filled.AutoStories)
    data object QiMen : Screen("divination/qimen", "奇门遁甲", Icons.Filled.Extension)
    data object QiZheng : Screen("divination/qizheng", "七政四余", Icons.Filled.Star)
    data object FengShui : Screen("divination/fengshui", "风水", Icons.Filled.Explore)
    data object MayaTzolkin : Screen("divination/maya", "玛雅历", Icons.Filled.Star)
    data object Lenormand : Screen("divination/lenormand", "雷诺曼", Icons.Filled.Casino)
    data object Chakra : Screen("divination/chakra", "脉轮", Icons.Filled.Star)
    data object ClassicalAstrology : Screen("divination/classical/all", "古典占星", Icons.Filled.Star)
    data object HumanDesign : Screen("divination/humandesign", "人类图", Icons.Filled.AutoStories)
    data object LawOfAttraction : Screen("divination/loa", "吸引力法则", Icons.Filled.Star)
    data object ThirteenMoon : Screen("divination/dreamspell", "13 月亮历", Icons.Filled.Casino)
    data object NineStars : Screen("divination/ninestars", "九星气学", Icons.Filled.Star)
    data object TibetanAstrology : Screen("divination/tibetan", "西藏占星", Icons.Filled.Star)
    data object Onmyodo : Screen("divination/onmyodo", "阴阳道", Icons.Filled.AutoStories)
    data object Mahabote : Screen("divination/mahabote", "缅甸黄道带", Icons.Filled.Star)
    data object KhmerAstrology : Screen("divination/khmer", "高棉占星", Icons.Filled.Explore)
    data object TajulMuluk : Screen("divination/tajulmuluk", "Tajul Muluk", Icons.Filled.Casino)
    data object NagaRain : Screen("divination/naga", "那伽占雨", Icons.Filled.Extension)
    data object NadiAstrology : Screen("divination/naadi", "纳迪占星", Icons.Filled.AutoStories)
    data object Vastu : Screen("divination/vastu", "瓦斯图", Icons.Filled.Explore)
    data object BabylonianAstrology : Screen("divination/babylonian", "巴比伦占星", Icons.Filled.Star)
    data object HellenisticAstrology : Screen("divination/hellenistic", "希腊占星", Icons.Filled.Star)
    data object ArabicAstrology : Screen("divination/arabic", "阿拉伯占星", Icons.Filled.Casino)
    data object PersianAstrology : Screen("divination/persian", "波斯占星", Icons.Filled.Explore)
    data object YemeniAstrology : Screen("divination/yemeni", "也门占星", Icons.Filled.Star)
    data object KabbalahAstrology : Screen("divination/kabbalah", "犹太占星", Icons.Filled.AutoStories)
    data object Ifa : Screen("divination/ifa", "艾法预言", Icons.Filled.Extension)
    data object CelticTree : Screen("divination/celtic", "凯尔特树历", Icons.Filled.Explore)
    data object Palmistry : Screen("divination/palm", "手相", Icons.Filled.Explore)
    data object HermesAlchemy : Screen("divination/hermes", "赫尔墨斯·炼金术", Icons.Filled.Extension)
    data object MayaGalactic : Screen("divination/maya-galactic", "玛雅星系印记", Icons.Filled.Star)
    data object AztecAstrology : Screen("divination/aztec", "阿兹特克占星", Icons.Filled.Explore)
    data object MedicineWheel : Screen("divination/medicinewheel", "北美药轮", Icons.Filled.Extension)
    data object Physiognomy : Screen("divination/physiognomy", "相术", Icons.Filled.Explore)
    data object Nameology : Screen("divination/nameology", "姓名学", Icons.Filled.Casino)
    data object CrystalBall : Screen("divination/crystalball", "水晶球", Icons.Filled.Star)
    data object TaiYi : Screen("divination/taiyi", "太乙神数", Icons.Filled.AutoStories)
    data object LiuRen : Screen("divination/liuren", "大六壬", Icons.Filled.Extension)
    data object Prasna : Screen("divination/prasna", "普拉萨那", Icons.Filled.Star)
    data object IChingCast : Screen("divination/iching", "易经六爻占", Icons.Filled.Casino)
    data object LotDraw : Screen("divination/lot/{system}", "抽签占卜", Icons.Filled.Casino)
    data object TwentyEightMansions : Screen("divination/ershiba", "二十八宿", Icons.Filled.Explore)
}

/**
 * 底部 5 个主 tab 用「常驻 + 透明度交叉淡入」实现，而不是 NavHost 的销毁/重建跳转。
 * 这样切 tab 时屏幕早已 compose 完毕，只做 alpha 动画（纯 GPU，不触发布局/重算），
 * 彻底消除「切回东方/西方时整屏重新构建导致的卡顿」。占卜 tab 内部保留独立的 NavHost 处理子页跳转。
 */
@Composable
fun XuanjiApp() {
    // 今日、排盘、探索、记录、我的；运势体系切换收在「今日」页内。
    var selectedTab by rememberSaveable { mutableStateOf(Screen.Today.route) }
    val items = listOf(
        Screen.Today, Screen.Charts, Screen.Explore, Screen.History, Screen.Profile
    )
    val immersiveStageOpen = remember { mutableStateOf(false) }
    val mysticGuideVisible = remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        AppModule.repository.mysticGuideEnabledFlow.collect { enabled ->
            mysticGuideVisible.value = enabled
        }
    }

    CompositionLocalProvider(
        LocalImmersiveStageOpen provides immersiveStageOpen,
        LocalMysticGuideVisible provides mysticGuideVisible
    ) {
        Scaffold(
            // 底栏与页面同色，去掉 M3 默认的深灰容器，
            // 让底部看起来是一整块连续的深色，不再出现一条色带断层。
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (!immersiveStageOpen.value) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.background,
                        tonalElevation = 0.dp
                    ) {
                        items.forEach { screen ->
                            NavigationBarItem(
                                icon = { Icon(screen.icon, contentDescription = screen.label) },
                                label = { Text(screen.label) },
                                selected = selectedTab == screen.route,
                                onClick = { selectedTab = screen.route },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFFD9C27E),
                                    selectedTextColor = Color(0xFFD9C27E),
                                    indicatorColor = Color.Transparent,
                                    unselectedIconColor = Color(0xFFB7ABC8),
                                    unselectedTextColor = Color(0xFFB7ABC8)
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                Modifier.fillMaxSize().padding(
                    if (immersiveStageOpen.value) PaddingValues(0.dp) else innerPadding
                )
            ) {
                KeepAliveTab(active = selectedTab == Screen.Today.route) { TodayFortuneHub() }
                KeepAliveTab(active = selectedTab == Screen.Charts.route) { LifetimeChartHub() }
                KeepAliveTab(active = selectedTab == Screen.Explore.route) {
                    ExploreHub(onOpenToday = { selectedTab = Screen.Today.route })
                }
                KeepAliveTab(active = selectedTab == Screen.History.route) { HistoryScreen() }
                KeepAliveTab(active = selectedTab == Screen.Profile.route) { ProfileScreen() }
            }
        }
    }
}

@Composable
private fun TodayFortuneHub() {
    var selectedModeName by rememberSaveable { mutableStateOf(TodayFortuneMode.Composite.name) }
    val selectedMode = TodayFortuneMode.entries.firstOrNull { it.name == selectedModeName }
        ?: TodayFortuneMode.Composite
    val onSelectMode: (TodayFortuneMode) -> Unit = { selectedModeName = it.name }

    when (selectedMode) {
        TodayFortuneMode.Composite -> CompositeFortuneScreen(onTodayModeChange = onSelectMode)
        TodayFortuneMode.Eastern -> EasternScreen(onTodayModeChange = onSelectMode)
        TodayFortuneMode.Western -> WesternScreen(onTodayModeChange = onSelectMode)
    }
}

@Composable
private fun LifetimeChartHub() {
    var selectedChart by rememberSaveable { mutableStateOf("eastern") }
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("一生命盘", style = MaterialTheme.typography.headlineSmall)
            Text(
                "出生盘与长期结构；今日、周、月、年运势请在「今日」查看。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedChart == "eastern",
                    onClick = { selectedChart = "eastern" },
                    label = { Text("东方本命") }
                )
                FilterChip(
                    selected = selectedChart == "western",
                    onClick = { selectedChart = "western" },
                    label = { Text("西方本命") }
                )
            }
        }
        Box(Modifier.weight(1f)) {
            if (selectedChart == "eastern") EasternScreen(chartOnly = true)
            else WesternScreen(chartOnly = true)
        }
    }
}

@Composable
private fun ExploreHub(onOpenToday: () -> Unit) {
    var selectedSection by rememberSaveable { mutableStateOf("divination") }
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("探索", style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("divination" to "占卜体系", "tests" to "心理测试", "games" to "角色游戏")
                    .forEach { (key, label) ->
                        FilterChip(
                            selected = selectedSection == key,
                            onClick = { selectedSection = key },
                            label = { Text(label) }
                        )
                    }
            }
        }
        Box(Modifier.weight(1f)) {
            when (selectedSection) {
                "divination" -> DivinationRoot()
                "tests" -> TestHubScreen()
                else -> ExploreGamesHub(onOpenToday = onOpenToday)
            }
        }
    }
}

@Composable
private fun ExploreGamesHub(onOpenToday: () -> Unit) {
    val profile by AppModule.repository.userProfileFlow.collectAsStateWithLifecycle(initialValue = null)
    val profileKey = profile?.let(AppModule.actionRepository::profileKey).orEmpty()
    var activeGameId by rememberSaveable { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("四位来客各有一款互动玩法。进度在本机按角色保存。", style = MaterialTheme.typography.bodyMedium)
        if (activeGameId.isNotBlank() && activeGameId != "xiangqi") {
            val game = CompanionGameCatalog.byId(activeGameId)
            game?.let {
                CompanionGameCard(
                    gameId = it.id,
                    characterId = it.characterId,
                    profileKey = profileKey,
                    onClose = { activeGameId = "" }
                )
            }
        } else if (activeGameId == "xiangqi") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.large
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("中国象棋 · 墨衡", style = MaterialTheme.typography.titleMedium)
                    Text("棋局使用真实象棋规则与存档。请先进入「今日」页，唤起人物舞台并切换到墨衡，再选择「来一盘象棋」。", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onOpenToday) { Text("前往今日") }
                        OutlinedButton(onClick = { activeGameId = "" }) { Text("返回游戏列表") }
                    }
                }
            }
        }

        CompanionGameCatalog.all.forEach { game ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(game.title, style = MaterialTheme.typography.titleMedium)
                    Text(game.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(
                        onClick = { activeGameId = game.id },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (game.id == "xiangqi") "查看象棋入口" else "开始 / 继续")
                    }
                }
            }
        }
    }
}

/**
 * 常驻容器：无论 active 与否都保持内容 compose（不销毁），
 * 仅用 alpha 控制可见性，active 用 zIndex 置于顶层以保证点击命中。
 */
@Composable
private fun KeepAliveTab(active: Boolean, content: @Composable () -> Unit) {
    val alpha by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(180, easing = FastOutSlowInEasing)
    )
    Box(
        Modifier
            .fillMaxSize()
            .zIndex(if (active) 1f else 0f)
            .graphicsLayer { this.alpha = alpha }
    ) {
        content()
    }
}

/**
 * 占卜主 tab：内部独立的 NavHost，承载枢纽与所有子体系页。
 * 因外层 KeepAliveTab 始终 compose，本 NavHost 的状态（所在子页）在切走再切回时得以保留。
 */
@Composable
private fun DivinationRoot() {
    val nav = rememberNavController()
    BackHandler(enabled = nav.previousBackStackEntry != null) {
        nav.popBackStack()
    }
    NavHost(
        navController = nav,
        startDestination = Screen.Divination.route,
        enterTransition = {
            fadeIn(tween(180)) + slideInHorizontally(
                initialOffsetX = { it / 10 },
                animationSpec = tween(180, easing = FastOutSlowInEasing)
            )
        },
        exitTransition = {
            fadeOut(tween(180)) + slideOutHorizontally(
                targetOffsetX = { -it / 10 },
                animationSpec = tween(180, easing = FastOutSlowInEasing)
            )
        },
        popEnterTransition = {
            fadeIn(tween(180)) + slideInHorizontally(
                initialOffsetX = { -it / 10 },
                animationSpec = tween(180, easing = FastOutSlowInEasing)
            )
        },
        popExitTransition = {
            fadeOut(tween(180)) + slideOutHorizontally(
                targetOffsetX = { it / 10 },
                animationSpec = tween(180, easing = FastOutSlowInEasing)
            )
        }
    ) {
        composable(Screen.Divination.route) {
            DivinationHub(onNavigate = { nav.navigate(it) })
        }
        composable(
            "divination/region/{key}",
            arguments = listOf(navArgument("key") { type = NavType.StringType })
        ) { backStack ->
            val key = backStack.arguments?.getString("key") ?: ""
            RegionScreen(
                regionKey = key,
                onNavigate = { nav.navigate(it) },
                onBack = { nav.popBackStack() }
            )
        }
        composable(
            "divination/subregion/{regionKey}/{subKey}",
            arguments = listOf(
                navArgument("regionKey") { type = NavType.StringType },
                navArgument("subKey") { type = NavType.StringType }
            )
        ) { backStack ->
            val regionKey = backStack.arguments?.getString("regionKey") ?: ""
            val subKey = backStack.arguments?.getString("subKey") ?: ""
            SubregionScreen(
                regionKey = regionKey,
                subKey = subKey,
                onNavigate = { nav.navigate(it) },
                onBack = { nav.popBackStack() }
            )
        }
        composable(
            "divination/group/{regionKey}/{groupKey}",
            arguments = listOf(
                navArgument("regionKey") { type = NavType.StringType },
                navArgument("groupKey") { type = NavType.StringType }
            )
        ) { backStack ->
            val regionKey = backStack.arguments?.getString("regionKey") ?: ""
            val groupKey = backStack.arguments?.getString("groupKey") ?: ""
            GroupScreen(
                regionKey = regionKey,
                groupKey = groupKey,
                onNavigate = { nav.navigate(it) },
                onBack = { nav.popBackStack() }
            )
        }
        composable(Screen.TodayOracle.route) { TodayOracleScreen() }
        composable(Screen.Tarot.route) { TarotScreen() }
        composable(Screen.Numerology.route) { NumerologyScreen() }
        composable(Screen.Vedic.route) { VedicScreen() }
        composable(Screen.Rune.route) { RuneScreen() }
        composable(Screen.Ziwei.route) { ZiweiScreen() }
        composable(Screen.LiuYao.route) { LiuYaoScreen() }
        composable(Screen.MeiHua.route) { MeiHuaScreen() }
        composable(Screen.QiMen.route) { QiMenScreen() }
        composable(Screen.QiZheng.route) { QiZhengScreen() }
        composable(Screen.FengShui.route) { FengShuiScreen() }
        composable(Screen.MayaTzolkin.route) { MayaTzolkinScreen() }
        composable(Screen.Lenormand.route) { LenormandScreen() }
        composable(Screen.Chakra.route) { ChakraScreen() }
        composable(
            "divination/classical/{tradition}",
            arguments = listOf(navArgument("tradition") { type = NavType.StringType })
        ) { backStack ->
            val tradition = backStack.arguments?.getString("tradition") ?: "all"
            ClassicalAstrologyScreen(initialTradition = tradition)
        }
        composable(Screen.HumanDesign.route) { HumanDesignScreen() }
        composable(Screen.LawOfAttraction.route) { LawOfAttractionScreen() }
        composable(Screen.ThirteenMoon.route) { ThirteenMoonScreen() }
        composable(Screen.NineStars.route) { NineStarsScreen() }
        composable(Screen.TibetanAstrology.route) { TibetanAstrologyScreen() }
        composable(Screen.Onmyodo.route) { OnmyodoScreen() }
        composable(Screen.Mahabote.route) { MahaboteScreen() }
        composable(Screen.KhmerAstrology.route) { KhmerAstrologyScreen() }
        composable(Screen.TajulMuluk.route) { TajulMulukScreen() }
        composable(Screen.NagaRain.route) { NagaRainScreen() }
        composable(Screen.NadiAstrology.route) { NadiAstrologyScreen() }
        composable(Screen.Vastu.route) { VastuScreen() }
        composable(Screen.BabylonianAstrology.route) { BabylonianAstrologyScreen() }
        composable(Screen.HellenisticAstrology.route) { HellenisticAstrologyScreen() }
        composable(Screen.ArabicAstrology.route) { ArabicAstrologyScreen() }
        composable(Screen.PersianAstrology.route) { PersianAstrologyScreen() }
        composable(Screen.YemeniAstrology.route) { YemeniAstrologyScreen() }
        composable(Screen.KabbalahAstrology.route) { KabbalahAstrologyScreen() }
        composable(Screen.Ifa.route) { IfaScreen() }
        composable(Screen.CelticTree.route) { CelticTreeCalendarScreen() }
        composable(Screen.Palmistry.route) { PalmistryScreen() }
        composable(Screen.HermesAlchemy.route) { HermesAlchemyScreen() }
        composable(Screen.MayaGalactic.route) { MayaGalacticScreen() }
        composable(Screen.AztecAstrology.route) { AztecAstrologyScreen() }
        composable(Screen.MedicineWheel.route) { MedicineWheelScreen() }
        composable(Screen.Physiognomy.route) { PhysiognomyScreen() }
        composable(Screen.Nameology.route) { NameologyScreen() }
        composable(Screen.CrystalBall.route) { CrystalBallScreen() }
        composable(Screen.TaiYi.route) { TaiYiScreen() }
        composable(Screen.LiuRen.route) { LiuRenScreen() }
        composable(Screen.Prasna.route) { PrasnaScreen() }
        composable(Screen.IChingCast.route) { IChingScreen() }
        composable(
            "divination/lot/{system}",
            arguments = listOf(navArgument("system") { type = NavType.StringType })
        ) { entry ->
            LotDrawScreen(systemKey = entry.arguments?.getString("system"))
        }
        composable(Screen.TwentyEightMansions.route) { TwentyEightMansionsScreen() }
        composable(
            "divination/ref/{key}",
            arguments = listOf(navArgument("key") { type = NavType.StringType })
        ) { backStack ->
            val key = backStack.arguments?.getString("key") ?: ""
            ReferenceScreen(key)
        }
    }
}
