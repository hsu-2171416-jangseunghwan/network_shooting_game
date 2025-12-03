package game.weapon.impl;

import java.util.Collections;
import java.util.List;
import game.core.Entity;
import game.entity.Bullet;
import game.weapon.Weapon;

public class BasicWeapon extends Weapon {

    public BasicWeapon() {
        super(); // Weapon의 기본 생성자 호출
    }

    @Override
    public List<Bullet> createBullets(Entity owner) {
        // 옵션 A에서는 Player가 직접 총알을 생성하므로 여기선 아무 것도 안 함
        return Collections.emptyList();
    }
}
