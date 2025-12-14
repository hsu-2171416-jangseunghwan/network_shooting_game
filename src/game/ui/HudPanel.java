package game.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.io.InputStream;

import game.entity.Player;
import game.manager.ResourceManager;

/**
 * ✨ STELLAR IMPACT - HUD PANEL (Figma 기반 디자인)
 * 구조 동일 / 디자인만 변경
 */

public class HudPanel {
    private final Player player;
    private long stageStartTime;
    private int totalTime = 60; // 기본 제한 시간
    private Font orbitronFont;
    private int currenScore = 0;
    
    //pause
    private long pausedAccumulated = 0;  
    private long pausedStart = 0;
    private boolean paused = false;

    // 아이템 아이콘 (OFF)
    private Image speedOff, healOff, shieldOff, powerOff;
    // 아이템 아이콘 (ON)
    private Image speedOn, healOn, shieldOn, powerOn;
    // 보스전 여부
    private boolean isBossStage = false;

    private Integer bossHp = null;
    private Integer bossMaxHp = null;
    private Integer bossPhase = null;
    
    private int remainingTimeFromServer = -1;
    // 공용 리소스 매니저
    private static final ResourceManager RM = new ResourceManager();

    public HudPanel(Player player) {
        this.player = player;
        this.stageStartTime = System.currentTimeMillis();
        loadFonts();
        loadIcons();
    }
    
    public void setRemainingTimeFromServer(int sec) {
        this.remainingTimeFromServer = Math.max(0, sec);
    }

    
    
    public void pauseTimer() {
        if (!paused) {
            paused = true;
            pausedStart = System.currentTimeMillis();
        }
    }

    public void resumeTimer() {
        if (paused) {
            paused = false;
            pausedAccumulated += System.currentTimeMillis() - pausedStart;
        }
    }
    
    public void setBossHp(int hp, int maxHp, int phase) {
        this.bossHp = hp;
        this.bossMaxHp = maxHp;
        this.bossPhase = phase;
    }

    // ===========================================================
    // 🟡 폰트 로드 (Orbitron)
    // ===========================================================
    private void loadFonts() {
        try (InputStream is = getClass().getResourceAsStream("/font/Orbitron-Regular.ttf")) {
            if (is != null) {
                orbitronFont = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(16f);
                System.out.println("✅ Orbitron 폰트 로드 완료!");
            } else {
                orbitronFont = new Font("Consolas", Font.BOLD, 16);
                System.err.println("⚠ Orbitron 폰트를 찾을 수 없어 기본 폰트 사용");
            }
        } catch (Exception e) {
            orbitronFont = new Font("Consolas", Font.BOLD, 16);
            System.err.println("⚠ Orbitron 로드 실패, 기본 폰트로 대체");
        }
    }

    // ===========================================================
    // 🟩 아이콘 로드 (OFF/ON 상태) - ResourceManager 사용
    // ===========================================================
    private void loadIcons() {
        // resources/image/ui/XXXX.png 에 맞춰서 로드
        speedOff  = RM.getImage("SPEED_OFF");
        healOff   = RM.getImage("HEAL_OFF");
        shieldOff = RM.getImage("SHILED_OFF");
        powerOff  = RM.getImage("POWER_OFF");

        speedOn   = RM.getImage("SPEED_ON");
        healOn    = RM.getImage("HEAL_ON");
        shieldOn  = RM.getImage("SHILDED_ON");
        powerOn   = RM.getImage("POWER_ON");

        System.out.println("✅ HUD 아이콘 로드 시도 완료");
    }

    // ===========================================================
    // 🕒 타이머 관리
    // ===========================================================
    public void setTotalTime(int seconds) { this.totalTime = seconds; }
    public void resetTimer() { this.stageStartTime = System.currentTimeMillis(); }
    
    /*
    public int getRemainingTime() {

        long now = System.currentTimeMillis();

        long elapsed;

        if (paused) {
            elapsed = (pausedStart - stageStartTime) - pausedAccumulated;
        } else {
            elapsed = (now - stageStartTime) - pausedAccumulated;
        }

        elapsed /= 1000;

        return Math.max(0, totalTime - (int) elapsed);
    }
    
    public boolean isTimeUp() { return getRemainingTime() <= 0; }
    */
    public boolean isTimeUp() {
        if (remainingTimeFromServer >= 0) {
            return remainingTimeFromServer <= 0;
        }
        return false;
    }
    private String formatTime(int sec) {
        int min = sec / 60;
        int s = sec % 60;
        return String.format("%02d:%02d", min, s);
    }

    // ===========================================================
    // 🟦 메인 렌더
    // ===========================================================
    public void render(Graphics2D g, int width, int height) {
        int leftWidth = 150;
        int rightWidth = 150;
        int centerWidth = width - leftWidth - rightWidth;

        // Glow 경계선
        renderDividers(g, leftWidth, centerWidth, height);
        // 왼쪽 아이템 패널
        renderLeftPanel(g, 0, 0, leftWidth, height);
        // 중앙 TIME
        if (isBossStage) {
            renderBossHpBar(g, leftWidth, centerWidth, height);
        } else {
            renderCenterPanel(g, leftWidth, centerWidth, height);
        }
        // 오른쪽 SCORE
        renderRightPanel(g, width - rightWidth, 0, rightWidth, height);
    }

