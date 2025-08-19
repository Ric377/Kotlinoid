package com.example.kotlinoid

import android.app.Activity
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutResult
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
        "textHint" to Color(0xFF757575),
        "button" to Color(0xFF424242),
        "buttonText" to Color(0xFFFFFFFF)
    )

    val DarkPalette = mapOf(
        "background" to Color(0xFF121212),
        "paddle" to Color(0xFFE0E0E0),
        "ball" to Color(0xFFEF5350),
        "textPrimary" to Color(0xFFFFFFFF),
        "textHint" to Color(0xFFBDBDBD),
        "button" to Color(0xFFE0E0E0),
        "buttonText" to Color(0xFF121212)
    )

    val brickSet1 = listOf(Color(0xFFD32F2F), Color(0xFFD32F2F), Color(0xFFF57C00), Color(0xFFF57C00))
    val brickSet2 = listOf(Color(0xFFFBC02D), Color(0xFFFBC02D), Color(0xFF388E3C), Color(0xFF388E3C))
}

/**
 * Хранит дизайны уровней.
 */
object Levels {
    val maps = listOf(
        listOf(
            "XXXXXXXXXX",
            "X-X-XX-X-X",
            "XXXXXXXXXX",
            "X-X-XX-X-X",
            "XXXXXXXXXX",
            "X-X-XX-X-X",
            "XXXXXXXXXX",
            "X-X-XX-X-X"
        ),
        listOf(
            "----XX----",
            "---XXXX---",
            "--XXXXXX--",
            "-XXXXXXXX-",
            "XXXXXXXXXX",
            "---XXXX---",
            "---XXXX---",
            "---XXXX---"
        ),
        listOf(
            "X-X-X-X-X-",
            "-X-X-X-X-X",
            "X-X-X-X-X-",
            "-X-X-X-X-X",
            "X-X-X-X-X-",
            "-X-X-X-X-X",
            "X-X-X-X-X-",
            "-X-X-X-X-X"
        )
    )
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
    val currentLevel: Int = 1,
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
    val context = LocalContext.current

    var tapCount by remember { mutableStateOf(0) }
    var lastTapTime by remember { mutableStateOf(0L) }

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
                detectTapGestures(
                    onTap = { offset ->
                        val pauseIconLeft = size.width - 100f
                        val pauseIconTop = 20f
                        val isIconTapped = offset.x > pauseIconLeft && offset.y < pauseIconTop + 80f

                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastTapTime < 400L) {
                            tapCount++
                        } else {
                            tapCount = 1
                        }
                        lastTapTime = currentTime

                        if (tapCount == 5) {
                            tapCount = 0
                            if (gameState.status == GameStatus.RUNNING) {
                                val nextLevel = gameState.currentLevel + 1
                                gameState = if (nextLevel > Levels.maps.size) {
                                    gameState.copy(status = GameStatus.GAME_WON)
                                } else {
                                    gameState.copy(status = GameStatus.LEVEL_COMPLETE)
                                }
                                return@detectTapGestures
                            }
                        }

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
                            GameStatus.LEVEL_COMPLETE, GameStatus.GAME_WON -> {
                                val nextLevel = gameState.currentLevel + 1
                                gameState = if (nextLevel > Levels.maps.size && gameState.status != GameStatus.GAME_WON) {
                                    gameState.copy(status = GameStatus.GAME_WON)
                                } else if (gameState.status != GameStatus.GAME_WON) {
                                    initializeLevel(canvasSize, nextLevel).copy(
                                        score = gameState.score,
                                        lives = gameState.lives
                                    )
                                } else {
                                    initializeLevel(canvasSize, 1)
                                }
                            }
                            GameStatus.GAME_OVER -> {
                                val buttonWidth = size.width / 2.5f
                                val buttonHeight = 150f
                                val buttonY = size.height * 0.6f
                                val restartButtonX = size.width / 2 - buttonWidth - 20f
                                val exitButtonX = size.width / 2 + 20f

                                val restartRect = Rect(restartButtonX, buttonY, restartButtonX + buttonWidth, buttonY + buttonHeight)
                                val exitRect = Rect(exitButtonX, buttonY, exitButtonX + buttonWidth, buttonY + buttonHeight)

                                if (restartRect.contains(offset)) {
                                    gameState = initializeLevel(canvasSize, 1)
                                } else if (exitRect.contains(offset)) {
                                    (context as? Activity)?.finish()
                                }
                            }
                        }
                    }
                )
            }
    ) {
        if (!gameState.gameInitialized) {
            canvasSize = size
            gameState = initializeLevel(size, 1)
        }
        drawGame(this, gameState, textMeasurer, colors)
    }
}

