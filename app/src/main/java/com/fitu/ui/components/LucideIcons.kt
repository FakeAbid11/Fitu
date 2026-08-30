package com.fitu.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// Footprints Icon (Steps)
val FootprintsIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Footprints",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(4f, 16f)
            verticalLineTo(13.62f)
            curveTo(4f, 11.5f, 2.97f, 10.5f, 3f, 8f)
            curveTo(3.03f, 5.28f, 4.49f, 2f, 7.5f, 2f)
            curveTo(9.37f, 2f, 10f, 3.8f, 10f, 5.5f)
            curveTo(10f, 8.61f, 8f, 11.16f, 8f, 14.18f)
            verticalLineTo(16f)
            arcTo(2f, 2f, 0f, true, true, 4f, 16f)
            close()
        }
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(20f, 20f)
            verticalLineTo(17.62f)
            curveTo(20f, 15.5f, 21.03f, 14.5f, 21f, 12f)
            curveTo(20.97f, 9.28f, 19.51f, 6f, 16.5f, 6f)
            curveTo(14.63f, 6f, 14f, 7.8f, 14f, 9.5f)
            curveTo(14f, 12.61f, 16f, 15.16f, 16f, 18.18f)
            verticalLineTo(20f)
            arcTo(2f, 2f, 0f, true, false, 20f, 20f)
            close()
        }
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(16f, 17f)
            horizontalLineTo(20f)
        }
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(4f, 13f)
            horizontalLineTo(8f)
        }
    }.build()

// Apple Icon (Nutrition)
val AppleIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Apple",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 6.528f)
            verticalLineTo(3f)
            arcTo(1f, 1f, 0f, false, true, 13f, 2f)
        }
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(18.237f, 21f)
            arcTo(15f, 15f, 0f, false, false, 22f, 11f)
            arcTo(6f, 6f, 0f, false, false, 12f, 6.528f)
            arcTo(6f, 6f, 0f, false, false, 2f, 11f)
            arcTo(15.1f, 15.1f, 0f, false, false, 5.763f, 21f)
            arcTo(3f, 3f, 0f, false, false, 9.411f, 21.648f)
            arcTo(5.5f, 5.5f, 0f, false, true, 14.589f, 21.648f)
            arcTo(3f, 3f, 0f, false, false, 18.237f, 21f)
        }
    }.build()

// House Icon (Home/Dashboard)
val HouseIcon: ImageVector
    get() = ImageVector.Builder(
        name = "House",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(15f, 21f)
            verticalLineTo(13f)
            arcTo(1f, 1f, 0f, false, false, 14f, 12f)
            horizontalLineTo(10f)
            arcTo(1f, 1f, 0f, false, false, 9f, 13f)
            verticalLineTo(21f)
        }
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(3f, 10f)
            arcTo(2f, 2f, 0f, false, true, 3.709f, 8.472f)
            lineTo(10.709f, 2.472f)
            arcTo(2f, 2f, 0f, false, true, 13.291f, 2.472f)
            lineTo(20.291f, 8.472f)
            arcTo(2f, 2f, 0f, false, true, 21f, 10f)
            verticalLineTo(19f)
            arcTo(2f, 2f, 0f, false, true, 19f, 21f)
            horizontalLineTo(5f)
            arcTo(2f, 2f, 0f, false, true, 3f, 19f)
            close()
        }
    }.build()

// Circle User Icon (Profile)
val CircleUserIcon: ImageVector
    get() = ImageVector.Builder(
        name = "CircleUser",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(18f, 20f)
            arcTo(6f, 6f, 0f, false, false, 6f, 20f)
        }
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 14f)
            arcTo(4f, 4f, 0f, false, false, 12f, 6f)
            arcTo(4f, 4f, 0f, false, false, 12f, 14f)
            close()
        }
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 22f)
            arcTo(10f, 10f, 0f, false, false, 12f, 2f)
            arcTo(10f, 10f, 0f, false, false, 12f, 22f)
            close()
        }
    }.build()

// Dumbbell Icon (AI Coach)
val DumbbellIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Dumbbell",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(17.596f, 12.768f)
            arcTo(2f, 2f, 0f, true, false, 20.425f, 9.939f)
            lineTo(18.657f, 8.172f)
            arcTo(2f, 2f, 0f, false, false, 21.485f, 5.343f)
            lineTo(18.657f, 2.515f)
            arcTo(2f, 2f, 0f, false, false, 15.828f, 5.343f)
            lineTo(14.061f, 3.575f)
            arcTo(2f, 2f, 0f, true, false, 11.232f, 6.404f)
            close()
        }
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(2.5f, 21.5f)
            lineTo(3.9f, 20.1f)
        }
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(20.1f, 3.9f)
            lineTo(21.5f, 2.5f)
        }
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(5.343f, 21.485f)
            arcTo(2f, 2f, 0f, true, false, 8.172f, 18.657f)
            lineTo(9.939f, 20.425f)
            arcTo(2f, 2f, 0f, true, false, 12.768f, 17.596f)
            lineTo(6.404f, 11.232f)
            arcTo(2f, 2f, 0f, true, false, 3.575f, 14.061f)
            lineTo(5.343f, 15.828f)
            arcTo(2f, 2f, 0f, false, false, 2.515f, 18.657f)
            close()
        }
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(9.6f, 14.4f)
            lineTo(14.4f, 9.6f)
        }
    }.build()
