import Minefield.Chars.defuse
import Minefield.Chars.empty
import Minefield.Chars.mine
import Minefield.Chars.notExplored
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

fun main() = Game.startGame()

object Game {
    private var steps = 0
    var state = State.PLAYING

    fun startGame() {
        println("How many mines do you want on the field?")
        val scan = Scanner(System.`in`)
        val minesCount = scan.nextInt()
        val minefield = Minefield(minesCount)
        minefield.print()

        while (state == State.PLAYING) {
            println("Set/unset mines marks or claim a cell as free: ")
            val y = scan.nextInt()
            val x = scan.nextInt()
            val command = scan.next()
            scan.nextLine()

            when (command) {
                Command.MINE.name.lowercase() -> {
                    state = minefield.setOrUnsetMine(x - 1, y - 1)
                }
                Command.FREE.name.lowercase() -> {
                    state = minefield.claimFreeCell(++steps, x - 1, y - 1)
                }
            }


            minefield.print()
            println()
        }

        when (state) {
            State.LOST -> println("You stepped on a mine and failed!")
            State.WON -> println("Congratulations! You found all the mines!")
            else -> println("Something goes wrong!")
        }
    }

    enum class Command {
        FREE,
        MINE
    }

    enum class State {
        PLAYING,
        WON,
        LOST
    }
}

class Minefield(private val minesCount: Int) {
    private val rowSize = 9
    private val rowIndice = 9

    private val minefield = Array(9) {
        CharArray(rowSize) { '0' }
    }

    private var minefieldPlayerSteps = Array(9) {
        CharArray(rowSize) { '.' }
    }

    private var minefieldWithGrid = Array(rowIndice + 3) { CharArray(rowSize + 3) }

    private var mineIndices = listOf<String>()
    private var defuseIndices = listOf<String>()
    private var playerOpenedIndices = arrayOf<String>()

    object Chars {
        const val mine = 'X'
        const val notExplored = '.'
        const val defuse = '*'
        const val empty = '/'
    }

    fun setOrUnsetMine(x: Int, y: Int): Game.State {
        if (minefieldPlayerSteps[x][y] == defuse) {
            minefieldPlayerSteps[x][y] = notExplored
            defuseIndices = defuseIndices - "$x $y"
        } else if (minefieldPlayerSteps[x][y] == notExplored) {
            defuseIndices = defuseIndices + "$x $y"
            minefieldPlayerSteps[x][y] = defuse
        }

        return if (areAllMinesDefused()) {
            Game.State.WON
        } else {
            Game.State.PLAYING
        }
    }

    fun claimFreeCell(step: Int, x: Int, y: Int): Game.State {
        val stepIndice = "$x $y"

        if (step == 1) {
            var mineFreeIndicesFirstStep = arrayOf<String>()
            for (i in max(0, x - 1)..min(rowIndice - 1, x + 1)) {
                for (j in max(0, y - 1)..min(rowSize - 1, y + 1)) {
                    val stepAround = "$i $j"
                    mineFreeIndicesFirstStep += stepAround
                }
            }
            fillField(mineFreeIndicesFirstStep)
        }

        return if (stepIndice in mineIndices) {
            Game.State.LOST
        } else {
            safeOpenCell(x, y)
            if (rowSize * rowIndice - playerOpenedIndices.size == mineIndices.size) {
                Game.State.WON
            } else {
                Game.State.PLAYING
            }
        }
    }

    fun print() {
        updateGrid()
        for (i in this.minefieldWithGrid.indices) {
            for (j in this.minefieldWithGrid[i].indices) {
                print(this.minefieldWithGrid[i][j])
            }
            println()
        }
    }

    private fun fillField(mineFreeIndicesFirstStep: Array<String>) {
        placeMines(mineFreeIndicesFirstStep)
        placeHints()
    }

    private fun placeMines(mineFreeIndicesFirstStep: Array<String>) {
        while (mineIndices.size != minesCount) {
            var mineIndice = "${Random.nextInt(0, rowIndice)} ${Random.nextInt(0, rowSize)}"
            while (mineIndice in mineIndices || mineIndice in mineFreeIndicesFirstStep) {
                mineIndice = "${Random.nextInt(0, rowIndice)} ${Random.nextInt(0, rowSize)}"
            }
            mineIndices = mineIndices + mineIndice
        }

        for (point in mineIndices) {
            val (i, j) = point.split(' ')
            minefield[i.toInt()][j.toInt()] = 'X'
        }
    }

