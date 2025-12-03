package game.movement;

import game.entity.Enemy;

import java.awt.geom.Rectangle2D;

/** 단순히 일정 속도로 아래로 이동 */
public class LinearMove implements MovementPattern {
    private double speed;

    public LinearMove(double speed) {
        this.speed = speed;
    }

    @Override
    public void update(Enemy e, long dt) {
        double dtSec = dt / 1000.0;
        e.setPosition(e.getX(), e.getY() + speed * dtSec);

        if (e.getPlayArea() != null && !e.getPlayArea().intersects(e.getBounds())) {
            e.destroy(); // 화면 벗어나면 제거
        }
    }
}
