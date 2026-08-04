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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
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

sealed class Screen(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    data object Eastern : Screen("eastern", "东方", Icons.Filled.AutoStories)
    data object Western : Screen("western", "西方", Icons.Filled.Star)
    data object Divination : Screen("divination", "占卜", Icons.Filled.Casino)
    data object History : Screen("history", "历史", Icons.Filled.History)
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
 * 底部 6 个主 tab 用「常驻 + 透明度交叉淡入」实现，而不是 NavHost 的销毁/重建跳转。
 * 这样切 tab 时屏幕早已 compose 完毕，只做 alpha 动画（纯 GPU，不触发布局/重算），
 * 彻底消除「切回东方/西方时整屏重新构建导致的卡顿」。占卜 tab 内部保留独立的 NavHost 处理子页跳转。
 */
@Composable
fun XuanjiApp() {
    // 综合作为首页（第一个 tab），其余按序排列
    var selectedTab by rememberSaveable { mutableStateOf(Screen.Composite.route) }
    val items = listOf(
        Screen.Composite, Screen.Eastern, Screen.Western,
        Screen.Divination, Screen.Test, Screen.History, Screen.Profile
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = selectedTab == screen.route,
                        onClick = { selectedTab = screen.route }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            KeepAliveTab(active = selectedTab == Screen.Composite.route) { CompositeFortuneScreen() }
            KeepAliveTab(active = selectedTab == Screen.Eastern.route) { EasternScreen() }
            KeepAliveTab(active = selectedTab == Screen.Western.route) { WesternScreen() }
            KeepAliveTab(active = selectedTab == Screen.Divination.route) { DivinationRoot() }
            KeepAliveTab(active = selectedTab == Screen.Test.route) { TestHubScreen() }
            KeepAliveTab(active = selectedTab == Screen.History.route) { HistoryScreen() }
            KeepAliveTab(active = selectedTab == Screen.Profile.route) { ProfileScreen() }
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
        composable(Screen.NineStars.route) { NineStarsScreen() }
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
