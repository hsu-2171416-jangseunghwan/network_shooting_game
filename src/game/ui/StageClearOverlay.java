package game.ui;
import java.awt.*;

public class StageClearOverlay {
    private boolean visible = false;
    private long showStart;

    public void show() {
        visible = true;
        showStart = System.currentTimeMillis();
    }

    public boolean isVisible() {
        return visible && System.currentTimeMillis() - showStart < 3000; // 3초 표시
    }

    public void render(Graphics2D g) {
        if (!isVisible()) return;
        g.setColor(Color.WHITE);
        g.setFont(g.getFont().deriveFont(Font.BOLD, 48f));
        g.drawString("STAGE CLEAR", 60, 400);
    }
}
