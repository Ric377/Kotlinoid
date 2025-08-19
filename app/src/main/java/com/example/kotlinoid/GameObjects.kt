package com.example.kotlinoid

import android.graphics.RectF
import androidx.compose.ui.graphics.Color

/**
 * Представляет один кирпич в игре.
 */
data class Brick(
    val rect: RectF,
    val color: Color,
    val isVisible: Boolean = true
)

/**
 * Представляет игровой мяч.
 */
data class Ball(
    val cx: Float,
    val cy: Float,
    val radius: Float,
    val dx: Float,
    val dy: Float
)

/**
 * Представляет платформу, управляемую игроком.
 */
data class Paddle(
    val rect: RectF
)

/**
 * Определяет текущее состояние игрового процесса.
 */
enum class GameStatus {
    READY,
    RUNNING,
    GAME_OVER
}