package game.entity;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import game.core.Entity;
import game.enumset.EntityType;
import game.enumset.Team;
import game.manager.ResourceManager;

/**
 * 플레이어 주변에 따라다니는 쉴드 이펙트
 * 이미지 키: image/shield_effect.png (없으면 투명 원으로 대체)
 */
public class ShieldEffect extends Entity {

    private static final ResourceManager rm = new ResourceManager();
    private static BufferedImage sprite;

    static {
        try {
            sprite = rm.getImage("shield_effect");
        } catch (RuntimeException e) {
            sprite = null;
        }
    }

    private final Player owner;
    private final long expireAtMs;

    public ShieldEffect(Player owner, long durationMs) {
        super(EntityType.EFFECT, Team.NEUTRAL,
                owner.getX(), owner.getY(), 1, 1);

        this.owner = owner;
        this.expireAtMs = System.currentTimeMillis() + Math.max(0, durationMs);

        if (sprite != null) {
            setSprite(sprite); // 크기는 스프라이트 기준으로 자동 설정
        }
    }

    @Override
    public void update(long dt) {
        if (!alive) return;

        // 플레이어 위치 따라가기 (중앙 맞추기)
        double x = owner.getX() + owner.getW() / 2.0 - getW() / 2.0;
        double y = owner.getY() + owner.getH() / 2.0 - getH() / 2.0;
        setPosition(x, y);

        long now = System.currentTimeMillis();
        if (!owner.isAlive() || now >= expireAtMs || !owner.hasShield()) {
            destroy();
        }
    }

    @Override
    public void render(Graphics2D g) {
        if (!alive) return;
        if (sprite != null) {
            super.render(g);
        } else {
            // 스프라이트 없으면 임시로 파란 원
            g.setColor(new java.awt.Color(80, 200, 255, 120));
            g.fillOval((int)getX(), (int)getY(), (int)getW(), (int)getH());
        }
    }

    @Override
    public void onCollision(Entity other) {
        // 이펙트이므로 충돌 없음
    }
}