/**
 * Обновляет состояние игры для следующего кадра.
 */
private fun updateGameState(currentState: GameState, canvasSize: Size): GameState {
    var currentBall = currentState.ball ?: return currentState
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
        return if (lives <= 0) {
            currentState.copy(lives = 0, status = GameStatus.GAME_OVER)
        } else {
            val newBall = currentBall.copy(cx = canvasWidth / 2, cy = canvasHeight / 2)
            currentState.copy(ball = newBall, lives = lives, status = GameStatus.READY)
        }
    }

    val ballRect = RectF(
        currentBall.cx - currentBall.radius,
        currentBall.cy - currentBall.radius,
        currentBall.cx + currentBall.radius,
        currentBall.cy + currentBall.radius
    )

    if (currentBall.dy > 0 && ballRect.intersect(currentState.paddle!!.rect)) {
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

    if (newBricks.none { it.isVisible }) {
        val nextLevel = currentState.currentLevel + 1
        return if (nextLevel > Levels.maps.size) {
            currentState.copy(status = GameStatus.GAME_WON, bricks = newBricks, score = score)
        } else {
            currentState.copy(status = GameStatus.LEVEL_COMPLETE, bricks = newBricks, score = score)
        }
    }

    return currentState.copy(ball = currentBall, bricks = newBricks, score = score, lives = lives)
}


/**
 * Инициализирует состояние игры для конкретного уровня.
 */
private fun initializeLevel(canvasSize: Size, level: Int): GameState {
    val canvasWidth = canvasSize.width
    val canvasHeight = canvasSize.height
    val bricks = mutableListOf<Brick>()
    val levelMap = Levels.maps[level - 1]

    val numRows = levelMap.size
    val numBricksPerRow = levelMap[0].length
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
            if (levelMap[i][j] == 'X') {
                val brickRect = Rect(left = currentX, top = currentY, right = currentX + brickWidth, bottom = currentY + brickHeight)
                bricks.add(Brick(rect = brickRect.toRectF(), color = colors[i % colors.size]))
            }
            currentX += brickWidth + brickSpacing
        }
        currentY += brickHeight + brickSpacing
    }

    val paddleWidth = canvasWidth / 5
    val paddleHeight = canvasHeight / 40
    val paddleX = (canvasWidth - paddleWidth) / 2
    val paddleY = canvasHeight - paddleHeight * 6
    val paddleRect = Rect(paddleX, paddleY, paddleX + paddleWidth, paddleY + paddleHeight)
    val paddle = Paddle(rect = paddleRect.toRectF())

    val ballRadius = canvasWidth / 30
    val ball = Ball(cx = canvasWidth / 2, cy = canvasHeight / 2, radius = ballRadius, dx = 8f, dy = -8f)

    return GameState(
        bricks = bricks,
        ball = ball,
        paddle = paddle,
        currentLevel = level,
        gameInitialized = true,
        status = GameStatus.READY
    )
}

/**
 * Отрисовывает все игровые объекты на Canvas.
 */
