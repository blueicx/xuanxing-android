package com.xuanji.app.ui.divination

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xuanji.app.domain.divination.DEFAULT_FEATURES
import com.xuanji.app.domain.divination.FEATURE_LABELS
import com.xuanji.app.domain.divination.FEATURE_OPTIONS
import com.xuanji.app.domain.divination.Palmistry
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation

// ── 手形外轮廓（Catmull-Rom 平滑闭合曲线，384 点，与 Python 验证图完全一致）──
private val HAND_OUTLINE = listOf(
    0.46f to 0.95f, 0.4512f to 0.9495f, 0.4424f to 0.9492f, 0.4335f to 0.9491f,
    0.4245f to 0.949f, 0.4156f to 0.9489f, 0.4066f to 0.9488f, 0.3977f to 0.9486f,
    0.3887f to 0.9481f, 0.3799f to 0.9474f, 0.371f to 0.9464f, 0.3623f to 0.945f,
    0.3536f to 0.9432f, 0.345f to 0.9408f, 0.3365f to 0.9379f, 0.3282f to 0.9343f,
    0.32f to 0.93f, 0.3119f to 0.925f, 0.3037f to 0.9194f, 0.2955f to 0.9131f,
    0.2874f to 0.9064f, 0.2793f to 0.8992f, 0.2713f to 0.8915f, 0.2634f to 0.8834f,
    0.2556f to 0.875f, 0.248f to 0.8663f, 0.2405f to 0.8572f, 0.2331f to 0.848f,
    0.226f to 0.8386f, 0.2191f to 0.829f, 0.2125f to 0.8194f, 0.2061f to 0.8097f,
    0.20f to 0.80f, 0.194f to 0.79f, 0.188f to 0.7793f, 0.1821f to 0.7682f,
    0.1762f to 0.7566f, 0.1704f to 0.7448f, 0.1648f to 0.7326f, 0.1595f to 0.7204f,
    0.1544f to 0.7081f, 0.1496f to 0.6959f, 0.1453f to 0.6838f, 0.1413f to 0.672f,
    0.1379f to 0.6605f, 0.135f to 0.6495f, 0.1327f to 0.639f, 0.131f to 0.6291f,
    0.13f to 0.62f, 0.1298f to 0.6114f, 0.1303f to 0.6031f, 0.1315f to 0.5952f,
    0.1334f to 0.5875f, 0.1358f to 0.5802f, 0.1386f to 0.5731f, 0.142f to 0.5664f,
    0.1456f to 0.56f, 0.1496f to 0.5539f, 0.1538f to 0.5481f, 0.1581f to 0.5427f,
    0.1626f to 0.5375f, 0.1671f to 0.5327f, 0.1715f to 0.5281f, 0.1758f to 0.5239f,
    0.18f to 0.52f, 0.1843f to 0.517f, 0.189f to 0.5153f, 0.1941f to 0.5147f,
    0.1995f to 0.515f, 0.2051f to 0.5159f, 0.2109f to 0.5172f, 0.2167f to 0.5186f,
    0.2225f to 0.52f, 0.2282f to 0.5211f, 0.2338f to 0.5216f, 0.2392f to 0.5213f,
    0.2442f to 0.52f, 0.2489f to 0.5175f, 0.2531f to 0.5134f, 0.2569f to 0.5077f,
    0.26f to 0.50f, 0.2625f to 0.4899f, 0.2643f to 0.4773f, 0.2655f to 0.4627f,
    0.2662f to 0.4462f, 0.2666f to 0.4284f, 0.2667f to 0.4095f, 0.2665f to 0.3899f,
    0.2663f to 0.37f, 0.2659f to 0.3501f, 0.2657f to 0.3305f, 0.2655f to 0.3116f,
    0.2656f to 0.2938f, 0.266f to 0.2773f, 0.2668f to 0.2627f, 0.2681f to 0.2501f,
    0.27f to 0.24f, 0.2725f to 0.2316f, 0.2754f to 0.2241f, 0.2789f to 0.2174f,
    0.2827f to 0.2116f, 0.2867f to 0.2066f, 0.291f to 0.2026f, 0.2955f to 0.1996f,
    0.30f to 0.1975f, 0.3045f to 0.1965f, 0.309f to 0.1964f, 0.3133f to 0.1975f,
    0.3173f to 0.1997f, 0.3211f to 0.203f, 0.3246f to 0.2075f, 0.3275f to 0.2131f,
    0.33f to 0.22f, 0.332f to 0.2293f, 0.3336f to 0.242f, 0.3348f to 0.2574f,
    0.3358f to 0.2752f, 0.3365f to 0.2947f, 0.337f to 0.3154f, 0.3373f to 0.337f,
    0.3375f to 0.3587f, 0.3376f to 0.3802f, 0.3377f to 0.401f, 0.3378f to 0.4204f,
    0.338f to 0.438f, 0.3382f to 0.4532f, 0.3386f to 0.4657f, 0.3392f to 0.4748f,
    0.34f to 0.48f, 0.3408f to 0.4816f, 0.3415f to 0.4805f, 0.3421f to 0.4769f,
    0.3427f to 0.4709f, 0.3432f to 0.463f, 0.3437f to 0.4534f, 0.3443f to 0.4423f,
    0.345f to 0.43f, 0.3459f to 0.4168f, 0.3469f to 0.4029f, 0.3482f to 0.3885f,
    0.3498f to 0.3741f, 0.3518f to 0.3597f, 0.3541f to 0.3457f, 0.3568f to 0.3324f,
    0.36f to 0.32f, 0.3638f to 0.3075f, 0.3681f to 0.2937f, 0.373f to 0.279f,
    0.3783f to 0.2636f, 0.384f to 0.2476f, 0.3899f to 0.2313f, 0.3961f to 0.215f,
    0.4025f to 0.1987f, 0.4089f to 0.1829f, 0.4154f to 0.1676f, 0.4217f to 0.153f,
    0.428f to 0.1395f, 0.434f to 0.1273f, 0.4397f to 0.1164f, 0.4451f to 0.1073f,
    0.45f to 0.10f, 0.4547f to 0.0943f, 0.4593f to 0.0896f, 0.464f to 0.086f,
    0.4685f to 0.0834f, 0.473f to 0.0818f, 0.4773f to 0.0811f, 0.4816f to 0.0814f,
    0.4856f to 0.0825f, 0.4895f to 0.0845f, 0.4932f to 0.0873f, 0.4967f to 0.0909f,
    0.4999f to 0.0953f, 0.5029f to 0.1004f, 0.5056f to 0.1063f, 0.508f to 0.1128f,
    0.51f to 0.12f, 0.5116f to 0.1288f, 0.5127f to 0.14f, 0.5134f to 0.1532f,
    0.5137f to 0.168f, 0.5138f to 0.1841f, 0.5135f to 0.201f, 0.5131f to 0.2186f,
    0.5125f to 0.2363f, 0.5118f to 0.2537f, 0.5112f to 0.2707f, 0.5105f to 0.2867f,
    0.51f to 0.3014f, 0.5096f to 0.3145f, 0.5095f to 0.3255f, 0.5096f to 0.3341f,
    0.51f to 0.34f, 0.5106f to 0.3433f, 0.5112f to 0.3447f, 0.5118f to 0.3443f,
    0.5124f to 0.3423f, 0.5131f to 0.3388f, 0.5138f to 0.3342f, 0.5147f to 0.3285f,
    0.5156f to 0.3219f, 0.5167f to 0.3146f, 0.518f to 0.3068f, 0.5194f to 0.2988f,
    0.521f to 0.2905f, 0.5229f to 0.2824f, 0.525f to 0.2744f, 0.5273f to 0.2669f,
    0.53f to 0.26f, 0.533f to 0.2529f, 0.5365f to 0.245f, 0.5403f to 0.2363f,
    0.5445f to 0.227f, 0.5489f to 0.2174f, 0.5535f to 0.2074f, 0.5582f to 0.1974f,
    0.5631f to 0.1875f, 0.5681f to 0.1778f, 0.573f to 0.1685f, 0.5779f to 0.1597f,
    0.5827f to 0.1517f, 0.5874f to 0.1446f, 0.5919f to 0.1385f, 0.5961f to 0.1335f,
    0.60f to 0.13f, 0.6038f to 0.1276f, 0.6075f to 0.1258f, 0.6113f to 0.1248f,
    0.6151f to 0.1245f, 0.6188f to 0.1249f, 0.6224f to 0.126f, 0.626f to 0.1277f,
    0.6294f to 0.13f, 0.6327f to 0.133f, 0.6358f to 0.1365f, 0.6387f to 0.1407f,
    0.6415f to 0.1455f, 0.644f to 0.1508f, 0.6463f to 0.1567f, 0.6483f to 0.1631f,
    0.65f to 0.17f, 0.6513f to 0.1782f, 0.6523f to 0.1881f, 0.6528f to 0.1996f,
    0.653f to 0.2123f, 0.653f to 0.226f, 0.6528f to 0.2404f, 0.6524f to 0.2551f,
    0.6519f to 0.27f, 0.6513f to 0.2847f, 0.6507f to 0.299f, 0.6502f to 0.3126f,
    0.6498f to 0.3252f, 0.6495f to 0.3365f, 0.6494f to 0.3462f, 0.6495f to 0.3542f,
    0.65f to 0.36f, 0.6506f to 0.3639f, 0.6511f to 0.3663f, 0.6517f to 0.3673f,
    0.6522f to 0.3671f, 0.6528f to 0.3658f, 0.6534f to 0.3636f, 0.6541f to 0.3606f,
    0.655f to 0.3569f, 0.656f to 0.3527f, 0.6572f to 0.348f, 0.6586f to 0.3432f,
    0.6603f to 0.3382f, 0.6623f to 0.3333f, 0.6645f to 0.3285f, 0.6671f to 0.324f,
    0.67f to 0.32f, 0.6734f to 0.3158f, 0.6773f to 0.311f, 0.6817f to 0.3055f,
    0.6865f to 0.2995f, 0.6916f to 0.2933f, 0.6969f to 0.2868f, 0.7025f to 0.2802f,
    0.7081f to 0.2737f, 0.7138f to 0.2675f, 0.7196f to 0.2615f, 0.7252f to 0.256f,
    0.7307f to 0.2511f, 0.736f to 0.2469f, 0.741f to 0.2436f, 0.7457f to 0.2412f,
    0.75f to 0.24f, 0.7541f to 0.2396f, 0.7581f to 0.2398f, 0.7621f to 0.2406f,
    0.766f to 0.2419f, 0.7699f to 0.2437f, 0.7736f to 0.2461f, 0.7772f to 0.249f,
    0.7806f to 0.2525f, 0.7839f to 0.2565f, 0.787f to 0.2611f, 0.7898f to 0.2662f,
    0.7924f to 0.2719f, 0.7948f to 0.2781f, 0.7968f to 0.2848f, 0.7986f to 0.2921f,
    0.80f to 0.30f, 0.801f to 0.3087f, 0.8017f to 0.3183f, 0.802f to 0.3288f,
    0.802f to 0.3402f, 0.8016f to 0.3522f, 0.8011f to 0.3647f, 0.8003f to 0.3778f,
    0.7994f to 0.3912f, 0.7983f to 0.405f, 0.7971f to 0.4188f, 0.7959f to 0.4328f,
    0.7946f to 0.4467f, 0.7934f to 0.4605f, 0.7921f to 0.474f, 0.791f to 0.4872f,
    0.79f to 0.50f, 0.7891f to 0.5125f, 0.7883f to 0.525f, 0.7874f to 0.5375f,
    0.7866f to 0.55f, 0.7858f to 0.5625f, 0.785f to 0.575f, 0.7841f to 0.5875f,
    0.7831f to 0.60f, 0.7821f to 0.6125f, 0.7809f to 0.625f, 0.7795f to 0.6375f,
    0.778f to 0.65f, 0.7764f to 0.6625f, 0.7745f to 0.675f, 0.7724f to 0.6875f,
    0.77f to 0.70f, 0.7674f to 0.7128f, 0.7648f to 0.726f, 0.7619f to 0.7396f,
    0.759f to 0.7535f, 0.7559f to 0.7675f, 0.7526f to 0.7816f, 0.7492f to 0.7956f,
    0.7456f to 0.8094f, 0.7419f to 0.8229f, 0.7379f to 0.836f, 0.7338f to 0.8486f,
    0.7295f to 0.8605f, 0.7249f to 0.8718f, 0.7202f to 0.8822f, 0.7152f to 0.8916f,
    0.71f to 0.90f, 0.7046f to 0.9073f, 0.6988f to 0.9138f, 0.6929f to 0.9194f,
    0.6867f to 0.9242f, 0.6803f to 0.9284f, 0.6737f to 0.9319f, 0.667f to 0.9349f,
    0.66f to 0.9375f, 0.6529f to 0.9397f, 0.6456f to 0.9415f, 0.6383f to 0.9431f,
    0.6308f to 0.9445f, 0.6232f to 0.9459f, 0.6155f to 0.9472f, 0.6078f to 0.9485f,
    0.60f to 0.95f, 0.5921f to 0.9514f, 0.5839f to 0.9525f, 0.5756f to 0.9534f,
    0.5671f to 0.954f, 0.5585f to 0.9544f, 0.5497f to 0.9545f, 0.5408f to 0.9545f,
    0.5319f to 0.9544f, 0.5229f to 0.9541f, 0.5138f to 0.9537f, 0.5048f to 0.9532f,
    0.4957f to 0.9526f, 0.4867f to 0.952f, 0.4777f to 0.9513f, 0.4688f to 0.9506f
)

