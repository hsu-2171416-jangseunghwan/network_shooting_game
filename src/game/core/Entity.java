package game.core;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.UUID;

import game.enumset.EntityType;
import game.enumset.Team;
import game.interfaces.Collidable;
import game.interfaces.Movable;
import game.interfaces.Renderable;
import game.interfaces.Updatable;

/** 모든 게임 오브젝트의 공통 기반.
 *  - dt(ms), vx/vy(px/s) 기준 이동
 *  - 스프라이트/사이즈/충돌 박스/생존 관리
 *  - 하위 클래스: update()/onCollision()만 오버라이드해 사용
 */
public abstract class Entity implements Updatable, Renderable, Collidable, Movable {

    // 식별/메타
    protected final UUID id;
    protected EntityType type;
    protected Team team;

    // 트랜스폼/물리
    protected Point2D.Double position;     // 좌상단 기준
    protected Dimension size;              // 폭/높이
    protected double vx, vy;               // 속도(px/s)
    protected double speed;                // 선택적
    protected double rotationDeg;          // 회전(도)

    // 렌더/충돌
    protected BufferedImage sprite;
    protected Shape hitbox;                // null이면 기본 AABB

    // 상태
    protected boolean alive = true;

    // 화면 경계
    protected Rectangle2D playArea;        

    protected Entity(EntityType type, Team team, double x, double y, double w, double h) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.team = team;
        this.position = new Point2D.Double(x, y);
        this.size = new Dimension((int) w, (int) h);
    }

    // ───────────────── Movable 구현 ─────────────────
    @Override
    public void setVelocity(double vx, double vy) { this.vx = vx; this.vy = vy; }

    @Override 
    public Point2D getPosition() { return position; }

    @Override
    public void move(long dt) {
        double dtSec = dt / 1000.0;
        position.x += vx * dtSec;
        position.y += vy * dtSec;
        clampToPlayArea();
    }

    // ───────────────── Updatable ─────────────────
    @Override
    public void update(long dt) {
        if (!alive) return;
        move(dt);
    }

    // ───────────────── Renderable ─────────────────
    @Override
    public void render(Graphics2D g) {
        if (!alive || sprite == null) return;

        if (rotationDeg == 0) {
            g.drawImage(sprite, (int) position.x, (int) position.y, null);
        } else {
            AffineTransform old = g.getTransform();
            g.rotate(Math.toRadians(rotationDeg),
                    position.x + size.width / 2.0,
                    position.y + size.height / 2.0);
            g.drawImage(sprite, (int) position.x, (int) position.y, null);
            g.setTransform(old);
        }
    }

    // ───────────────── Collidable ─────────────────
    @Override
    public Rectangle2D getBounds() {
        if (hitbox != null) {
            AffineTransform tx = AffineTransform.getTranslateInstance(position.x, position.y);
            return tx.createTransformedShape(hitbox).getBounds2D();
        }
        return new Rectangle2D.Double(position.x, position.y, size.width, size.height);
    }

    @Override
    public void onCollision(Entity other) {
        // 하위 클래스에서 오버라이드
    }

    // ───────────────── getters/setters ─────────────────
    public UUID getId() { return id; }
    public EntityType getEntityType() { return type; }
    public Team getTeam() { return team; }
    public boolean isAlive() { return alive; }
    public void destroy() { alive = false; }

    public double getX() { return position.x; }
    public double getY() { return position.y; }
    public double getW() { return size.getWidth(); }
    public double getH() { return size.getHeight(); }

    public void setPosition(double x, double y) { position.setLocation(x, y); }
    public void setSpeed(double speed) { this.speed = speed; }
    public void setRotationDeg(double deg) { this.rotationDeg = deg; }

    public void setSprite(BufferedImage sprite) {
        this.sprite = sprite;
        if (sprite != null) {
            this.size = new Dimension(sprite.getWidth(), sprite.getHeight());
        }
    }

    public void setHitbox(Shape shape) { this.hitbox = shape; }
    public void setPlayArea(Rectangle2D area) { this.playArea = area; }

    protected void clampToPlayArea() {
        if (playArea == null) return;

        double x = Math.max(playArea.getMinX(), Math.min(position.x, playArea.getMaxX() - size.width));
        double y = Math.max(playArea.getMinY(), Math.min(position.y, playArea.getMaxY() - size.height));
        position.setLocation(x, y);
    }

    public boolean intersects(Entity other) {
        return getBounds().intersects(other.getBounds());
    }

    // ✔ HEAD(Develop)에서 온 코드
    public EntityType getType() {
        return type;
    }

    // ✔ Feature-상빈에서 온 필수 기능
    public void setX(double x) { this.position.x = x; }
    public void setY(double y) { this.position.y = y; }
}
