package de.schatzsuche.app.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import de.schatzsuche.app.data.model.HuntTheme
import de.schatzsuche.app.ui.theme.ThemePalette
import kotlin.math.cos
import kotlin.math.sin

internal fun clampMapPanOffset(
    offset: Offset,
    viewportWidth: Float,
    viewportHeight: Float,
    contentWidth: Float,
    contentHeight: Float
): Offset {
    val maxX = (contentWidth - viewportWidth).coerceAtLeast(0f)
    val maxY = (contentHeight - viewportHeight).coerceAtLeast(0f)
    return Offset(
        x = offset.x.coerceIn(0f, maxX),
        y = offset.y.coerceIn(0f, maxY)
    )
}

internal fun DrawScope.drawTreasureMapBackground(
    theme: HuntTheme,
    palette: ThemePalette,
    width: Float,
    height: Float
) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                palette.surface,
                palette.background,
                palette.background.copy(alpha = 0.92f)
            ),
            startY = 0f,
            endY = height
        ),
        size = Size(width, height)
    )

    when (theme) {
        HuntTheme.PIRATES -> drawPiratesMapBackground(palette, width, height)
        HuntTheme.SPACE -> drawSpaceMapBackground(palette, width, height)
        HuntTheme.KNIGHTS -> drawKnightsMapBackground(palette, width, height)
        HuntTheme.EGYPT -> drawEgyptMapBackground(palette, width, height)
        HuntTheme.CLASSIC -> drawClassicMapBackground(palette, width, height)
        HuntTheme.JUNGLE -> drawJungleMapBackground(palette, width, height)
    }

    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                palette.background.copy(alpha = 0.45f)
            ),
            center = Offset(width / 2f, height / 2f),
            radius = width.coerceAtLeast(height) * 0.75f
        ),
        size = Size(width, height)
    )
}

private fun DrawScope.drawPiratesMapBackground(palette: ThemePalette, width: Float, height: Float) {
    val lineColor = palette.accent.copy(alpha = 0.12f)
    val spacing = 36f
    var y = spacing
    while (y < height) {
        drawLine(lineColor, Offset(0f, y), Offset(width, y), strokeWidth = 1f)
        y += spacing
    }
    var x = spacing
    while (x < width) {
        drawLine(lineColor, Offset(x, 0f), Offset(x, height), strokeWidth = 1f)
        x += spacing
    }

    val waveColor = palette.secondary.copy(alpha = 0.18f)
    for (row in 0..3) {
        val baseY = height * (0.55f + row * 0.12f)
        val path = Path()
        path.moveTo(0f, baseY)
        var px = 0f
        while (px <= width) {
            val py = baseY + sin(px / 42f + row) * 6f
            path.lineTo(px, py)
            px += 12f
        }
        drawPath(path, waveColor, style = Stroke(width = 2f))
    }

    drawCircle(
        color = palette.accent.copy(alpha = 0.08f),
        radius = width * 0.22f,
        center = Offset(width * 0.82f, height * 0.18f)
    )
}

