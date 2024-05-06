package com.mudit.composetetris

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource

enum class DIRECTION {
    LEFT,
    RIGHT,
    DOWN,
}

enum class Tetrimino(val intType: Int, val color: Color, val initialPosition: IntArray) {
    L(1, Color(0xFFfa8231), intArrayOf(4, 14, 24, 25)),
    J(2, Color(0xFF3867d6), intArrayOf(4, 14, 23, 24)),
    I(3, Color(0xFF45aaf2), intArrayOf(4, 5, 6, 7)),
    O(4, Color(0xFFf7b731), intArrayOf(4, 5, 14, 15)),
    S(5, Color(0xFFeb3b5a), intArrayOf(22 - 20, 23 - 20, 31 - 20, 32 - 20)),
    Z(6, Color(0xFF20bf6b), intArrayOf(33 - 20, 34 - 20, 44 - 20, 45 - 20)),
    T(7, Color(0xFFa55eea), intArrayOf(10 - 20, 11 - 20, 12 - 20, 21 - 20))
}

@Composable
fun Pixel(pixelRes: Int, color: Color) {

    val filter = ColorFilter.tint(color, blendMode = BlendMode.Color)


    Image(
        modifier = Modifier.fillMaxSize(),
        painter = painterResource(id = pixelRes),
        contentDescription = "",
        colorFilter = filter
    )
}

class Piece(val type: Tetrimino) {

    var pixelRes: Int = 0
    var position: IntArray
    var placed = false
    var color = Color.Transparent

    init {
        this.pixelRes = R.drawable.element_grey_square
        this.color = type.color
        this.position = type.initialPosition
    }


    constructor(piece: Piece) : this(piece.type) {
        pixelRes = piece.pixelRes
        position = IntArray(piece.position.size) {
            piece.position[it]
        }
    }


}