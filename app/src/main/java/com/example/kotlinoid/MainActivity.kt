package com.example.kotlinoid

import android.graphics.RectF
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.example.kotlinoid.ui.theme.KotlinoidTheme
import kotlinx.coroutines.delay

/**
 * Хранит цветовые палитры для светлой и тёмной тем.
 */
object AppColors {
    val LightPalette = mapOf(
        "background" to Color(0xFFF5F5F5),
        "paddle" to Color(0xFF212121),
        "ball" to Color(0xFFD32F2F),
        "textPrimary" to Color(0xFF424242),
        "textHint" to Color(0xFF757575)
    )

    val DarkPalette = mapOf(
        "background" to Color(0xFF121212),
        "paddle" to Color(0xFFE0E0E0),
        "ball" to Color(0xFFEF5350),
        "textPrimary" to Color(0xFFFFFFFF),
        "textHint" to Color(0xFFBDBDBD)
    )

    val brickSet1 = listOf(Color(0xFFD32F2F), Color(0xFFD32F2F), Color(0xFFF57C00), Color(0xFFF57C00))
    val brickSet2 = listOf(Color(0xFFFBC02D), Color(0xFFFBC02D), Color(0xFF388E3C), Color(0xFF388E3C))
}

/**
 * Содержит полное состояние игрового поля в определенный момент времени.
 */
data class GameState(
    val bricks: List<Brick> = emptyList(),
    val ball: Ball? = null,
    val paddle: Paddle? = null,
    val score: Int = 0,
    val lives: Int = 3,
    val status: GameStatus = GameStatus.READY,
    val gameInitialized: Boolean = false
)

/**
 * Главная и единственная Activity приложения.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KotlinoidTheme {
                val isDarkTheme = isSystemInDarkTheme()
                val colors = if (isDarkTheme) AppColors.DarkPalette else AppColors.LightPalette

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = colors["background"]!!
                ) {
                    GameScreen(colors)
                }
            }
        }
    }
}

/**
 * Основной Composable-компонент, отображающий игровое поле.
 */
@Composable
fun GameScreen(colors: Map<String, Color>) {
    var gameState by remember { mutableStateOf(GameState()) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    val textMeasurer = rememberTextMeasurer()

    LaunchedEffect(Unit) {
        while (true) {
            if (gameState.gameInitialized && gameState.status == GameStatus.RUNNING) {
                gameState = updateGameState(gameState, canvasSize)
            }
            delay(16)
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    if (gameState.status == GameStatus.PAUSED) return@detectDragGestures
                    val oldPaddle = gameState.paddle ?: return@detectDragGestures
                    val paddleWidth = oldPaddle.rect.width()
                    var newX = change.position.x - paddleWidth / 2

                    if (newX < 0) newX = 0f
                    if (newX + paddleWidth > canvasSize.width) {
                        newX = canvasSize.width - paddleWidth
                    }

                    val newPaddleRect = RectF(newX, oldPaddle.rect.top, newX + paddleWidth, oldPaddle.rect.bottom)
                    val newPaddle = oldPaddle.copy(rect = newPaddleRect)
                    gameState = gameState.copy(paddle = newPaddle)
                }
            }
            .pointerInput(gameState.status) {
                detectTapGestures { offset ->
                    val pauseIconLeft = size.width - 100f
                    val pauseIconTop = 20f
                    val isIconTapped = offset.x > pauseIconLeft && offset.y < pauseIconTop + 80f

                    when (gameState.status) {
                        GameStatus.READY -> {
                            gameState = gameState.copy(status = GameStatus.RUNNING)
                        }
                        GameStatus.RUNNING -> {
                            if (isIconTapped) {
                                gameState = gameState.copy(status = GameStatus.PAUSED)
                            }
                        }
                        GameStatus.PAUSED -> {
                            if (isIconTapped) {
                                gameState = gameState.copy(status = GameStatus.RUNNING)
                            }
                        }
                        else -> {}
                    }
                }
            }
    ) {
        if (!gameState.gameInitialized) {
            canvasSize = size
            gameState = initializeGame(size)
        }
        drawGame(this, gameState, textMeasurer, colors)
    }
}

/**
 * Обновляет состояние игры для следующего кадра.
 */