// ── 掌纹标注数据（Python 视觉验证过的坐标）──
private data class PalmAnnotation(
    val number: Int,
    val name: String,
    val desc: String,
    val badgeAnchor: Pair<Float, Float>,
    val leaderEnd: Pair<Float, Float>,
    val linePoints: List<Pair<Float, Float>>
)

private val PALM_ANNOTATIONS = listOf(
    PalmAnnotation(
        number = 1, name = "生命线",
        desc = "揭示了构成生命基础的生命力、健康和生活方式等。",
        badgeAnchor = 0.10f to 0.86f,
        leaderEnd = 0.23f to 0.80f,
        linePoints = listOf(
            0.30f to 0.56f, 0.27f to 0.60f, 0.24f to 0.66f,
            0.21f to 0.74f, 0.20f to 0.82f, 0.22f to 0.88f, 0.24f to 0.92f
        )
    ),
    PalmAnnotation(
        number = 2, name = "头脑线",
        desc = "揭示了价值观、思考方式、思考倾向等。",
        badgeAnchor = 0.08f to 0.62f,
        leaderEnd = 0.31f to 0.63f,
        linePoints = listOf(
            0.30f to 0.63f, 0.43f to 0.62f, 0.55f to 0.62f,
            0.66f to 0.63f, 0.74f to 0.65f
        )
    ),
    PalmAnnotation(
        number = 3, name = "感情线",
        desc = "揭示了内心的感情与性格、情绪的丰富性。",
        badgeAnchor = 0.92f to 0.54f,
        leaderEnd = 0.73f to 0.56f,
        linePoints = listOf(
            0.29f to 0.54f, 0.42f to 0.51f, 0.55f to 0.51f,
            0.68f to 0.53f, 0.75f to 0.56f
        )
    ),
    PalmAnnotation(
        number = 4, name = "命运线",
        desc = "揭示的是生活态度、精神状况、命运的趋势。",
        badgeAnchor = 0.90f to 0.80f,
        leaderEnd = 0.48f to 0.70f,
        linePoints = listOf(
            0.48f to 0.90f, 0.48f to 0.78f, 0.48f to 0.64f, 0.48f to 0.52f
        )
    )
)

