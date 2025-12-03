package game.network;

import java.io.*;
import java.net.*;
import game.main.Game;

public class NetworkClient {

    private Socket socket;
    private PrintWriter out;
    private Game game;

    public NetworkClient(Game game, String ip, int port) {
        this.game = game;

        try {
            socket = new Socket(ip, port);
            System.out.println("서버 연결 성공!");

            out = new PrintWriter(socket.getOutputStream(), true);

            // 서버 메시지 수신 스레드
            new Thread(() -> {
                try {
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(socket.getInputStream()));

                    String msg;
                    while ((msg = in.readLine()) != null) {
                        game.onNetworkPacket(msg);
                    }

                } catch (Exception e) {
                    System.out.println("서버와 연결 끊김");
                }
            }).start();

        } catch (Exception e) {
            System.out.println("서버 접속 실패: " + e.getMessage());
        }
    }

    // 키 입력 전송
    public void sendInput(int pid, int x, int y, int fire) {
        out.println("/input/" + pid + "/" + x + "/" + y + "/" + fire);
    }

    // READY 신호 전송
    public void sendReady(int pid) {
        out.println("/ready/" + pid);
    }
    
    public void send(String msg) {
        out.println(msg);
    }
}
