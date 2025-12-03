package game.entity;

import game.core.Entity;
import game.enumset.BulletType;
import game.enumset.EntityType;
import game.enumset.Team;
import game.manager.ResourceManager;

import java.awt.*;
import java.awt.geom.AffineTransform;

/**
 * Bullet
 * - 직선/유도 발사체 (플레이어 또는 적의 공격)
 * - 일정 수명 후 소멸
 */
public class Bullet extends Entity {

    private int damage;           // 피해량
    private double lifetimeMs;    // 총 생존 시간
    private long bornTime;        // 생성 시각
    private BulletType bulletType;// 탄종
    private Entity owner;         // 발사자(팀 판정용)
    private Entity homingTarget;  // 유도 타겟 (미사일 등)

    private double dirRad;        // 진행 방향(라디안)
    private double speed;         // 탄속(px/s)
    private static final ResourceManager rm = new ResourceManager();

    public Bullet(BulletType type, Entity owner) {
        super(EntityType.BULLET, owner.getTeam(),
                owner.getX(), owner.getY(), 8, 16);

        this.bulletType = type;
        this.owner = owner;
        this.alive = true;
        this.bornTime = System.currentTimeMillis();

        // 탄종별 기본 설정
        switch (type) {
            case BASIC -> {
                this.damage = 10;
                this.speed = 480;
                this.lifetimeMs = 2000;
            }
            case RAPID -> {
                this.damage = 6;
                this.speed = 700;
                this.lifetimeMs = 1500;
            }
            case CANNON -> {
                this.damage = 25;
                this.speed = 400;
                this.lifetimeMs = 2500;
            }
            case MISSILE -> {
                this.damage = 18;
                this.speed = 360;
                this.lifetimeMs = 4000;
            }
            case LASER -> {
                this.damage = 18;
                this.speed = 600;
                this.lifetimeMs = 2000;
                // ★ 레이저 이미지 적용
                var img = rm.getImage("bullet_laser");
                if (img != null) setSprite(img); // 크기 자동 동기화
            }
            case SPREAD -> {
                this.damage = 8;      // 빔보단 약하게
                this.speed = 260;     // 너무 느리지도 빠르지도 않게
                this.lifetimeMs = 2200;
            }
        }
    }

    public void setDamage(int dmg) { this.damage = dmg; }
    public int getDamage() { return damage; }

    /** 방향(라디안)을 지정하고 내부 속도 벡터 갱신 */
    public void setDirection(double radians) {
        this.dirRad = radians;
        this.vx = Math.cos(radians) * speed;
        this.vy = Math.sin(radians) * speed;
    }

    public void setSpeed(double spd) {
        this.speed = spd;
        // 기존 방향이 설정되어 있다면 즉시 갱신
        this.vx = Math.cos(dirRad) * speed;
        this.vy = Math.sin(dirRad) * speed;
    }

    public double getSpeed() { return speed; }

    public void setHomingTarget(Entity target) { this.homingTarget = target; }

    public BulletType getBulletType() { return bulletType; }

    @Override
    public void update(long dt) {
        if (!alive) return;

        // 유도 탄의 경우, 목표를 향해 방향 보정
        if (homingTarget != null && homingTarget.isAlive()) {
            double tx = homingTarget.getX() + homingTarget.getW() / 2.0;
            double ty = homingTarget.getY() + homingTarget.getH() / 2.0;
            double dx = tx - (getX() + getW() / 2.0);
            double dy = ty - (getY() + getH() / 2.0);
            double angle = Math.atan2(dy, dx);
            setDirection(angle);
        }

        super.update(dt);

        // 수명 초과 시 제거
        if (System.currentTimeMillis() - bornTime > lifetimeMs) {
            destroy();
        }

        // 화면 영역을 벗어나면 제거 (playArea 있을 경우)
        if (playArea != null) {
            if (!playArea.intersects(getBounds())) {
                destroy();
            }
        }
    }

    @Override
    public void render(Graphics2D g) {
        if (!alive) return;

        AffineTransform old = g.getTransform();

        // 💡 여기에서 회전 각도 보정 (+90도)
        g.rotate(Math.atan2(vy, vx) + Math.PI / 2,
                getX() + getW() / 2.0,
                getY() + getH() / 2.0);

        if (sprite != null) {
            g.drawImage(sprite, (int) getX(), (int) getY(), null);
        } else {
            Color color;
            switch (bulletType) {
                case BASIC -> color = Color.YELLOW;
                case RAPID -> color = new Color(255, 150, 0);
                case CANNON -> color = new Color(255, 80, 80);
                case MISSILE -> color = new Color(120, 200, 255);
                case LASER -> color = new Color(120, 255, 255);
                case SPREAD -> color = new Color(180, 255, 120);
                default -> color = Color.WHITE;
            }
            g.setColor(color);
            g.fillRoundRect((int) getX(), (int) getY(),
                    (int) getW(), (int) getH(), 4, 4);
        }

        g.setTransform(old);
    }

    @Override
    public void onCollision(Entity other) {
        // 아군/적군 구분: 같은 팀이면 무시
        if (other.getTeam() == this.team) return;

        // 피격 처리 (Damageable 대상)
        if (other instanceof game.interfaces.Damageable d) {
            d.takeDamage(damage);
        }

        if (other instanceof game.entity.Enemy en) {
            // 적이 방금 죽었으면(owner가 Player일 때 점수 지급)
            if (!en.isAlive() && owner instanceof game.entity.Player p) {
                int base = en.getScoreValue(); // Enemy에 scoreValue 게터 필요
                double mul = game.status.FeverRegistry.scoreMul(p); // 피버 배수
                p.addScore((int) Math.round(base * Math.max(1.0, mul)));
            }
        }

        destroy();
    }
}
