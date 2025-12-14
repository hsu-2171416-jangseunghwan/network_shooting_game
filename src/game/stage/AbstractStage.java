package game.stage;

import java.awt.Graphics2D;

import GameStateTracker.java.GameStateTracker;
import game.entity.BossSingle;
import game.entity.Player;
import game.main.Game;
import game.manager.EntityManager;
import game.manager.ResourceManager;
import game.status.RunStats;
import game.ui.UIManager;


public abstract class AbstractStage {

    protected final EntityManager entityManager;
    protected final ResourceManager resourceManager;
    protected final Player player;
    protected final UIManager ui;

    protected AbstractStage nextStage;

    protected int stageTimeLimit = 60; // 보스전은 -1
    protected boolean stageEnded = false;
    protected long stageStartTime;
    // ★ RunStats를 모든 스테이지가 공유하게 된다
    protected RunStats runStats;

    public AbstractStage(EntityManager em, ResourceManager rm, Player p, UIManager ui) {
        this.entityManager = em;
        this.resourceManager = rm;
        this.player = p;
        this.ui = ui;
        this.runStats = null;  
    }
 // ★ 생성자에 RunStats 한 개 추가
    public AbstractStage(EntityManager entityManager, ResourceManager rm,
                         Player player, UIManager ui, RunStats runStats) {
        this.entityManager = entityManager;
        this.resourceManager = rm;
        this.player = player;
        this.ui = ui;

        this.runStats = runStats;   // ★ 저장
    }

    public void start() {
        System.out.println("[" + getClass().getSimpleName() + "] 스테이지 시작");
        stageEnded = false;
        stageStartTime = System.currentTimeMillis();

        if (stageTimeLimit > 0) {
            ui.getHud().setTotalTime(stageTimeLimit);
            ui.getHud().resetTimer();
        }

        spawnEnemies();
    }

    public void update(long dt) {
        if (stageEnded) return;
        
        // 🔥 PAUSED 상태에서는 timeUp 검사 금지
        if (GameStateTracker.isPaused()) return;

     // ❌ 멀티플레이에서는 HUD 타이머로 스테이지 종료 판단 금지
        if (!Game.isMultiplayer()) {
            if (stageTimeLimit > 0 && ui.getHud().isTimeUp()) {
                ui.showResult(
                    player.getScore(),
                    player.getCollectedCount(),
                    getStageNumber(),
                    System.currentTimeMillis() - stageStartTime
                );
                stageEnded = true;
            }
        }
    }

    public void setNextStage(AbstractStage next) {
        this.nextStage = next;
    }
    
    public boolean isEnded() { return stageEnded; }
    public AbstractStage getNextStage() { return nextStage; }

    public abstract void render(Graphics2D g);
    public abstract void spawnEnemies();
    public abstract int getStageNumber();
    public abstract boolean isCleared();
    public abstract BossSingle getBoss();
}
