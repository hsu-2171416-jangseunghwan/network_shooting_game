package game.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

class EnemyState {
    int id;
    double x, y;
    double speedY = 120;  // 기본 하강 속도
    int hp;

    EnemyState(int id, double x, double y, int hp) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.hp = hp;
    }
}

class BulletState {
    int id;
    int ownerId; // 1 or 2 (누가 쏜 총알인지)
    double x, y;
    double vx, vy;

    BulletState(int id, int ownerId, double x, double y, double vx, double vy) {
        this.id = id;
        this.ownerId = ownerId;
        this.x = x; this.y = y;
        this.vx = vx; this.vy = vy;
    }
}

class PlayerState {
    double x, y;
    boolean firing = false;
}


public class Server {
    private ServerSocket serverSocket;
    private Socket[] clients = new Socket[2];
    private PrintWriter[] out = new PrintWriter[2];

    boolean ready1 = false;
    boolean ready2 = false;

    boolean clear1 = false;
    boolean clear2 = false;
    
    private ConcurrentHashMap<Integer, EnemyState> enemies = new ConcurrentHashMap<>();
    private int nextEnemyId = 1;
    
    private volatile int currentStage = 0;
    
    private final Random rand = new Random();
    
    private ConcurrentHashMap<Integer, BulletState> bullets = new ConcurrentHashMap<>();
    private int nextBulletId = 1;
    

    private PlayerState[] players = new PlayerState[3];
    
    private long lastFireTimeP1 = 0;
    private long lastFireTimeP2 = 0;

    private final long FIRE_DELAY = 150; 
    
    public Server(int port) throws Exception {
        serverSocket = new ServerSocket(port);
        System.out.println("서버 실행 중...");

        // 1번 클라이언트 접속
        clients[0] = serverSocket.accept();
        out[0] = new PrintWriter(clients[0].getOutputStream(), true);
        out[0].println("/setid/1");
        System.out.println("P1 접속");

        // 2번 클라이언트 접속
        clients[1] = serverSocket.accept();
        out[1] = new PrintWriter(clients[1].getOutputStream(), true);
        out[1].println("/setid/2");
        System.out.println("P2 접속");

        players[1] = new PlayerState();
        players[2] = new PlayerState();

        // 시작 위치 (게임 화면 기준, 원하는 값으로 수정 가능)
        players[1].x = 250; players[1].y = 680;
        players[2].x = 350; players[2].y = 680;
        
        // 각 플레이어 입력 스레드
        new ClientHandler(clients[0], 1).start();
        new ClientHandler(clients[1], 2).start();
        
        startServerLoop();
        
    }

    // 전체 메시지 전송
    public synchronized void broadcast(String msg) {
        out[0].println(msg);
        out[1].println(msg);
    }

    // 클라이언트 처리 스레드
    private class ClientHandler extends Thread {
        Socket socket;
        int id;

        public ClientHandler(Socket s, int id) {
            this.socket = s;
            this.id = id;
        }

