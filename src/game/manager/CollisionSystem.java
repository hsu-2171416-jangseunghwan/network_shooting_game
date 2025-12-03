package game.manager;

import game.bosseffect.ExplosionEffect;
import game.core.Entity;
//★
import game.enumset.EntityType;
import game.enumset.ItemSharePolicy;
import game.enumset.ItemType;
import game.entity.Player;
import game.interfaces.Updatable;
import game.item.Item;
import game.mode.LocalCoopConfig;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * CollisionSystem
 * - EntityManager의 엔티티들 간 충돌 판정 및 처리
 * - Team/Item/Bullet 별 판정 로직
 * - LocalCoopConfig에 따른 친선사격 및 겹침 정책 반영
 */

public class CollisionSystem implements Updatable {

    private final EntityManager entityManager;
    private LocalCoopConfig coopConfig;

    public CollisionSystem(EntityManager em) {
        this.entityManager = em;
    }

    public void setCoopConfig(LocalCoopConfig cfg) {
        this.coopConfig = cfg;
    }

    @Override
    public void update(long dt) {
        List<Entity> entities = entityManager.findByType(Entity.class); // 전체 가져오기

        for (int i = 0; i < entities.size(); i++) {
            Entity a = entities.get(i);
            if (!a.isAlive()) continue;

            for (int j = i + 1; j < entities.size(); j++) {
                Entity b = entities.get(j);
                if (!b.isAlive()) continue;

                // 빠른 AABB 충돌 체크
                if (!a.getBounds().intersects(b.getBounds())) continue;

                // 팀 / 타입 조건에 따라 충돌 무시 여부 판정
                if (!shouldCollide(a, b)) continue;

                // 실제 충돌 처리
                a.onCollision(b);
                b.onCollision(a);
                
             // ───────────────────────────────
             // 캐논 폭발 / 피버 / 무기 지급
             // ───────────────────────────────
             try {
                 // 💥 캐논 폭발 (Bullet ↔ Enemy)
                 if ((a.getEntityType() == EntityType.BULLET && b.getEntityType() == EntityType.ENEMY) ||
                     (a.getEntityType() == EntityType.ENEMY && b.getEntityType() == EntityType.BULLET)) {

                     game.entity.Bullet bullet = (a instanceof game.entity.Bullet) ? 
                             (game.entity.Bullet) a : (game.entity.Bullet) b;

                     if (bullet.getBulletType() == game.enumset.BulletType.CANNON) {
                         var bb = bullet.getBounds();
                         double cx = bb.getCenterX(), cy = bb.getCenterY();
                         entityManager.damageEnemiesInRadius(cx, cy, 120, 25); // 반경120, 피해25
                         // 폭발 이펙트
                         entityManager.add(new ExplosionEffect(cx, cy));
                     }
                 }

                 // ⚡ 플레이어가 아이템을 먹었을 때
                 if ((a.getEntityType() == EntityType.PLAYER && b.getEntityType() == EntityType.ITEM) ||
                     (a.getEntityType() == EntityType.ITEM && b.getEntityType() == EntityType.PLAYER)) {

                     game.entity.Player player = (a instanceof game.entity.Player) ? 
                             (game.entity.Player) a : (game.entity.Player) b;
                     game.item.Item item = (a instanceof game.item.Item) ? 
                             (game.item.Item) a : (game.item.Item) b;
                     
                     // 1) 무기 / Fever 아이템 : 먹은 사람만 적용
                     switch (item.getItemType()) {
                         case FEVER -> game.status.FeverRegistry.startScoreFever(player, 6000); // 6초간 점수 2배
                         case CANNON -> player.switchWeapon(new game.weapon.impl.CannonWeapon(30));
                         case LASER  -> player.switchWeapon(new game.weapon.impl.LaserWeapon(50));
                         case MISSILE-> player.switchWeapon(new game.weapon.impl.MissileWeapon(entityManager,20));
                         default -> {}
                     }
                     // 2) 공유형 아이템 (HEAL/POWER_UP/SHIELD/SPEED)
                     if (coopConfig != null &&
                         (coopConfig.itemSharePolicy == ItemSharePolicy.PERSISTENT_ONLY ||
                          coopConfig.itemSharePolicy == ItemSharePolicy.ALL)) {

                         ItemType t = item.getItemType();
                         boolean share =
                                 t == ItemType.HEAL ||
                                 t == ItemType.POWER_UP ||
                                 t == ItemType.SHIELD ||
                                 t == ItemType.SPEED;

                         if (share) {
                             var players = entityManager.findByType(Player.class);
                             for (Player p : players) {
                                 if (p == player) continue;   // 이미 먹은 사람은 Item.onCollision에서 처리됨
                                 if (!p.isAlive()) continue;
                                 item.applyTo(p);             // 같은 효과를 복사 적용
                             }
                         }
                     }
                 }
             } catch (Exception e) {
                 e.printStackTrace();
             }
            }
        }
    }
    
    // ────────────────────────────────────────────────
    // 충돌 필터링 
    // player 와 item, bullet type이 NEUTRAL 이라 총알 item 충돌이 되버림
    // ────────────────────────────────────────────────
    private boolean isItem(Entity e)   { return e.getEntityType() == EntityType.ITEM; }
    private boolean isBullet(Entity e) { return e.getEntityType() == EntityType.BULLET; }
    private boolean isPlayer(Entity e) { return e.getEntityType() == EntityType.PLAYER; }
   
    /** 충돌 여부 판단 로직 (팀/정책 기반) */
    private boolean shouldCollide(Entity a, Entity b) {
        // 둘 다 살아있지 않으면 패스
        if (!a.isAlive() || !b.isAlive()) return false;
        
        // 아이템은 플레이어와만 충돌
        if (isItem(a) || isItem(b)) {
            Entity other = isItem(a) ? b : a;
            return isPlayer(other);
        }
        
        // 총알끼리는 충돌 무시
        if (isBullet(a) && isBullet(b)) return false;
        
        // 동일 팀이면 (단, Enemy vs Player는 허용)
        if (a.getTeam() == b.getTeam()) {
        	
            // 같은 PLAYER 간 충돌만 막음 (협동모드)
            if (a.getTeam().name().equals("PLAYER")) {
                if (coopConfig != null) {
                    if (!coopConfig.allowOverlap) return false;
                    if (!coopConfig.friendlyFire) return false;
                }
                return false; // 플레이어끼리는 무시
            }
            // 적-적 간 충돌은 무시
            if (a.getTeam().name().equals("ENEMY")) return false;
        }

        // 중립-중립 무의미
        if (a.getTeam().name().equals("NEUTRAL") && b.getTeam().name().equals("NEUTRAL"))
            return false;

        return true;
    }
}