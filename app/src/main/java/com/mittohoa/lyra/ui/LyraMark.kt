package com.mittohoa.lyra.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Dau hieu Lyra, ban Android.
 *
 * Hinh hoc dong bo voi `resources/icon.svg` cua ban Windows va
 * `res/drawable/ic_launcher.xml` - sua mot cho thi sua ca ba. Toa do goc trong
 * khung 512, o day chia lai theo kich thuoc thuc.
 *
 * Khi `busy = true` no cu dong, va cu dong theo dung nghia cua chinh no:
 *   - Cap not lac nhe quanh dau xa, nhu dang bat nhip
 *   - Thanh loi ben duoi chay tu trai sang, nhu mot dong lyric dang hien ra
 * Hai chuyen dong cung chu ky 1,5 giay nen chung an nhip nhau - giong het ban
 * Windows.
 *
 * Ve bang `Canvas` chu khong dung `AnimatedVectorDrawable`: o day chi co vai
 * hinh don gian, ma Canvas thi khong phai nap va phan tich mot file XML nao ca.
 */
@Composable
fun LyraMark(
    size: Dp = 40.dp,
    busy: Boolean = false,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "lyra")

    // Khong ban thi giu nguyen mot hinh tinh - khong co animation nao chay nen
    // cung khong co khung hinh nao bi ve lai
    val rock by if (busy) {
        transition.animateFloat(
            initialValue = -5f,
            targetValue = 5f,
            animationSpec = infiniteRepeatable(
                animation = tween(750, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "rock"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }

    val sweep by if (busy) {
        transition.animateFloat(
            initialValue = 0.12f,
            targetValue = 0.12f,
            animationSpec = infiniteRepeatable(
                // Day ra roi giu mot nhip - dong lyric hien xong con dung do mot luc
                animation = keyframes {
                    durationMillis = 1500
                    0.12f at 0
                    1f at 825
                    1f at 1200
                    0.12f at 1500
                },
                repeatMode = RepeatMode.Restart
            ),
            label = "sweep"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
    }

    Canvas(modifier.size(size)) {
        val k = this.size.minDimension / 512f
        drawMark(k, rock, sweep)
    }
}

private val TILE_TOP = Color(0xFF7C3AED)
private val TILE_BOTTOM = Color(0xFF4338CA)

private fun DrawScope.drawMark(k: Float, rockDegrees: Float, sweepFraction: Float) {
    // Nen bo tron
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(TILE_TOP, TILE_BOTTOM),
            start = Offset(16 * k, 16 * k),
            end = Offset(496 * k, 496 * k)
        ),
        topLeft = Offset(16 * k, 16 * k),
        size = Size(480 * k, 480 * k),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(116 * k)
    )

    // Cap not, lac quanh diem giua hai not ngay duoi dau xa
    rotate(rockDegrees, pivot = Offset(256 * k, 220 * k)) {
        translate(left = 9 * k, top = -6 * k) {
            // Hai than not
            drawRect(Color.White, Offset(207.4f * k, 125 * k), Size(19 * k, 179 * k))
            drawRect(Color.White, Offset(357.4f * k, 97 * k), Size(19 * k, 179 * k))

            // Dau xa noi hai than
            drawPath(
                Path().apply {
                    moveTo(207.4f * k, 125 * k)
                    lineTo(376.4f * k, 97 * k)
                    lineTo(376.4f * k, 137 * k)
                    lineTo(207.4f * k, 165 * k)
                    close()
                },
                Color.White
            )

            // Hai dau not, nghieng 25 do nhu not nhac that
            for ((cx, cy) in listOf(172f to 300f, 322f to 272f)) {
                rotate(-25f, pivot = Offset(cx * k, cy * k)) {
                    drawOval(
                        Color.White,
                        topLeft = Offset((cx - 58) * k, (cy - 33) * k),
                        size = Size(116 * k, 66 * k)
                    )
                }
            }
        }
    }

    // Thanh loi: nen mo luon thay, phan sang chay tu trai sang
    val barLeft = 152 * k
    val barTop = 372 * k
    val barWidth = 208 * k
    val barHeight = 48 * k
    val radius = androidx.compose.ui.geometry.CornerRadius(24 * k)

    drawRoundRect(
        Color.White.copy(alpha = 0.28f),
        topLeft = Offset(barLeft, barTop),
        size = Size(barWidth, barHeight),
        cornerRadius = radius
    )
    drawRoundRect(
        Color.White,
        topLeft = Offset(barLeft, barTop),
        size = Size(barWidth * sweepFraction, barHeight),
        cornerRadius = radius
    )
}
