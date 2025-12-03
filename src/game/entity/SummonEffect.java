package game.entity;

import game.core.Entity;
import game.enumset.EntityType;
import game.enumset.Team;
import java.awt.*;


//
public class SummonEffect extends Entity {
    private long lifeMs;   // 이펙트 지속 시간
    private float alpha = 1f;

    public SummonEffect(double x, double y, double w, double h, long lifeMs) {
        super(EntityType.EFFECT, Team.ENEMY, x, y, w, h);
        this.lifeMs = lifeMs;
    }

    @Override
    public void update(long dt) {
        lifeMs -= dt;
        if (lifeMs <= 0) {
            destroy();
        } else {
            // 점점 사라지게
            alpha = Math.max(0f, lifeMs / 500f);
        }
    }

    @Override
    public void render(Graphics2D g) {
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.setColor(new Color(100, 200, 255, 180)); // 푸른빛
        g.fillOval((int)getX(), (int)getY(), (int)getW(), (int)getH());
        g.setComposite(old);
    }
}
