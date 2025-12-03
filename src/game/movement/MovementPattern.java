package game.movement;

import game.entity.Enemy;

public interface MovementPattern {
    void update(Enemy enemy, long dt);
}
