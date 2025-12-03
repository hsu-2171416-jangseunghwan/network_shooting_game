// src/game/entity/ElectricWave.java
package game.bosseffect;
//Boss phase 전용 임시 이펙트

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

import game.core.Entity;
import game.entity.Player;
import game.enumset.EntityType;
import game.enumset.Team;

public class ElectricWave extends Entity {

    private long lifeMs;

    public ElectricWave(double x, double y, double w, double h, long lifeMs) {
        super(EntityType.EFFECT, Team.ENEMY, x, y, w, h);
        this.lifeMs = lifeMs;
    }

    @Override
    public void update(long dt) {
        lifeMs -= dt;
        if (lifeMs <= 0) {
            destroy();
        }
    }

    @Override
    public void render(Graphics2D g) {
        // lifeMs 남은 비율로 반지름 계산
        float t = Math.max(0f, Math.min(1f, lifeMs / 700f)); // 0~1
        // 1에서 0으로 줄어드니까 뒤집기
        float progress = 1f - t;

        int maxR = (int)(Math.max(getW(), getH()) * 0.7); // 최대 반경
        int r = (int)(maxR * progress);

        int cx = (int)(getX() + getW()/2);
        int cy = (int)(getY() + getH()/2);

        g.setColor(new Color(0, 200, 255, 120));
        g.setStroke(new BasicStroke(6));
        g.drawOval(cx - r, cy - r, r*2, r*2);
    }


    @Override
    public void onCollision(game.core.Entity other) {
        // 플레이어만 때리기
        if (other instanceof Player p) {
        	if (p.isDodging()) {//성공했을때는 안 맞음
        		System.out.println("[ElectricWave] 회피 성공");
        		p.triggerDodgeSuccessDisplay(); // ← 추가
        		 p.clearStun();      // 🔥 혹시 이전에 걸렸던 스턴도 모두 해제
                return;
            }
            p.stun(2000);
        }
    }
}
