package com.example.tic_tac_toe

import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tic_tac_toe.R
import com.example.tic_tac_toe.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    //Доступ к элементам интерфейса
    private lateinit var binding: ActivitySettingsBinding

    //Переменные для хранения значений настроек
    private var currentSoundValue = 0
    private var currentLvl = 0
    private var currentRules = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        //Инициализация binding
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Обработка системных окон
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //Получение текущих настроек
        val data = getSettingsInfo()

        //Установка значений
        currentLvl = data.lvl
        currentRules = data.rules
        currentSoundValue = data.soundValue

        //Установка правил игры
        when(currentRules){
            1 -> binding.checkBoxVertical.isChecked = true
            2 -> binding.checkBoxHorizontal.isChecked = true
            3 -> {
                binding.checkBoxVertical.isChecked = true
                binding.checkBoxHorizontal.isChecked = true
            }
            4 -> binding.checkBoxDiagonal.isChecked = true
            5 ->{
                binding.checkBoxVertical.isChecked = true
                binding.checkBoxDiagonal.isChecked = true
            }
            6 ->{
                binding.checkBoxHorizontal.isChecked = true
                binding.checkBoxDiagonal.isChecked = true
            }
            7 ->{
                binding.checkBoxVertical.isChecked = true
                binding.checkBoxHorizontal.isChecked = true
                binding.checkBoxDiagonal.isChecked = true
            }
        }

        //Установка видимости кнопок изменения сложности
        if(currentLvl == 0){
            binding.prevLvl.visibility = View.INVISIBLE
        } else if (currentLvl == 2) {
            binding.nextLvl.visibility = View.INVISIBLE
        }

        //Установка текста текущей сложности и прогресса звука
        binding.infoLevel.text = resources.getStringArray(R.array.game_levels)[currentLvl]
        binding.soundBar.progress = currentSoundValue

        //Обработку кнопки назад
        binding.toback.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        //Обработчик кнопки
        binding.prevLvl.setOnClickListener{
            currentLvl--

            if(currentLvl == 0){
                binding.prevLvl.visibility = View.INVISIBLE
            } else if (currentLvl == 1) {
                binding.nextLvl.visibility = View.VISIBLE
            }

            binding.infoLevel.text = resources.getStringArray(R.array.game_levels)[currentLvl]

            updateLvl(currentLvl)
        }

        //Обработчик кнопки
        binding.nextLvl.setOnClickListener{
            currentLvl++

            if(currentLvl == 1){
                binding.prevLvl.visibility = View.VISIBLE
            } else if (currentLvl == 2){
                binding.nextLvl.visibility = View.INVISIBLE
            }

            binding.infoLevel.text = resources.getStringArray(R.array.game_levels)[currentLvl]

            updateLvl(currentLvl)
        }

        //Обработка изменения ползунка громкости
        binding.soundBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean){
                currentSoundValue = p1
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                updateSoundValue(currentSoundValue)
            }
        })

        //Обработка изменений чекбоксов правил
        binding.checkBoxVertical.setOnCheckedChangeListener { _, isChecked ->
            currentRules = if (isChecked) currentRules or 1 else currentRules and 6
            updateRules(currentRules)
        }

        binding.checkBoxDiagonal.setOnCheckedChangeListener { _, isChecked ->
            currentRules = if (isChecked) currentRules or 4 else currentRules and 3
            updateRules(currentRules)
        }

        binding.checkBoxHorizontal.setOnCheckedChangeListener { _, isChecked ->
            currentRules = if (isChecked) currentRules or 2 else currentRules and 5
            updateRules(currentRules)
        }

    }

    //Сохранение значения громкости
    private fun updateSoundValue(value: Int){
        with(getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE).edit()){
            putInt(PREF_SOUND_VALUE, value)
            apply()
        }
        setResult(RESULT_OK)
    }

    //Сохранение уровня сложности
    private fun updateLvl(lvl: Int){
        with(getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE).edit()){
            putInt(PREF_LVL, lvl)
            apply()
        }
        setResult(RESULT_OK)
    }

    //Сохранение правил игры
    private fun updateRules(rules: Int){
        with(getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE).edit()){
            putInt(PREF_RULES, rules)
            apply()
        }

        setResult(RESULT_OK)

    }

    //Получение текущих настроек
    private fun getSettingsInfo() : SettingInfo{
        with(getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE)){
            val soundValue = getInt(PREF_SOUND_VALUE, 0)
            val lvl = getInt(PREF_LVL, 0)
            val rules = getInt(PREF_RULES, 0)

            return SettingInfo(soundValue, lvl, rules)
        }
    }

    data class SettingInfo(val soundValue: Int, val lvl: Int, val rules: Int)

    companion object{
        const val PREF_SOUND_VALUE = "pref_sound_value"
        const val PREF_LVL = "pref_lvl"
        const val PREF_RULES = "pref_rules"
    }
}
