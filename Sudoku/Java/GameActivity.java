package com.iot.sudokugame;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Stack;

public class GameActivity extends AppCompatActivity {
    // UI 요소 및 게임 상태 변수
    private final TextView[][] cells = new TextView[9][9];
    private int selectedRow = -1;
    private int selectedCol = -1;
    private final Button[] numberButtons = new Button[10];

    // 코드로 직접 만들 Drawable 객체
    private Drawable defaultCellBackground;
    private Drawable selectedCellBackground;
    // 하이라이트 배경 변수
    private Drawable highlightedCellBackground;

    // 타이머 관련 변수 추가
    private TextView timertextView;
    private final Handler timerHandler = new Handler();
    private int seconds = 0;

    // 현재 세션에서 저장 여부를 추적하는 변수 추가
    private boolean isGameSavedInSession = true;
    private static class Move {
        final int row, col, previousValue;
        Move(int row, int col, int previousValue) {
            this.row = row; this.col = col; this.previousValue = previousValue;
        }
    }
    private final Stack<Move> moveHistory = new Stack<>();

    // 예시 스도쿠 퍼즐 (0은 빈 칸)
    private final int[][] puzzle = {
            {5, 3, 0, 0, 7, 0, 0, 0, 0},
            {6, 0, 0, 1, 9, 5, 0, 0, 0},
            {0, 9, 8, 0, 0, 0, 0, 6, 0},
            {8, 0, 0, 0, 6, 0, 0, 0, 3},
            {4, 0, 0, 8, 0, 3, 0, 0, 1},
            {7, 0, 0, 0, 2, 0, 0, 0, 6},
            {0, 6, 0, 0, 0, 0, 2, 8, 0},
            {0, 0, 0, 4, 1, 9, 0, 0, 5},
            {0, 0, 0, 0, 8, 0, 0, 7, 9}
    };

    // 정답 스도쿠 배열 추가
    private final int[][] solution = {
            {5, 3, 4, 6, 7, 8, 9, 1, 2},
            {6, 7, 2, 1, 9, 5, 3, 4, 8},
            {1, 9, 8, 3, 4, 2, 5, 6, 7},
            {8, 5, 9, 7, 6, 1, 4, 2, 3},
            {4, 2, 6, 8, 5, 3, 7, 9, 1},
            {7, 1, 3, 9, 2, 4, 8, 5, 6},
            {9, 6, 1, 5, 3, 7, 2, 8, 4},
            {2, 8, 7, 4, 1, 9, 6, 3, 5},
            {3, 4, 5, 2, 8, 6, 1, 7, 9}
    };

    // 1초마다 실행될 코드 (Runnable 객체)
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            seconds++;  // 1초 증가
            timertextView.setText(formatTime(seconds)); // TextView 업데이트
            timerHandler.postDelayed(this, 1000);   // 1초 후에 다시 실행
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        timertextView = findViewById(R.id.timer);

        createCellDrawables();  // Drawable 객체들을 먼저 생성
        createBoard();

        boolean loadSavedGame = getIntent().getBooleanExtra("loadSavedGame", false);
        if (loadSavedGame) {
            loadSavedGame();
            isGameSavedInSession = true;    // 불러온 상태는 '저장됨'으로 시작
        }
        else {
            loadPuzzle();
            isGameSavedInSession = true;   // 새 게임eh '저장됨'으로 시작
        }

        // 숫자 버튼 상태 업데이트 호출 추가
        updateNumberPadState();

