package game.weapon;

import game.entity.Enemy;
import java.util.List;
import game.core.Entity;

public interface FirePattern {
    void fire(Enemy owner, List<Entity> outBullets);
}
