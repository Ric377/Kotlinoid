package com.example.kotlinoid

import android.graphics.RectF
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.example.kotlinoid.ui.theme.KotlinoidTheme
import kotlinx.coroutines.delay

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
                detectTapGestures {
                    if (gameState.status == GameStatus.READY) {
                        gameState = gameState.copy(status = GameStatus.RUNNING)
                    }
                }
            }
    ) {
        if (!gameState.gameInitialized) {
            canvasSize = size
            gameState = initializeGame(size)
        }
        drawGame(this, gameState, textMeasurer)
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
    val brickTopOffset = canvasHeight * 0.1f // Сдвинем кирпичи чуть ниже для текста
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
private fun drawGame(drawScope: DrawScope, gameState: GameState, textMeasurer: TextMeasurer) {
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

    drawScope.drawText(
        textMeasurer = textMeasurer,
        text = "Score: ${gameState.score}",
        topLeft = Offset(20f, 10f),
        style = TextStyle(fontSize = 20.sp, color = Color.Black)
    )
    drawScope.drawText(
        textMeasurer = textMeasurer,
        text = "Lives: ${gameState.lives}",
        topLeft = Offset(drawScope.size.width - 150f, 10f),
        style = TextStyle(fontSize = 20.sp, color = Color.Red)
    )

    when (gameState.status) {
        GameStatus.READY -> {
            val textLayoutResult = textMeasurer.measure("Tap to Start")
            drawScope.drawText(
                textLayoutResult = textLayoutResult,
                color = Color.Black,
                topLeft = Offset(
                    x = (drawScope.size.width - textLayoutResult.size.width) / 2,
                    y = (drawScope.size.height - textLayoutResult.size.height) / 2
                )
            )
        }
        GameStatus.GAME_OVER -> {
            drawScope.drawRect(color = Color.White.copy(alpha = 0.7f))
            val textLayoutResult = textMeasurer.measure("Game Over")
            drawScope.drawText(
                textLayoutResult = textLayoutResult,
                color = Color.Red,
                topLeft = Offset(
                    x = (drawScope.size.width - textLayoutResult.size.width) / 2,
                    y = (drawScope.size.height - textLayoutResult.size.height) / 2
                )
            )
        }
        GameStatus.RUNNING -> {}
    }
}

/**
 * Конвертирует Compose Rect в Android Graphics RectF.
 */
fun androidx.compose.ui.geometry.Rect.toRectF(): android.graphics.RectF {
    return android.graphics.RectF(this.left, this.top, this.right, this.bottom)
}