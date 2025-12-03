package game.weapon;

import game.core.Entity;
import game.entity.Bullet;
import game.entity.Enemy;
import game.enumset.BulletType;

/** 
 * 정면으로 3발(좌/중/우) 쏘는 패턴.
 * - 각 탄은 아래(90도)로 동일 속도로 이동.
 * - 오프셋으로 발사 위치 살짝 벌려줌.
 */
public class TripleCannonFire implements FirePattern {
    private final double speed;      // 탄속(px/s)
    private final int damage;        // 데미지
    private final double gap;        // 좌/우로 벌릴 간격(px)
    private final double downRad = Math.toRadians(90);

    public TripleCannonFire(double speed, int damage, double gap) {
        this.speed = speed;
        this.damage = damage;
        this.gap = gap;
    }

    @Override
    public void fire(Enemy owner, java.util.List<Entity> out) {
        // 적의 하단 중앙을 기준점으로 잡는다
        double cx = owner.getX() + owner.getW() / 2.0;
        double y  = owner.getY() + owner.getH();

        // 좌/중/우 3발
        double[] xs = { cx - gap, cx, cx + gap };
        for (double x : xs) {
            Bullet b = new Bullet(BulletType.CANNON, owner);
            b.setPosition(x - b.getW()/2.0, y); // 탄 중심 정렬
            b.setSpeed(speed);
            b.setDirection(downRad);           // 아래로
            b.setDamage(damage);
            out.add(b);
        }
    }
}