private fun DrawScope.drawSpaceMapBackground(palette: ThemePalette, width: Float, height: Float) {
    val nebula = Brush.radialGradient(
        colors = listOf(
            palette.secondary.copy(alpha = 0.35f),
            Color.Transparent
        ),
        center = Offset(width * 0.3f, height * 0.25f),
        radius = width * 0.55f
    )
    drawCircle(brush = nebula, radius = width * 0.55f, center = Offset(width * 0.3f, height * 0.25f))

    val nebula2 = Brush.radialGradient(
        colors = listOf(
            palette.accent.copy(alpha = 0.2f),
            Color.Transparent
        ),
        center = Offset(width * 0.75f, height * 0.65f),
        radius = width * 0.4f
    )
    drawCircle(brush = nebula2, radius = width * 0.4f, center = Offset(width * 0.75f, height * 0.65f))

    repeat(48) { index ->
        val seed = index * 97 + 13
        val x = (seed % 1000) / 1000f * width
        val y = ((seed * 3) % 1000) / 1000f * height
        val starRadius = if (index % 7 == 0) 2.2f else 1.2f
        val alpha = if (index % 5 == 0) 0.55f else 0.28f
        drawCircle(
            color = palette.onBackground.copy(alpha = alpha),
            radius = starRadius,
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawKnightsMapBackground(palette: ThemePalette, width: Float, height: Float) {
    val parchment = palette.secondary.copy(alpha = 0.14f)
    repeat(18) { row ->
        val y = row * (height / 18f)
        drawLine(
            parchment,
            Offset(0f, y + sin(row.toFloat()) * 3f),
            Offset(width, y + cos(row.toFloat()) * 3f),
            strokeWidth = 1f
        )
    }

    val stoneColor = palette.primary.copy(alpha = 0.12f)
    repeat(24) { index ->
        val cx = ((index * 173) % 1000) / 1000f * width
        val cy = ((index * 311) % 1000) / 1000f * height
        val r = 8f + (index % 4) * 4f
        drawCircle(stoneColor, r, Offset(cx, cy))
    }

    drawLine(
        palette.mapPath.copy(alpha = 0.1f),
        Offset(width * 0.1f, height * 0.85f),
        Offset(width * 0.9f, height * 0.85f),
        strokeWidth = 3f
    )
}

private fun DrawScope.drawEgyptMapBackground(palette: ThemePalette, width: Float, height: Float) {
    val duneColor = palette.secondary.copy(alpha = 0.16f)
    for (layer in 0..4) {
        val baseY = height * (0.35f + layer * 0.13f)
        val path = Path()
        path.moveTo(0f, height)
        path.lineTo(0f, baseY)
        var px = 0f
        while (px <= width) {
            val py = baseY + sin(px / 55f + layer * 1.2f) * 14f
            path.lineTo(px, py)
            px += 10f
        }
        path.lineTo(width, height)
        path.close()
        drawPath(path, duneColor.copy(alpha = 0.12f + layer * 0.02f))
    }

    val pyramidColor = palette.accent.copy(alpha = 0.1f)
    listOf(0.18f to 0.42f, 0.72f to 0.48f).forEach { (fx, fy) ->
        val base = width * 0.14f
        val cx = width * fx
        val cy = height * fy
        val path = Path().apply {
            moveTo(cx, cy - base * 0.9f)
            lineTo(cx - base, cy)
            lineTo(cx + base, cy)
            close()
        }
        drawPath(path, pyramidColor)
    }
}

private fun DrawScope.drawClassicMapBackground(palette: ThemePalette, width: Float, height: Float) {
    val contourColor = palette.mapDot.copy(alpha = 0.14f)
    repeat(6) { ring ->
        val radius = width * (0.12f + ring * 0.08f)
        drawCircle(
            color = contourColor,
            radius = radius,
            center = Offset(width * 0.5f, height * (0.35f + ring * 0.1f)),
            style = Stroke(width = 1.5f)
        )
    }

    val treeColor = palette.secondary.copy(alpha = 0.16f)
    repeat(14) { index ->
        val x = ((index * 211) % 1000) / 1000f * width
        val y = ((index * 157) % 1000) / 1000f * height
        drawCircle(treeColor, 5f, Offset(x, y))
        drawCircle(treeColor.copy(alpha = 0.1f), 9f, Offset(x, y - 6f))
    }

    drawLine(
        palette.mapPath.copy(alpha = 0.12f),
        Offset(width * 0.08f, height * 0.2f),
        Offset(width * 0.92f, height * 0.75f),
        strokeWidth = 2f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 14f))
    )
}

private fun DrawScope.drawJungleMapBackground(palette: ThemePalette, width: Float, height: Float) {
    val canopyColor = palette.secondary.copy(alpha = 0.2f)
    repeat(5) { layer ->
        val y = height * (0.08f + layer * 0.16f)
        drawLine(
            canopyColor,
            Offset(0f, y),
            Offset(width, y + sin(layer * 1.4f) * 10f),
            strokeWidth = 3f
        )
    }

    val vineColor = palette.mapDot.copy(alpha = 0.22f)
    listOf(0.15f, 0.42f, 0.68f, 0.88f).forEachIndexed { index, fx ->
        val path = Path()
        val x = width * fx
        path.moveTo(x, 0f)
        var py = 0f
        while (py <= height * 0.85f) {
            val px = x + sin(py / 38f + index) * 12f
            path.lineTo(px, py)
            py += 14f
        }
        drawPath(path, vineColor, style = Stroke(width = 2.5f))
    }

    val leafColor = palette.mapPath.copy(alpha = 0.14f)
    repeat(20) { index ->
        val cx = ((index * 191) % 1000) / 1000f * width
        val cy = ((index * 277) % 1000) / 1000f * height
        drawCircle(leafColor, 10f + (index % 3) * 4f, Offset(cx, cy))
        drawCircle(leafColor.copy(alpha = 0.1f), 6f, Offset(cx + 8f, cy - 5f))
        drawCircle(leafColor.copy(alpha = 0.1f), 6f, Offset(cx - 7f, cy - 4f))
    }

    val riverColor = palette.accent.copy(alpha = 0.1f)
    val river = Path()
    river.moveTo(width * 0.05f, height * 0.92f)
    var px = width * 0.05f
    while (px <= width * 0.95f) {
        val py = height * 0.88f + sin(px / 48f) * 10f
        river.lineTo(px, py)
        px += 12f
    }
    drawPath(river, riverColor, style = Stroke(width = 5f))
}

private fun DrawScope.drawMapMarker(
    center: Offset,
    fillColor: Color,
    radius: Float,
    ringColor: Color
) {
    drawCircle(
        color = Color.Black.copy(alpha = 0.35f),
        radius = radius + 5f,
        center = center
    )
    drawCircle(
        color = ringColor,
        radius = radius + 2.5f,
        center = center
    )
    drawCircle(color = fillColor, radius = radius, center = center)
}

internal fun DrawScope.drawTreasureMapContent(
    theme: HuntTheme,
    totalSteps: Int,
    completedSteps: Int,
    palette: ThemePalette,
    animateLatest: Boolean,
    dashPhase: Float,
    contentWidth: Float,
    contentHeight: Float
) {
    if (totalSteps <= 0) return

    drawTreasureMapBackground(theme, palette, contentWidth, contentHeight)

    val stepHeight = contentHeight / (totalSteps + 1)
    val centerX = contentWidth / 2f
    val startPoint = Offset(centerX, stepHeight * 0.5f)

    fun locationForStep(stepIndex: Int): Offset {
        val xOffset = if (stepIndex % 2 == 0) -60f else 60f
        return Offset(centerX + xOffset, stepHeight * (stepIndex + 1))
    }

    val treasurePoint = locationForStep(totalSteps - 1)
    val ringColor = palette.onBackground.copy(alpha = 0.92f)

    if (completedSteps > 0) {
        val path = Path()
        path.moveTo(startPoint.x, startPoint.y)
        for (i in 0 until completedSteps.coerceAtMost(totalSteps)) {
            path.lineTo(locationForStep(i).x, locationForStep(i).y)
        }
        drawPath(
            path = path,
            color = Color.Black.copy(alpha = 0.25f),
            style = Stroke(width = 7f, cap = StrokeCap.Round)
        )
        drawPath(
            path = path,
            color = palette.mapPath,
            style = Stroke(
                width = 4f,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(12f, 8f),
                    if (animateLatest) dashPhase else 0f
                )
            )
        )
    }

    drawMapMarker(startPoint, palette.accent, radius = 14f, ringColor = ringColor)

    if (totalSteps > 1) {
        for (index in 0 until totalSteps - 1) {
            val point = locationForStep(index)
            val isCompleted = index < completedSteps
            val isCurrent = index == completedSteps
            val fillColor = when {
                isCompleted -> palette.mapPath
                isCurrent -> palette.accent
                else -> palette.mapDot.copy(alpha = 0.65f)
            }
            drawMapMarker(point, fillColor, radius = 10f, ringColor = ringColor)
        }
    }

    drawMapMarker(
        center = treasurePoint,
        fillColor = Color(0xFFFFD700),
        radius = 16f,
        ringColor = ringColor
    )
    drawCircle(
        color = palette.accent.copy(alpha = 0.85f),
        radius = 6f,
        center = treasurePoint
    )
}
