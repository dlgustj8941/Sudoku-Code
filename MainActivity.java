package com.iot.sudokugame;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    private Button continueButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        continueButton = findViewById(R.id.btnContinue);
        Button newGameButton = findViewById(R.id.btnNewGame);
        Button recordButton = findViewById(R.id.btnRecord);
        Button settingButton = findViewById(R.id.btnSetting);
        Button rulesButton = findViewById(R.id.btnRules);

        android.content.res.ColorStateList purpleColorStateList = android.content.res.ColorStateList.valueOf(Color.parseColor("#6200EE"));

        continueButton.setBackgroundTintList(purpleColorStateList);
        newGameButton.setBackgroundTintList(purpleColorStateList);
        recordButton.setBackgroundTintList(purpleColorStateList);
        settingButton.setBackgroundTintList(purpleColorStateList);
        rulesButton.setBackgroundTintList(purpleColorStateList);

        // Continue 버튼 클릭 리스너 추가
        continueButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GameActivity.class);
            intent.putExtra("loadSavedGame", true); // 불러오기 모드로 실행
            startActivity(intent);
        });

        // 'NEW GAME' 버튼 클릭 시, 이전 저장 데이터 삭제
        newGameButton.setOnClickListener(v -> {
            // 새 게임 시작 전 저장된 데이터 삭제
            SharedPreferences prefs = getSharedPreferences("SudokuGame", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear(); // 모든 저장 데이터 삭제
            editor.apply();

            Intent intent = new Intent(MainActivity.this, GameActivity.class);
            startActivity(intent);
        });

        recordButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecordActivity.class);
            startActivity(intent);
        });

        settingButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingActivity.class);
            startActivity(intent);
        });

        setupRulesButton();
    }
    private void setupRulesButton() {
        Button rulesButton = findViewById(R.id.btnRules);
        rulesButton.setOnClickListener(v -> {
            // AlertDialog.Builder를 사용해 팝업창을 생성합니다.
            new AlertDialog.Builder(this)
                    .setTitle("스도큐 규칙")     // 팝업창 제목
                    .setMessage(
                            "기본 규칙\n" +
                            "1. 모든 행에는 1부터 9까지의 숫자가 중복 없이 한 번씩만 들어가야 합니다.\n" +
                            "2. 모든 열에는 1부터 9까지의 숫자가 중복 없이 한 번씩만 들어가야 합니다.\n" +
                            "3. 3x3 박스 안에는 1부터 9까지의 숫자가 중복 없이 한 번씩만 들어가야 합니다."
                    )   // 팝업창 내용
                    .setPositiveButton("확인", (dialog, which) -> {
                        // '확인' 버튼을 눌렀을 때의 동작(아무것도 안 하므로 비워둡니다.)
                        dialog.dismiss();   // 팝업창 닫기
                    })
                    .show();    // 팝업창을 화면에 표시
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 메인 화면이 다시 보일 때마다 저장된 게임이 있는지 확인
        if (CheckSavedGame()) {
            continueButton.setVisibility(View.VISIBLE);
        } else {
            continueButton.setVisibility(View.GONE);
        }
    }

    // SharedPreferences를 확인하도록 로직 변경
    private boolean CheckSavedGame() {
        SharedPreferences prefs = getSharedPreferences("SudokuGame", MODE_PRIVATE);
        return prefs.getBoolean("isGameSaved", false);
    }

    // DP 단위를 Pixel 단위로 변환하는 유틸리티 함수
    // 화면 밀도에 다라 일관된 크기를 유지하기 위해 필요합니다.
    private float dpToPx(int dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics()
        );
    }
}