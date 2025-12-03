package game.bosseffect;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import game.core.Entity;
import game.enumset.EntityType;
import game.enumset.Team;
import game.manager.ResourceManager;

/**
 * 캐논 탄이 적에 맞았을 때 잠깐 나오는 폭발 이펙트
 * 이미지 키: image/bomb_effect.png
 */
public class ExplosionEffect extends Entity {

    private static final ResourceManager rm = new ResourceManager();
    private static BufferedImage sprite;

    static {
        try {
            sprite = rm.getImage("bomb_effect");
        } catch (RuntimeException e) {
            sprite = null;
        }
    }

    private long lifeMs = 400;
    private long ageMs = 0;

    public ExplosionEffect(double centerX, double centerY) {
        super(EntityType.EFFECT, Team.NEUTRAL, centerX, centerY, 1, 1);

        if (sprite != null) {
            setSprite(sprite);
            // 중심 기준으로 위치 보정
            setPosition(centerX - getW() / 2.0, centerY - getH() / 2.0);
        }
    }

    @Override
    public void update(long dt) {
        if (!alive) return;
        ageMs += dt;
        if (ageMs >= lifeMs) destroy();
    }

    @Override
    public void render(Graphics2D g) {
        if (!alive) return;
        if (sprite != null) {
            super.render(g);
        }
    }

    @Override
    public void onCollision(Entity other) {
        // 이펙트이므로 충돌 없음
    }
}
