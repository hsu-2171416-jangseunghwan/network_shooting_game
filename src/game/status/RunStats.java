package game.status;

import game.enumset.EnemyKind;
import game.enumset.ItemType;
import java.util.EnumMap;

public class RunStats {

    // 적 처치 수
    private EnumMap<EnemyKind, Integer> enemyKills =
            new EnumMap<>(EnemyKind.class);

    // ⭐ 아이템 종류별 획득 수
    private EnumMap<ItemType, Integer> itemUsed =
            new EnumMap<>(ItemType.class);

    public RunStats() {
        reset();
    }

    // ----------------------------------------------------------
    // RESET
    // ----------------------------------------------------------
    public void reset() {
        // 적 처치수 초기화
        for (EnemyKind kind : EnemyKind.values()) {
            enemyKills.put(kind, 0);
        }

        // 아이템 획득수 초기화
        for (ItemType type : ItemType.values()) {
            itemUsed.put(type, 0);
        }
    }

    // ----------------------------------------------------------
    // 적 처치 기록
    // ----------------------------------------------------------
    public void onEnemyKilled(EnemyKind kind) {
        if (kind == null) return;
        enemyKills.put(kind, enemyKills.get(kind) + 1);
    }

    // ----------------------------------------------------------
    // 아이템 획득 기록
    // ----------------------------------------------------------
    public void onItemAcquired(ItemType type) {
        if (type == null) return;
        itemUsed.put(type, itemUsed.get(type) + 1);
    }

    // ----------------------------------------------------------
    // Getter들
    // ----------------------------------------------------------

    public int getEnemyKills(EnemyKind kind) {
        return enemyKills.getOrDefault(kind, 0);
    }

    // 아이템 타입별 횟수 반환
    public int getSpeedItemUsed() {
        return itemUsed.getOrDefault(ItemType.SPEED, 0);
    }

    public int getHealItemUsed() {
        return itemUsed.getOrDefault(ItemType.HEAL, 0);
    }

    public int getShieldItemUsed() {
        return itemUsed.getOrDefault(ItemType.SHIELD, 0);
    }

    public int getPowerItemUsed() {
        return itemUsed.getOrDefault(ItemType.POWER_UP, 0);
    }
}
