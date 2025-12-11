package game.stage;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import game.item.ItemDropTable;
import game.entity.BossSingle;
import game.entity.Enemy;
import game.entity.Player;
import game.enumset.BulletType;
import game.enumset.EnemyKind;
import game.manager.EntityManager;
import game.manager.ResourceManager;
import game.movement.LinearMove;
import game.movement.MovementPattern;
import game.movement.ZigZagMove;
import game.status.RunStats;
import game.ui.UIManager;
import game.weapon.ArcSpreadFire;
import game.weapon.FirePattern;
import game.weapon.LinearFire;
import game.weapon.TripleCannonFire;
import game.main.Game;

public class Stage2 extends AbstractStage {

    private final List<Enemy> enemies = new ArrayList<>();
    private final Random rng = new Random();
    private long elapsedMs = 0;

    public Stage2(EntityManager em, ResourceManager rm, Player player, UIManager ui, RunStats runStats) {
        super(em, rm, player, ui, runStats);
        this.stageTimeLimit = 30000; 
    }

    @Override
    public int getStageNumber() { return 2; }

    @Override
    public void start() {
        super.start();
        enemies.clear();
        elapsedMs = 0;
    }

    // 🔥 Stage1 잡몹 재활용용
    private void spawnStage1Enemies() {

        if (elapsedMs % 2000 < 16) {

            BufferedImage src = resourceManager.getImage("스테이지2잡몸");
            if (src == null) return;

            int w = 150, h = 120;

            Image scaled = src.getScaledInstance(w, h, Image.SCALE_SMOOTH);
            BufferedImage sprite = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = sprite.createGraphics();
            g2.drawImage(scaled, 0, 0, null);
            g2.dispose();

            Rectangle area = player.getPlayArea();
            int minX = (int) area.getX() + 20;
            int maxX = (int)(area.getX() + area.getWidth() - w - 20);

            int spawnX = rng.nextInt(maxX - minX) + minX;

            Enemy e = new Enemy(spawnX, -h, sprite)
                .withTarget(player)
                .withDropTable(new ItemDropTable())
                .withEntityManager(entityManager)
                .withFire(new LinearFire(BulletType.BASIC))
                .withMovement(new LinearMove(120))
                .withRunStats(runStats);

            e.setPlayArea(area);
            e.setKind(EnemyKind.STAGE1);

            enemies.add(e);
            entityManager.add(e);
        }
    }

    @Override
    public void spawnEnemies() {

        if (elapsedMs % 1000 < 16) {

            BufferedImage src = resourceManager.getImage("스테이지1잡몸");
            if (src == null) return;

            int w = 200, h = 130;

            Image scaled = src.getScaledInstance(w, h, Image.SCALE_SMOOTH);
            BufferedImage sprite = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = sprite.createGraphics();
            g2.drawImage(scaled, 0, 0, null);
            g2.dispose();

            FirePattern pattern;
            int r = rng.nextInt(3);
            if (r == 0) pattern = new LinearFire(BulletType.BASIC);
            else if (r == 1) pattern = new TripleCannonFire(360, 10, 12);
            else pattern = new ArcSpreadFire(5, 60, 120, 280, 6);

            Rectangle area = player.getPlayArea();
            int minX = (int) area.getX() + 20;
            int maxX = (int)(area.getX() + area.getWidth() - w - 20);

            int spawnX = rng.nextInt(maxX - minX) + minX;

            Enemy e = new Enemy(spawnX, -h, sprite)
                .withTarget(player)
                .withDropTable(new ItemDropTable())
                .withEntityManager(entityManager)
                .withFire(pattern)
                .withMovement(new ZigZagMove(120, 40, 1))
                .withRunStats(runStats);

            e.setPlayArea(area);
            e.setKind(EnemyKind.STAGE2);

            enemies.add(e);
            entityManager.add(e);
        }
    }

    @Override
    public void update(long dt) {

        super.update(dt);
        elapsedMs += dt;

        // 🔥 멀티플레이면 클라이언트에서 적 생성 금지
        if (Game.isMultiplayer()) {
            return;
        }

        if (!stageEnded) {

            spawnEnemies();
            spawnStage1Enemies();

            for (Enemy e : enemies) {
                e.update(dt);
                entityManager.addAll(e.drainSpawned());
            }

            enemies.removeIf(e -> !e.isAlive());

        } else {
            if (ui.isResultVisible()) {
                nextStage = new Stage3Boss(entityManager, resourceManager, player, ui, runStats);
            }
        }
    }

    @Override
    public void render(Graphics2D g) {
        entityManager.render(g);
        for (Enemy e : enemies) e.render(g);
    }

    @Override
    public boolean isCleared() {
        return elapsedMs > stageTimeLimit;
    }

    @Override
    public BossSingle getBoss() {
        return null;
    }
}
