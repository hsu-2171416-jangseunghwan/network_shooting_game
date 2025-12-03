package game.interfaces;

public interface PickupReceiver {
	void heal(int amount);
	void addShield(int stacks);
	void addPower(int levels);
	void addSpeed(long durationMs, double bonusPxperSec);
	void addFever(int amount);
}
