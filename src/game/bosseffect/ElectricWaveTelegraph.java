// src/game/entity/ElectricWaveTelegraph.java
package game.bosseffect;
//Boss phase 전용 임시 이펙트
import game.core.Entity;
import game.enumset.EntityType;
import game.enumset.Team;

import java.awt.*;

public class ElectricWaveTelegraph extends Entity {

    private long lifeMs;

    public ElectricWaveTelegraph(double x, double y, double w, double h, long lifeMs) {
        super(EntityType.EFFECT, Team.NEUTRAL, x, y, w, h);
        this.lifeMs = lifeMs;
    }

    @Override
    public void update(long dt) {
        lifeMs -= dt;
        if (lifeMs <= 0) destroy();
    }

    @Override
    public void render(Graphics2D g) {
        // 얇은 테두리로 "곧 전체 파동 온다" 느낌
        g.setColor(new Color(80, 200, 255, 80));
        g.drawRect((int)getX(), (int)getY(), (int)getW(), (int)getH());
    }
}
