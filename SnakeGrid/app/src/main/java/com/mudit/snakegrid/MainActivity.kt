package com.mudit.snakegrid

import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mudit.snakegrid.ui.theme.SnakeGridTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SnakeGridTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Grid(10, 10)
                }
            }
        }
    }
}

enum class Direction {
    LEFT,
    RIGHT,
    UP,
    DOWN
}

@Composable
fun Grid(row: Int, col: Int) {

    var tick by remember {
        mutableIntStateOf(1)
    }

    val snake = intArrayOf(11,12,13,14)

    val direction by remember {
        mutableStateOf(Direction.RIGHT)
    }

    LaunchedEffect("Timer") {
        while (true) {
            delay(350)
            if (direction == Direction.RIGHT) {
                for (i in 0 until snake.size - 1) {
                    snake[i] = snake[i + 1]
                }
                if (snake.last() % (row - 1) == 0) {
                    snake[snake.size - 1] = snake[snake.size - 1] / row
                } else {
                    snake[snake.size - 1] = snake.last() + 1
                }
            }else if(direction == Direction.LEFT){
                for (i in 0 until snake.size - 1) {
                    snake[i] = snake[i + 1]
                }
                if (snake.last() % (row - 1) == 0) {
                    snake[snake.size - 1] = snake[snake.size - 1] / row
                } else {
                    snake[snake.size - 1] = snake.last() + 1
                }
            }


            tick = tick + 1
            Log.e("mudit", tick.toString())
        }
    }


    var measuredHeight by remember {
        mutableIntStateOf(0)
    }


    Row {
       
        Text(text = "Mudit Sen $tick", color = Color.Black)
        
        Button(onClick = {  }) {
            
        }
    }



    
    LazyVerticalGrid(columns = GridCells.Fixed(10), Modifier.onGloballyPositioned {
        measuredHeight =
            (it.size.width / (10 * Resources.getSystem().displayMetrics.density)).toInt()
    }) {
        tick
        items(row * col) {

            val modifier = if (snake.contains(it)) {
                Modifier
                    .background(Color.White)
            } else {
                Modifier
                    .background(Color.Black)
            }
            Box(
                modifier = modifier
                    .height(Dp(measuredHeight.toFloat()))
                    .border(0.1.dp, Color.White)
            )

        }
    }


}


@Preview(showBackground = true)
@Composable
fun Grid10x10Preview() {
    SnakeGridTheme {
        Grid(10, 10)
    }
}