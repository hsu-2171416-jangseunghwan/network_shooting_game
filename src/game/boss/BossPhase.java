package game.boss;

import game.entity.BossSingle;

public interface BossPhase {
    void enter(BossSingle boss);
    void update(BossSingle boss, long dt);
    boolean isComplete(BossSingle boss);
}