package game.movement;

import java.awt.Rectangle;

import game.entity.Enemy;

public class ZigZagMove implements MovementPattern {
    private final double speedY;      // 세로로 내려가는 속도 (px/sec)
    private final double amplitude;   // 좌우로 흔들리는 폭 (px)
    private final double frequency;   // 1초당 왕복 횟수 (Hz)

    private double time = 0;          // 누적 시간(초)
    private Double baseX = null;      // 처음 스폰된 X 좌표

    public ZigZagMove(double speedY, double amplitude, double frequency) {
        this.speedY = speedY;
        this.amplitude = amplitude;
        this.frequency = frequency;
    }

    @Override
    public void update(Enemy e, long dt) {
        double sec = dt / 1000.0;
        time += sec;

        // 1️⃣ 처음 스폰된 X를 중심점으로 고정
        if (baseX == null) {
            baseX = e.getX();
        }

        // 2️⃣ 세로 이동
        e.setY(e.getY() + speedY * sec);

        // 3️⃣ 좌우 지그재그 (sin 기반)
        double offset = Math.sin(time * frequency) * amplitude;
        double newX = baseX + offset;

        // 4️⃣ playArea 안에서만 움직이도록 안전하게 클램프
        Rectangle play = (Rectangle) e.getPlayArea();
        if (play != null) {
            double minX = play.getMinX();
            double maxX = play.getMaxX() - e.getW();

            if (newX < minX) newX = minX;
            if (newX > maxX) newX = maxX;
        }

        e.setX(newX);
    }
}
