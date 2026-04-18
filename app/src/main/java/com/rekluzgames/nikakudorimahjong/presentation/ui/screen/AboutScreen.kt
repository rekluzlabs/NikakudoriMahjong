/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 * This code and its assets are the exclusive property of Rekluz Games.
 * Unauthorized copying, distribution, or commercial use is strictly prohibited.
 */

package com.rekluzgames.nikakudorimahjong.presentation.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rekluzgames.nikakudorimahjong.domain.model.GameState
import com.rekluzgames.nikakudorimahjong.presentation.viewmodel.GameViewModel
import com.rekluzgames.nikakudorimahjong.presentation.ui.component.AlphabetTile
import com.rekluzgames.nikakudorimahjong.presentation.ui.component.MenuPillButton
import com.rekluzgames.nikakudorimahjong.presentation.ui.component.OverlayContainer
import androidx.compose.ui.res.stringResource
import com.rekluzgames.nikakudorimahjong.R
import com.rekluzgames.nikakudorimahjong.BuildConfig
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

import android.annotation.SuppressLint

// ─── About Petal data ───────────────────────────────────────────────────────────────
private data class AboutPetal(
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

private const val WINDOW_RIGHT = 0.42f
private const val WINDOW_TOP = 0.32f
private const val WINDOW_BOTTOM = 0.72f

private fun randomWindowPetal(): AboutPetal {
    val depth = Random.nextFloat()
    val sizeBase = Random.nextFloat() * 6f + 4f
    return AboutPetal(
        x = WINDOW_RIGHT - Random.nextFloat() * 0.04f,
        y = WINDOW_TOP + Random.nextFloat() * (WINDOW_BOTTOM - WINDOW_TOP),
        vx = if (Random.nextFloat() < 0.6f) {
            (0.0006f + depth * 0.0010f) + Random.nextFloat() * 0.0005f
        } else {
            -((0.0003f + depth * 0.0006f) + Random.nextFloat() * 0.0003f)
        },
        vy = Random.nextFloat() * 0.0012f + 0.0004f,
        rotation = Random.nextFloat() * 360f,
        rotSpeed = (Random.nextFloat() * 3f) - 1.5f,
        alpha = 0.3f + depth * 0.65f,
        fade = Random.nextFloat() * 0.0008f + 0.0004f,
        size = sizeBase * (0.5f + depth * 0.7f),
        scaleX = Random.nextFloat() * 0.6f + 0.4f,
        wobble = Random.nextFloat() * (2f * PI.toFloat()),
        wobbleSpeed = Random.nextFloat() * 0.025f + 0.008f + depth * 0.01f,
        color = petalColors.random(),
        depth = depth
    )
}

@SuppressLint("DiscouragedApi", "LocalContextResourcesRead")
@Composable
fun AboutScreen(viewModel: GameViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var petals by remember { mutableStateOf<List<AboutPetal>>(emptyList()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(120)
            if (petals.size < 80) {
                petals = petals + randomWindowPetal()
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(16L)
            petals = petals
                .map { p ->
                    val newWobble = p.wobble + p.wobbleSpeed
                    p.copy(
                        wobble = newWobble,
                        x = p.x + p.vx,
                        y = p.y + p.vy + sin(newWobble) * 0.0008f,
                        rotation = p.rotation + p.rotSpeed,
                        alpha = (p.alpha - p.fade).coerceAtLeast(0f)
                    )
                }
                .filter { it.alpha > 0.01f && it.y < 1.05f && it.x < 1.1f && it.x > -0.1f }
        }
    }

    OverlayContainer {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .aspectRatio(1.5f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, Color(0xFF00BFFF).copy(alpha = 0.15f), RoundedCornerShape(24.dp))
        ) {
            Image(
                painter = painterResource(R.drawable.about_bg),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        drawContent()
                        val sorted = petals.sortedBy { it.depth }
                        for (p in sorted) {
                            val cx = p.x * size.width
                            val cy = p.y * size.height
                            val w = p.size * 2f * p.scaleX
                            val h = p.size

                            val rad = Math.toRadians(p.rotation.toDouble()).toFloat()
                            val halfW = w / 2f
                            val halfH = h / 2f

                            with(drawContext.canvas.nativeCanvas) {
                                save()
                                translate(cx, cy)
                                rotate(Math.toDegrees(rad.toDouble()).toFloat())
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

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.weight(3f))

                when (uiState.aboutStage) {
                    0 -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1.2f).padding(end = 16.dp)) {
                                Text(
                                    text = stringResource(R.string.how_to_play),
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.about_description),
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    lineHeight = 20.sp
                                )
                                Spacer(Modifier.height(20.dp))
                                Text(
                                    text = stringResource(R.string.about_github_link),
                                    color = Color(0xFF00BFFF),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        uriHandler.openUri("https://github.com/rekluz/NikakudoriMahjong/")
                                    }
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.version_label, BuildConfig.VERSION_NAME),
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row {
                                    "REKLUZ".forEachIndexed { i, c ->
                                        AlphabetTile(c, !uiState.clearedAboutTiles.contains(i)) {
                                            viewModel.onAboutTileClick(i, 11)
                                        }
                                    }
                                }
                                Row {
                                    "GAMES".forEachIndexed { i, c ->
                                        AlphabetTile(c, !uiState.clearedAboutTiles.contains(i + 6)) {
                                            viewModel.onAboutTileClick(i + 6, 11)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(24.dp))
                                Box(modifier = Modifier.width(180.dp)) {
                                    MenuPillButton(
                                        text = stringResource(R.string.btn_done),
                                        color = Color(0xFF2A2A2A)
                                    ) {
                                        viewModel.changeState(GameState.PLAYING)
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        val scrollState = rememberScrollState()

                        LaunchedEffect(Unit) {
                            delay(300)
                            scrollState.animateScrollTo(120)
                            delay(400)
                            scrollState.animateScrollTo(0)
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .verticalScroll(scrollState)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                        .background(Color.DarkGray)
                                        .border(3.dp, Color(0xFF00BFFF), CircleShape)
                                        .padding(4.dp)
                                        .clickable {
                                            viewModel.closeAbout()
                                            viewModel.changeState(GameState.PLAYING)
                                        }
                                ) {
                                    val id = context.resources.getIdentifier(
                                        "my_photo", "drawable", context.packageName
                                    )
                                    if (id != 0) {
                                        Image(
                                            painter = painterResource(id),
                                            contentDescription = "Developer Photo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = stringResource(R.string.about_created_by),
                                        color = Color.Gray,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = stringResource(R.string.developer_name),
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            Box(Modifier.width(260.dp)) {
                                MenuPillButton(
                                    stringResource(R.string.about_thank_you),
                                    color = Color(0xFF00BFFF)
                                ) {
                                    viewModel.closeAbout()
                                    viewModel.changeState(GameState.PLAYING)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(1f))
            }
        }
    }
}