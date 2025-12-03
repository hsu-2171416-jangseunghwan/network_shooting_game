package game.interfaces;

import game.entity.BossSingle;

public interface BossPhase {
	void enter(BossSingle boss);            // 페이즈 시작 초기화
    void update(BossSingle boss, long dt);  // 페이즈 로직(탄막/타이밍)
    boolean isComplete(BossSingle boss);    // 종료 조건(HP/시간 등)
}
