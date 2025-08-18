package com.example.kotlinoid

import android.graphics.RectF
import androidx.compose.ui.graphics.Color

/**
 * Представляет один кирпич в игре.
 */
data class Brick(
    val rect: RectF,
    val color: Color,
    val isVisible: Boolean = true // <-- Изменили var на val
)

/**
 * Представляет игровой мяч.
 */
data class Ball(
    val cx: Float, // <-- Изменили var на val
    val cy: Float, // <-- Изменили var на val
    val radius: Float,
    val dx: Float, // <-- Изменили var на val
    val dy: Float  // <-- Изменили var на val
)

/**
 * Представляет платформу, управляемую игроком.
 */
data class Paddle(
    val rect: RectF
)