package game.entity;

// ★
import game.manager.EntityManager;

import game.core.Entity;
import game.enumset.EntityType;
import game.enumset.Team;
import game.interfaces.*;
import game.status.RunStats;
import game.weapon.FirePattern;
import game.item.Item;
import game.item.ItemDropTable;
import game.movement.MovementPattern;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.ArrayList;
import game.enumset.EnemyKind;

import game.manager.AudioManager;

public class Enemy extends Entity implements Movable, Shootable, Damageable {

    private final List<Entity> spawned = new ArrayList<>();
    private MovementPattern movementPattern;  // 이동 패턴
    private FirePattern firePattern;          // 탄막 패턴
    private Path2D movementPath;              // Path 기반 이동용 (선택)
    private ItemDropTable dropTable;          // 드랍 테이블
    private EntityManager entityManager;      // ★ 드랍된 아이템 등록용

    private int hp, maxHp;
    private long fireCooldown;
    private long lastShotTime;
    private int scoreValue;
    private Player target;
    
    protected EnemyKind kind;   // ★ 이 적이 어떤 종류인지 저장
    private RunStats runStats;     // ★ 이 적이 죽었을 때 기록을 남길 RunStats
    

    // ───────────────────────────────
    // 생성자
    // ───────────────────────────────
    public Enemy(double x, double y, BufferedImage sprite) {
        super(EntityType.ENEMY, Team.ENEMY, x, y,
                (sprite != null ? sprite.getWidth() : 40),
                (sprite != null ? sprite.getHeight() : 40));

        setSprite(sprite);
        this.maxHp = 30;
        this.hp = maxHp;
        this.fireCooldown = 1200;
        this.speed = 120;
        this.scoreValue = 100;
        this.alive = true;
    }

    // ───────────────────────────────
    // 패턴 Setter
    // ───────────────────────────────
    public Enemy withMovement(MovementPattern pattern) { this.movementPattern = pattern; return this; }
    public Enemy withFire(FirePattern pattern) { this.firePattern = pattern; return this; }
    public Enemy withTarget(Player p) { this.target = p; return this; }
    public Enemy withDropTable(ItemDropTable t) { this.dropTable = t; return this; }
    public Enemy withEntityManager(EntityManager em) { this.entityManager = em; return this; } // ★
    public int getScoreValue(){ return scoreValue; }
    public Enemy withRunStats(RunStats stats) {
        this.runStats = stats;
        return this;
    }

    // ───────────────────────────────
    // Shootable 구현
    // ───────────────────────────────
    @Override
    public void shoot() {
        long now = System.currentTimeMillis();
        if (firePattern == null) return;
        if (now - lastShotTime < fireCooldown) return;
        
        lastShotTime = now;
        AudioManager.playSFX("enemy_shoot_laser.wav");
        System.out.println("[Enemy] Shoot triggered");
        firePattern.fire(this, spawned);
    }

    public void setFireCoolDown(long cooldown) { this.fireCooldown = cooldown; }
    public long getFireCooldown() { return fireCooldown; }
    public FirePattern getFirePattern() { return firePattern; }

    // ───────────────────────────────
    // Damageable 구현
    // ───────────────────────────────
    @Override
    public void takeDamage(int dmg) {
        if (!alive) return;
        
        AudioManager.playSFX("enemy_hit.wav");
        
        hp -= Math.max(0, dmg);

        if (hp <= 0) {
            hp = 0;
            onDeath();
        }
    }

    public int getHp() { return hp; }

    // ───────────────────────────────
    // 사망 처리
    // ───────────────────────────────
    public void onDeath() {
        alive = false;

        // 점수 처리
        if (target != null) {
            target.addScore(scoreValue);
            System.out.println("[Enemy] Destroyed → +" + scoreValue
                    + "pts (Player Score: " + target.getScore() + ")");
        }

        // 아이템 드랍 처리
        if (dropTable != null) {
            Item drop = dropTable.tryDrop(this); // ItemDropTable이 Item을 리턴한다고 가정
            if (drop != null && entityManager != null) {
            	 if (runStats != null) {
                     drop.setRunStats(runStats);
                 }
                entityManager.add(drop);
                System.out.println("[Enemy] dropped " + drop.getItemType());
            }
        }
        
     // ★ 디버그 로그
        System.out.println("[Enemy] onDeath kind=" + kind
                + ", runStatsNull=" + (runStats == null));
        
        // ★ 여기부터: 통계 기록 남기기
        if (runStats != null && kind != null) {
            runStats.onEnemyKilled(kind);
            System.out.println("[Enemy] Kill recorded for kind = " + kind);
        }


        System.out.println("[Enemy] Destroyed, +" + scoreValue + " pts");
    }

    // ───────────────────────────────
    // 이동 / 업데이트
    // ───────────────────────────────
    @Override
    public void move(long dt) {
        if (movementPattern != null) {
            movementPattern.update(this, dt);
        } else {
            double dtSec = dt / 1000.0;
            position.y += speed * dtSec;
        }

        if (playArea != null && position.y > playArea.getHeight()) {
            destroy();
        }
    }

    @Override
    public void update(long dt) {
        if (!alive) return;

        move(dt);

        if (target != null) {
            double dy = target.getY() - getX();
            if (dy > -400 && dy < 800) shoot();
        }

        if (playArea != null && !playArea.intersects(getBounds())) {
            destroy();
        }
    }

    // ───────────────────────────────
    // 렌더링
    // ───────────────────────────────
    @Override
    public void render(Graphics2D g) {
        if (!alive) return;

        if (sprite != null) {
            g.drawImage(sprite, (int)getX(), (int)getY(), null);
        } else {
            g.setColor(Color.RED);
            g.fillOval((int)getX(), (int)getY(), (int)getW(), (int)getH());
        }

        // HP bar
        int barW = (int)getW();
        int barH = 4;
        int bx = (int)getX();
        int by = (int)(getY() - barH - 2);

        g.setColor(new Color(0,0,0,100));
        g.fillRect(bx - 1, by - 1, barW + 2, barH + 2);

        float ratio = Math.max(0f, Math.min(1f, hp / (float)maxHp));

        g.setColor(Color.RED);
        g.fillRect(bx, by, (int)(barW * ratio), barH);

        g.setColor(Color.DARK_GRAY);
        g.drawRect(bx - 1, by - 1, barW + 2, barH + 2);
    }

    // ───────────────────────────────
    // 기타
    // ───────────────────────────────
    public List<Entity> drainSpawned() {
        List<Entity> out = new ArrayList<>(spawned);
        spawned.clear();
        return out;
    }

    /** BossPhase3 등에서 적이나 탄을 외부에서 등록할 때 필요 */
    public void spawnEntity(Entity e) {
        spawned.add(e);
    }

    public Rectangle2D getPlayArea() { return playArea; }

    @Override
    public void onCollision(Entity other) {
        if (other instanceof Player p) {
            p.takeDamage(15);
            destroy();
        }
    }
    public EnemyKind getKind() {            // 종류 조회용
        return kind;
    }

    public void setKind(EnemyKind kind) {   // 생성 후에 종류 지정용
        this.kind = kind;
    }
}
