package com.iot.sudokugame;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class RecordActivity extends AppCompatActivity {
    private TextView timetextView;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        // SharedPreferences 초기화(GameActivity에서 사용한 것과 이름이 같아야 합니다.)
        prefs = getSharedPreferences("SudokuGame", MODE_PRIVATE);

        timetextView = findViewById(R.id.time);
        Button deleteButton = findViewById(R.id.delete);
        Button clearButton = findViewById(R.id.clear);
        Button backButton = findViewById(R.id.back);
        // 저장된 기록을 불러와서 화면에 표시
        loadAndDisplayRecords();
        // '삭제' 버튼 리스너 설정
        deleteButton.setOnClickListener(v -> {
            // '보통' 난이도 기록만 삭제
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("bestTime_normal"); // "bestTime_normal" 이라는 키의 데이터만 삭제
            editor.apply();
            Toast.makeText(this, "보통 난이도 기록이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
            loadAndDisplayRecords(); // 화면 새로고침
        });
        // '초기화' 버튼 리스너 설정
        clearButton.setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit();
            // 모든 난이도의 최고 기록 관련 키들을 삭제 (저장된 게임은 유지)
            editor.remove("bestTime_normal");
            // editor.remove("bestTime_easy"); // 나중에 추가될 다른 난이도 기록
            // editor.remove("bestTime_hard"); // 나중에 추가될 다른 난이도 기록
            editor.apply();

            Toast.makeText(this, "모든 기록이 초기화되었습니다.", Toast.LENGTH_SHORT).show();
            loadAndDisplayRecords(); // 화면 새로고침
        });
        // '메인으로 돌아가기' 버튼 리스너 설정
        backButton.setOnClickListener(v -> {
            finish(); // 현재 화면 닫기
        });
    }
    // SharedPreferences에서 기록을 불러와 TextView에 표시하는 메소드
    private void loadAndDisplayRecords() {
        // "bestTime_normal" 키로 저장된 최고 기록(초 단위)을 불러옵니다.
        // 저장된 값이 없으면 -1을 기본값으로 사용합니다.
        int bestTimeSeconds = prefs.getInt("bestTime_normal", -1);

        if (bestTimeSeconds != -1) {
            // 저장된 기록이 있으면 "00:00" 형태로 변환하여 표시
            int minutes = bestTimeSeconds / 60;
            int secs = bestTimeSeconds % 60;
            timetextView.setText(String.format(java.util.Locale.getDefault(),"%02d:%02d", minutes, secs));
        } else {
            // 저장된 기록이 없으면 "--:--"로 표시
            timetextView.setText("--:--");
        }

        // TODO: 나중에 다른 난이도 기록을 불러와 표시하는 코드 추가
    }
}
