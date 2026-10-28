package com.carpet.app.ui

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import com.carpet.app.ui.theme.Type
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Path
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.carpet.app.domain.Emotion
import com.carpet.app.domain.GrowthStage
import com.carpet.app.domain.Species
import com.carpet.app.ui.theme.Palette
import kotlinx.coroutines.delay
import kotlin.random.Random

/** Background scene behind the character; follows the mood. */
enum class Scene(val key: String) { DAY("day"), RAIN("rain"), CHARGE("charge"), GARAGE("garage"), NURSERY("nursery") }

fun sceneFor(emotion: Emotion): Scene = when (emotion) {
    Emotion.HUNGRY -> Scene.CHARGE
    Emotion.SICK -> Scene.GARAGE
    Emotion.RAINY -> Scene.RAIN
    Emotion.CALM, Emotion.GLAD -> Scene.DAY
}

/**
 * Rounded card with a scene illustration; [content] stands on its ground line.
 * The ground sits at 88% of the card height, matching the road/floor in the scenes.
 */
@Composable
fun StageCard(scene: Scene, modifier: Modifier = Modifier, content: @Composable (ground: androidx.compose.ui.unit.Dp, maxSize: androidx.compose.ui.unit.Dp) -> Unit) {
    val context = LocalContext.current
    BoxWithConstraints(
        modifier
            .clip(RoundedCornerShape(32.dp))
            .background(Brush.verticalGradient(listOf(Palette.Mist, Palette.White))),
    ) {
        Crossfade(targetState = scene, animationSpec = tween(500), label = "scene") { s ->
            val bg = remember(s) { drawableId(context, "bg_${s.key}") }
            if (bg != null) {
                Image(
                    painter = painterResource(bg),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        val ground = maxHeight * 0.12f
        // Character box: as large as fits under the sky, square.
        val size = min(maxWidth * 0.92f, maxHeight * 0.80f)
        content(ground, size)
    }
}

/**
 * The character. Images come from the Codex-made art (res/drawable-nodpi):
 *   pet_<species>_<stage>_<emotion>  — best match
 *   pet_<species>_youth_<emotion>    — the grown-up drawing, shrunk when the stage has none
 *   pet_<species>_youth_calm         — any species before its expressions exist
 *   pet_<species>_<stage>_blink      — eyes-closed frame swapped in for a blink
 * [sizeRatio] is the drawn height relative to a grown-up (see Growth.sizeRatio), so the friend
 * gets a little bigger with every drive, not only when the stage changes.
 * Breathing, bouncing and wobbling are done in code.
 */
@Composable
fun CarPetCharacter(
    species: Species,
    emotion: Emotion,
    stage: GrowthStage,
    modifier: Modifier = Modifier,
    sizeRatio: Float = defaultSize(stage),
    colorFilter: ColorFilter? = null,
) {
    val context = LocalContext.current
    val art = remember(species, emotion, stage) { resolveSprite(context, species, emotion, stage) }
    val sprite = art.copy(scale = sizeRatio / artHeight(art.stage))
    val blinkRes = remember(species, art.stage) { drawableId(context, "pet_${art.species.key}_${art.stage.key}_blink") }

    val t = rememberInfiniteTransition(label = "pet")
    val breath by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breath")
    val bounce by t.animateFloat(0f, 1f, infiniteRepeatable(tween(480, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bounce")
    val wobble by t.animateFloat(-1f, 1f, infiniteRepeatable(tween(260, easing = LinearEasing), RepeatMode.Reverse), label = "wobble")
    val sway by t.animateFloat(-1f, 1f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "sway")
    val rain by t.animateFloat(0f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "rain")
    val liftPx = with(LocalDensity.current) { 14.dp.toPx() }

    var blinking by remember { mutableStateOf(false) }
    LaunchedEffect(emotion, blinkRes) {
        blinking = false
        if (emotion != Emotion.CALM || blinkRes == null) return@LaunchedEffect
        while (true) {
            delay(Random.nextLong(2_600, 4_800))
            blinking = true
            delay(140)
            blinking = false
        }
    }

    Box(
        modifier
            .aspectRatio(1f)
            .semantics { contentDescription = "${species.label}, ${stage.label}, ${emotion.label}" },
        contentAlignment = Alignment.BottomCenter,
    ) {
        // Soft contact shadow on the ground
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width * 0.46f * sizeRatio
            drawOval(
                color = Palette.Heading.copy(alpha = 0.10f),
                topLeft = Offset((size.width - w) / 2f, size.height * 0.955f),
                size = androidx.compose.ui.geometry.Size(w, size.height * 0.05f),
            )
        }
        Crossfade(targetState = art, animationSpec = tween(220), label = "sprite") { s ->
            val res = if (blinking && s.emotion == Emotion.CALM && blinkRes != null) blinkRes else s.res
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                Image(
                    painter = painterResource(res),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.BottomCenter,
                    colorFilter = colorFilter,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0.5f, 1f)
                            val squash = if (s.emotion == Emotion.HUNGRY) 0.98f else 1f
                            scaleY = sprite.scale * squash * (1f + 0.018f * breath)
                            scaleX = sprite.scale * (1f - 0.006f * breath)
                            translationY = if (s.emotion == Emotion.GLAD) -liftPx * bounce else 0f
                            rotationZ = when (s.emotion) {
                                Emotion.SICK -> 2f * wobble
                                Emotion.HUNGRY -> 1.2f * sway
                                else -> 0f
                            }
                        },
                )
            }
        }
        if (emotion == Emotion.RAINY) {
            Canvas(Modifier.fillMaxSize()) {
                val u = size.width / 100f
                for ((i, x) in listOf(8f, 20f, 80f, 92f).withIndex()) {
                    val y = (10f + ((rain + i * 0.27f) % 1f) * 72f) * size.height / 100f
                    drawLine(Palette.Rain, Offset(x * u, y), Offset(x * u - 1.2f * u, y + 4f * u), 1.4f * u, StrokeCap.Round)
                }
            }
        }
    }
}

/** Phases of the egg on the draw screen. */
enum class EggPhase { IDLE, SHAKING, CRACKING }

/** The egg: rocks gently, shakes hard, then cracks with light. */
@Composable
fun EggView(phase: EggPhase, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val egg = remember { drawableId(context, "pet_egg") }
    val crack = remember { drawableId(context, "pet_egg_crack") }
    val t = rememberInfiniteTransition(label = "egg")
    val slow by t.animateFloat(-1f, 1f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "slow")
    val fast by t.animateFloat(-1f, 1f, infiniteRepeatable(tween(110, easing = LinearEasing), RepeatMode.Reverse), label = "fast")
    val res = if (phase == EggPhase.CRACKING && crack != null) crack else egg
    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.BottomCenter) {
        if (res != null) {
            Crossfade(targetState = res, animationSpec = tween(160), label = "egg") { r ->
                Image(
                    painter = painterResource(r),
                    contentDescription = if (phase == EggPhase.CRACKING) "알이 깨지는 중" else "알",
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.BottomCenter,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0.5f, 1f)
                            rotationZ = when (phase) {
                                EggPhase.IDLE -> 2.5f * slow
                                EggPhase.SHAKING -> 6f * fast
                                EggPhase.CRACKING -> 0f
                            }
                        },
                )
            }
        }
    }
}

