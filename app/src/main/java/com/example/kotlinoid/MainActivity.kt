package com.example.kotlinoid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
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
import com.example.kotlinoid.ui.theme.KotlinoidTheme
import kotlinx.coroutines.delay

/**
 * Содержит полное состояние игрового поля в определенный момент времени.
 *
 * @property bricks список всех кирпичей на поле.
 * @property ball игровой мяч.
 * @property paddle платформа игрока.
 * @property gameInitialized флаг, показывающий, было ли состояние игры инициализировано.
 */
data class GameState(
    val bricks: MutableList<Brick> = mutableListOf(),
    var ball: Ball? = null,
    var paddle: Paddle? = null,
    var gameInitialized: Boolean = false
)

/**
 * Главная и единственная Activity приложения.
 * Отвечает за запуск и отображение Composable-контента.
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
 * Вся игровая логика и отрисовка происходят внутри этого компонента.
 */
@Composable
fun GameScreen() {
    var gameState by remember { mutableStateOf(GameState()) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    LaunchedEffect(Unit) {
        while (true) {
            if (gameState.gameInitialized) {
                val newGameState = updateGameState(gameState, canvasSize)
                gameState = newGameState
            }
            delay(16)
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        if (!gameState.gameInitialized) {
            canvasSize = size
            initializeGame(this, gameState)
            gameState = gameState.copy(gameInitialized = true)
        }
        drawGame(this, gameState)
    }
}

/**
 * Обновляет состояние игры для следующего кадра.
 * Отвечает за движение объектов и обработку столкновений.
 *
 * @param currentState Текущее состояние игры.
 * @param canvasSize Размеры игрового поля.
 * @return Новое, обновленное состояние игры.
 */
private fun updateGameState(currentState: GameState, canvasSize: Size): GameState {
    val ball = currentState.ball ?: return currentState

    ball.cx += ball.dx
    ball.cy += ball.dy

    val canvasWidth = canvasSize.width
    val canvasHeight = canvasSize.height

    if (ball.cx - ball.radius < 0 || ball.cx + ball.radius > canvasWidth) {
        ball.dx = -ball.dx
    }

    if (ball.cy - ball.radius < 0) {
        ball.dy = -ball.dy
    }

    // TODO: Добавить логику проигрыша при столкновении с нижней стеной

    return currentState.copy()
}


/**
 * Инициализирует начальное состояние игры, создавая кирпичи, мяч и платформу.
 * Эта функция должна вызываться только один раз при старте.
 *
 * @param drawScope контекст рисования, предоставляющий размеры Canvas.
 * @param gameState текущее состояние игры для его наполнения объектами.
 */
private fun initializeGame(drawScope: DrawScope, gameState: GameState) {
    val canvasWidth = drawScope.size.width
    val canvasHeight = drawScope.size.height

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
            val brickRect = androidx.compose.ui.geometry.Rect(
                left = currentX,
                top = currentY,
                right = currentX + brickWidth,
                bottom = currentY + brickHeight
            )
            gameState.bricks.add(Brick(rect = brickRect.toRectF(), color = colors[i]))
            currentX += brickWidth + brickSpacing
        }
        currentY += brickHeight + brickSpacing
    }

    val paddleWidth = canvasWidth / 5
    val paddleHeight = canvasHeight / 40
    val paddleX = (canvasWidth - paddleWidth) / 2
    val paddleY = canvasHeight - paddleHeight * 3
    val paddleRect = androidx.compose.ui.geometry.Rect(paddleX, paddleY, paddleX + paddleWidth, paddleY + paddleHeight)
    gameState.paddle = Paddle(rect = paddleRect.toRectF())

    val ballRadius = canvasWidth / 30
    gameState.ball = Ball(
        cx = canvasWidth / 2,
        cy = canvasHeight / 2,
        radius = ballRadius,
        dx = 8f,
        dy = -8f
    )
}

/**
 * Отрисовывает все игровые объекты на Canvas на основе текущего состояния игры.
 *
 * @param drawScope контекст для выполнения команд рисования.
 * @param gameState текущее состояние игры, которое необходимо отрисовать.
 */
private fun drawGame(drawScope: DrawScope, gameState: GameState) {
    gameState.bricks.forEach { brick ->
        if (brick.isVisible) {
            drawScope.drawRect(
                color = brick.color,
                topLeft = Offset(brick.rect.left, brick.rect.top),
                size = Size(brick.rect.width(), brick.rect.height())
            )
        }
    }

    gameState.paddle?.let { paddle ->
        drawScope.drawRect(
            color = Color.Black,
            topLeft = Offset(paddle.rect.left, paddle.rect.top),
            size = Size(paddle.rect.width(), paddle.rect.height())
        )
    }

    gameState.ball?.let { ball ->
        drawScope.drawCircle(
            color = Color.Magenta,
            radius = ball.radius,
            center = Offset(ball.cx, ball.cy)
        )
    }
}

/**
 * Конвертирует Compose Rect в Android Graphics RectF для совместимости с моделью данных.
 */
fun androidx.compose.ui.geometry.Rect.toRectF(): android.graphics.RectF {
    return android.graphics.RectF(this.left, this.top, this.right, this.bottom)
}