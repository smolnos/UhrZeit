package com.example.stundenuhr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay
import java.lang.Math.toRadians
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                GoldenSunClockModified()
            }
        }
    }
}

@Composable
fun GoldenSunClockModified() {
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    // Berechne den Helligkeitsfaktor (max. um 12 Uhr, minimal um Mitternacht)
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin")).apply {
        timeInMillis = currentTimeMillis
    }
    val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)
    val timeInHours = hourOfDay + minute / 60f

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Mit BoxWithConstraints wird ein quadratisches Canvas anhand der kleineren Seite bestimmt (Quer-/Hochformat)
        BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val canvasSizeDp = if (maxWidth < maxHeight) maxWidth else maxHeight
            Canvas(modifier = Modifier.size(canvasSizeDp)) {
                val canvasSize = size.minDimension
                val center = Offset(x = size.width / 2, y = size.height / 2)

                // Definiere einen sicheren Rand
                val safePadding = canvasSize * 0.1f
                val overallOuterRadius = canvasSize / 2 - safePadding

                // Das Zifferblatt (Sonne) nimmt 70 % des verfügbaren Radius ein
                val clockRadius = overallOuterRadius * 0.85f
                // Verlängerte Sonnenstrahlen (Ticks): 25 % des overallOuterRadius
                val rayLength = overallOuterRadius * 0.1f
                // Die Zahlen werden außerhalb der Tick-Endpunkte gezeichnet.
                // Hier wurde der zusätzliche Offset auf 6 % erhöht, um mehr Abstand zu schaffen.
                val numberRadius = clockRadius + rayLength + overallOuterRadius * 0.06f

                // Skalierte Größen
                val outerStrokeWidth = overallOuterRadius * 0.03f
                val tickStrokeWidth = overallOuterRadius * 0.03f
                val handStrokeWidth = overallOuterRadius * 0.05f
                val centralCircleRadius = overallOuterRadius * 0.06f
                // Schriftgröße (ca. 8 % der Canvas-Größe)
                val textSize = canvasSize * 0.08f

                // 1. Zifferblatt (Sonne) mit radialem Farbverlauf
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFF8E1), Color(0xFFFFECB3)), // von hellem zu dunklerem Beige
                        center = center,
                        radius = clockRadius
                    ),
                    center = center,
                    radius = clockRadius
                )

                // 2. Rahmen
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFBCAAA4), Color(0xFF8D6E63))
                    ),
                    center = center,
                    radius = clockRadius,
                    style = Stroke(width = outerStrokeWidth)
                )

                // 3. Sonnenstrahlen (Ticks)
                for (i in 0 until 12) {
                    val angle = toRadians(i * 30.0 - 90.0)
                    val start = Offset(
                        x = center.x + clockRadius * cos(angle).toFloat(),
                        y = center.y + clockRadius * sin(angle).toFloat()
                    )
                    val end = Offset(
                        x = center.x + (clockRadius + rayLength) * cos(angle).toFloat(),
                        y = center.y + (clockRadius + rayLength) * sin(angle).toFloat()
                    )
                    drawLine(
                        color = Color(0xFF5D4037),
                        start = start,
                        end = end,
                        strokeWidth = tickStrokeWidth,
                        cap = StrokeCap.Round
                    )
                }

                // 4. Zahlen: Diese werden an einem Radius außerhalb der Ticks gezeichnet
                val textPaint = android.graphics.Paint().apply {
                    this.textSize = textSize
                    textAlign = android.graphics.Paint.Align.CENTER
                    isFakeBoldText = true
                    color = android.graphics.Color.parseColor("#3E2723")  // Elegantes dunkles Braun
                    setShadowLayer(5f, 3f, 3f, android.graphics.Color.DKGRAY)
                    typeface = android.graphics.Typeface.create("serif", android.graphics.Typeface.BOLD)
                }
                for (i in 1..12) {
                    val angle = toRadians(i * 30.0 - 90.0)
                    val numberPos = Offset(
                        x = center.x + numberRadius * cos(angle).toFloat(),
                        y = center.y + numberRadius * sin(angle).toFloat()
                    )
                    val fm = textPaint.fontMetrics
                    val textY = numberPos.y - ((fm.ascent + fm.descent) / 2)
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawText(i.toString(), numberPos.x, textY, textPaint)
                    }
                }

                // 5. Stundenzeiger
                val hour = calendar.get(Calendar.HOUR)
                val minute = calendar.get(Calendar.MINUTE)
                val hourAngle = toRadians(((hour % 12) + minute / 60f) * 30f - 90.0)
                val handLength = clockRadius * 0.95f
                val handEnd = Offset(
                    x = center.x + handLength * cos(hourAngle).toFloat(),
                    y = center.y + handLength * sin(hourAngle).toFloat()
                )
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF3E2723), Color(0xFF5D4037))
                    ),
                    start = center,
                    end = handEnd,
                    strokeWidth = handStrokeWidth,
                    cap = StrokeCap.Round
                )

                // 6. Zentraler Drehpunkt
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, Color(0xFFFFE082)),
                        center = center,
                        radius = centralCircleRadius
                    ),
                    center = center,
                    radius = centralCircleRadius
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BeautifulClockPreview() {
    MaterialTheme {
        GoldenSunClockModified()
    }
}
