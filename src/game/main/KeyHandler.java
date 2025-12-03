package game.main;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class KeyHandler extends KeyAdapter {

    private Game game;

    public KeyHandler(Game game) {
        this.game = game;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        game.onKeyPressed(e);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        game.onKeyReleased(e);
    }
}
