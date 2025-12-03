package game.interfaces;

import java.awt.event.KeyEvent;

public interface InputHandler {
	void onKeyPressed(KeyEvent e);  // 키 Down
    void onKeyReleased(KeyEvent e); // 키 Up
}
