package com.example.kotlinoid

import android.graphics.RectF
import androidx.compose.ui.graphics.Color

/**
 * Представляет один кирпич в игре.
 *
 * @property rect Прямоугольник, определяющий позицию и размеры кирпича.
 * @property color Цвет кирпича.
 * @property isVisible Флаг, который показывает, активен ли кирпич.
 */
data class Brick(
    val rect: RectF,
    val color: Color,
    var isVisible: Boolean = true
)

/**
 * Представляет игровой мяч.
 *
 * @property cx Координата X центра мяча.
 * @property cy Координата Y центра мяча.
 * @property radius Радиус мяча.
 * @property dx Скорость (velocity) мяча по оси X.
 * @property dy Скорость (velocity) мяча по оси Y.
 */
data class Ball(
    var cx: Float,
    var cy: Float,
    val radius: Float,
    var dx: Float,
    var dy: Float
)

/**
 * Представляет платформу, управляемую игроком.
 *
 * @property rect Прямоугольник, определяющий позицию и размеры платформы.
 */
data class Paddle(
    val rect: RectF
)