/** Places [content] (a square box of [size]) standing on the stage card's ground. */
@Composable
fun OnGround(ground: androidx.compose.ui.unit.Dp, size: androidx.compose.ui.unit.Dp, content: @Composable (Modifier) -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        content(Modifier.size(size).offset(y = -ground))
    }
}

/** [stage] and [species] say which drawing was found, which may differ from the ones asked for. */
/** Height the grown-up drawing fills in its square box, feet to ears (see prepare_hd_assets.py). */
private const val ART_FILL = 0.95f

/**
 * What the friend says, in an opaque bubble just above its head. The bubble follows the
 * drawing's current size, so it stays close to a baby and clears a grown-up's ears.
 */
@Composable
fun HeadBubble(text: String, ground: Dp, size: Dp, sizeRatio: Float) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val headTop = maxHeight - ground - size * (ART_FILL * sizeRatio)
        val room = (headTop - 10.dp).coerceIn(140.dp, maxHeight)
        Box(
            Modifier
                .fillMaxWidth()
                .height(room)
                .padding(start = 20.dp, end = 20.dp, top = 20.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(shape = RoundedCornerShape(24.dp), color = Palette.White) {
                    Text(
                        text.keepWordsTogether(),
                        style = Type.speech.copy(fontSize = 24.sp, lineHeight = 34.sp),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 16.dp),
                    )
                }
                // Tail pointing down at the head
                Canvas(Modifier.size(width = 26.dp, height = 12.dp)) {
                    val w = this.size.width
                    val h = this.size.height
                    drawPath(
                        Path().apply {
                            moveTo(0f, 0f)
                            lineTo(w, 0f)
                            lineTo(w / 2f, h)
                            close()
                        },
                        Palette.White,
                    )
                }
            }
        }
    }
}