    private fun placeHints() {
        for (i in this.minefield.indices) {
            for (j in this.minefield[i].indices) {
                if (minefield[i][j] == mine) {
                    placeHint(i, j)
                }
            }
        }
    }

    private fun placeHint(row: Int, indice: Int) {
        val aroundCells = getAroundCells(row, indice)
        for (cell in aroundCells) {
            val (roundX, roundY) = cell.split(' ')
            minefield[roundX.toInt()][roundY.toInt()] = additionHint(roundX.toInt(), roundY.toInt())
        }
    }

    private fun additionHint(row: Int, indice: Int): Char {
        if (minefield[row][indice] != mine) {
            return (minefield[row][indice].toString().toInt() + 1).toString()[0]
        }
        return minefield[row][indice]
    }

    private fun safeOpenCell(x: Int, y: Int) {
        playerOpenedIndices += "$x $y"
        if (minefield[x][y] != '0') {
            minefieldPlayerSteps[x][y] = minefield[x][y]
        } else {
            minefieldPlayerSteps[x][y] = empty
            val aroundCells = getAroundCells(x, y)
            for (cell in aroundCells) {
                if (cell !in playerOpenedIndices) {
                    val (roundX, roundY) = cell.split(' ')
                    safeOpenCell(roundX.toInt(), roundY.toInt())
                }
            }
        }
    }

    private fun getAroundCells(x: Int, y: Int): Array<String> {
        var aroundCells = arrayOf<String>()
        for (i in max(0, x - 1)..min(rowIndice - 1, x + 1)) {
            for (j in max(0, y - 1)..min(rowSize - 1, y + 1)) {
                val stepAround = "$i $j"
                if ("$i $j" != "$x $y") {
                    aroundCells += stepAround
                }
            }
        }
        return aroundCells
    }

    private fun areAllMinesDefused(): Boolean {
        if (defuseIndices.size != mineIndices.size) {
            return false
        } else {
            for (defuse in defuseIndices) {
                if (defuse !in mineIndices) {
                    return false
                }
            }
        }
        return true
    }

    private fun updateGrid() {
        if (minefieldWithGrid[0][0] == Char.MIN_VALUE) {
            startupHeader(minefieldWithGrid)
            startupSeparators(minefieldWithGrid)
            startupContent(minefieldWithGrid)
        } else {
            if (Game.state == Game.State.LOST) {
                updateContent(minefieldWithGrid, true)
            } else {
                updateContent(minefieldWithGrid)
            }
        }
    }

    private fun updateContent(minefieldWithGrid: Array<CharArray>, reveal: Boolean = false) {
        for (i in 2..minefieldWithGrid.size - 2) {
            for (j in minefieldWithGrid[i].indices) {
                when (j) {
                    in 2..minefieldWithGrid.size - 2 -> {
                        if (!reveal || minefield[i - 2][j - 2] != mine) {
                            minefieldWithGrid[i][j] = minefieldPlayerSteps[i - 2][j - 2]
                        } else {
                            minefieldWithGrid[i][j] = minefield[i - 2][j - 2]
                        }
                    }
                }
            }
        }
    }

    private fun startupContent(minefieldWithGrid: Array<CharArray>) {
        for (i in 2..minefieldWithGrid.size - 2) {
            for (j in minefieldWithGrid[i].indices) {
                when (j) {
                    0 -> minefieldWithGrid[i][j] = (i - 1).toString()[0]
                    1, minefieldWithGrid[i].size - 1 -> minefieldWithGrid[i][j] = '|'
                    else -> minefieldWithGrid[i][j] = minefieldPlayerSteps[i - 2][j - 2]
                }
            }
        }
    }

    private fun startupSeparators(minefieldWithGrid: Array<CharArray>) {
        for (i in minefieldWithGrid[0].indices) {
            when (i) {
                1, minefieldWithGrid[0].size - 1 -> {
                    minefieldWithGrid[1][i] = '|'
                    minefieldWithGrid[minefieldWithGrid.size - 1][i] = '|'
                }
                else -> {
                    minefieldWithGrid[1][i] = '—'
                    minefieldWithGrid[minefieldWithGrid.size - 1][i] = '—'
                }
            }
        }
    }

    private fun startupHeader(header: Array<CharArray>) {
        for (i in header[0].indices) {
            when (i) {
                0 -> header[0][i] = ' '
                1, header[0].size - 1 -> header[0][i] = '|'
                else -> header[0][i] = ((i - 2) + 1).toString()[0]
            }
        }
    }
}
