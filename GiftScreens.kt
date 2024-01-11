package com.eastx7.kiosk.uiutilities

//import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.eastx7.kiosk.theme.VertFieldSpacer
import com.eastx7.kiosk.theme.KioskTypography
import com.eastx7.kiosk.theme.md_theme_light_primary
import java.util.*
import kotlin.math.sqrt

@Composable
fun GiftBody(
    innerPadding: PaddingValues,
    coverText: String,
    drawableRes: Int,
    giftImageUrl: String,
    giftText: String,
    giftIsYoursText: String,
    giftIsSentText: String,
    onGiftIsTaken: () -> Unit,
) {

    var coverIsVisible by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
    ) {
        GiftCard(
            giftImageUrl = giftImageUrl,
            giftText = giftText,
            giftIsYoursText = giftIsYoursText,
            giftIsSentText = if (!coverIsVisible) giftIsSentText else ""
        )
        if (coverIsVisible) {
            GiftCover(
                coverText = coverText,
                drawableRes = drawableRes,
                onCoverCleared = {
                    coverIsVisible = false
                    onGiftIsTaken()
                }
            )
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun GiftCard(
    giftImageUrl: String,
    giftText: String,
    giftIsYoursText: String,
    giftIsSentText: String
) {
    val textStyle = KioskTypography.headlineMedium.copy(color = Color.White)
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Red)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center),
            verticalArrangement = Arrangement.Center
        ) {

            GlideImage(
                model = giftImageUrl,
                contentDescription = "gift",
                modifier = Modifier
                    .width(500.dp)
                    .wrapContentHeight(),
            )

            VertFieldSpacer()
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                text = giftText +
                        if (giftIsSentText.isNotEmpty()) {
                            " $giftIsYoursText"
                        } else {
                            ""
                        },
                style = textStyle,
                textAlign = TextAlign.Center
            )
            if (giftIsSentText.isNotEmpty()) {
                VertFieldSpacer()
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    text = giftIsSentText,
                    style = textStyle,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun GiftCover(
    coverText: String,
    drawableRes: Int,
    onCoverCleared: () -> Unit,
) {
    val coverPattern = ShaderBrush(
        ImageShader(
            ImageBitmap.imageResource(drawableRes),
            TileMode.Repeated,
            TileMode.Repeated
        )
    )

    val coverTextStyle = KioskTypography.headlineLarge.copy(color = md_theme_light_primary)
    val textMeasurer = rememberTextMeasurer()
    val textLayoutResult: TextLayoutResult =
        textMeasurer.measure(
            text = AnnotatedString(coverText),
            style = coverTextStyle,
        )

    val textSize = textLayoutResult.size
    var centerOffset = Offset.Zero
    var maxErasedDistance = 0f
    var erasedDistance = 0f

    fun PointerInputChange.getDistance(): Float {
        val x = position.x - previousPosition.x
        val y = position.y - previousPosition.y
        return sqrt(x * x + y * y)
    }

    data class EraserPoint(
        val startFlag: Boolean = false,
        val x: Float = 0f,
        val y: Float = 0f,
    )

    val erasedPath = remember { mutableStateListOf<EraserPoint>() }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged {
                centerOffset = Offset(it.width / 2f, it.height / 2f)
                //Ten swipes are enough to get the present
                maxErasedDistance = 10 * minOf(it.width, it.height).toFloat()
            }
            .background(Color.Transparent)
            .pointerInput("eraser") {
                detectDragGestures(
                    onDragStart = { offset ->
                        erasedPath.add(
                            EraserPoint(
                                startFlag = true,
                                x = offset.x,
                                y = offset.y
                            )
                        )
                    },
                    onDrag = { change, offset ->
                        erasedPath.add(
                            EraserPoint(
                                startFlag = false,
                                x = change.position.x,
                                y = change.position.y
                            )
                        )
                        erasedDistance += change.getDistance()
                        if (erasedDistance > maxErasedDistance) {
                            onCoverCleared()
                        }
                    }
                )
            }
    ) {
        with(drawContext.canvas.nativeCanvas) {
//            val canvasWidth = size.width
//            val canvasHeight = size.height
            val checkPoint = saveLayer(null, null)
            drawRect(
                brush = coverPattern
            )

            drawText(
                textMeasurer = textMeasurer,
                text = coverText,
                style = coverTextStyle,
                topLeft = Offset(
                    centerOffset.x - textSize.width / 2f,
                    centerOffset.y - textSize.height / 2f
                ),
            )
            drawPath(
                path = androidx.compose.ui.graphics.Path().apply {
                    for (eraserPoint in erasedPath) {
                        if (eraserPoint.startFlag) {
                            moveTo(eraserPoint.x, eraserPoint.y)
                        } else {
                            lineTo(eraserPoint.x, eraserPoint.y)
                        }
                    }
                },
                style = Stroke(
                    width = 120f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                ),
                blendMode = BlendMode.Clear,
                color = Color.Transparent,
            )
            restoreToCount(checkPoint)
        }
    }
}
