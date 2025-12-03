package game.main;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JPanel;
import javax.swing.Timer;

import game.network.NetworkClient;

public class GamePanel extends JPanel implements ActionListener, KeyListener {

	 private final Game game;
	    private final Timer timer;

	    public GamePanel(Game game) {
	        this.game = game;

	        NetworkClient network = new NetworkClient(game, "127.0.0.1", 30000);
	        game.setNetwork(network);

	        setPreferredSize(new Dimension(800, 800));
	        setFocusable(true);
	        addKeyListener(this);
	        setRequestFocusEnabled(true);
	        timer = new Timer(16, this);
	    }


	    public void start() {
	        timer.start();

	        requestFocusInWindow();
	        requestFocus();

	        new java.util.Timer().schedule(new java.util.TimerTask() {
	            @Override
	            public void run() {
	                requestFocusInWindow();
	            }
	        }, 100);
	    }

    @Override
    public void actionPerformed(ActionEvent e) {
        game.update(16);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        game.render((Graphics2D) g, getWidth(), getHeight());
    }

    @Override public void keyPressed(KeyEvent e)  { game.onKeyPressed(e); }
    @Override public void keyReleased(KeyEvent e) { game.onKeyReleased(e); }
    @Override public void keyTyped(KeyEvent e) {}
}
