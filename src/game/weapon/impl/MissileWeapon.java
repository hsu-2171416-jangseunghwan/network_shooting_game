package game.weapon.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import game.manager.EntityManager;
import game.core.Entity;
import game.entity.Bullet;
import game.entity.Player;
import game.enumset.BulletType;
import game.weapon.Weapon;

public class MissileWeapon extends Weapon {
    private final EntityManager em;
    private int ammo;

    public MissileWeapon(EntityManager em, int initialAmmo) {
        super(BulletType.MISSILE);          // ✅ 미사일 타입
        this.em = em;
        this.ammo = Math.max(0, initialAmmo);
        setFireCooldown(520);
    }

    @Override
    public boolean canFire() {
        return ammo > 0 && super.canFire();
    }

    @Override
    public List<Bullet> createBullets(Entity owner) {
        if (ammo <= 0) {
            if (owner instanceof Player p) p.switchWeapon(null);
            return Collections.emptyList();
        }
        ammo--;

        // ✅ 기본 멀티샷 패턴대로 미사일 생성
        List<Bullet> list = super.createBullets(owner);
        if (list.isEmpty()) return list;

        // 🔥 유도 타겟 하나 잡아서 모든 미사일에 동일 타겟 설정
        double sx = owner.getX() + owner.getW()/2.0;
        double sy = owner.getY() + owner.getH()/2.0;
        var target = em.findNearestEnemy(sx, sy, 600);

        if (target != null) {
            for (Bullet m : list) {
                m.setHomingTarget(target);
            }
        }

        if (ammo == 0 && owner instanceof Player p) {
            p.switchWeapon(null);
        }
        return list;
    }
}
