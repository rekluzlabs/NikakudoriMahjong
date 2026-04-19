/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 */

package com.rekluzgames.nikakudorimahjong.presentation.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rekluzgames.nikakudorimahjong.R
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

private data class Petal(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var rotation: Float,
    var rotSpeed: Float,
    var alpha: Float,
    var fade: Float,
    var size: Float,
    var scaleX: Float,
    var wobble: Float,
    var wobbleSpeed: Float,
    var color: Color,
    var depth: Float
)

private val petalColors = listOf(
    Color(0xFFFFB7C5), Color(0xFFFF8FAB), Color(0xFFFFC8D6),
    Color(0xFFFFE4EC), Color(0xFFFF9AB5), Color(0xFFFFD6E0),
    Color(0xFFFFCCD8)
)

private fun randomWindowPetal(): Petal {
    val depth = Random.nextFloat()
    val sizeBase = Random.nextFloat() * 6f + 4f
    val isHeavy = Random.nextFloat() > 0.7f

    return Petal(
        x = 0.32f + Random.nextFloat() * 0.15f,
        y = 0.35f + Random.nextFloat() * 0.10f,

        vx = if (Random.nextFloat() < 0.8f) {
            (0.0006f + depth * 0.0010f) + Random.nextFloat() * 0.0005f
        } else {
            -((0.0003f + depth * 0.0006f) + Random.nextFloat() * 0.0003f)
        },

        vy = if (isHeavy) {
            Random.nextFloat() * 0.0025f + 0.0020f
        } else {
            Random.nextFloat() * 0.0010f + 0.0004f
        },

        rotation = Random.nextFloat() * 360f,
        rotSpeed = (Random.nextFloat() * 3f) - 1.5f,
        alpha = 0.5f + depth * 0.5f,

        fade = if (isHeavy) {
            Random.nextFloat() * 0.0002f + 0.0001f
        } else {
            Random.nextFloat() * 0.0008f + 0.0004f
        },

        size = sizeBase * (0.5f + depth * 0.7f),
        scaleX = Random.nextFloat() * 0.6f + 0.4f,
        wobble = Random.nextFloat() * (2f * PI.toFloat()),
        wobbleSpeed = Random.nextFloat() * 0.025f + 0.008f + depth * 0.01f,
        color = petalColors.random(),
        depth = depth
    )
}

