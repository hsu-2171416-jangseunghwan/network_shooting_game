package game.manager;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import game.core.Entity;
import game.interfaces.Renderable;
import game.interfaces.Updatable;
import game.mode.LocalCoopConfig;   // ★ 추가

/**
 * EntityManager
 * - 게임 내 모든 Entity(플레이어, 적, 총알, 아이템 등)의 생명주기 관리
 * - 안전한 추가/제거 큐 구조 (update 중 직접 수정 방지)
 */
public class EntityManager implements Updatable, Renderable {

    private final List<Entity> entities = new ArrayList<>();
    private final List<Entity> toAdd = new ArrayList<>();
    private final List<Entity> toRemove = new ArrayList<>();
    private final CollisionSystem collisionSystem = new CollisionSystem(this);
    
    // ★ 추가: 코옵 설정 넘겨주기
    public void setCoopConfig(LocalCoopConfig cfg) {
        collisionSystem.setCoopConfig(cfg);
    }
    
    /** 엔티티 등록 (다음 프레임에 반영됨) */
    public void add(Entity e) {
        if (e != null) toAdd.add(e);
    }

    /** 엔티티 제거 예약 (다음 프레임에 반영됨) */
    public void remove(Entity e) {
        if (e != null) toRemove.add(e);
    }

    /** 특정 타입(Entity 하위클래스)만 추출 */
    @SuppressWarnings("unchecked")
    public <T extends Entity> List<T> findByType(Class<T> class1) {
        List<T> result = new ArrayList<>();
        for (Entity e : entities) {
            if (class1.isInstance(e)) result.add((T) e);
        }
        return result;
    }

    /** 모든 엔티티 갱신 */
    @Override
    public void update(long dt) {
        // 새로 추가된 엔티티 반영
        if (!toAdd.isEmpty()){
        entities.addAll(toAdd); toAdd.clear();
        }

        // 각 엔티티 갱신
        for (Entity e : entities) {
            e.update(dt);
        }

        // 죽은 객체 + 제거 예약된 객체 삭제
        Iterator<Entity> it = entities.iterator();
        while (it.hasNext()) {
            Entity e = it.next();
            if (!e.isAlive() || toRemove.contains(e)) {
                it.remove();
            }
        }
        toRemove.clear();

        collisionSystem.update(dt);
    }

    /** 모든 엔티티 렌더링 */
    @Override
    public void render(Graphics2D g) {
        for (Entity e : entities) {
            e.render(g);
        }
    }

    public void clearAll() {
        entities.clear();
        toAdd.clear();
        toRemove.clear();
    }

    public void addAll(List<? extends Entity> list) {
        if (list != null && !list.isEmpty()) toAdd.addAll(list);
    }

    public List<Entity> getAll() {
        return entities;
    }

    // ★ 추가: 유도탄/폭발용 유틸
    public game.entity.Enemy findNearestEnemy(double x, double y, double maxDist){
        game.entity.Enemy best = null;
        double bestD2 = (maxDist<=0)? Double.POSITIVE_INFINITY : maxDist*maxDist;
        for (game.core.Entity e : entities){
            if (!(e instanceof game.entity.Enemy enemy)) continue;
            var bb = enemy.getBounds();
            double dx = bb.getCenterX()-x, dy = bb.getCenterY()-y, d2 = dx*dx+dy*dy;
            if (d2 < bestD2){ bestD2 = d2; best = enemy; }
        }
        return best;
    }

    public void damageEnemiesInRadius(double cx, double cy, double radius, int damage){
        double r2 = radius*radius;
        for (game.core.Entity e : entities){
            if (!(e instanceof game.entity.Enemy enemy)) continue;
            var bb = enemy.getBounds();
            double dx = bb.getCenterX()-cx, dy = bb.getCenterY()-cy;
            if (dx*dx + dy*dy <= r2) enemy.takeDamage(damage);
        }
    }

    // ★ 기존 develop에 있던 정리용 메서드도 유지
    public void removeAllByType(String typeName) {
        entities.removeIf(e -> e.getType().name().equalsIgnoreCase(typeName));
    }

    public void clearAllExcept(Object keepEntity) {
        entities.removeIf(e -> e != keepEntity);
    }
}
