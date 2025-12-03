package game.weapon;

import game.core.Entity;
import game.entity.Bullet;
import game.enumset.BulletType;
import game.manager.ResourceManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Weapon
 * - 탄 생성기 (발사 규칙, 연발 수, 확산각, 쿨타임 등)
 * - Player 또는 Enemy 가 사용
 */
public class Weapon {

    private BulletType bulletType;   // 탄 종류
    private int level;               // 강화 단계
    private long fireCooldown;       // 발사 간격(ms)
    private int burstCount;          // 한 번에 발사하는 탄 수
    private double spreadAngle;      // 발사 각도 확산 (도 단위)
    
    //★
    private long lastFireTime = 0;
    private static final ResourceManager rm = new ResourceManager();
    
    private String fireSfx = "plyer_shoot.wav";
    
    public Weapon() {}
    
    public Weapon(BulletType type) {
        this.bulletType = type;
        this.level = 1;
        this.fireCooldown = 180; // 기본 0.18초 간격
        this.burstCount = 1;
        this.spreadAngle = 0; // 직선 발사
    }

    // ───────────────────────────────
    // Setter / Getter
    // ───────────────────────────────
    public void setLevel(int level) { this.level = Math.max(1, level); }
    public int getLevel() { return level; }
    public long getFireCooldown() { return fireCooldown; }
    public void setFireCooldown(long cd) { this.fireCooldown = Math.max(30, cd); }
    public void setBurstCount(int c) { this.burstCount = Math.max(1, c); }
    public void setSpreadAngle(double deg) { this.spreadAngle = Math.max(0, deg); }
    public BulletType getBulletType() { return bulletType; }
    public void setFireSfx(String name) {
        this.fireSfx = name;
    }

    public String getFireSfx() {
        return fireSfx;
    }

    // ───────────────────────────────
    // 총알 생성
    // ───────────────────────────────
    // 지금 발사 가능한가?
    public boolean canFire() {
    	return System.currentTimeMillis() - lastFireTime >= fireCooldown;
    }
    
    /** 발사(기본: 위로) */
    public List<Bullet> createBullets(Entity owner) {
        return createBullets(owner, -90.0); // 스크린 기준 위쪽 각도
    }
    
    /** 발사(임의 각도 중심, 확산 적용) */
    public List<Bullet> createBullets(Entity owner, double baseAngleDeg) {
        lastFireTime = System.currentTimeMillis();
        List<Bullet> list = new ArrayList<>();

        double start = baseAngleDeg - (spreadAngle * 0.5);
        double step  = (burstCount == 1) ? 0 : (spreadAngle / (burstCount - 1));

        for (int i = 0; i < burstCount; i++) {
            double angDeg = (burstCount == 1) ? baseAngleDeg : start + step * i;
            double rad = Math.toRadians(angDeg);

            Bullet b = new Bullet(bulletType, owner);

            // ⚙ 탄 스펙
            b.setSpeed(600 + level * 20);
            b.setDamage(10 + level * 60);

            // 🖼 스킨
            switch (bulletType) {
                case BASIC   -> b.setSprite(rm.getImage("basic"));
                case RAPID   -> b.setSprite(rm.getImage("bullet_fast"));
                case CANNON  -> b.setSprite(rm.getImage("cannon_ball"));
                case MISSILE -> b.setSprite(rm.getImage("missile"));
                case LASER	 -> b.setSprite(rm.getImage("laser"));
            }

            // 🔧 발사 위치(총구 중앙)
            double x = owner.getX() + owner.getW() / 2.0 - b.getW() / 2.0;
            double y = owner.getY() - b.getH();
            b.setPosition(x, y);

            // 🔺 진행 방향(라디안) 지정
            b.setDirection(rad);

            list.add(b);
        }
        return list;
    }

    // ───────────────────────────────
    // 레벨 업 (단계별 강화)
    // ───────────────────────────────
    public void levelUp() {
        level++;
        switch (level) {
            case 2 -> {
                burstCount = 2;
                spreadAngle = 10;
            }
            case 3 -> {
                burstCount = 3;
                spreadAngle = 20;
            }
            case 4 -> {
                burstCount = 4;
                spreadAngle = 30;
            }
            case 5 -> {
                burstCount = 5;
                spreadAngle = 40;
            }
            default -> {
                burstCount = Math.min(6, burstCount + 1);
                spreadAngle = Math.min(45, spreadAngle + 5);
            }
        }
        // 쿨타임도 살짝 단축 (최소 60ms)
        fireCooldown = Math.max(60, fireCooldown - 10);
    }
}
