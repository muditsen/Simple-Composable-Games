package com.mudit.composrtictactoe

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                    containerColor = Color(0xFF424242)
                ) { innerPadding ->
                    HomePage(innerPadding)
                }
            }
        }
    }
}

const val ROWS = 3
const val COLUMNS = 3

@Composable
fun HomePage(innerPadding: PaddingValues) {

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
            CharArray(ROWS * COLUMNS) {
                ' '
            }
        )
    }
    
    var turn by remember {
        mutableStateOf('x')
    }

    val checkWinner = {
        var j = 0
        for (i in 0 until ROWS) {
            if (displayXorO[i * COLUMNS + j] != ' ' &&
                displayXorO[i * COLUMNS + j] == displayXorO[i * COLUMNS + j + 1] &&
                displayXorO[i * COLUMNS + j + 1] == displayXorO[i * COLUMNS + j + 2]
            ) {
                //Turn Won
                println("$turn won")
            }
            j++
        }

        j = 0
        for (i in 0 until COLUMNS) {
            if (displayXorO[i * ROWS + j] != ' ' &&
                displayXorO[i * ROWS + j] == displayXorO[(i + 1) * ROWS + j] &&
                displayXorO[(i + 1) * ROWS + j] == displayXorO[(i + 2) * ROWS + j]
            ) {
                //Turn Won
                println("$turn won")
            }
            j++
        }
    }

    LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.padding(16.dp)) {
        item(span = {
            GridItemSpan(3)
        }) {
            Text(
                text = "Turn Of ${turn.uppercase()}",
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = TextAlign.Center
            )
        }
      
        for (i in 0 until ROWS * COLUMNS) {
            item {
                Box(
                    contentAlignment = Alignment.Center, modifier = Modifier
                        .aspectRatio(1f)
                        .border(0.5.dp, Color.Gray)
                        .clickable {
                            displayXorO[i] = turn
                            displayXorO = displayXorO.clone()
                            turn = if (turn == 'x') 'o' else 'x'
                            checkWinner()
                        }
                ) {
                    Text(
                        textAlign = TextAlign.Center,
                        text = displayXorO[i].toString(),
                        modifier = Modifier,
                        color = Color.White,
                        fontSize = 40.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }

            }
        }

    }

}

@Preview(showBackground = true)
@Composable
fun HomePagePreview() {
    ComposrTicTacToeTheme {
        HomePage(innerPadding = PaddingValues(16.dp))
    }
}