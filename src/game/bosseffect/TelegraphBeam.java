// src/game/entity/TelegraphBeam.java
package game.bosseffect;
//Boss phase 전용 임시 이펙트
import game.core.Entity;
import game.enumset.EntityType;
import game.enumset.Team;

import java.awt.*;

/**
 * 보스가 "여기로 쏜다" 하고 미리 보여주는 라인(요네 궁)
 */
public class TelegraphBeam extends Entity {

    private long lifeMs;  // 남은 시간(ms)

    public TelegraphBeam(double x, double y, double w, double h, long lifeMs) {
        // 중립 이펙트로 둘게. 충돌 필요 없음
        super(EntityType.EFFECT, Team.NEUTRAL, x, y, w, h);
        this.lifeMs = lifeMs;
    }
    
    
    

    @Override
    public void update(long dt) {
        lifeMs -= dt;
        if (lifeMs <= 0) {
            destroy(); // 수명 끝나면 사라짐
        }
    }

    @Override
    public void render(Graphics2D g) {
        if (!isAlive()) return;

        // 투명 빨간색으로 길게 긋기
        g.setColor(new Color(255, 0, 0, 90));
        g.fillRect((int)getX(), (int)getY(), (int)getW(), (int)getH());

        // 테두리 살짝
        g.setColor(new Color(255, 80, 80, 180));
        g.drawRect((int)getX(), (int)getY(), (int)getW(), (int)getH());
    }
}
