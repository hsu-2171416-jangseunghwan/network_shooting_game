package game.weapon;
//부채꼴 모양으로 발사하는 모션(잡몸용)


import game.core.Entity;
import game.entity.Bullet;
import game.entity.Enemy;
import game.enumset.BulletType;

/**
 * 중앙을 기준으로 startDeg~endDeg 사이에 N발 고르게 뿌림(도 단위).
 */
public class ArcSpreadFire implements FirePattern {
    private final int count;
    private final double startDeg, endDeg;
    private final double speed;
    private final int damage;

    public ArcSpreadFire(int count, double startDeg, double endDeg, double speed, int damage) {
        this.count = Math.max(1, count);
        this.startDeg = startDeg;
        this.endDeg = endDeg;
        this.speed = speed;
        this.damage = damage;
    }

    @Override
    public void fire(Enemy owner, java.util.List<Entity> out) {
        double ox = owner.getX() + owner.getW()/2.0;
        double oy = owner.getY() + owner.getH();

        double step = (count > 1) ? (endDeg - startDeg) / (count - 1) : 0;
        for (int i = 0; i < count; i++) {
            double deg = startDeg + step * i;
            double rad = Math.toRadians(deg);

            Bullet b = new Bullet(BulletType.SPREAD, owner);
            b.setPosition(ox - b.getW()/2.0, oy);
            b.setSpeed(speed);
            b.setDirection(rad);
            b.setDamage(damage);
            out.add(b);
        }
    }
}