private fun drawGame(drawScope: DrawScope, gameState: GameState, textMeasurer: TextMeasurer, colors: Map<String, Color>) {
    gameState.bricks.forEach { brick ->
        if (brick.isVisible) {
            drawScope.drawRoundRect(color = brick.color, topLeft = Offset(brick.rect.left, brick.rect.top), size = Size(brick.rect.width(), brick.rect.height()), cornerRadius = CornerRadius(8f, 8f))
        }
    }
    gameState.paddle?.let { paddle ->
        drawScope.drawRoundRect(color = colors["paddle"]!!, topLeft = Offset(paddle.rect.left, paddle.rect.top), size = Size(paddle.rect.width(), paddle.rect.height()), cornerRadius = CornerRadius(15f, 15f))
    }
    gameState.ball?.let { ball ->
        drawScope.drawCircle(color = colors["ball"]!!, radius = ball.radius, center = Offset(ball.cx, ball.cy))
    }

    val topOffset = 40f

    drawScope.drawText(textMeasurer = textMeasurer, text = "Score: ${gameState.score}", topLeft = Offset(40f, topOffset), style = TextStyle(fontSize = 20.sp, color = colors["textPrimary"]!!))

    val heartIcon = "❤️"
    val livesText = buildString { repeat(gameState.lives) { append(heartIcon) } }
    drawScope.drawText(textMeasurer = textMeasurer, text = livesText, topLeft = Offset(40f, topOffset + 65f), style = TextStyle(fontSize = 20.sp))

    val iconLeft = drawScope.size.width - 80f
    val iconTop = topOffset
    val iconSize = 50f

    if (gameState.status == GameStatus.RUNNING) {
        drawScope.drawRoundRect(colors["paddle"]!!, topLeft = Offset(iconLeft, iconTop), size = Size(15f, iconSize), cornerRadius = CornerRadius(5f, 5f))
        drawScope.drawRoundRect(colors["paddle"]!!, topLeft = Offset(iconLeft + 25f, iconTop), size = Size(15f, iconSize), cornerRadius = CornerRadius(5f, 5f))
    }

    if (gameState.status == GameStatus.PAUSED) {
        val path = Path().apply {
            moveTo(iconLeft, iconTop); lineTo(iconLeft, iconTop + iconSize); lineTo(iconLeft + iconSize * 0.8f, iconTop + iconSize / 2); close()
        }
        drawScope.drawPath(path, color = colors["paddle"]!!)
    }

    val overlayAlpha = 0.8f
    val center = Offset(drawScope.size.width / 2, drawScope.size.height / 2)

    fun drawOverlayText(text: String, style: TextStyle) {
        val layoutResult = textMeasurer.measure(text, style)
        drawScope.drawText(layoutResult, topLeft = Offset(center.x - layoutResult.size.width / 2, center.y - layoutResult.size.height / 2))
    }

    fun drawCenteredText(text: String, style: TextStyle, yOffset: Float, xOffset: Float = 0f) {
        val layoutResult = textMeasurer.measure(text, style)
        drawScope.drawText(layoutResult, topLeft = Offset(center.x - layoutResult.size.width / 2 + xOffset, yOffset))
    }

    when (gameState.status) {
        GameStatus.READY -> {
            drawCenteredText("Level ${gameState.currentLevel}", TextStyle(fontSize = 28.sp, color = colors["textHint"]!!), drawScope.size.height * 0.6f)
            drawCenteredText("Tap to Start", TextStyle(fontSize = 28.sp, color = colors["textHint"]!!), drawScope.size.height * 0.65f)
        }
        GameStatus.GAME_OVER -> {
            drawScope.drawRect(color = colors["background"]!!.copy(alpha = overlayAlpha))
            drawOverlayText("Game Over", TextStyle(fontSize = 48.sp, color = colors["ball"]!!))

            val buttonWidth = drawScope.size.width / 2.5f
            val buttonHeight = 150f
            val buttonY = drawScope.size.height * 0.6f
            val restartButtonX = center.x - buttonWidth - 20f
            val exitButtonX = center.x + 20f

            drawScope.drawRoundRect(colors["button"]!!, topLeft = Offset(restartButtonX, buttonY), size = Size(buttonWidth, buttonHeight), cornerRadius = CornerRadius(15f, 15f))
            drawScope.drawRoundRect(colors["button"]!!, topLeft = Offset(exitButtonX, buttonY), size = Size(buttonWidth, buttonHeight), cornerRadius = CornerRadius(15f, 15f))

            drawCenteredText("Restart", TextStyle(fontSize = 24.sp, color = colors["buttonText"]!!), buttonY + buttonHeight / 2 - 30, (restartButtonX - center.x) + buttonWidth / 2)
            drawCenteredText("Exit", TextStyle(fontSize = 24.sp, color = colors["buttonText"]!!), buttonY + buttonHeight / 2 - 30, (exitButtonX - center.x) + buttonWidth / 2)
        }
        GameStatus.PAUSED -> {
            drawScope.drawRect(color = colors["background"]!!.copy(alpha = overlayAlpha))
            drawOverlayText("Paused", TextStyle(fontSize = 48.sp, color = colors["textPrimary"]!!))
        }
        GameStatus.LEVEL_COMPLETE -> {
            drawScope.drawRect(color = colors["background"]!!.copy(alpha = overlayAlpha))
            drawOverlayText("Level ${gameState.currentLevel} Complete!", TextStyle(fontSize = 36.sp, color = colors["textPrimary"]!!))
            drawCenteredText("Tap to continue", TextStyle(fontSize = 24.sp, color = colors["textHint"]!!), drawScope.size.height * 0.65f)
        }
        GameStatus.GAME_WON -> {
            drawScope.drawRect(color = colors["background"]!!.copy(alpha = overlayAlpha))
            drawOverlayText("You Win!", TextStyle(fontSize = 48.sp, color = AppColors.brickSet2[3]))
            drawCenteredText("Tap to play again", TextStyle(fontSize = 24.sp, color = colors["textHint"]!!), drawScope.size.height * 0.65f)
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