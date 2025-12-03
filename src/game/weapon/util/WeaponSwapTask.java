package game.weapon.util;

import game.core.Entity;
import game.entity.Player;
import game.enumset.EntityType;
import game.enumset.Team;
import game.weapon.Weapon;

public class WeaponSwapTask extends Entity {
    private final Player player; private final Weapon next;
    public WeaponSwapTask(Player p, Weapon next){
        super(EntityType.EFFECT, Team.NEUTRAL, 0,0,1,1);
        this.player = p; this.next = next;
    }
    @Override public void update(long dt){
        if (player != null && next != null) player.switchWeapon(next);
        destroy();
    }
    @Override public void render(java.awt.Graphics2D g){ /* no draw */ }
}
