package game.interfaces;

import java.awt.geom.Rectangle2D;

import game.core.Entity;

public interface Collidable {
	Rectangle2D getBounds();     // AABB(충돌영역) 반환
    void onCollision(Entity other); // 충돌 처리 콜백
}
