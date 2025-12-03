package game.ui;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import game.status.RunStats;
import game.enumset.EnemyKind;
import game.manager.ResourceManager;

public class ResultPanel {

    private boolean visible = false;
    private int score, items, combo;
    private long clearTime;

    private RunStats runStats;

    private BufferedImage[] enemyIcons;
    private Font orbitronFont;

    public ResultPanel(RunStats stats, ResourceManager rm) {
        this.runStats = stats;
        loadFont();
        loadEnemyIcons(rm);
    }

    private void loadEnemyIcons(ResourceManager rm) {
        enemyIcons = new BufferedImage[3];
        enemyIcons[0] = rm.getImage("스테이지1잡몸");
        enemyIcons[1] = rm.getImage("스테이지2잡몸");
        enemyIcons[2] = rm.getImage("Boss1");
    }

    private void loadFont() {
        try (InputStream is = getClass().getResourceAsStream("/font/Orbitron-Regular.ttf")) {
            if (is != null)
                orbitronFont = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(16f);
            else
                orbitronFont = new Font("Consolas", Font.PLAIN, 16);
        } catch (Exception e) {
            orbitronFont = new Font("Consolas", Font.PLAIN, 16);
        }
    }

    public void show(int score, int items, int combo, long clearTime) {
        this.visible = true;
        this.score = score;
        this.items = items;
        this.combo = combo;
        this.clearTime = clearTime;
    }

    public void hide() { visible = false; }
    public boolean isVisible() { return visible; }

    // ============================================================
    // RENDER
    // ============================================================
    public void render(Graphics2D g, int width, int height) {
        if (!visible) return;

        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, width, height);

        int panelW = 540;
        int panelH = 330;
        int panelX = (width - panelW) / 2;
        int panelY = height / 2 - panelH / 2;

        g.setColor(new Color(10, 40, 70, 150));
        g.fillRoundRect(panelX, panelY, panelW, panelH, 25, 25);

        g.setColor(new Color(0, 255, 255, 150));
        g.setStroke(new BasicStroke(3f));
        g.drawRoundRect(panelX, panelY, panelW, panelH, 25, 25);

        // ============================================================
        // TITLE
        // ============================================================
        g.setFont(orbitronFont.deriveFont(Font.BOLD, 28f));
        g.setColor(Color.WHITE);
        String title = "RESULTS";
        int tw = g.getFontMetrics().stringWidth(title);
        g.drawString(title, (width - tw) / 2, panelY - 25);

        // ============================================================
        // TOTAL SCORE 위치 아래로 15px 내려줌
        // ============================================================
        g.setFont(orbitronFont.deriveFont(Font.BOLD, 22f));
        g.setColor(new Color(0, 255, 255));
        String scoreLabel = "TOTAL SCORE : " + score;
        int sw = g.getFontMetrics().stringWidth(scoreLabel);
        g.drawString(scoreLabel, (width - sw) / 2, panelY + 35);


        // ============================================================
        // LEFT COLUMN (ENEMIES)
        // ============================================================
        int colLeftX = panelX + 40;
        int colLeftY = panelY + 80;

        g.setFont(orbitronFont.deriveFont(Font.BOLD, 17f));
        g.setColor(new Color(0, 255, 255));
        g.drawString("ENEMIES DEFEATED", colLeftX, colLeftY);

        g.setFont(orbitronFont.deriveFont(Font.PLAIN, 14f));
        g.setColor(new Color(220, 240, 255));

        int iconSize = 58;
        int rowGap = iconSize + 14;
        int rowStartY = colLeftY + 40;

        int stage1Kills = runStats.getEnemyKills(EnemyKind.STAGE1);
        int stage2Kills = runStats.getEnemyKills(EnemyKind.STAGE2);
        int bossKills = runStats.getEnemyKills(EnemyKind.BOSS);

        String[] labels = {"Stage 1", "Stage 2", "Boss"};
        int[] kills = {stage1Kills, stage2Kills, bossKills};

        for (int i = 0; i < 3; i++) {
            int y = rowStartY + rowGap * i;

            if (enemyIcons[i] != null) {
                g.drawImage(enemyIcons[i], colLeftX, y - iconSize + 15,
                        iconSize, iconSize, null);
            }

            g.drawString(labels[i] + " : " + kills[i],
                    colLeftX + iconSize + 18, y);
        }

        // ============================================================
        // RIGHT COLUMN — ITEMS 제목을 ENEMIES와 정렬
        // ============================================================
        int colRightX = panelX + panelW - 200;

        // ITEMS 헤더를 ENEMIES DEFEATED 위치와 맞춤
        int itemsHeaderY = colLeftY;

        // COMBO 먼저 표시
        //g.setFont(orbitronFont.deriveFont(Font.PLAIN, 16f));
        //g.setColor(new Color(190, 240, 255));
        //g.drawString("COMBO : " + combo, colRightX, itemsHeaderY + 35);

        // ITEMS 제목
        g.setFont(orbitronFont.deriveFont(Font.BOLD, 17f));
        g.setColor(new Color(0, 255, 255));
        g.drawString("ITEMS", colRightX, colLeftY);

        // 실제 리스트 시작 y
        int ry = itemsHeaderY + 75;

        g.setFont(orbitronFont.deriveFont(Font.PLAIN, 15f));
        g.setColor(new Color(220, 240, 255));
        g.drawString("SPEED  : " + runStats.getSpeedItemUsed(), colRightX, itemsHeaderY + 65); 
        g.drawString("HEAL   : " + runStats.getHealItemUsed(), colRightX, itemsHeaderY + 95);  
        g.drawString("SHIELD : " + runStats.getShieldItemUsed(), colRightX, itemsHeaderY + 125);
        g.drawString("POWER  : " + runStats.getPowerItemUsed(), colRightX, itemsHeaderY + 155);
        
        int dividerX = panelX + panelW / 2;
        g.setColor(new Color(0, 255, 255, 80));   // Cyan Glow 느낌
        g.setStroke(new BasicStroke(2f));
        g.drawLine(dividerX, panelY + 70, dividerX, panelY + panelH - 50);
    
    }
}