@Composable
fun WelcomeScreen(onStartGame: () -> Unit) {

    var imageVisible by remember { mutableStateOf(false) }
    var titleVisible by remember { mutableStateOf(false) }
    var tapVisible by remember { mutableStateOf(false) }
    var petals by remember { mutableStateOf<List<Petal>>(emptyList()) }

    val imageAlpha by animateFloatAsState(if (imageVisible) 1f else 0f, tween(1800))
    val titleAlpha by animateFloatAsState(if (titleVisible) 1f else 0f, tween(1400))
    val titleOffsetY by animateFloatAsState(
        if (titleVisible) 0f else -28f,
        tween(1400, easing = EaseOutCubic)
    )

    var tapBlink by remember { mutableStateOf(true) }

    LaunchedEffect(tapVisible) {
        if (!tapVisible) return@LaunchedEffect
        while (true) {
            tapBlink = true
            delay(750)
            tapBlink = false
            delay(750)
        }
    }

    LaunchedEffect(Unit) {
        imageVisible = true
        delay(500)
        titleVisible = true
        delay(2200)
        tapVisible = true
    }

    LaunchedEffect(imageVisible) {
        if (!imageVisible) return@LaunchedEffect
        while (true) {
            delay(120)
            if (petals.size < 80) {
                petals = petals + randomWindowPetal()
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(16)
            petals = petals.map { p ->
                val newWobble = p.wobble + p.wobbleSpeed
                p.copy(
                    wobble = newWobble,
                    x = p.x + p.vx,
                    y = p.y + p.vy + sin(newWobble) * 0.0008f,
                    rotation = p.rotation + p.rotSpeed,
                    alpha = (p.alpha - p.fade).coerceAtLeast(0f)
                )
            }.filter {
                it.alpha > 0.01f && it.y < 1.05f && it.x in -0.1f..1.1f
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { if (tapVisible) onStartGame() },
        contentAlignment = Alignment.Center
    ) {

        Image(
            painter = painterResource(R.drawable.welcome_day),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = imageAlpha }
        )

        // Gradient overlay
        Box(
            Modifier.fillMaxSize().drawWithContent {
                drawContent()
                drawRect(Brush.verticalGradient(0f to Color(0xB80A0300), 0.5f to Color.Transparent))
                drawRect(Brush.verticalGradient(0.62f to Color.Transparent, 1f to Color(0xC50A0300)))
                drawRect(
                    Brush.radialGradient(
                        0f to Color.Transparent,
                        1f to Color(0x660A0300),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.width * 0.85f
                    )
                )
            }
        )

        // ✅ FIXED PARTICLE LAYER
        Box(
            Modifier.fillMaxSize().drawWithContent {
                drawContent()

                val sorted = petals.sortedBy { it.depth }
                for (p in sorted) {
                    val cx = p.x * size.width
                    val cy = p.y * size.height
                    val w = p.size * 2f * p.scaleX
                    val h = p.size

                    val halfW = w / 2f
                    val halfH = h / 2f

                    with(drawContext.canvas.nativeCanvas) {
                        save()
                        translate(cx, cy)
                        rotate(p.rotation)

                        drawOval(
                            android.graphics.RectF(-halfW, -halfH, halfW, halfH),
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.argb(
                                    (p.alpha * 255).toInt(),
                                    (p.color.red * 255).toInt(),
                                    (p.color.green * 255).toInt(),
                                    (p.color.blue * 255).toInt()
                                )
                                isAntiAlias = true
                            }
                        )

                        restore()
                    }
                }
            }
        )

        // UI CONTENT
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .graphicsLayer {
                    alpha = titleAlpha
                    translationY = titleOffsetY
                }
                .padding(horizontal = 24.dp)
        ) {

            val font = FontFamily(Font(R.font.zen_antique_soft))

            val mainStyle = TextStyle(
                fontFamily = font,
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE8C87A),
                letterSpacing = 3.sp,
                textAlign = TextAlign.Center,
                shadow = Shadow(Color.Black.copy(0.6f), Offset(2f, 2f), 4f)
            )

            val outlineStyle = mainStyle.copy(color = Color.Black)

            TextWithThickOutline(
                stringResource(R.string.title_nikakudori),
                mainStyle,
                outlineStyle,
                2.dp,
                4.dp
            )

            Spacer(Modifier.height(4.dp))

            TextWithThickOutline(
                stringResource(R.string.title_mahjong),
                mainStyle,
                outlineStyle,
                2.dp,
                4.dp
            )

            Spacer(Modifier.height(20.dp))

            if (tapVisible) {
                Text(
                    stringResource(R.string.tap_to_play),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (tapBlink) Color(0xFFE8C87A) else Color(0xFFE8C87A).copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun TextWithThickOutline(
    text: String,
    style: TextStyle,
    outlineStyle: TextStyle,
    outlineThickness: Dp,
    shadow3DOffset: Dp
) {
    Box(contentAlignment = Alignment.Center) {

        listOf(
            outlineThickness to outlineThickness,
            -outlineThickness to outlineThickness,
            outlineThickness to -outlineThickness,
            -outlineThickness to -outlineThickness
        ).forEach { (x, y) ->
            Text(text = text, style = outlineStyle, modifier = Modifier.offset(x, y))
        }

        Text(
            text = text,
            style = outlineStyle.copy(
                shadow = Shadow(
                    Color.Black.copy(0.5f),
                    Offset(shadow3DOffset.value * 1.5f, shadow3DOffset.value * 1.5f),
                    shadow3DOffset.value * 2f
                )
            ),
            modifier = Modifier.offset(shadow3DOffset, shadow3DOffset)
        )

        Text(text = text, style = style)
    }
}