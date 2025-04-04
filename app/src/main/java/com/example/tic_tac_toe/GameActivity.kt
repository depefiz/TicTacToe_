package com.example.tic_tac_toe

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings.Global.putLong
import android.provider.Settings.Global.putString
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.provider.FontsContractCompat.Columns
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tic_tac_toe.SettingsActivity.Companion.PREF_LVL
import com.example.tic_tac_toe.SettingsActivity.Companion.PREF_RULES
import com.example.tic_tac_toe.SettingsActivity.Companion.PREF_SOUND_VALUE
import com.example.tic_tac_toe.SettingsActivity.SettingInfo
import com.example.tic_tac_toe.databinding.ActivityGameBinding
import com.example.tic_tac_toe.databinding.ActivitySettingsBinding

class GameActivity : AppCompatActivity() {
    // Привязка к макету активности
    private lateinit var binding: ActivityGameBinding
    //Массив с состоянием игрового поля
    private lateinit var gameField: Array<Array<String>>
    // Информация из настроек
    private lateinit var settinsInfo: SettingsActivity.SettingInfo
    //Музыка
    private lateinit var mediaPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        //Инициализация привязки к макету активности
        binding = ActivityGameBinding.inflate(layoutInflater)
        // Обработчики кликов для всплывающего меню и выхода
        binding.toPopupMenu.setOnClickListener{
            showPopupMenu()
        }

        binding.toGameClose.setOnClickListener{
            onBackPressed()
        }
        //Обработчики кликов для всех клеток игрвого поля
        binding.cell11.setOnClickListener{
            MakeStepOfUser(0, 0)
        }

        binding.cell12.setOnClickListener{
            MakeStepOfUser(0, 1)
        }

        binding.cell13.setOnClickListener{
            MakeStepOfUser(0, 2)
        }
        binding.cell21.setOnClickListener{
            MakeStepOfUser(1, 0)
        }

        binding.cell22.setOnClickListener{
            MakeStepOfUser(1, 1)
        }

        binding.cell23.setOnClickListener{
            MakeStepOfUser(1,2)
        }

        binding.cell31.setOnClickListener{
            MakeStepOfUser(2, 0)
        }

        binding.cell32.setOnClickListener{
            MakeStepOfUser(2, 1)
        }

        binding.cell33.setOnClickListener{
            MakeStepOfUser(2, 2)
        }

        //Инициализация активности и установка xml макета
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        //Получение данных и сохраненной игре
        val time = intent.getLongExtra(MainActivity.EXTRA_TIME, 0)
        val gameField = intent.getStringExtra(MainActivity.EXTRA_GAME_FIELD)

        //Инициализация новой игры или восстановление уже существующей
        if(gameField != null && time != 0L && gameField != ""){
            restartGame(time, gameField)
        } else {
            initGameField()
        }

        //Получение настроек
        settinsInfo = getSettingsInfo()

        //Настройки медиаплеера
        mediaPlayer = MediaPlayer.create(this, R.raw.music)
        mediaPlayer.isLooping = true
        setVolumeMediaPlayer(settinsInfo.soundValue)