    // ===========================================================
    // ⚡ 경계선
    // ===========================================================
    private void renderDividers(Graphics2D g, int leftWidth, int centerWidth, int height) {
        int[] dividerX = {leftWidth, leftWidth + centerWidth};

        if (isBossStage) {
            float pulse = (float)(0.5 + 0.5 * Math.sin(System.currentTimeMillis() * 0.004));
            int alpha = (int)(120 + pulse * 120);
            g.setColor(new Color(255, 60, 120, alpha));
            g.setStroke(new BasicStroke(4f));
            for (int x : dividerX) g.drawLine(x, 0, x, height);
            return;
        }

        for (int x : dividerX) {
            GradientPaint grad = new GradientPaint(
                x - 3, 0, new Color(0, 255, 246, 80),
                x + 3, height, new Color(0, 255, 246, 160)
            );
            g.setPaint(grad);
            g.setStroke(new BasicStroke(3f));
            g.drawLine(x, 0, x, height);
        }
    }

    // ===========================================================
    // 🔹 왼쪽 COLLECTED 패널
    // ===========================================================
    private void renderLeftPanel(Graphics2D g, int x, int y, int w, int h) {
        g.setFont(orbitronFont.deriveFont(13f));
        
        int itemCount = 4;
        int iconSize = 64;   // 기본 아이콘 크기
        int gap = 20;
        int totalHeight = (iconSize * itemCount) + (gap * (itemCount - 1));
        int startY = h - totalHeight - 50;

        // 패널 배경
        g.setColor(new Color(0, 0, 0, 77));
        g.fillRoundRect(x + 10, startY - 30, w - 20, totalHeight + 60, 15, 15);
        g.setColor(new Color(0, 255, 246, 150));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(x + 10, startY - 30, w - 20, totalHeight + 60, 15, 15);

        // COLLECTED 텍스트
        g.setFont(orbitronFont.deriveFont(Font.BOLD, 14f));
        String title = "COLLECTED";
        int textWidth = g.getFontMetrics().stringWidth(title);
        g.setColor(new Color(0, 255, 246));
        g.drawString(title, x + (w - textWidth) / 2, startY - 10);

        // 아이콘 배열
        Image[] offs    = { speedOff, healOff, shieldOff, powerOff };
        Image[] ons     = { speedOn,  healOn,  shieldOn,  powerOn  };
        String[] labels = { "SPEED", "HEAL", "SHIELD", "POWER" };

        boolean[] active = new boolean[4];
        if (player != null) {
            active[0] = player.isSpeedBuffActive();
            active[1] = player.isHealBuffActive();
            active[2] = player.isShieldBuffActive();
            active[3] = player.isPowerBuffActive();
        }

        long now = System.currentTimeMillis(); // 🔹 펄스용 시간값

        for (int i = 0; i < itemCount; i++) {
            int yy = startY + i * (iconSize + gap);
            int iconX = x + (w - iconSize) / 2;

            boolean isActive = active[i];

            // ============================
            // ① 아이콘 배경 박스
            // ============================
            if (isActive) {
                float pulse = (float)(0.5 + 0.5 * Math.sin(now * 0.008));
                int baseAlpha = 80;
                int extraAlpha = (int)(pulse * 80);

                g.setColor(new Color(0, 255, 246, baseAlpha + extraAlpha));
                g.fillRoundRect(iconX - 4, yy - 4, iconSize + 8, iconSize + 8, 10, 10);

                g.setColor(new Color(0, 255, 246, 120 + extraAlpha / 2));
                g.setStroke(new BasicStroke(2.5f));
                g.drawRoundRect(iconX - 4, yy - 4, iconSize + 8, iconSize + 8, 10, 10);
            } else {
                g.setColor(new Color(255, 255, 255, 60));
                g.fillRoundRect(iconX, yy, iconSize, iconSize, 8, 8);
                g.setColor(new Color(0, 255, 246, 120));
                g.setStroke(new BasicStroke(1.5f));
                g.drawRoundRect(iconX, yy, iconSize, iconSize, 8, 8);
            }

            // ============================
            // ② 아이콘 이미지 (ON/OFF)
            // ============================
            Image icon = offs[i];
            if (isActive && ons[i] != null) {
                icon = ons[i];
            }

            if (icon != null) {
                int drawX = iconX;
                int drawY = yy;

                if (isActive) {
                    int grown = iconSize + 6;
                    drawX = iconX - 3;
                    drawY = yy - 3;
                    g.drawImage(icon, drawX, drawY, grown, grown, null);
                } else {
                    g.drawImage(icon, drawX, drawY, iconSize, iconSize, null);
                }
            }

            // ============================
            // ③ 아이템 라벨 텍스트
            // ============================
            g.setFont(orbitronFont.deriveFont(Font.PLAIN, 13f));

            if (isActive) {
                g.setColor(new Color(0, 255, 246, 230));
            } else {
                g.setColor(new Color(0, 255, 246, 180));
            }

            int lw = g.getFontMetrics().stringWidth(labels[i]);
            g.drawString(labels[i],
                    iconX + (iconSize - lw) / 2,
                    yy + iconSize + 15
            );
        }
    }