private data class Sprite(
    @DrawableRes val res: Int,
    val emotion: Emotion,
    val species: Species,
    val stage: GrowthStage,
    val scale: Float = 1f,
)

private val GrowthStage.key get() = name.lowercase()
private val Emotion.key get() = when (this) {
    Emotion.CALM -> "calm"
    Emotion.HUNGRY -> "hungry"
    Emotion.SICK -> "sick"
    Emotion.GLAD -> "glad"
    Emotion.RAINY -> "rainy"
}

private fun resolveSprite(context: Context, species: Species, emotion: Emotion, stage: GrowthStage): Sprite {
    val sp = species.key
    val y = GrowthStage.YOUTH
    drawableId(context, "pet_${sp}_${stage.key}_${emotion.key}")?.let { return Sprite(it, emotion, species, stage) }
    // Happy or calm: looking its age matters more (a baby, an elder with a cane). Hungry, sick or
    // wet: the face matters more, so the grown-up drawing with that face, shrunk, comes first.
    val ageFirst = emotion == Emotion.CALM || emotion == Emotion.GLAD
    val byAge = drawableId(context, "pet_${sp}_${stage.key}_calm")?.let { Sprite(it, emotion, species, stage) }
    val byFace = drawableId(context, "pet_${sp}_youth_${emotion.key}")?.let { Sprite(it, emotion, species, y) }
    (if (ageFirst) byAge ?: byFace else byFace ?: byAge)?.let { return it }
    drawableId(context, "pet_${sp}_youth_calm")?.let { return Sprite(it, emotion, species, y) }
    // Last resort: the fox.
    if (species != Species.FOX) return resolveSprite(context, Species.FOX, emotion, stage)
    return Sprite(drawableId(context, "pet_fox_youth_calm")!!, emotion, species, y)
}

/** Drawn height of each stage's own drawing relative to the grown-up one (baby art is drawn small). */
private fun artHeight(stage: GrowthStage): Float = if (stage == GrowthStage.BABY) 0.62f else 1f

/** Size for callers without a Growth (hello screen, friend box): the size at the stage's start. */
internal fun defaultSize(stage: GrowthStage): Float = com.carpet.app.domain.Growth(stage.fromKm.toFloat()).sizeRatio

// Art arrives in batches, so look drawables up by name and fall back when one is missing.
@SuppressLint("DiscouragedApi")
internal fun drawableId(context: Context, name: String): Int? =
    context.resources.getIdentifier(name, "drawable", context.packageName).takeIf { it != 0 }
