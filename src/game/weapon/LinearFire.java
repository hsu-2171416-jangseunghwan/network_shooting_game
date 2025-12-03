package game.weapon;

import game.core.Entity;
import game.entity.Bullet;
import game.entity.Enemy;
import game.enumset.BulletType;

import java.util.List;
import java.util.ArrayList;

public class LinearFire implements FirePattern {

    private final BulletType bulletType;

    public LinearFire(BulletType type) {
        this.bulletType = type;
    }

    @Override
    public void fire(Enemy owner, List<Entity> outBullets) {
        Bullet b = new Bullet(bulletType, owner);
        b.setSpeed(300);
        b.setDamage(10);
        b.setDirection(Math.toRadians(90)); // 아래로 발사
        b.setPosition(owner.getX() + owner.getW() / 2 - b.getW() / 2,
                      owner.getY() + owner.getH());
        b.setVelocity(0, b.getSpeed());
        outBullets.add(b);
    }
}
