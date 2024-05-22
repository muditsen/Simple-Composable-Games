package com.mudit.composrtictactoe

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mudit.composrtictactoe.ui.theme.ComposrTicTacToeTheme

/***********************************************************************
 *
 * Step1: Create 3x3 Grid
 */

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ComposrTicTacToeTheme {

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                ) { innerPadding ->
                    GradientBackground {
                        Navigator(innerPadding)
                    }

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
            Game(navController)
        }
    }
}


const val SIZE = 3

fun checkWinner(displayXorO: Array<CharArray>): Boolean {

    for (i in 0 until SIZE) {
        var isWon = true
        for (j in 0 until SIZE - 1) {
            if (displayXorO[i][j] == ' ' || displayXorO[i][j] != displayXorO[i][j + 1]) {
                isWon = false
            }
        }
        if (isWon) {
            Log.e("Mudit Log", "ROW")
            return true
        }
    }

    for (i in 0 until SIZE) {
        var isWon = true
        for (j in 0 until SIZE - 1) {
            if (displayXorO[j][i] == ' ' || displayXorO[j][i] != displayXorO[j + 1][i]) {
                isWon = false
            }
        }
        if (isWon) {
            Log.e("Mudit Log", "COLUMN")
            return true
        }
    }


    var isWon = true
    for (i in 0 until SIZE - 1) {
        if (displayXorO[i][i] == ' ' || displayXorO[i][i] != displayXorO[i + 1][i + 1]) {
            isWon = false
        }
    }

    isWon = true
    for (i in 0 until SIZE - 1) {
        if (displayXorO[i][SIZE - i - 1] == ' ' || displayXorO[i][SIZE - i - 1] != displayXorO[i + 1][SIZE - 1 - i - 1]) {
            isWon = false
        }
    }

    Log.e("Mudit Log ", "$isWon Diagonal")
    return isWon
}

@Composable
fun Game(navController: NavController? = null, innerPadding: PaddingValues? = null) {

    /******
     * Create a 3x3 Lazy veritical grid.
     * Create box to center align text
     * Create text in box display x and o
     *
     *
     * Step 2: Create a 2D matrix of Characters
     * Step 3: Create a turn and x and o state. (x goes first)
     */


    var displayXorO by remember {
        mutableStateOf(
            Array(SIZE) {
                CharArray(SIZE) {
                    ' '
                }
            }
        )
    }

    var turn by remember {
        mutableStateOf('x')
    }

    var isWon by remember {
        mutableStateOf(false)
    }

    var dialogVisible by remember {
        mutableStateOf(false)
    }

    var dialogText by remember {
        mutableStateOf("")
    }


    val resetGame: () -> Unit = {
        displayXorO = Array(SIZE) {
            CharArray(SIZE) {
                ' '
            }
        }
        turn = 'x'
    }

    val checkAllFilled: () -> Boolean = {
        var isAllFilled = true
        for (i in 0 until SIZE) {
            for (j in 0 until SIZE) {
                if (displayXorO[i][j] == ' ') {
                    isAllFilled = false
                    break
                }
            }
            if (!isAllFilled) {
                break
            }

        }
        isAllFilled
    }

    LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.padding(16.dp)) {
        item(span = {
            GridItemSpan(3)
        }) {
            Text(
                text = if (isWon) "${turn.uppercase()} won" else "Turn Of ${turn.uppercase()}",
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = TextAlign.Center
            )
        }

        for (i in 0 until SIZE) {
            for (j in 0 until SIZE) {
                item {
                    Box(
                        contentAlignment = Alignment.Center, modifier = Modifier
                            .aspectRatio(1f)
                            .padding(5.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .clickable {
                                if (displayXorO[i][j] == ' ') {
                                    displayXorO[i][j] = turn
                                    displayXorO = displayXorO.clone()
                                    if (!checkWinner(displayXorO)) {
                                        if (checkAllFilled()) {
                                            dialogText = "No one won"
                                            dialogVisible = true
                                        } else {
                                            turn = if (turn == 'x') 'o' else 'x'
                                        }
                                    } else {
                                        dialogText = "${turn.uppercase()} won"
                                        dialogVisible = true
                                    }
                                }

                            }
                    ) {
                        if (displayXorO[i][j] == 'x') {
                            Image(
                                painter = painterResource(id = R.drawable.ic_x),
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(24.dp)
                                    .fillMaxSize()
//                            textAlign = TextAlign.Center,
//                            text = displayXorO[i][j].toString(),
//                            color = Color.White,
//                            fontSize = 40.sp,
//                            fontFamily = FontFamily.SansSerif
                            )
                        } else if (displayXorO[i][j] == 'o') {
                            Image(
                                painter = painterResource(id = R.drawable.ic_o),
                                contentDescription = null,
//                            textAlign = TextAlign.Center,
//                            text = displayXorO[i][j].toString(),
                                modifier = Modifier
                                    .padding(24.dp)
                                    .fillMaxSize()
//                            color = Color.White,
//                            fontSize = 40.sp,
//                            fontFamily = FontFamily.SansSerif
                            )
                        }

                    }
                }
            }
        }

        item(span = {
            GridItemSpan(3)
        }) {
            Text(
                text = "RESET",
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable {
                        resetGame()
                    },
                textAlign = TextAlign.Center
            )
        }


    }


    if (dialogVisible) {
        Dialog(onDismissRequest = { resetGame(); dialogVisible = false }) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(16.dp)
                    .background(Color.White)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(text = "$turn Won")
                Text(text = "Play Again", modifier = Modifier.clickable {
                    resetGame()
                    dialogVisible = false
                })
            }

        }
    }

}

@Preview(showBackground = true)
@Composable
fun HomePagePreview() {
    ComposrTicTacToeTheme {
        Game(innerPadding = PaddingValues(16.dp))
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
            painter = painterResource(id = R.drawable.tic_tac_toe),
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        )

        Text(
            text = "Play",
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable {
                    navController?.navigate(Screen.Screen2.route)
                },
            textAlign = TextAlign.Center
        )

    }
}

@Composable
fun GradientBackground(compose: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF00D2FF), Color(0xFF3A7BD5)),
//                    start = Offset(0f, 0f), // Start position of the gradient (x, y)
//                    end = Offset(
//                        LocalConfiguration.current.screenWidthDp.toFloat(),
//                        LocalConfiguration.current.screenHeightDp.toFloat()
//                    ), // End position of the gradient (x, y)
                    tileMode = androidx.compose.ui.graphics.TileMode.Clamp // TileMode
                )
            )
    ) {
        compose()
    }
}