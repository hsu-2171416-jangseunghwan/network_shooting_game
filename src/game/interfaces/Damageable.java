package game.interfaces;

public interface Damageable {
	void takeDamage(int dmg);    // 피해 적용
    int getHp();                 // 현재 HP
    boolean isAlive();           // 생존 여부
}