        mediaPlayer.start()
        binding.chronometer.start() //Запуск таймера

    }

    //Выгрузка данных медиаплеера при закрытии активности
    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }
    override fun onStop() {
        super.onStop()
        mediaPlayer.release()
    }
    //Установка громкости
    private fun setVolumeMediaPlayer(soundValue: Int){
        val volume = soundValue / 100.0
        mediaPlayer.setVolume(volume.toFloat(), volume.toFloat() )
    }
    //Инициализация пустого игрового поля
    private fun initGameField(){
        gameField = Array(3,){Array(3){" "} }
    }
    //Игровой ход, обновление данных, выгрузка данных в UI
    private fun makeStep(row: Int, column: Int, symbol: String){
        gameField[row][column] = symbol

        makeStepUI("$row$column", symbol)
    }

    //Обновление UI
    private fun makeStepUI(position: String, symbol: String){
        val resID = when(symbol){
            "X" -> R.drawable.cross_icon_foreground
            "0" -> R.drawable.zero
            else -> return
        }
        //Установку нужной иконки в нужную клетку поля
        when(position){
            "00" -> binding.cell11.setImageResource(resID)
            "01" -> binding.cell12.setImageResource(resID)
            "02" -> binding.cell13.setImageResource(resID)
            "10" -> binding.cell21.setImageResource(resID)
            "11" -> binding.cell22.setImageResource(resID)
            "12" -> binding.cell23.setImageResource(resID)
            "20" -> binding.cell31.setImageResource(resID)
            "21" -> binding.cell32.setImageResource(resID)
            "22" -> binding.cell33.setImageResource(resID)
        }
    }

    //Обработка хода пользователя
    private  fun MakeStepOfUser(row: Int, column: Int){
        if(isEmptyField(row, column)){
            makeStep(row, column, "X")
            //Проверка на победу
            val status = checkGameField(row, column, "X")
            if(status.status){
                showGameStatus(STATUS_PLAYER_WIN)
                return
            }

            //При остатке незаполненных полей, ход ИИ
            if (!isFilledGameField()){
                val resultCell = makeStepOfAI()

                //Проверка на победу ИИ
                val statusAI = checkGameField(resultCell.row, resultCell.column, "0")
                if(statusAI.status){
                    showGameStatus(STATUS_PLAYER_LOSE)
                    return
                }

                //Проверка на ничью
                if(isFilledGameField()){
                    showGameStatus(STATUS_PLAYER_DRAW)
                    return
                }
            } else { //Ничья при заполнении поля
                showGameStatus(STATUS_PLAYER_DRAW)
            }

        } else { //Уведомление при клике на занятое поле
            Toast.makeText(this, "Поле уже занято", Toast.LENGTH_SHORT).show()
        }
    }

    //Проверка заполненности клетки
    private fun isEmptyField(row: Int, column: Int): Boolean {
        return gameField[row][column] == " "
    }

    //Выбор алгоритма ИИ в зависимости от сложности
    private fun makeStepOfAI(): CellGameField {
        return when(settinsInfo.lvl){
            0 -> makeStepOfAIEasyLvl() //Легкий - Случайный выбор клетки каждый ход
            1 -> makeStepOfAIMediumLvl() // Средний - упрощеный minimax алгоритм
            2 -> makeStepOfAIHardLvl() //Сложный - полноценный minimax алгоритм
            else -> CellGameField(0, 0)
        }
    }

    //Представление клетки игрового поля
    data class CellGameField(val row: Int, val column: Int)

    //Алгоритм ИИ для высокого уровня сложности
    private fun makeStepOfAIHardLvl() : CellGameField {
        var bestScore = Double.NEGATIVE_INFINITY
        var move = CellGameField(0,0)

        //Создание копии игрового поля
        var board = gameField.map{ it.clone() }.toTypedArray()

        //Перебор ходов
        board.forEachIndexed { indexRow, cols ->
            cols.forEachIndexed { indexCols, cell ->
                if(board[indexRow][indexCols] == " "){
                    board[indexRow][indexCols] = "0" //Пробный ход
                    val score = minimax(board, false) //Оценка хода
                    board[indexRow][indexCols] = " " //Отмена пробного хода
                    if(score > bestScore){ //Запись результата пробного хода
                        bestScore = score
                        move = CellGameField(indexRow, indexCols)
                    }
                }
            }
        }

        makeStep(move.row, move.column, "0") //Совершение лучшего хода

        return move
    }
    //Алгоритм минимакс для оценки хода
    private fun minimax(board: Array<Array<String>>, isMaximizing: Boolean): Double {
        //Проверка на завершение игры
        val result = checkWinner(board)
        result?.let{
            return scores[result]!! //Возврат исхода
        }
        //Алгоритм максимизации
        if(isMaximizing){
            var bestScore = Double.NEGATIVE_INFINITY
            board.forEachIndexed { indexRow, cols ->
                cols.forEachIndexed { indexCols, cell ->
                    if(board[indexRow][indexCols] == " "){
                        board[indexRow][indexCols] = "0"
                        val score = minimax(board, false)
                        board[indexRow][indexCols] = " "
                        if(score > bestScore){
                            bestScore = score
                        }
                    }
                }
            }

            return bestScore
        }
        else{
            //Алгоритм минимизации
            var bestScore = Double.POSITIVE_INFINITY
            board.forEachIndexed { indexRow, cols ->
                cols.forEachIndexed { indexCols, cell ->
                    if(board[indexRow][indexCols] == " "){
                        board[indexRow][indexCols] = "X"
                        val score = minimax(board, true)
                        board[indexRow][indexCols] = " "
                        if(score < bestScore){
                            bestScore = score
                        }
                    }
                }
            }
            return bestScore
        }
    }
    //Проверка победителя
    private fun checkWinner(board: Array<Array<String>>): Int? {
        //Счетчики
        var countRowsHu = 0
        var countRowsAI = 0
        var countLDHu = 0
        var countLDAI = 0
        var countRDHu = 0
        var countRDAI = 0
        board.forEachIndexed { indexRow, cols ->
            //Проверка строк
            if(cols.all { it == "X" })
                return  STATUS_PLAYER_WIN
            else if(cols.all { it == "0" })
                return STATUS_PLAYER_LOSE

            countRowsHu = 0
            countRowsAI = 0

            cols.forEachIndexed { indexCols, s ->
                //Проверка столбцов
                if (board[indexCols][indexRow] == "X")
                    countRowsHu++
                else if(board[indexCols][indexRow] == "0")
                    countRowsAI++

                //Провераа диагоналей
                if(indexRow == indexCols && board[indexRow][indexCols] == "X")
                    countLDHu++
                else if(indexRow == indexCols && board[indexRow][indexCols] == "0")
                    countLDAI++

                if(indexRow == 2-indexCols && board[indexRow][indexCols] == "X")
                    countRDHu++
                else if(indexRow == 2-indexCols && board[indexRow][indexCols] == "0")
                    countRDAI++
            }
            //Проверка победителя
            if(countRowsHu == 3 || countLDHu == 3 || countRDHu == 3)
                return STATUS_PLAYER_WIN
            else if(countRowsAI == 3 || countLDAI == 3 || countRDAI == 3)
                return STATUS_PLAYER_LOSE


        }

        //Проверка на ничью
        board.forEach {
            if(it.find { it == " " } != null)
                return null
        }
        return STATUS_PLAYER_DRAW
    }
    //Алгоритм для среднего уровня сложности(упрощенный алгоритм minimax, анализирует только ходы ИИ)
    private fun makeStepOfAIMediumLvl() : CellGameField {
        var bestScore = Double.NEGATIVE_INFINITY
        var move = CellGameField(0,0)

        var board = gameField.map{ it.clone() }.toTypedArray()

        board.forEachIndexed { indexRow, cols ->
            cols.forEachIndexed { indexCols, cell ->
                if(board[indexRow][indexCols] == " "){
                    board[indexRow][indexCols] = "0"
                    val score = minimax(board, false)
                    board[indexRow][indexCols] = " "
                    if(score > bestScore){
                        bestScore = score
                        move = CellGameField(indexRow, indexCols)
                    }
                }
            }
        }

        makeStep(move.row, move.column, "0")

        return move
    }
    //Алгоритм ИИ для легкого уровня сложности
    private fun makeStepOfAIEasyLvl() : CellGameField{
        var randRow = 0
        var randColumn = 0

        do {
            randRow = (0..2).random()
            randColumn = (0..2).random()

        } while (!isEmptyField(randRow, randColumn))

        makeStep(randRow, randColumn, "0")

        return  CellGameField(randRow, randColumn)
    }
    //Проверка состояния игрового поля
    private fun checkGameField(x: Int, y: Int, symbol: String): StatusInfo{
        var row = 0
        var column = 0
        var leftDiagonal = 0
        var rightDiagonal = 0
        var n = gameField.size

        //Подсчет символов
        for(i in 0..2){
            if(gameField[x][i] == symbol)
                row++
            if(gameField[i][y] == symbol)
                column++
            if(gameField[i][i] == symbol)
                leftDiagonal++
            if(gameField[i][n - i - 1] == symbol)
                rightDiagonal++

        }
        //Проверка условий победы в зависимости от условий
        return when(settinsInfo.rules){
            1 -> { //Только столбцы
                if(column == n)
                    StatusInfo(true, symbol)
                else
                    StatusInfo(false, "")
            }
            2 -> { //Только строки
                if(row == n)
                    StatusInfo(true, symbol)
                else
                    StatusInfo(false, "")
            }
            3 -> { //Строки и Столбцы
                if(row == n || column == n)
                    StatusInfo(true, symbol)
                else
                    StatusInfo(false, "")
            }
            4 -> { //Только диагонали
                return if(leftDiagonal == n || rightDiagonal == n)
                    StatusInfo(true, symbol)
                else
                    StatusInfo(false, "")
            }
            5 ->{ //Столбцы и диагонали
                return if(column == n || leftDiagonal == n || rightDiagonal == n)
                    StatusInfo(true, symbol)
                else
                    StatusInfo(false, "")
            }
            6 ->{ //Строки или диагонали
                return if(row == n || leftDiagonal == n || rightDiagonal == n)
                    StatusInfo(true, symbol)
                else
                    StatusInfo(false, "")
            }
            7 ->{ //Любое направление
                return if(column == n || row == n || leftDiagonal == n || rightDiagonal == n)
                    StatusInfo(true, symbol)
                else
                    StatusInfo(false, "")
            }

            else -> StatusInfo(false, "")
        }

    }
    //Хранение информации о статусе игры
    data class StatusInfo(val status: Boolean, val side: String)

    //Показ результата сыграной партии
    private fun showGameStatus(status: Int){
        val dialog = Dialog(this, R.style.Theme_TicTacToe)
        with(dialog){
            window?.setBackgroundDrawable(ColorDrawable(Color.argb(50, 0, 0, 0)))
            setContentView(R.layout.dialog_popup_status_game)
            setCancelable(true)
        }

        val image = dialog.findViewById<ImageView>(R.id.dialog_image)
        val text = dialog.findViewById<TextView>(R.id.dialog_text)
        val button = dialog.findViewById<TextView>(R.id.dialog_ok)

        button.setOnClickListener{
            onBackPressed()
        }

        when(status){
            STATUS_PLAYER_WIN -> {
                image.setImageResource(R.drawable.win)
                text.text = "Вы выиграли!"
            }
            STATUS_PLAYER_LOSE -> {
                image.setImageResource(R.drawable.lose)
                text.text = "Вы проиграли!"
            }
            STATUS_PLAYER_DRAW -> {
                image.setImageResource(R.drawable.draw)
                text.text = "Ничья!"
            }
        }

        dialog.show()
    }
    //Обработка других активностей
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == REQUEST_POPUP_MENU){
            if (resultCode == RESULT_OK){
                settinsInfo = getSettingsInfo()

                mediaPlayer = MediaPlayer.create(this, R.raw.music)
                mediaPlayer.isLooping = true
                setVolumeMediaPlayer(settinsInfo.soundValue)

                mediaPlayer.start()
            }
        } else{
            super.onActivityResult(requestCode, resultCode, data)
        }

    }
    //Показ всплывающего меню
    private fun showPopupMenu(){
        val dialog = Dialog(this@GameActivity, R.style.Theme_TicTacToe)
        with(dialog){
            window?.setBackgroundDrawable(ColorDrawable(Color.argb(50, 0, 0, 0)))
            setContentView(R.layout.dialog_popup_menu)
            setCancelable(true)
        }

        val toContinue = dialog.findViewById<Button>(R.id.dialog_continue)
        val toSettings = dialog.findViewById<Button>(R.id.dialog_settings)
        val toExit = dialog.findViewById<Button>(R.id.dialog_exit)

        toContinue.setOnClickListener{
            dialog.dismiss()
        }
        toSettings.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, SettingsActivity::class.java)
            startActivityForResult(intent, REQUEST_POPUP_MENU)

            settinsInfo = getSettingsInfo()
            setVolumeMediaPlayer(settinsInfo.soundValue)
        }
        toExit.setOnClickListener {
            //Сохранение игры
            val elapsedMills = SystemClock.elapsedRealtime() - binding.chronometer.base
            if (::gameField.isInitialized) {
                val gameFieldString = convertGameFieldToString(gameField)
            }
            val gameField = convertGameFieldToString(gameField)
            saveGame(elapsedMills, gameField)
            dialog.dismiss()
            onBackPressed()
        }

        dialog.show()
    }
    //Проверка заполненности игрового поля
    private fun isFilledGameField() : Boolean{
        gameField.forEach { strings ->
            if (strings.find { it == " " } != null)
                return false
        }
        return true
    }
    //Конвертация состояния игрового поля из массива в строку для сохранений
    private fun convertGameFieldToString(gameField: Array<Array<String>>) : String{
        val tmpArray = arrayListOf<String>()
        gameField.forEach { tmpArray.add(it.joinToString(";")) }
        return tmpArray.joinToString("\n")
    }
    //Сохранение игры
    private fun saveGame(time: Long, gameField: String){
        with(getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE).edit()){
            putLong(PREF_TIME, time)
            putString(PREF_GAME_FIELD, gameField)
            apply()
        }
    }
    //Воссстановление игровой сессии из данных
    private fun restartGame(time: Long, gameField: String){
        binding.chronometer.base = SystemClock.elapsedRealtime() - time

        this.gameField = arrayOf()
        //Разбор строки обратно в двумерный массив
        val rows = gameField.split("\n")
        rows.forEach{
            val columns = it.split(";")
            this.gameField += columns.toTypedArray()
        }
        //Восстановление UI
        this.gameField.forEachIndexed{ indexRow, strings ->
            strings.forEachIndexed { indexColumn, s ->
                makeStep(indexRow, indexColumn, this.gameField[indexRow][indexColumn])
            }
        }
    }
    //Получение настроек
    private fun getSettingsInfo() : SettingInfo {
        with(getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE)){
            val soundValue = getInt(PREF_SOUND_VALUE, 50)
            val lvl = getInt(PREF_LVL, 0)
            val rules = getInt(PREF_RULES, 7)

            return SettingInfo(soundValue, lvl, rules)
        }
    }

    companion object{
        const val STATUS_PLAYER_WIN = 1
        const val STATUS_PLAYER_LOSE = 2
        const val STATUS_PLAYER_DRAW = 3

        val scores = hashMapOf(Pair(STATUS_PLAYER_WIN, -1.0), Pair(STATUS_PLAYER_LOSE, 1.0), Pair(STATUS_PLAYER_DRAW, 0.0))

        const val PREF_TIME = "pref_time"
        const val PREF_GAME_FIELD = "pref_game_field"

        const val REQUEST_POPUP_MENU = 123

    }

}