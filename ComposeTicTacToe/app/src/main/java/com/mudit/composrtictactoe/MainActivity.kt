package com.mudit.composrtictactoe

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.Navigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navOptions
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
                        GameNavigator(innerPadding)
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
fun GameNavigator(paddingValues: PaddingValues) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Screen1.route) {
        composable(Screen.Screen1.route) {
            Menu(navController)
        }
        composable(Screen.Screen2.route, arguments = listOf(navArgument("selected") {
            defaultValue = 'x'
        })) {
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
    if (isWon) {
        Log.e("Mudit Log", "DIAGONAL")
        return true
    }

    isWon = true
    for (i in 0 until SIZE - 1) {
        if (displayXorO[i][SIZE - i - 1] == ' ' || displayXorO[i][SIZE - i - 1] != displayXorO[i + 1][SIZE - 1 - i - 1]) {
            isWon = false
        }
    }
    if (isWon) {
        Log.e("Mudit Log", "CROSS DIAGONAL")
        return true
    }
    return false
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

    var xWonCount by remember {
        mutableStateOf(0)
    }

    var oWonCount by remember {
        mutableStateOf(0)
    }

    LaunchedEffect(key1 = true) {
        turn = navController?.currentBackStackEntry?.arguments?.getChar("selected") ?: 'x'
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
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 40.dp, bottom = 40.dp)
            ) {
                Text(
                    text = if (isWon) "${turn.uppercase()} won" else "Turn Of",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(
                        shadow = Shadow(
                            Color(0x55000000),
                            offset = Offset(5f, 5f),
                            blurRadius = 16f
                        )
                    ),
                    modifier = Modifier
                        .padding(16.dp),
                    textAlign = TextAlign.Center
                )

                if (turn == 'x') {
                    Image(
                        painter = painterResource(id = R.drawable.img_x),
                        contentDescription = null,
                        modifier = Modifier.size(50.dp)
                    )
                } else if (turn == 'o') {
                    Image(
                        painter = painterResource(id = R.drawable.img_o),
                        contentDescription = null,
                        modifier = Modifier.size(50.dp)
                    )
                }
            }

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
                                        if (turn == 'x') {
                                            xWonCount++
                                        } else {
                                            oWonCount++
                                        }
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
                            )
                        } else if (displayXorO[i][j] == 'o') {
                            Image(
                                painter = painterResource(id = R.drawable.ic_o),
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(24.dp)
                                    .fillMaxSize()
                            )
                        }

                    }
                }
            }
        }

        item(span = {
            GridItemSpan(3)
        }) {
            Column {
                Text(
                    text = "RESET",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(
                        shadow = Shadow(
                            Color(0x55000000),
                            offset = Offset(5f, 5f),
                            blurRadius = 16f
                        )
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable {
                            resetGame()
                        },
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Score",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(
                        shadow = Shadow(
                            Color(0x55000000),
                            offset = Offset(5f, 5f),
                            blurRadius = 16f
                        )
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable {
                            resetGame()
                        },
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_x),
                        contentDescription = null,
                        modifier = Modifier.size(45.dp)
                    )

                    Text(
                        text = "$xWonCount",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(
                            shadow = Shadow(
                                Color(0x55000000),
                                offset = Offset(5f, 5f),
                                blurRadius = 16f
                            )
                        ),
                        modifier = Modifier.padding(start = 10.dp, end = 10.dp),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Image(
                        painter = painterResource(id = R.drawable.img_o),
                        contentDescription = null,
                        modifier = Modifier.size(45.dp)
                    )

                    Text(
                        text = "$oWonCount",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(
                            shadow = Shadow(
                                Color(0x55000000),
                                offset = Offset(5f, 5f),
                                blurRadius = 16f
                            )
                        ),
                        modifier = Modifier
                            .padding(start = 10.dp, end = 10.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

        }


    }


    if (dialogVisible) {
        Dialog(
            onDismissRequest = { resetGame(); dialogVisible = false }) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                Text(
                    text = "$turn Won",
                    color = Color(0xFF222222),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(
                        shadow = Shadow(
                            Color(0x55000000),
                            offset = Offset(5f, 5f),
                            blurRadius = 16f
                        )
                    ),
                    modifier = Modifier
                        .padding(16.dp)
                        .clickable {
                            resetGame()
                        },
                    textAlign = TextAlign.Center
                )
                Text(text = "Play Again",
                    color = Color(0xFF222222),
                    style = TextStyle(
                        shadow = Shadow(
                            Color(0x55000000),
                            offset = Offset(5f, 5f),
                            blurRadius = 16f
                        )
                    ),
                    modifier = Modifier
                        .padding(16.dp)
                        .clickable {
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

        Text(
            text = "TIC TAC TOE",
            fontSize = 52.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            style = TextStyle(
                shadow = Shadow(
                    Color(0x99000000),
                    offset = Offset(5f, 5f),
                    blurRadius = 16f
                )
            ),
            modifier = Modifier
                .fillMaxWidth()

                .fillMaxHeight(0.25f)
                .padding(16.dp)
                .clickable {
                    navController?.navigate(Screen.Screen2.route)
                },
            textAlign = TextAlign.Center
        )

        Text(
            text = "Pick who goes first?",
            fontSize = 24.sp,
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable {
                    navController?.navigate(Screen.Screen2.route + "?selected=x")
                },
            textAlign = TextAlign.Center
        )


        Row {
            Image(
                painter = painterResource(id = R.drawable.img_x),
                contentDescription = null,
                modifier = Modifier
                    .weight(0.8f)
                    .padding(start = 24.dp)
                    .clickable {
                        navController?.navigate(Screen.Screen2.route)
                    }
            )

            Image(
                painter = painterResource(id = R.drawable.img_o),
                contentDescription = null,
                modifier = Modifier
                    .weight(0.8f)
                    .padding(end = 24.dp)
                    .clickable {
                        navController?.navigate(Screen.Screen2.route)
                    }
            )
        }

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
                    tileMode = androidx.compose.ui.graphics.TileMode.Clamp // TileMode
                )
            )
    ) {
        compose()
    }
}

@Preview
@Composable
fun MenuPreview() {
    Menu()
}

