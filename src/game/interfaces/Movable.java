package game.interfaces;

import java.awt.geom.Point2D;

public interface Movable {
	void setVelocity(double vx, double vy); // 속도 설정
    Point2D getPosition();                  // 위치 반환
    void move(long dt);                     // 이동 갱신
}