@Composable
fun PalmistryScreen() {
    val selected = remember { mutableStateMapOf<String, String>().apply { putAll(DEFAULT_FEATURES) } }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("欧洲手相学", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text("根据手掌形状、手指比例、拇指大小与主要掌纹（生命线、头脑线、感情线、命运线等）推断性格与人生倾向。",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        FortuneCard {
            SectionTitle("掌纹参考图")
            Spacer(Modifier.height(8.dp))
            PalmDiagram()
            Spacer(Modifier.height(12.dp))
            PalmLegend()
        }

        FortuneCard {
            SectionTitle("逐项特征 · 点击查看解读")
            Spacer(Modifier.height(8.dp))
            FEATURE_LABELS.keys.forEach { key ->
                val label = FEATURE_LABELS[key] ?: key
                val chosen = selected[key] ?: DEFAULT_FEATURES[key] ?: ""
                Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(4.dp))
                (FEATURE_OPTIONS[key] ?: emptyList()).forEach { option ->
                    FilterChip(selected = chosen == option, onClick = { selected[key] = option }, label = { Text(option) },
                        modifier = Modifier.padding(vertical = 3.dp))
                    Spacer(Modifier.height(3.dp))
                }
                val interp = Palmistry.interpretFeature(key, chosen)
                if (interp.isNotEmpty()) Text("· $interp", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 10.dp))
            }
        }

        val report = Palmistry.generate(selected.toMap())
        FortuneCard { SectionTitle("综合解读"); Spacer(Modifier.height(8.dp)); Text(report.summary, style = MaterialTheme.typography.bodyMedium) }
        SystemExplanation("palm")
    }
}

