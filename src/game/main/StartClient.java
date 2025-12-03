package game.main;

import javax.swing.JFrame;

public class StartClient {

    public static void main(String[] args) {

        // 1) 게임 객체 생성
        Game game = new Game();

        // 2) 게임 패널 생성 (Game을 넣어줌)
        GamePanel panel = new GamePanel(game);

        // 3) Swing 윈도우 구성
        JFrame frame = new JFrame("Stellar Impact - Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // 4) 패널 실행
        panel.start();
    }
}