    // ===========================================================
    // 🔸 중앙 TIME HUD
    // ===========================================================
    private void renderCenterPanel(Graphics2D g, int startX, int width, int height) {
        int marginX = 20;
        int boxW = width - (marginX * 2);
        int boxH = 50;
        int boxX = startX + marginX;
        int boxY = 15;

        g.setColor(new Color(0, 0, 0, 170));
        g.fillRoundRect(boxX, boxY, boxW, boxH, 18, 18);

        g.setColor(new Color(255, 215, 0, 200));
        g.setStroke(new BasicStroke(3f));
        g.drawRoundRect(boxX, boxY, boxW, boxH, 18, 18);

        g.setFont(orbitronFont.deriveFont(Font.BOLD, 20f));
        g.setColor(Color.WHITE);
        String timeStr;

        if (remainingTimeFromServer >= 0) {
            timeStr = "TIME " + formatTime(remainingTimeFromServer);
        } else {
            timeStr = "TIME --:--";
        }
        
        int textWidth = g.getFontMetrics().stringWidth(timeStr);
        int textX = boxX + (boxW - textWidth) / 2;
        int textY = boxY + 33;
        g.drawString(timeStr, textX, textY);
    }

    // ===========================================================
    // 🔹 오른쪽 SCORE HUD
    // ===========================================================
    private void renderRightPanel(Graphics2D g, int x, int y, int w, int h) {
        
    	
    	int boxW = w - 40;
        int boxH = 70;
        int boxX = x + 20;
        int boxY = y + 20;

        g.setColor(new Color(10, 40, 70, 140));
        g.fillRoundRect(boxX, boxY, boxW, boxH, 16, 16);

        g.setColor(new Color(0, 255, 246, 100));
        g.setStroke(new BasicStroke(1.6f));
        g.drawRoundRect(boxX, boxY, boxW, boxH, 16, 16);

        g.setFont(orbitronFont.deriveFont(Font.BOLD, 16f));
        String title = "SCORE";
        int titleWidth = g.getFontMetrics().stringWidth(title);

        g.setFont(orbitronFont.deriveFont(Font.PLAIN, 20f));
        String scoreStr = String.format("%05d", player != null ? player.getScore() : 0);
        int scoreWidth = g.getFontMetrics().stringWidth(scoreStr);

        int titleX = boxX + (boxW - titleWidth) / 2;
        int scoreX = boxX + (boxW - scoreWidth) / 2;

        g.setFont(orbitronFont.deriveFont(Font.BOLD, 16f));
        g.setColor(new Color(200, 240, 255));
        g.drawString(title, titleX, boxY + 27);

        g.setFont(orbitronFont.deriveFont(Font.PLAIN, 20f));
        g.setColor(new Color(224, 247, 255));
        g.drawString(scoreStr, scoreX, boxY + 55);
    }

    private void renderBossHpBar(Graphics2D g, int startX, int width, int height) {

        if (bossHp == null || bossMaxHp == null) return;

        int hp = bossHp;
        int max = bossMaxHp;

        int marginX = 20;
        int barW = width - marginX * 2;
        int barH = 32;
        int barX = startX + marginX;
        int barY = 16;

        // 배경
        g.setColor(new Color(30, 0, 0, 160));
        g.fillRoundRect(barX, barY, barW, barH, 16, 16);

        float ratio = Math.max(0f, (float) hp / max);
        int fillW = (int)(barW * ratio);

        g.setColor(new Color(255, 70, 70, 220));
        g.fillRoundRect(barX, barY, fillW, barH, 16, 16);

        g.setColor(new Color(255, 200, 200));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(barX, barY, barW, barH, 16, 16);

        g.setFont(orbitronFont.deriveFont(Font.BOLD, 18f));
        String txt = "BOSS " + hp + " / " + max;
        int tw = g.getFontMetrics().stringWidth(txt);
        g.setColor(Color.WHITE);
        g.drawString(txt, barX + (barW - tw) / 2, barY + 22);

        if (bossPhase != null) {
            g.setFont(orbitronFont.deriveFont(Font.PLAIN, 14f));
            String phaseTxt = "PHASE " + bossPhase;
            int pw = g.getFontMetrics().stringWidth(phaseTxt);
            g.setColor(new Color(255, 220, 220));
            g.drawString(phaseTxt, barX + (barW - pw) / 2, barY + 47);
        }
    }


    
    // ===========================================================
    // ⚙ Setter
    // ===========================================================
    public void setBossStage(boolean boss) { this.isBossStage = boss; }
    public boolean isBossStage() { return isBossStage; }
}
