package com.xuanji.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.xuanji.app.di.AppModule
import com.xuanji.app.ui.StarSplashScreen
import com.xuanji.app.ui.XuanjiApp
import com.xuanji.app.ui.guide.GuideScreen
import com.xuanji.app.ui.theme.XuanjiTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 开屏页：必须在 setContent 之前调用
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // 全屏幕沉浸式：内容延伸到状态栏与导航栏下方，
        // 深色底 + 浅色图标由 Scaffold 统一分发 insets，页面不再出现多余空带。
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            XuanjiTheme {
                val scope = rememberCoroutineScope()
                // 启动动画页（五芒星闪耀 → 显现"玄星"）
                var splashDone by remember { mutableStateOf(false) }
                // 首次启动展示引导页，之后进入主界面
                var guideSeen by remember { mutableStateOf<Boolean?>(null) }
                LaunchedEffect(splashDone) {
                    if (splashDone) {
                        guideSeen = AppModule.repository.isGuideSeen()
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        !splashDone -> StarSplashScreen(onDone = { splashDone = true })
                        guideSeen == null -> { /* 开屏仍覆盖，等待引导状态 */ }
                        guideSeen == false -> GuideScreen(
                            onFinish = {
                                scope.launch { AppModule.repository.setGuideSeen() }
                                guideSeen = true
                            }
                        )
                        else -> XuanjiApp()
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    LaunchedEffect(Unit) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
        }
    }
}