        setupNumberPad();
        setupEraseButton();
        setupUndoButton();  // 되돌리기 버튼 설정
        setupBackButton();
        setupSaveButton();
        setupExitButton();
    }

    // '새 게임' 버튼 기능 구현 메소드
    private void setupExitButton() {
        Button exitButton = findViewById(R.id.exit);
        exitButton.setOnClickListener(v -> {
            // 'BACK' 버튼과 동일한 로직을 적용합니다.
            if (isGameSavedInSession) {
                finish();   // 저장된 상태면 그냥 나가기
            }
            else {
                // 저장되지 않은 상태면 확인 팝업 띄우기
                new AlertDialog.Builder(this)
                        .setTitle("나가기")
                        .setMessage("저장하지 않고 그냥 나가시겠습니까?")
                        .setPositiveButton("예", (dialog, which) -> finish())
                        .setNegativeButton("아니오", null)
                        .show();
            }
        });
    }

    // '저장'버튼 설정 메소드 추가(기존 back 버튼 ID와 다름)
    private void setupSaveButton() {
        Button saveButton = findViewById(R.id.save);
        saveButton.setOnClickListener(v -> {
            saveGame();
            Toast.makeText(this, "게임이 저장되었습니다.", Toast.LENGTH_SHORT).show();
            isGameSavedInSession = true;    // 저장이 완료되었으므로 상태를 true로 변경
        });
    }
    // 게임 상태를 저장하는 메소드 추가
    private void saveGame() {
        SharedPreferences prefs = getSharedPreferences("SudokuGame", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // 현재 보드 상태를 하나의 문자열로 변환
        StringBuilder boardString = new StringBuilder();
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                String cellText = cells[r][c].getText().toString();
                if(cellText.isEmpty()) {
                    boardString.append("0");    // 빈칸은 0으로 저장
                }
                else {
                    boardString.append(cellText);
                }
            }
        }
        // SharedPreferences에 데이터 저장
        editor.putString("savedBoard", boardString.toString());
        editor.putInt("savedTime", seconds);
        editor.putBoolean("isGameSaved", true); // 저장된 게임이 있다는 플래그
        // 변경사항 적용
        editor.apply();
    }
    // 저장된 게임을 불러오는 메소드 추가
    private void loadSavedGame() {
        SharedPreferences prefs = getSharedPreferences("SudokuGame", MODE_PRIVATE);
        String boardString = prefs.getString("savedBoard", "");
        seconds = prefs.getInt("savedTime", 0);

        if (!boardString.isEmpty()) {
            for (int r = 0; r < 9; r++) {
                for (int c = 0; c < 9; c++) {
                    char numChar = boardString.charAt(r * 9 + c);
                    if (numChar != '0') {
                        cells[r][c].setText(String.valueOf(numChar));
                        // 원본 퍼즐에 숫자가 없던 칸(사용자 입력)만 파란색으로 표시
                        if (puzzle[r][c] == 0) {
                            cells[r][c].setTextColor(Color.BLUE);
                        }
                    }
                }
            }
        }
    }

    private void setupBackButton() {
        // XML에 있는 '뒤로 가기' 버튼을 ID로 찾아옵니다.
        Button backButton = findViewById(R.id.back);
        // 버튼에 클릭 리스너를 설정합니다.
        backButton.setOnClickListener(V -> {
            // 현재 세션이 저장된 상태라면, 바로 종료
            if (isGameSavedInSession) {
                finish();
            }
            else {
                // 저장되지 않은 상태라면, 사용자에게 확인 팝업을 띄움
                new AlertDialog.Builder(this)
                        .setTitle("나가기")
                        .setMessage("저장하지 않고 그냥 나가시겠습니까?")
                        .setPositiveButton("예", ((dialog, which) -> finish()))
                        .setNegativeButton("아니오", null) // null을 넣으면 팝업만 닫힘
                        .show();
            }
        });
    }

    // 화면이 사용자에게 보일 때 타이머 시작
    @Override
    protected void onResume() {
        super.onResume();
        startTimer();
    }
    // 화면이 가려질 때 타이머 정지 (배터리 절약, 오류 방지)
    @Override
    protected void onPause() {
        super.onPause();
        stopTimer();
    }
    // 타이머 시작 메소드
    private void startTimer() {
        // 1초 뒤에 timerRunnable을 실행
        timerHandler.postDelayed(timerRunnable, 1000);
    }
    // 타이머 정지 메소드
    private void stopTimer() {
        // 예약되어 있던 timerRunnable을 제거
        timerHandler.removeCallbacks(timerRunnable);
    }
    // 초를 "00:00" 형태의 문자열로 변환하는 메소드
    private String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int secs = totalSeconds % 60;
        return String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, secs);
    }


    // 셀 배경 스타일을 자바 코드로 생성하는 메소드
    private void createCellDrawables() {
        // 기본 셀 스타일
        GradientDrawable defaultDrawable = new GradientDrawable();
        defaultDrawable.setShape(GradientDrawable.RECTANGLE);
        defaultDrawable.setColor(Color.WHITE);
        defaultDrawable.setStroke(dpToPx(1), Color.parseColor("#CCCCCC"));
        defaultCellBackground = defaultDrawable;

        // 선택된 셀 스타일
        GradientDrawable selectedDrawable = new GradientDrawable();
        selectedDrawable.setShape(GradientDrawable.RECTANGLE);
        selectedDrawable.setColor(Color.parseColor("#FFBBDEFB"));   // 밝은 파란색
        selectedDrawable.setStroke(dpToPx(1), Color.parseColor("#CCCCCC"));
        selectedCellBackground = selectedDrawable;

        // 강조된 셀 스타일(밝은 파란색)을 코드로 생성
        GradientDrawable highlightedDrawable = new GradientDrawable();
        highlightedDrawable.setShape(GradientDrawable.RECTANGLE);
        highlightedDrawable.setColor(Color.parseColor("#FFBBDEFB")); // 연한 회색
        highlightedDrawable.setStroke(dpToPx(1), Color.parseColor("#CCCCCC"));
        highlightedCellBackground = highlightedDrawable;
    }
    private void createBoard() {
        GridLayout sudokuBoard = findViewById(R.id.sudoku);
        for (int row = 0; row < 9; row++) {
            for(int col = 0; col < 9; col++) {
                TextView cell = new TextView(this);
                cells[row][col] = cell;

                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.rowSpec = GridLayout.spec(row, 1f);
                params.columnSpec = GridLayout.spec(col, 1f);
                params.width = 0;
                params.height = 0;
                cell.setLayoutParams(params);

                cell.setBackground(defaultCellBackground);
                cell.setGravity(Gravity.CENTER);
                cell.setTextSize(20f);
                cell.setTextColor(Color.BLACK);

                // 마진 설정
                int topMargin = (row % 3 == 0) ? 4 : 1;
                int leftMargin = (col % 3 == 0) ? 4 : 1;
                int bottomMargin = (row == 8) ? 4 : 1;
                int rightMargin = (col == 8) ? 4 : 1;
                if(row % 3 == 2 && row != 8) bottomMargin = 4;
                if(col % 3 == 2 && col != 8) rightMargin = 4;
                ((GridLayout.LayoutParams) cell.getLayoutParams()).setMargins(leftMargin, topMargin, rightMargin, bottomMargin);

                final int r = row;
                final int c = col;
                cell.setOnClickListener(v -> onCellClicked(r, c));

                sudokuBoard.addView(cell);
            }
        }
    }
    private void onCellClicked(int row, int col) {
        selectedRow = row;
        selectedCol = col;
        updateCellHighlights();
    }
    private void updateCellHighlights() {
        // 모든 셀 배경을 기본으로 초기화
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                cells[r][c].setBackground(defaultCellBackground);
            }
        }
        if (selectedRow == -1 || selectedCol == -1) return;
        // 선택된 행과 열 강조
        for (int i = 0; i < 9; i++) {
            cells[selectedRow][i].setBackground(highlightedCellBackground);
            cells[i][selectedCol].setBackground(highlightedCellBackground);
        }
        // 3x3 박스 강조
        int startRow = selectedRow / 3 * 3;
        int startCol = selectedCol / 3 * 3;
        for (int r = startRow; r < startRow + 3; r++) {
            for (int c = startCol; c < startCol + 3; c++) {
                cells[r][c].setBackground(highlightedCellBackground);
            }
        }
        // 같은 숫자를 가진 모든 셀을 회색으로 강조합니다.
        String selectedNumberText = cells[selectedRow][selectedCol].getText().toString();
        // 선택된 칸에 숫자가 있을 경우에만 실행
        if(!selectedNumberText.isEmpty()) {
            int selectedNumber = Integer.parseInt(selectedNumberText);
            for(int r = 0; r < 9; r++) {
                for (int c = 0; c < 9; c++) {
                    // 현재 셀의 숫자를 가져와서 비교
                    String currentCellText = cells[r][c].getText().toString();
                    if(!currentCellText.isEmpty()) {
                        int currentCellNumber = Integer.parseInt(currentCellText);
                        if(currentCellNumber == selectedNumber) {
                            cells[r][c].setBackground(highlightedCellBackground);
                        }
                    }
                }
            }
        }
        // 현재 선택된 셀을 파란색으로 강조
        cells[selectedRow][selectedCol].setBackground(selectedCellBackground);
    }

    private void loadPuzzle() {
        for (int row = 0; row < 9; row++) {
            for(int col = 0; col < 9; col++) {
                if (puzzle[row][col] != 0) {
                    cells[row][col].setText(String.valueOf(puzzle[row][col]));
                    cells[row][col].setTypeface(null, Typeface.BOLD);
                }
            }
        }
    }
    private void setupNumberPad() {
        int[] buttonIds = {0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9};
        for (int i = 1; i <= 9; i++) {
            numberButtons[i] = findViewById(buttonIds[i]);
            final int number = i;
            numberButtons[i].setOnClickListener(v -> onNumberPadClicked(number));
        }
    }
    private void onNumberPadClicked(int number) {
        // 셀이 선택되었는지 먼저 확인합니다.
        if (selectedRow == -1) {
            Toast.makeText(this, "먼저 셀을 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        // 선택된 칸이 원래 퍼즐의 빈칸(값이 0)이었는지 확인합니다.
        if(puzzle[selectedRow][selectedCol] == 0) {
            isGameSavedInSession = false;   // 보드에 변경사항이 생겼으므로 '저장 안됨' 상태로 변경
            // '실행 취소'를 위해 이전 값을 기록
            String previousText = cells[selectedRow][selectedCol].getText().toString();
            int previousValue = previousText.isEmpty() ? 0 : Integer.parseInt(previousText);
            moveHistory.push(new Move(selectedRow, selectedCol, previousValue));
            // 셀에 새로운 숫자를 입력
            cells[selectedRow][selectedCol].setText(String.valueOf(number));
            // 사용자가 입력한 숫자임을 표시하기 위해 색상을 파란색으로 변경
            cells[selectedRow][selectedCol].setTextColor(Color.BLUE);
            // 새로 추가된 기능들 호출
            validateAllCells();
            updateNumberPadState();
            checkGameCompletion();
        }
        else {
            // 빈칸이 아니었다면(기본 숫자 칸), 사용자에게 알림 메시지를 보여줍니다.
            Toast.makeText(this, "기본 숫자는 변경할 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }
    // 입력값 검증 (빨간색 표시)
    private void validateAllCells() {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                String text = cells[r][c].getText().toString();
                // 사용자가 입력한 숫자만 검증 (원본 퍼즐에 0인 칸)
                if (!text.isEmpty() && puzzle[r][c] == 0) {
                    int number = Integer.parseInt(text);
                    if (isValidMove(r, c, number)) {
                        cells[r][c].setTextColor(Color.BLUE);
                    }
                    else {
                        cells[r][c].setTextColor(Color.RED);
                    }
                }
            }
        }
    }
    private boolean isValidMove(int row, int col, int number) {
        // 행 검사
        for (int i = 0; i < 9; i++) {
            if (i == col) continue;
            String text = cells[row][i].getText().toString();
            if (!text.isEmpty() && Integer.parseInt(text) == number) return false;
        }
        // 열 검사
        for (int i = 0; i < 9; i++) {
            if (i == row) continue;
            String text = cells[i][col].getText().toString();
            if (!text.isEmpty() && Integer.parseInt(text) == number) return false;
        }
        // 3x3 박스 검사
        int startRow = row / 3 * 3;
        int startCol = col / 3 * 3;
        for (int r = startRow; r < startRow + 3; r++) {
            for (int c = startCol; c < startCol + 3; c++) {
                if (r == row && c == col) continue;
                String text = cells[r][c].getText().toString();
                if (!text.isEmpty() && Integer.parseInt(text) == number) return false;
            }
        }
        return true;
    }
    // 숫자 버튼 상태 업데이트 (9개 사용 시 숨기기)
    private void updateNumberPadState() {
        int[] counts = new int[10]; // 1-9까지 숫자 카운트 (0번 인덱스 미사용)
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                String text = cells[r][c].getText().toString();

                // 셀에 숫자가 있고, 그 숫자가 빨간색이 아닐 경우에만 카운트
                if (!text.isEmpty() && cells[r][c].getCurrentTextColor() != Color.RED) {
                    counts[Integer.parseInt(text)]++;
                }
            }
        }

        for (int i = 1; i <= 9; i++) {
            if (numberButtons[i] != null) {
                if (counts[i] >= 9) {
                    numberButtons[i].setVisibility(View.INVISIBLE);
                } else {
                    numberButtons[i].setVisibility(View.VISIBLE);
                }
            }
        }
    }
    // 게임 완료 확인
    private void checkGameCompletion() {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (cells[r][c].getText().toString().isEmpty()) {
                    return; // 빈칸이 있으면 즉시 종료
                }
            }
        }
        // 모든 칸이 채워졌을 경우, 정답과 비교
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                int currentNum = Integer.parseInt(cells[r][c].getText().toString());
                if (currentNum != solution[r][c]) {
                    Toast.makeText(this, "틀린 부분이 있습니다. 확인해주세요.", Toast.LENGTH_SHORT).show();
                    return; // 오답이 있으면 즉시 종료
                }
            }
        }
        // 모든 검사를 통과하면 최고 기록을 확인하고 저장합니다.
        stopTimer();
        // SharedPreferences를 엽니다.
        SharedPreferences prefs = getSharedPreferences("SudokuGame", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        // '이어하기' 여부와 관계없이 저장된 게임 데이터 삭제
        editor.remove("savedBoard");
        editor.remove("savedTime");
        editor.remove("isGameSaved");
        // 최고 기록 확인 및 저장(기존의 '보통' 난이도 최고 기록을 불러옵니다. 기록이 없으면 -1을 가져옵니다.)
        int previousBestTime = prefs.getInt("bestTime_normal", -1);
        String dialogMessage = "축하합니다. 스도쿠를 완성했습니다.\n걸린 시간: " + formatTime(seconds);
        // 기존 기록이 없거나, 현재 기록이 더 빠르면 최고 기록을 갱신합니다.
        if(previousBestTime == -1 || seconds < previousBestTime) {
            editor.putInt("bestTime_normal", seconds); // 새로운 최고 기록 저장
            dialogMessage += "\n\n🎉 최고 기록 갱신! 🎉"; // 최고 기록 갱신 메시지 추가
        }
        editor.apply(); // 변경사항(기록 저장 및 데이터 삭제) 최종 적용
        // 게임 클리어 팝업을 띄웁니다.
        new AlertDialog.Builder(this)
                .setTitle("스도쿠 클리어!")
                .setMessage(dialogMessage)
                .setPositiveButton("메인으로", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }
    private void setupEraseButton() {
        Button eraseButton = findViewById(R.id.erase);
        eraseButton.setOnClickListener(v -> {
            if (selectedRow == -1) {
                Toast.makeText(this, "먼저 셀을 선택해주세요.",Toast.LENGTH_SHORT).show();
                return;
            }
            if (puzzle[selectedRow][selectedCol] != 0) {
                return;
            }
            String previousText = cells[selectedRow][selectedCol].getText().toString();
            if (!previousText.isEmpty()) {
                isGameSavedInSession = false;
                int previousValue = Integer.parseInt(previousText);
                moveHistory.push(new Move(selectedRow, selectedCol, previousValue));
                cells[selectedRow][selectedCol].setText("");
                validateAllCells();
                updateNumberPadState();
            }
        });
    }
    private void setupUndoButton() {
        Button undoButton = findViewById(R.id.undo);
        undoButton.setOnClickListener(v -> {
            if (moveHistory.isEmpty()) {
                Toast.makeText(this, "더 이상 되돌릴 수 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            isGameSavedInSession = false;
            Move lastMove = moveHistory.pop();
            TextView cell = cells[lastMove.row][lastMove.col];
            if (lastMove.previousValue == 0) {
                cell.setText("");
            } else {
                cell.setText(String.valueOf(lastMove.previousValue));
            }
            validateAllCells();
            updateNumberPadState();
            onCellClicked(lastMove.row, lastMove.col);
        });
    }
    // DP를 Pixel로 변환하는 유틸리티 메소드
    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics()
        );
    }
}
