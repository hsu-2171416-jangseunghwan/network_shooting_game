package game.weapon;

import game.core.Entity;
import game.entity.Bullet;
import game.entity.Enemy;
import game.enumset.BulletType;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

// 🔥 보스 분노(QTE 실패) 전용 확산탄 패턴
// 기존 SpreadFirePattern은 그대로 두고, 이건 Stage3Boss에서만 사용
public class BossRageSpreadPattern implements FirePattern {

    private final ArcSpreadFire arcPattern;
    private final BufferedImage sprite;
    private final int bulletW, bulletH;

    public BossRageSpreadPattern(int count,
                                 double startDeg, double endDeg,
                                 double speed, int damage,
                                 BufferedImage sprite,
                                 int bulletW, int bulletH) {
        this.arcPattern = new ArcSpreadFire(count, startDeg, endDeg, speed, damage);
        this.sprite = sprite;
        this.bulletW = bulletW;
        this.bulletH = bulletH;
    }

    @Override
    public void fire(Enemy owner, List<Entity> out) {

        // ArcSpreadFire로 먼저 Bullet들을 생성
        List<Entity> temp = new ArrayList<>();
        arcPattern.fire(owner, temp);

        // SpreadFirePattern처럼 sprite만 세팅
        for (Entity e : temp) {
            if (e instanceof Bullet b) {

                // SpreadFirePattern과 완전 동일한 방식:
                if (sprite != null) {
                    b.setSprite(sprite);
                }

                // 크기 지정? SpreadFirePattern도 0이면 자동으로 기본 크기 사용하니까 필요 없음
                // b.setSize(bulletW, bulletH); → 없어도 무방
            }
            out.add(e);
        }
    }
}

