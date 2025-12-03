package game.weapon.impl;

import java.util.Collections;
import java.util.List;

import game.core.Entity;
import game.entity.Bullet;
import game.entity.Player;
import game.enumset.BulletType;
import game.weapon.Weapon;

public class LaserWeapon extends Weapon {
    private int ammo;

    public LaserWeapon(int initialAmmo) {
        super(BulletType.LASER);   // ✅ 레이저 타입
        this.ammo = Math.max(0, initialAmmo);
        setFireCooldown(800);
    }

    @Override
    public boolean canFire() {
        return ammo > 0 && super.canFire();   // 탄 + 쿨타임 둘 다 체크
    }

    @Override
    public List<Bullet> createBullets(Entity owner) {
        if (ammo <= 0) {
            if (owner instanceof Player p) p.switchWeapon(null);
            return Collections.emptyList();
        }

        ammo--;

        // ✅ Weapon의 레벨 기반 멀티샷 사용 (burstCount/angle 적용)
        List<Bullet> out = super.createBullets(owner);

        if (ammo == 0 && owner instanceof Player p) {
            p.switchWeapon(null);
        }
        return out;
    }
}
