package com.mudit.composetetris

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.mudit.composetetris.TetrisViewModel.Companion.ROWS
import kotlin.math.sign
import kotlin.random.Random

class TetrisViewModel : ViewModel() {

    companion object {
        const val ROWS = 20
        const val COLUMNS = 10
    }

    private val pieceArray = arrayOf(
        Piece(Tetrimino.L),
        Piece(Tetrimino.J),
        Piece(Tetrimino.I),
        Piece(Tetrimino.O),
        Piece(Tetrimino.S),
        Piece(Tetrimino.Z),
        Piece(Tetrimino.T)
    )

    val speed = 750L

    var score by mutableIntStateOf(0)

    fun getRandomPiece(): Piece {
        return pieceArray[Random(System.currentTimeMillis()).nextInt(0, 7)]
    }

    fun gameOver(currentPiece: Piece, grid: Array<IntArray>): Boolean {
        for (i in currentPiece.position) {
            if (i.row > 0 && i.column > 0) {
                return grid[i.row][i.column] > 0
            }
        }
        return false
    }

    fun removeLines(grid: Array<IntArray>): Array<IntArray> {
        for (i in grid.size - 1 downTo 0) {
            var isFull = true
            for (j in 0 until grid[i].size) {
                if (grid[i][j] == 0) {
                    isFull = false
                }
            }
            if (isFull) {
                score += 100
                for (row in i downTo 1) {
                    grid[row] = grid[row - 1]
                }
            }
        }
        return grid.clone()

    }


    fun movePiece(piece: Piece, direction: DIRECTION, grid: Array<IntArray>): Piece {
        when (direction) {
            DIRECTION.LEFT -> {

                for (i in 0 until piece.position.size) {
                    if (((piece.position[i].column) - 1) < 0) {
                        return piece
                    }
                }

                for (i in 0 until piece.position.size) {
                    val row = piece.position[i].row
                    val column = piece.position[i].column

                    if (row >= 0 && column - 1 > 0 && grid[row][column - 1] > 0) {
                        return piece
                    }
                }

                val tmpCurrPiece = Piece(piece)

                for (i in 0 until tmpCurrPiece.position.size) {
                    tmpCurrPiece.position[i] = tmpCurrPiece.position[i] - 1
                }
                return tmpCurrPiece

            }

            DIRECTION.RIGHT -> {
                val tmpCurrPiece = Piece(piece)
                for (i in 0 until tmpCurrPiece.position.size) {
                    if ((tmpCurrPiece.position[i].column) + 1 >= COLUMNS) {
                        return piece
                    }
                }

                for (i in 0 until piece.position.size) {
                    val row = piece.position[i].row
                    val column = piece.position[i].column

                    if (row > 0 && row < grid.size && column + 1 > 0 && grid[row][column + 1] > 0) {
                        return piece
                    }
                }

                for (i in 0 until tmpCurrPiece.position.size) {
                    tmpCurrPiece.position[i] = tmpCurrPiece.position[i] + 1
                }
                return tmpCurrPiece
            }

            DIRECTION.DOWN -> {
                val tmpCurrPiece = Piece(piece)
                for (i in tmpCurrPiece.position) {
                    if (i > 0) {
                        if ((i + 10) >= ROWS * COLUMNS) {
                            tmpCurrPiece.placed = true
                            return tmpCurrPiece
                        }
                        if (grid[(i + 10).row][(i + 10).column] > 0) {
                            tmpCurrPiece.placed = true
                            return tmpCurrPiece
                        }
                    }
                }
                for (i in 0 until tmpCurrPiece.position.size) {
                    tmpCurrPiece.position[i] = tmpCurrPiece.position[i] + 10
                }
                return tmpCurrPiece
            }
        }
    }


    private fun rotatePoint(pivot: Int, pos: Int): Pair<Int, Int> {
        val newRow = pivot.row + (pivot.column - pos.column)
        val newCol = pivot.column - (pivot.row - pos.row)
        return Pair(newRow, newCol)
    }

    fun rotatePiece(piece: Piece, grid: Array<IntArray>): Piece {
        if (piece.type == Tetrimino.O) {
            return piece
        }
        val tmpCurrPiece = Piece(piece)
        val pivot = piece.position[1]
        for (i in 0 until piece.position.size) {
            val rotatedPoint = rotatePoint(pivot, tmpCurrPiece.position[i])
            if (rotatedPoint.first < 0 || rotatedPoint.second < 0 || rotatedPoint.first >= ROWS || rotatedPoint.second >= COLUMNS) {
                return piece
            }
            if (grid[rotatedPoint.first][rotatedPoint.second] > 0) {
                return piece
            }
            tmpCurrPiece.position[i] = rotatedPoint.first * COLUMNS + rotatedPoint.second
        }
        return tmpCurrPiece
    }

    fun getColor(type: Int): Color {
        return pieceArray[type - 1].color
    }
}