package com.example.kotlinoid

import android.graphics.RectF
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import com.example.kotlinoid.ui.theme.KotlinoidTheme
import kotlinx.coroutines.delay

/**
 * Содержит полное состояние игрового поля в определенный момент времени.
 */
data class GameState(
    val bricks: List<Brick> = emptyList(), // <-- Изменили MutableList на List
    val ball: Ball? = null,
    val paddle: Paddle? = null,
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
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    GameScreen()
                }
            }
        }
    }
}

/**
 * Основной Composable-компонент, отображающий игровое поле.
 */
@Composable
fun GameScreen() {
    var gameState by remember { mutableStateOf(GameState()) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    LaunchedEffect(Unit) {
        while (true) {
            if (gameState.gameInitialized) {
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
    ) {
        if (!gameState.gameInitialized) {
            canvasSize = size
            gameState = initializeGame(size)
        }
        drawGame(this, gameState)
    }
}

/**
 * Обновляет состояние игры для следующего кадра.
 */
private fun updateGameState(currentState: GameState, canvasSize: Size): GameState {
    var currentBall = currentState.ball ?: return currentState
    var currentBricks = currentState.bricks
    val paddle = currentState.paddle ?: return currentState

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
        currentBall = currentBall.copy(cx = canvasWidth / 2, cy = canvasHeight / 2)
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
    val newBricks = currentBricks.map { brick ->
        if (!brickHit && brick.isVisible && ballRect.intersect(brick.rect)) {
            brickHit = true
            brick.copy(isVisible = false)
        } else {
            brick
        }
    }

    if (brickHit) {
        currentBall = currentBall.copy(dy = -currentBall.dy)
    }

    return currentState.copy(ball = currentBall, bricks = newBricks)
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
    val brickTopOffset = canvasHeight * 0.05f
    val spaceForBricks = canvasWidth - (numBricksPerRow + 1) * brickSpacing
    val brickWidth = spaceForBricks / numBricksPerRow
    val brickHeight = canvasHeight * 0.025f
    val colors = listOf(Color.Red, Color.Red, Color.Yellow, Color.Yellow, Color.Green, Color.Green, Color.Blue, Color.Blue)

    var currentY = brickTopOffset
    for (i in 0 until numRows) {
        var currentX = brickSpacing
        for (j in 0 until numBricksPerRow) {
            val brickRect = androidx.compose.ui.geometry.Rect(left = currentX, top = currentY, right = currentX + brickWidth, bottom = currentY + brickHeight)
            bricks.add(Brick(rect = brickRect.toRectF(), color = colors[i]))
            currentX += brickWidth + brickSpacing
        }
        currentY += brickHeight + brickSpacing
    }

    val paddleWidth = canvasWidth / 5
    val paddleHeight = canvasHeight / 40
    val paddleX = (canvasWidth - paddleWidth) / 2
    val paddleY = canvasHeight - paddleHeight * 3
    val paddleRect = androidx.compose.ui.geometry.Rect(paddleX, paddleY, paddleX + paddleWidth, paddleY + paddleHeight)
    val paddle = Paddle(rect = paddleRect.toRectF())

    val ballRadius = canvasWidth / 30
    val ball = Ball(cx = canvasWidth / 2, cy = canvasHeight / 2, radius = ballRadius, dx = 8f, dy = -8f)

    return GameState(bricks = bricks, ball = ball, paddle = paddle, gameInitialized = true)
}

/**
 * Отрисовывает все игровые объекты на Canvas.
 */
private fun drawGame(drawScope: DrawScope, gameState: GameState) {
    gameState.bricks.forEach { brick ->
        if (brick.isVisible) {
            drawScope.drawRect(color = brick.color, topLeft = Offset(brick.rect.left, brick.rect.top), size = Size(brick.rect.width(), brick.rect.height()))
        }
    }
    gameState.paddle?.let { paddle ->
        drawScope.drawRect(color = Color.Black, topLeft = Offset(paddle.rect.left, paddle.rect.top), size = Size(paddle.rect.width(), paddle.rect.height()))
    }
    gameState.ball?.let { ball ->
        drawScope.drawCircle(color = Color.Magenta, radius = ball.radius, center = Offset(ball.cx, ball.cy))
    }
}

/**
 * Конвертирует Compose Rect в Android Graphics RectF.
 */
fun androidx.compose.ui.geometry.Rect.toRectF(): android.graphics.RectF {
    return android.graphics.RectF(this.left, this.top, this.right, this.bottom)
}