/** APP 风格掌纹示意图：深色底 + 金色手形轮廓 + 四条主掌纹 + 编号圆圈 + 引线 */
@Composable
private fun PalmDiagram() {
    val gold = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    val dimGold = gold.copy(alpha = 0.35f)

    BoxWithConstraints(
        Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(14.dp)).background(surface)
    ) {
        val w = maxWidth; val h = maxHeight

        Canvas(Modifier.fillMaxSize()) {
            val cw = size.width; val ch = size.height

            // 手形轮廓（Catmull-Rom 密集点连线，与 Python 验证图一致）
            val handPath = Path().apply {
                val first = HAND_OUTLINE.first()
                moveTo(first.first * cw, first.second * ch)
                HAND_OUTLINE.drop(1).forEach { (fx, fy) -> lineTo(fx * cw, fy * ch) }
                close()
            }
            drawPath(handPath, gold.copy(alpha = 0.06f))
            drawPath(handPath, dimGold, style = Stroke(width = 1.8.dp.toPx()))

            // 四条主掌纹
            PALM_ANNOTATIONS.forEach { ann ->
                if (ann.linePoints.size >= 2) {
                    val lp = Path().apply {
                        val f = ann.linePoints.first(); moveTo(f.first * cw, f.second * ch)
                        ann.linePoints.drop(1).forEach { (fx, fy) -> lineTo(fx * cw, fy * ch) }
                    }
                    drawPath(lp, gold.copy(alpha = 0.18f), style = Stroke(width = 5.dp.toPx()))
                    drawPath(lp, gold, style = Stroke(width = 2.2.dp.toPx()))
                }
                // 虚线引线
                drawLine(dimGold,
                    Offset(ann.badgeAnchor.first * cw, ann.badgeAnchor.second * ch),
                    Offset(ann.leaderEnd.first * cw, ann.leaderEnd.second * ch),
                    strokeWidth = 1.3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f), 0f))
            }
        }

        // 编号圆圈（图上）
        PALM_ANNOTATIONS.forEach { ann ->
            Box(modifier = Modifier
                .offset(w * ann.badgeAnchor.first, h * ann.badgeAnchor.second)
                .background(gold, CircleShape).border(1.5.dp, gold.copy(alpha = 0.5f), CircleShape).padding(5.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) { Text("${ann.number}", color = surface, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
        }
    }
}

/** 图下说明列表：编号 + 名称 + 描述 */
@Composable
private fun PalmLegend() {
    val gold = MaterialTheme.colorScheme.primary; val onSurface = MaterialTheme.colorScheme.onSurface

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PALM_ANNOTATIONS.forEach { ann ->
            Row(verticalAlignment = androidx.compose.ui.Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.background(gold, CircleShape).border(1.5.dp, gold.copy(alpha = 0.5f), CircleShape).padding(6.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) { Text("${ann.number}", color = MaterialTheme.colorScheme.surface, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                Column {
                    Text(ann.name, color = gold, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    Text(ann.desc, color = onSurface.copy(alpha = 0.78f), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}
