package game.interfaces;

import game.entity.*;

public interface BossAttackPattern {
    // 패턴 시작할 때 한 번
    void enter(BossSingle boss);

    // 매 프레임 호출
    void update(BossSingle boss, long dt);
}