        public void run() {
            try {
                BufferedReader in =
                    new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String msg;
                while ((msg = in.readLine()) != null) {
                    System.out.println("[" + id + "] " + msg);

                    // 🔥 READY 패킷 처리
                    if (msg.startsWith("/ready")) {
                        if (id == 1) ready1 = true;
                        if (id == 2) ready2 = true;

                        System.out.println("P" + id + " READY");

                        // 두 명 모두 준비됨 → Stage1 시작 신호
                        if (ready1 && ready2) {
                            System.out.println(">>> 게임 시작! Stage1");
                            broadcast("/stage/start/1");
                            currentStage = 1;
                         // 🔥 플레이어들이 생성된 이후에 적 스폰
                            startStage1Spawning();
                        }

                        continue; // READY는 브로드캐스트하지 않음
                    }
                    
                    if (msg.startsWith("/clear/")) {

                        int clearedStage = Integer.parseInt(msg.split("/")[2]);

                        if (id == 1) clear1 = true;
                        if (id == 2) clear2 = true;

                        System.out.println("P" + id + " CLEAR (Stage " + clearedStage + ")");

                        // 두 명 모두 클리어했을 때만 다음 스테이지 진행
                        if (clear1 && clear2) {
                        	clearAllEnemies();
                        	clearAllBullets();
                        	
                            if (clearedStage == 1) {
                                broadcast("/stage/start/2");
                                System.out.println(">>> Stage2 시작");
                            }
                            else if (clearedStage == 2) {
                                broadcast("/stage/start/3");
                                System.out.println(">>> Stage3 시작");
                            }
                            else if (clearedStage == 3) {
                                broadcast("/gameclear");
                                System.out.println(">>> GAME CLEAR");
                            }

                            // 다음 라운드를 위해 초기화
                            clear1 = clear2 = false;
                        }

                        continue;
                    }
                    
                    if (msg.startsWith("/input/")) {

                        try {

                            String[] t = msg.split("/");

                            int pid = Integer.parseInt(t[2]);
                            int mx = Integer.parseInt(t[3]);
                            int my = Integer.parseInt(t[4]);
                            int fire = Integer.parseInt(t[5]);

                            PlayerState p = players[pid];

                            // 서버에서 플레이어 이동 처리
                            p.x += mx * 5;
                            p.y += my * 5;

                            // 총알 발사
                            if (fire == 1) {
                                spawnBulletFromPlayer(pid);
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        continue;
                    }

                }

                    // 🔥 그 외 메시지는 그대로 브로드캐스트
                    broadcast(msg);


            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void spawnEnemy(double x, double y) {
        int id = nextEnemyId++;

        EnemyState e = new EnemyState(id, x, y, 100); // HP 100
        enemies.put(id, e);

        // 클라이언트에게 적 생성 브로드캐스트
        broadcast("/enemy/spawn/" + id + "/" + x + "/" + y);
        System.out.println("Enemy Spawned: " + id + " at " + x + ", " + y);
    }
    
    private void startStage1Spawning() {
        new Thread(() -> {
            try {
                while (currentStage == 1) {
                    
                    spawnEnemyStage1();   // ★ 1마리 생성
                    Thread.sleep(1200);   // 1.2초 간격
                    
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    private void spawnEnemyStage1() {
        int id = nextEnemyId++;

        double x = 100 + rand.nextInt(300);
        double y = -120;

        EnemyState e = new EnemyState(id, x, y, 100);
        enemies.put(id, e);  // ★ 등록

        broadcast("/enemy/spawn/" + id + "/stage1/" + x + "/" + y);
        System.out.println("[SERVER] Stage1 enemy spawned: id=" + id);
    }


    
    private void updateBullets(double dt) {
        for (BulletState b : bullets.values()) {
            b.x += b.vx * dt;
            b.y += b.vy * dt;

            if (b.y < -50) {
                bullets.remove(b.id);
                broadcast("/bullet/remove/" + b.id);
            }
        }
    }
    
    private void sendBulletStates() {
        for (BulletState b : bullets.values()) {
            broadcast("/bullet/pos/" + b.id + "/" + b.x + "/" + b.y);
        }
    }
    
 // -------------------------
 // ★ 서버 틱 루프 (60 TPS) 
 // -------------------------
    private void startServerLoop() {

     new Thread(() -> {
         long last = System.currentTimeMillis();
         long tickDelay = 1000 / 60;   // 60FPS

         while (true) {
             long now = System.currentTimeMillis();
             long dt = now - last;

             if (dt >= tickDelay) {
                 last = now;

                 updateGame(dt);  // ★ 서버 게임 로직 업데이트 호출
             }

             try {
                 Thread.sleep(1); // CPU 절약
             } catch (Exception ignore) {}
         }
     }).start();
    }

    private void updateGame(long dt) {
	    // dt = 지난 프레임 경과 시간 (밀리초)
	    // 미래에 Enemy.update(dt), Bullet.update(dt) 등이 들어갈 자리
    	double sec = dt / 1000.0;
    	
    	if (currentStage == 1) {
	        updateEnemies(dt);
	    }
    	
    	updateBullets(sec);
    	checkBulletEnemyCollision();
    	sendPlayerStates();
    	sendBulletStates();
	    if (currentStage > 0) {
	        // 테스트용 로그
	        //System.out.println("SERVER UPDATE dt=" + dt);
	    }
	}
    
    private void updateEnemies(long dt) {
        double sec = dt / 1000.0;

        for (EnemyState e : enemies.values()) {
            // y 증가 → 아래로 이동
            e.y += e.speedY * sec;

            // 화면 아래로 나가면 삭제
            if (e.y > 900) {
                enemies.remove(e.id);
                continue;
            }

            // 클라이언트에게 위치 전송
            broadcast("/enemy/pos/" + e.id + "/" + e.x + "/" + e.y);
        }
    }
    
    private void checkBulletEnemyCollision() {

        for (BulletState b : bullets.values()) {
            for (EnemyState e : enemies.values()) {

                // 단순한 사각형 충돌 (정확도 올리고 싶으면 개선 가능)
                if (Math.abs(b.x - e.x) < 40 && Math.abs(b.y - e.y) < 40) {

                    // 데미지
                    e.hp -= 20;

                    // 총알 제거
                    bullets.remove(b.id);
                    broadcast("/bullet/remove/" + b.id);

                    // HP 업데이트 전송
                    broadcast("/enemy/hp/" + e.id + "/" + e.hp);

                    // 죽었으면 제거
                    if (e.hp <= 0) {
                        enemies.remove(e.id);
                        broadcast("/enemy/dead/" + e.id);
                    }
                }
            }
        }
    }
    
    private void sendPlayerStates() {
        for (int pid = 1; pid <= 2; pid++) {
            PlayerState p = players[pid];
            broadcast("/player/pos/" + pid + "/" + p.x + "/" + p.y);
        }
    }
    
    private void spawnBulletFromPlayer(int pid) {

        long now = System.currentTimeMillis();

        if (pid == 1 && now - lastFireTimeP1 < FIRE_DELAY) return;
        if (pid == 2 && now - lastFireTimeP2 < FIRE_DELAY) return;

        if (pid == 1) lastFireTimeP1 = now;
        if (pid == 2) lastFireTimeP2 = now;

        // 기존 총알 생성 코드 유지
        PlayerState p = players[pid];

        double px = p.x;
        double py = p.y - 20;

        double vx = 0;
        double vy = -400;

        int id = nextBulletId++;

        BulletState b = new BulletState(id, pid, px, py, vx, vy);
        bullets.put(id, b);

        broadcast("/bullet/spawn/" + id + "/" + pid + "/" + px + "/" + py);
    }
    
    private void clearAllEnemies() {
        for (int id : enemies.keySet()) {
            broadcast("/enemy/dead/" + id);
        }
        enemies.clear();
        System.out.println("[SERVER] All enemies cleared.");
    }

    private void clearAllBullets() {
        for (int id : bullets.keySet()) {
            broadcast("/bullet/remove/" + id);
        }
        bullets.clear();
        System.out.println("[SERVER] All bullets cleared.");
    }
    public static void main(String[] args) throws Exception {
        new Server(30000);
    }
}
