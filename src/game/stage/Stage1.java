package game.stage;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import game.entity.BossSingle;
import game.entity.Enemy;
import game.entity.Player;
import game.enumset.BulletType;
import game.enumset.EnemyKind;
import game.main.Game;
import game.manager.EntityManager;
import game.manager.ResourceManager;
import game.movement.LinearMove;
import game.status.RunStats;
import game.ui.UIManager;
import game.weapon.LinearFire;

public class Stage1 extends AbstractStage {

    private boolean clearedOnce = false;

    private long elapsedMs = 0;
    private final List<Enemy> enemies = new ArrayList<>();
    private final Random rng = new Random();

    /*
    public Stage1(EntityManager em, ResourceManager rm, Player player, UIManager ui) {
        super(em, rm, player, ui);
        this.stageTimeLimit = 5; // 테스트용
    }
    */
    
    public Stage1(EntityManager em, ResourceManager rm, Player player, UIManager ui, RunStats runStats) {
     super(em, rm, player, ui, runStats);
     this.stageTimeLimit = 10; // 테스트용
     }


    @Override
    public int getStageNumber() { return 1; }

    @Override
    public void start() {
        super.start();
        enemies.clear();
        elapsedMs = 0;
    }

 
    @Override
    public void spawnEnemies() {
    	   /*
        if (elapsedMs % 900 < 16) {

            BufferedImage src = resourceManager.getImage("스테이지2잡몸");
            if (src == null) return;

            int drawW = 150, drawH = 120;

            Image scaled = src.getScaledInstance(drawW, drawH, Image.SCALE_SMOOTH);
            BufferedImage sprite = new BufferedImage(drawW, drawH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = sprite.createGraphics();
            g2.drawImage(scaled, 0, 0, null);
            g2.dispose();

            Rectangle area = player.getPlayArea();

            int margin = 20;
            int minX = (int) area.getX() + margin;
            int maxX = (int) (area.getX() + area.getWidth() - drawW - margin);

            int spawnX = rng.nextInt(maxX - minX) + minX;

            Enemy e = new Enemy(spawnX, -drawH, sprite)
                    .withTarget(player)
                    .withMovement(new LinearMove(120))
                    .withFire(new LinearFire(BulletType.BASIC))
                    .withDropTable(new game.item.ItemDropTable()) // ★ 아이템 드랍
                    .withEntityManager(entityManager)           // ★ 매니저 주입
                    .withRunStats(runStats);   

            e.setPlayArea(area);
            e.setKind(EnemyKind.STAGE1); 

            enemies.add(e);
            entityManager.add(e);
        }
        */
    }

    
    @Override
    public void update(long dt) {
        super.update(dt);
        elapsedMs += dt;

        // 🔥 멀티플레이 모드일 경우: 로컬 적 생성 및 AI 완전 금지
        if (Game.isMultiplayer()) {
            return;   // 서버에서 준 적만 렌더됨
        }

        // 싱글플레이일 때만 기존 로직 실행
        if (!stageEnded) {

            spawnEnemies();

            for (Enemy e : enemies) {
                e.update(dt);
                entityManager.addAll(e.drainSpawned());
            }

            enemies.removeIf(e -> !e.isAlive());

        } else {

            if (!clearedOnce) {
                entityManager.clearAllExcept(player);
                clearedOnce = true;
            }

            if (nextStage == null) {
                nextStage = new Stage2(entityManager, resourceManager, player, ui, runStats);
            }
        }
    }


    @Override
    public void render(Graphics2D g) {
        entityManager.render(g);
    }

    @Override
    public boolean isCleared() {
        return elapsedMs > 15000;
    }

    @Override
    public BossSingle getBoss() {
        return null;
    }
}
