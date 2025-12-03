package game.interfaces;

public interface Shootable {
	void shoot();                // 발사 트리거
    long getFireCooldown();      // 발사 쿨타임(ms)
}
