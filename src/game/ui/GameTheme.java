package game.ui;

import java.awt.*;
import java.io.InputStream;

public class GameTheme {

    // Orbitron Global Font
    public static Font ORBITRON;

    static {
        try (InputStream is = GameTheme.class.getResourceAsStream("/font/Orbitron-Regular.ttf")) {
            if (is != null) {
                ORBITRON = Font.createFont(Font.TRUETYPE_FONT, is);
                System.out.println("⭐ Orbitron Global Font Loaded");
            } else {
                ORBITRON = new Font("Consolas", Font.PLAIN, 16);
                System.err.println("⚠ Orbitron font not found, fallback font");
            }
        } catch (Exception e) {
            ORBITRON = new Font("Consolas", Font.PLAIN, 16);
            System.err.println("⚠ Orbitron load failed, fallback used");
        }
    }

    // ────────────────────────────────
    // ★ FONT PRESETS (전체 UI 통일)
    // ────────────────────────────────
    public static Font TITLE = ORBITRON.deriveFont(Font.BOLD, 46f);
    public static Font SUBTITLE = ORBITRON.deriveFont(Font.BOLD, 30f);
    public static Font MENU_OPTION = ORBITRON.deriveFont(Font.PLAIN, 24f);
    public static Font COUNTDOWN = ORBITRON.deriveFont(Font.BOLD, 90f);
    public static Font READY = ORBITRON.deriveFont(Font.BOLD, 40f);
    public static Font HUD = ORBITRON.deriveFont(Font.PLAIN, 18f);
    public static Font SCORE = ORBITRON.deriveFont(Font.BOLD, 20f);
    public static Font RESULT_TITLE = ORBITRON.deriveFont(Font.BOLD, 28f);
    public static Font RESULT_TEXT = ORBITRON.deriveFont(Font.PLAIN, 16f);

    // ────────────────────────────────
    // ★ COLORS (네온 SF 스타일)
    // ────────────────────────────────
    public static final Color NEON_CYAN = new Color(0, 255, 255);
    public static final Color NEON_BLUE = new Color(120, 200, 255);
    public static final Color ENERGY_YELLOW = new Color(255, 240, 160);
    public static final Color ALERT_RED = new Color(255, 90, 90);
    public static final Color SOFT_WHITE = new Color(220, 240, 255);
    public static final Color PANEL_BG = new Color(0, 0, 0, 160);

    // 중앙 텍스트 헬퍼
    public static void drawCentered(Graphics2D g, String text, int w, int y) {
        int tw = g.getFontMetrics().stringWidth(text);
        g.drawString(text, (w - tw) / 2, y);
    }
}
      