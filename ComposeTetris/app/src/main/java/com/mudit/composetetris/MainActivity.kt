package com.mudit.composetetris

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mudit.composetetris.ui.theme.ComposeTetrisTheme
import kotlinx.coroutines.delay


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        setContent {
            ComposeTetrisTheme {
                // A surface container using the 'background' color from the theme
                Scaffold(containerColor = Color(0xff222f3e)) {
                    Navigator(it)
                }

            }
        }
    }
}

sealed class Screen(val route: String) {
    data object Screen1 : Screen("menu")
    data object Screen2 : Screen("game")
}

@Composable
fun Navigator(paddingValues: PaddingValues) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Screen1.route) {
        composable(Screen.Screen1.route) {
            Menu(navController)
        }
        composable(Screen.Screen2.route) {
            Grid(navController)
        }
    }
}

@Composable
fun Menu(navController: NavController? = null) {
    Column(
        modifier = Modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Image(
            contentDescription = null,
            contentScale = ContentScale.Fit,
            painter = painterResource(id = R.drawable.img_block_blitz),
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        )

        Image(
            contentDescription = null,
            contentScale = ContentScale.Fit,
            painter = painterResource(id = R.drawable.img_play),
            modifier = Modifier
                .fillMaxWidth(0.2f)
                .clickable {
                    navController?.navigate(Screen.Screen2.route)
                },
        )

        Image(
            contentDescription = null,
            contentScale = ContentScale.Fit,
            painter = painterResource(id = R.drawable.img_high_score),
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .clickable {
                    navController?.navigate(Screen.Screen2.route)
                },
        )

    }
}

@Composable
fun Grid(navController: NavController? = null, viewModel: TetrisViewModel = viewModel()) {

    var currentPiece by remember {
        mutableStateOf(viewModel.getRandomPiece())
    }

    var grid by remember {
        mutableStateOf(Array(TetrisViewModel.ROWS) {
            IntArray(TetrisViewModel.COLUMNS)
        })
    }

    LaunchedEffect("") {
        while (true) {
            delay(viewModel.speed)
            currentPiece = viewModel.movePiece(currentPiece, DIRECTION.DOWN, grid)
            if (currentPiece.placed) {
                for (i in currentPiece.position) {
                    grid[i.row][i.column] =
                        currentPiece.type.intType
                }
                currentPiece = viewModel.getRandomPiece()
            }
            grid = viewModel.removeLines(grid)
            if (viewModel.gameOver(currentPiece, grid)) {
                grid = Array(TetrisViewModel.ROWS) {
                    IntArray(TetrisViewModel.COLUMNS)
                }
                viewModel.score = 0
            }
        }
    }

    Column(
        modifier = Modifier
            .background(Color(0xff222f3e))
    ) {
        Text(
            text = "Score : ${viewModel.score}",
            color = Color.White,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight(600),
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .width(100.dp)
        ) {

            Spacer(
                modifier = Modifier
                    .weight(12.5f)
                    .wrapContentHeight()
                    .background(Color.Blue)
            )

            Color(0xff48dbfb)
            Box(
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier
                    .border(5.dp, Color.Gray, shape = RoundedCornerShape(5.dp))
                    .weight(70f)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(10),
                    userScrollEnabled = false,
                    modifier = Modifier
                        .padding(5.dp)
                        .fillMaxWidth()
                ) {
                    items(TetrisViewModel.ROWS * TetrisViewModel.COLUMNS) { index ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .background(Color(0x55576574))

                        ) {
                            // Text(text = "$index", fontSize = 8.sp)
                            if (currentPiece.position.contains(index)) {
                                Pixel(pixelRes = currentPiece.pixelRes, currentPiece.color)
                            } else if (grid[index.row][index.column] > 0) {
                                Pixel(
                                    pixelRes = R.drawable.element_grey_square,
                                    viewModel.getColor(grid[index.row][index.column])
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .border(
                                            0.1.dp,
                                            Color.Gray,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                )
                            }

                        }
                    }
                }
            }


            Spacer(
                modifier = Modifier
                    .weight(12.5f)
                    .wrapContentHeight()
                    .background(Color.Blue)
            )
        }


        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(top = 10.dp)
                .wrapContentHeight()
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Icon(
                    modifier = Modifier
                        .clickable {
                            currentPiece =
                                viewModel.movePiece(currentPiece, DIRECTION.LEFT, grid)
                        }
                        .padding(10.dp)
                        .weight(1f)
                        .height(50.dp)
                        .padding(10.dp),
                    tint = Color.White,
                    imageVector = Icons.AutoMirrored.Default.KeyboardArrowLeft,
                    contentDescription = null
                )

                Icon(
                    modifier = Modifier
                        .clickable {
                            currentPiece =
                                viewModel.rotatePiece(currentPiece, grid)
                        }
                        .padding(10.dp)
                        .weight(1f)
                        .height(50.dp)
                        .padding(10.dp),
                    tint = Color.White,
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null
                )
                Icon(
                    modifier = Modifier
                        .clickable {
                            currentPiece =
                                viewModel.movePiece(currentPiece, DIRECTION.RIGHT, grid)
                        }
                        .padding(10.dp)
                        .weight(1f)
                        .height(50.dp)
                        .padding(10.dp),
                    tint = Color.White,
                    imageVector = Icons.AutoMirrored.Default.KeyboardArrowRight,
                    contentDescription = null
                )
            }
            Icon(
                modifier = Modifier
                    .height(50.dp)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .clickable {
                        currentPiece = viewModel.movePiece(currentPiece, DIRECTION.DOWN, grid)
                    }
                    .fillMaxWidth(),

                tint = Color.White,
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null
            )
        }

    }


}


@Preview
@Composable
fun Preview() {
    ComposeTetrisTheme {
        // A surface container using the 'background' color from the theme
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Grid()
        }
    }
}

@Preview
@Composable
fun Preview2() {
    ComposeTetrisTheme {
        // A surface container using the 'background' color from the theme
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Menu()
        }
    }
}