package com.example.kotlinoid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.kotlinoid.ui.theme.KotlinoidTheme

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
    Canvas(modifier = Modifier.fillMaxSize()) {
        // TODO: Implement game objects rendering logic.
    }
}

