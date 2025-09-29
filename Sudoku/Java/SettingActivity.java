package com.iot.sudokugame;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingActivity extends AppCompatActivity {

    private SwitchMaterial soundSwitch;
    private SwitchMaterial vibrationSwitch;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        // SharedPreferences 초기화
        prefs = getSharedPreferences("SudokuGameSettings", MODE_PRIVATE);
        soundSwitch = findViewById(R.id.soundSwitch);
        vibrationSwitch = findViewById(R.id.vibrationSwitch);
        Button backButton = findViewById(R.id.back);
        // 저장된 설정값 불러와서 스위치 상태에 반영하기
        loadSettings();
        // 사운드 스위치 리스너 설정
        soundSwitch.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            // 스위치 상태가 변경될 때마다 ShardPreferences에 저장
            saveSetting("sound_enabled", isChecked);
        }));
        // 진동 스위치 리스너 설정
        vibrationSwitch.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            // 스위치 상태가 변경될 때마다 SharedPreferences에 저장
            saveSetting("vibration_enabled", isChecked);
        }));
        // '메인으로 돌아가기' 버튼 리스너 설정
        backButton.setOnClickListener(v -> finish());
    }
    // 저장된 설정값을 불러와 UI에 적용하는 메소드
    private void loadSettings() {
        // "sound_enabled" 키로 저장된 boolean 값을 불러옵니다.
        // 저장된 값이 없으면 기본값으로 true(ON)을 사용합니다.
        boolean soundEnabled = prefs.getBoolean("sound_enabled", true);
        soundSwitch.setChecked(soundEnabled);

        // "vibration_enabled" 키로 저장된 boolean 값을 불러옵니다.
        // 저장된 값이 없으면 기본값으로 true(ON)를 사용합니다.
        boolean vibrationEnabled = prefs.getBoolean("vibration_enabled", true);
        vibrationSwitch.setChecked(vibrationEnabled);
    }
    /*
     * 특정 설정을 SharedPreferences에 저장하는 메소드
     * @param key 저장할 설정의 이름 (키)
     * @param value 저장할 설정의 값
     */
    private void saveSetting(String key, boolean value) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }
}
