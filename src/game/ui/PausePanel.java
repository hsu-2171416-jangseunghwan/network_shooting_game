// game/ui/PausePanel.java
package game.ui;

import game.manager.ResourceManager;
import game.status.RunStats;
import game.enumset.EnemyKind;

import java.awt.*;

public class PausePanel {

    // ----------------- 상태 -----------------
    private boolean visible = false;       // 패널 보이기/숨기기
    private int selectedIndex = 0;         // 0 = RESUME, 1 = MENU

    // 재개 카운트다운
    private boolean counting = false;
    private long countdownStart = 0L;
    private int countdownSeconds = 3;

    // 통계/리소스
    private final RunStats runStats;
    private final ResourceManager rm;
    private Font orbitronFont;

    // 🔥 현재 점수(왼쪽에 TOTAL SCORE로 표시)
    private int currentScore = 0;

    // ----------------- 생성자 -----------------
    public PausePanel(RunStats runStats, ResourceManager rm) {
        this.runStats = runStats;
        this.rm = rm;
        loadFont();
    }

    private void loadFont() {
        try {
            // 프로젝트에 Orbitron 폰트 연결돼 있으면 여기서 로드해도 됨
            Font base = new Font("Dialog", Font.BOLD, 18);
            this.orbitronFont = base;
        } catch (Exception e) {
            this.orbitronFont = new Font("Dialog", Font.BOLD, 18);
        }
    }

    // ----------------- 외부에서 쓰는 제어 메서드 -----------------
    public void show() {
        visible = true;
        counting = false;
    }

    public void hide() {
        visible = false;
        counting = false;
    }

    public boolean isVisible() {
        return visible;
    }

    public void moveUp() {
        // 0<->1 토글
        selectedIndex = (selectedIndex + 1) % 2;
    }

    public void moveDown() {
        selectedIndex = (selectedIndex + 1) % 2;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    // 🔥 Game에서 현재 점수 넣어줄 때 사용
    public void setCurrentScore(int score) {
        this.currentScore = score;
    }

    // 재개 카운트다운
    public void startResumeCountdown() {
        counting = true;
        countdownStart = System.currentTimeMillis();
    }

    public boolean isCounting() {
        return counting;
    }

    public boolean isCountdownFinished() {
        if (!counting) return false;
        long elapsed = (System.currentTimeMillis() - countdownStart) / 1000;
        return elapsed >= countdownSeconds;
    }

    public void stopCountdown() {
        counting = false;
    }

    // ----------------- 렌더 -----------------
    public void render(Graphics2D g, int width, int height) {
        if (!visible) return;

        // 배경 어둡게
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRect(0, 0, width, height);

        // 메인 카드 박스 (메뉴 화면 스타일)
        int panelW = 520;
        int panelH = 260;
        int panelX = (width - panelW) / 2;
        int panelY = (height - panelH) / 2;

        g.setColor(new Color(5, 35, 70, 210));
        g.fillRoundRect(panelX, panelY, panelW, panelH, 24, 24);

        g.setStroke(new BasicStroke(2.5f));
        g.setColor(new Color(0, 255, 246, 180));
        g.drawRoundRect(panelX, panelY, panelW, panelH, 24, 24);

        // 제목 "PAUSED"
        g.setFont(orbitronFont.deriveFont(Font.BOLD, 30f));
        g.setColor(new Color(200, 240, 255));
        String title = "PAUSED";
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(title);
        g.drawString(title, panelX + (panelW - tw) / 2, panelY + 45);

        // ----------------- 왼쪽: 통계 (RunStats) -----------------
        int statsX = panelX + 40;
        int statsY = panelY + 80;

        g.setFont(orbitronFont.deriveFont(Font.BOLD, 18f));
        g.setColor(new Color(0, 255, 246));
        g.drawString("ENEMIES DEFEATED", statsX, statsY);

        g.setFont(orbitronFont.deriveFont(Font.PLAIN, 16f));
        g.setColor(new Color(220, 235, 255));

        int lineGap = 28;
        statsY += 35;

        if (runStats != null) {
            int s1 = runStats.getEnemyKills(EnemyKind.STAGE1);
            int s2 = runStats.getEnemyKills(EnemyKind.STAGE2);
            int b  = runStats.getEnemyKills(EnemyKind.BOSS);

            g.drawString("Stage 1 : " + s1, statsX, statsY);
            statsY += lineGap;
            g.drawString("Stage 2 : " + s2, statsX, statsY);
            statsY += lineGap;
            g.drawString("Boss    : " + b,  statsX, statsY);
            statsY += lineGap * 2; // 한 줄 띄우기

            // 🔥 총 점수
            g.setFont(orbitronFont.deriveFont(Font.BOLD, 18f));
            g.setColor(new Color(255, 230, 140));
            g.drawString("TOTAL SCORE : " + currentScore, statsX, statsY);

        } else {
            g.drawString("No data", statsX, statsY);
        }

        // ----------------- 오른쪽: 버튼들 (RESUME / MENU) -----------------
        int btnW = 220;
        int btnH = 52;
        int btnX = panelX + panelW - btnW - 40;
        int btnY = panelY + 80;
        int btnGap = 70;

        String[] labels = {"RESUME", "MENU"};

        for (int i = 0; i < 2; i++) {
            boolean selected = (i == selectedIndex);

            int bx = btnX;
            int by = btnY + i * btnGap;

            // 버튼 배경
            if (selected) {
                g.setColor(new Color(180, 200, 0, 220)); // 노랑-올리브
            } else {
                g.setColor(new Color(0, 120, 180, 220)); // 파랑
            }
            g.fillRoundRect(bx, by, btnW, btnH, 16, 16);

            g.setStroke(new BasicStroke(2f));
            g.setColor(new Color(0, 255, 246, 200));
            g.drawRoundRect(bx, by, btnW, btnH, 16, 16);

            // 버튼 텍스트
            g.setFont(orbitronFont.deriveFont(Font.BOLD, 20f));
            fm = g.getFontMetrics();
            int lw = fm.stringWidth(labels[i]);

            if (selected)
                g.setColor(new Color(255, 255, 180));
            else
                g.setColor(new Color(220, 240, 255));

            int tx = bx + (btnW - lw) / 2;
            int ty = by + (btnH + fm.getAscent() - fm.getDescent()) / 2 - 2;
            g.drawString(labels[i], tx, ty);
        }

        // ----------------- 카운트다운 표시 (재개 직전) -----------------
        if (counting) {
            long elapsed = (System.currentTimeMillis() - countdownStart) / 1000;
            int remain = Math.max(0, countdownSeconds - (int) elapsed);

            g.setFont(orbitronFont.deriveFont(Font.BOLD, 60f));
            g.setColor(new Color(255, 255, 0, 230));
            String num = String.valueOf(remain);
            fm = g.getFontMetrics();
            int nx = width / 2 - fm.stringWidth(num) / 2;
            int ny = panelY - 30;
            g.drawString(num, nx, ny);
        }
    }
}