private fun updateGameState(currentState: GameState, canvasSize: Size): GameState {
    var currentBall = currentState.ball ?: return currentState
    val paddle = currentState.paddle ?: return currentState
    var score = currentState.score
    var lives = currentState.lives

    if (currentState.status != GameStatus.RUNNING) return currentState

    currentBall = currentBall.copy(
        cx = currentBall.cx + currentBall.dx,
        cy = currentBall.cy + currentBall.dy
    )

    val canvasWidth = canvasSize.width
    val canvasHeight = canvasSize.height

    if (currentBall.cx - currentBall.radius < 0 || currentBall.cx + currentBall.radius > canvasWidth) {
        currentBall = currentBall.copy(dx = -currentBall.dx)
    }

    if (currentBall.cy - currentBall.radius < 0) {
        currentBall = currentBall.copy(dy = -currentBall.dy)
    }

    if (currentBall.cy + currentBall.radius > canvasHeight) {
        lives--
        if (lives <= 0) {
            return currentState.copy(lives = 0, status = GameStatus.GAME_OVER)
        } else {
            val newBall = currentBall.copy(cx = canvasWidth / 2, cy = canvasHeight / 2)
            return currentState.copy(
                ball = newBall,
                lives = lives,
                status = GameStatus.READY
            )
        }
    }

    val ballRect = RectF(
        currentBall.cx - currentBall.radius,
        currentBall.cy - currentBall.radius,
        currentBall.cx + currentBall.radius,
        currentBall.cy + currentBall.radius
    )

    if (currentBall.dy > 0 && ballRect.intersect(paddle.rect)) {
        currentBall = currentBall.copy(dy = -currentBall.dy)
    }

    var brickHit = false
    val newBricks = currentState.bricks.map { brick ->
        if (!brickHit && brick.isVisible && ballRect.intersect(brick.rect)) {
            brickHit = true
            score += 100
            brick.copy(isVisible = false)
        } else {
            brick
        }
    }

    if (brickHit) {
        currentBall = currentBall.copy(dy = -currentBall.dy)
    }

    return currentState.copy(ball = currentBall, bricks = newBricks, score = score, lives = lives)
}


/**
 * Инициализирует начальное состояние игры.
 */
private fun initializeGame(canvasSize: Size): GameState {
    val canvasWidth = canvasSize.width
    val canvasHeight = canvasSize.height
    val bricks = mutableListOf<Brick>()

    val numRows = 8
    val numBricksPerRow = 10
    val brickSpacing = canvasWidth * 0.01f
    val brickTopOffset = canvasHeight * 0.15f
    val spaceForBricks = canvasWidth - (numBricksPerRow + 1) * brickSpacing
    val brickWidth = spaceForBricks / numBricksPerRow
    val brickHeight = canvasHeight * 0.025f
    val colors = AppColors.brickSet1 + AppColors.brickSet2

    var currentY = brickTopOffset
    for (i in 0 until numRows) {
        var currentX = brickSpacing
        for (j in 0 until numBricksPerRow) {
            val brickRect = androidx.compose.ui.geometry.Rect(left = currentX, top = currentY, right = currentX + brickWidth, bottom = currentY + brickHeight)
            bricks.add(Brick(rect = brickRect.toRectF(), color = colors[i % colors.size]))
            currentX += brickWidth + brickSpacing
        }
        currentY += brickHeight + brickSpacing
    }

    val paddleWidth = canvasWidth / 5
    val paddleHeight = canvasHeight / 40
    val paddleX = (canvasWidth - paddleWidth) / 2
    val paddleY = canvasHeight - paddleHeight * 6
    val paddleRect = androidx.compose.ui.geometry.Rect(paddleX, paddleY, paddleX + paddleWidth, paddleY + paddleHeight)
    val paddle = Paddle(rect = paddleRect.toRectF())

    val ballRadius = canvasWidth / 30
    val ball = Ball(cx = canvasWidth / 2, cy = canvasHeight / 2, radius = ballRadius, dx = 8f, dy = -8f)

    return GameState(bricks = bricks, ball = ball, paddle = paddle, gameInitialized = true)
}

/**
 * Отрисовывает все игровые объекты на Canvas.
 */
