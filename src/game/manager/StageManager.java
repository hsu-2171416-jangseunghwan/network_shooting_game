package game.manager;

import java.awt.Graphics2D;

import game.stage.AbstractStage;
import game.ui.UIManager;

public class StageManager {

    private AbstractStage currentStage;
    private UIManager uiManager;

    public StageManager(AbstractStage startStage, UIManager ui) {
        this.currentStage = startStage;
        this.uiManager = ui;
    }

    public AbstractStage getCurrentStage() { return currentStage; }

    public void update(long dt) {
        if (currentStage == null) return;

        currentStage.update(dt);

        // 결과창 떠 있으면 업데이트 멈춤
        if (uiManager != null && uiManager.isResultVisible()) return;
    }

    public void render(Graphics2D g) {
        if (currentStage != null)
            currentStage.render(g);
    }

    // Game이 READY 후 다음 스테이지로 넘길 때 사용
    public void goToNextStage() {
        if (currentStage != null && currentStage.getNextStage() != null) {
            currentStage = currentStage.getNextStage();
            currentStage.start();
        }
    }
}
