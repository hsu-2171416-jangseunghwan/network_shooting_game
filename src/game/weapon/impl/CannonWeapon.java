package game.weapon.impl;

import java.util.Collections;
import java.util.List;

import game.core.Entity;
import game.entity.Bullet;
import game.entity.Player;
import game.enumset.BulletType;
import game.weapon.Weapon;

public class CannonWeapon extends Weapon {
    private int ammo;

    public CannonWeapon(int initialAmmo){
        super(BulletType.CANNON);     // ✅ 캐논 타입 설정 + 기본 레벨/패턴 사용
        this.ammo = Math.max(0, initialAmmo);
        setFireCooldown(420);
    }

    @Override
    public boolean canFire(){ 
        // ✅ 탄도 남아 있고, 쿨타임도 돌아야 발사 가능
        return ammo > 0 && super.canFire();
    }

    @Override
    public List<Bullet> createBullets(Entity owner){
        if (ammo <= 0) {
            if (owner instanceof Player p) p.switchWeapon(null);
            return Collections.emptyList();
        }

        ammo--;

        // ✅ Weapon의 멀티샷/스프레드 로직 그대로 사용
        List<Bullet> out = super.createBullets(owner);

        if (ammo == 0 && owner instanceof Player p) {
            p.switchWeapon(null);
        }
        return out;
    }
}