private fun drawGame(drawScope: DrawScope, gameState: GameState, textMeasurer: TextMeasurer, colors: Map<String, Color>) {
    gameState.bricks.forEach { brick ->
        if (brick.isVisible) {
            drawScope.drawRoundRect(
                color = brick.color,
                topLeft = Offset(brick.rect.left, brick.rect.top),
                size = Size(brick.rect.width(), brick.rect.height()),
                cornerRadius = CornerRadius(8f, 8f)
            )
        }
    }
    gameState.paddle?.let { paddle ->
        drawScope.drawRoundRect(
            color = colors["paddle"]!!,
            topLeft = Offset(paddle.rect.left, paddle.rect.top),
            size = Size(paddle.rect.width(), paddle.rect.height()),
            cornerRadius = CornerRadius(15f, 15f)
        )
    }
    gameState.ball?.let { ball ->
        drawScope.drawCircle(color = colors["ball"]!!, radius = ball.radius, center = Offset(ball.cx, ball.cy))
    }

    val topOffset = 40f

    drawScope.drawText(
        textMeasurer = textMeasurer,
        text = "Score: ${gameState.score}",
        topLeft = Offset(40f, topOffset),
        style = TextStyle(fontSize = 20.sp, color = colors["textPrimary"]!!)
    )

    val heartIcon = "❤️"
    val livesText = buildString {
        repeat(gameState.lives) {
            append(heartIcon)
        }
    }
    drawScope.drawText(
        textMeasurer = textMeasurer,
        text = livesText,
        topLeft = Offset(40f, topOffset + 65f),
        style = TextStyle(fontSize = 20.sp)
    )

    val iconLeft = drawScope.size.width - 80f
    val iconTop = topOffset
    val iconSize = 50f

    if (gameState.status == GameStatus.RUNNING) {
        drawScope.drawRoundRect(colors["paddle"]!!, topLeft = Offset(iconLeft, iconTop), size = Size(15f, iconSize), cornerRadius = CornerRadius(5f, 5f))
        drawScope.drawRoundRect(colors["paddle"]!!, topLeft = Offset(iconLeft + 25f, iconTop), size = Size(15f, iconSize), cornerRadius = CornerRadius(5f, 5f))
    }

    if (gameState.status == GameStatus.PAUSED) {
        val path = Path().apply {
            moveTo(iconLeft, iconTop)
            lineTo(iconLeft, iconTop + iconSize)
            lineTo(iconLeft + iconSize * 0.8f, iconTop + iconSize / 2)
            close()
        }
        drawScope.drawPath(path, color = colors["paddle"]!!)
    }

    when (gameState.status) {
        GameStatus.READY -> {
            val text = "Tap to Start"
            val textStyle = TextStyle(fontSize = 28.sp, color = colors["textHint"]!!)
            val textLayoutResult = textMeasurer.measure(text, textStyle)
            drawScope.drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(
                    x = (drawScope.size.width - textLayoutResult.size.width) / 2,
                    y = drawScope.size.height * 0.65f
                )
            )
        }
        GameStatus.GAME_OVER -> {
            drawScope.drawRect(color = colors["background"]!!.copy(alpha = 0.8f))
            val text = "Game Over"
            val textStyle = TextStyle(fontSize = 48.sp, color = colors["ball"]!!)
            val textLayoutResult = textMeasurer.measure(text, textStyle)
            drawScope.drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(
                    x = (drawScope.size.width - textLayoutResult.size.width) / 2,
                    y = (drawScope.size.height - textLayoutResult.size.height) / 2
                )
            )
        }
        GameStatus.PAUSED -> {
            drawScope.drawRect(color = colors["background"]!!.copy(alpha = 0.8f))
            val text = "Paused"
            val textStyle = TextStyle(fontSize = 48.sp, color = colors["textPrimary"]!!)
            val textLayoutResult = textMeasurer.measure(text, textStyle)
            drawScope.drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(
                    x = (drawScope.size.width - textLayoutResult.size.width) / 2,
                    y = (drawScope.size.height - textLayoutResult.size.height) / 2
                )
            )
        }
        else -> {}
    }
}

/**
 * Конвертирует Compose Rect в Android Graphics RectF.
 */
fun androidx.compose.ui.geometry.Rect.toRectF(): android.graphics.RectF {
    return android.graphics.RectF(this.left, this.top, this.right, this.bottom)
}