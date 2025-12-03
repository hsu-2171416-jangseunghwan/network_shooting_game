package game.mode;

import game.enumset.ItemSharePolicy;

/**
 * 로컬 2인 협동 모드용 정책 설정
 * - friendlyFire : 아군 공격 허용 여부
 * - allowOverlap : 플레이어끼리 겹칠 수 있는지 여부
 * - itemSharePolicy : 아이템 공유 정책
 * - reviveDelayMs : 다운 후 부활 지연 시간
 */
public class LocalCoopConfig {

    public boolean friendlyFire;                 // 아군 피격 허용 여부
    public boolean allowOverlap;                 // 플레이어 간 겹침 허용
    public ItemSharePolicy itemSharePolicy;      // 아이템 공유 정책
    public long reviveDelayMs;                   // 부활 지연(ms)

    /** 기본 생성자 (기본값: 친선사격X, 겹침O, 지속형만 공유, 부활 3초) */
    public LocalCoopConfig() {
        this.friendlyFire = false;
        this.allowOverlap = true;
        this.itemSharePolicy = ItemSharePolicy.PERSISTENT_ONLY;
        this.reviveDelayMs = 3000;
    }

    /** 디폴트 정책 생성기 (정적 팩토리) */
    public static LocalCoopConfig defaultCoop() {
        return new LocalCoopConfig();
    }
}
