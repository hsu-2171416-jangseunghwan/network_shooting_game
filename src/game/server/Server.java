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
    double speedY = 120;
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
    int ownerId;
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
    int hp = 100;
    boolean firing = false;
}

class BossState {
    int id;
    double x, y;
    int hp;
    int maxHp;
    boolean alive = true;
    
    int hitW = 160;
    int hitH = 160;
    
    BossState(int id, double x, double y, int hp) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.hp = hp;
        this.maxHp = hp;
    }
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

    private ConcurrentHashMap<Integer, BulletState> bullets = new ConcurrentHashMap<>();
    private int nextBulletId = 1;

    private volatile int currentStage = 0;

    private final Random rand = new Random();

    private PlayerState[] players = new PlayerState[3];

    private long lastFireTimeP1 = 0;
    private long lastFireTimeP2 = 0;
    private final long FIRE_DELAY = 150;

    private volatile boolean serverRunning = true;

    // 타이머
    private long stageStartTime = 0;
    
    private static final long STAGE1_TIME = 10000;
    private static final long STAGE2_TIME = 10000; // 테스트용
    
    private static final double P1_START_X = 250;
    private static final double P2_START_X = 450;
    private static final double PLAYER_START_Y = 680;
    
    private BossState boss = null;
    private int bossId = 999; // 적과 겹치지 않게 고정 ID 추천


    public Server(int port) throws Exception {

        serverSocket = new ServerSocket(port);

        clients[0] = serverSocket.accept();
        out[0] = new PrintWriter(clients[0].getOutputStream(), true);
        out[0].println("/setid/1");

        clients[1] = serverSocket.accept();
        out[1] = new PrintWriter(clients[1].getOutputStream(), true);
        out[1].println("/setid/2");

        players[1] = new PlayerState();
        players[2] = new PlayerState();

        players[1].x = 250; players[1].y = 680;
        players[2].x = 350; players[2].y = 680;

        new ClientHandler(clients[0], 1).start();
        new ClientHandler(clients[1], 2).start();

        startServerLoop();
    }

    public synchronized void broadcast(String msg) {
        if (!serverRunning) return;
        try {
            out[0].println(msg);
            out[1].println(msg);
        } catch (Exception ignore) {}
    }

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

                    if (msg.startsWith("/ready")) {
                        if (id == 1) ready1 = true;
                        if (id == 2) ready2 = true;

                        if (ready1 && ready2) {
                        	
                        	 players[1].x = P1_START_X;
                        	 players[1].y = PLAYER_START_Y;
                        	 players[2].x = P2_START_X;
                        	 players[2].y = PLAYER_START_Y;
                        	    
                            broadcast("/stage/start/1");
                            currentStage = 1;
                            stageStartTime = System.currentTimeMillis();
                            startStage1Spawning();
                        }
                        continue;
                    }

                    if (msg.startsWith("/input/")) {

                        String[] t = msg.split("/");
                        int pid = Integer.parseInt(t[2]);
                        int mx = Integer.parseInt(t[3]);
                        int my = Integer.parseInt(t[4]);
                        int fire = Integer.parseInt(t[5]);

                        PlayerState p = players[pid];
                        p.x += mx * 5;
                        p.y += my * 5;

                        if (fire == 1) spawnBulletFromPlayer(pid);

                        continue;
                    }
                    
                 // ===============================
                 // 🔥 Boss QTE 결과 처리 (Stage3)
                 // ===============================
                 if (msg.startsWith("/boss/qte/") && currentStage == 3 && boss != null) {

                     String[] t = msg.split("/");
                     String result = t[3]; // success or fail

                     System.out.println("[Server] QTE result = " + result);

                     if ("success".equals(result)) {

                         // ✅ 성공: 보스 HP 대량 감소 (20%)
                         int damage = (int)(boss.maxHp * 0.2);
                         boss.hp -= damage;

                         if (boss.hp < 0) boss.hp = 0;

                         System.out.println("[Server] QTE SUCCESS → boss hp -" + damage);

                     } else if ("fail".equals(result)) {

                         // ❌ 실패: 분노 모드 트리거 (지금은 로그만)
                         System.out.println("[Server] QTE FAIL → boss rage");

                         // 👉 나중에 여기에:
                         // boss.rageMode = true;
                         // boss.rageStartTime = System.currentTimeMillis();
                     }

                     // 🔥 상태 즉시 동기화
                     broadcastBossState();

                     // 🔥 사망 체크
                     if (boss.hp <= 0 && boss.alive) {
                         boss.hp = 0;
                         boss.alive = false;
                         endStage3();
                     }

                     continue;
                 }

                 

                    broadcast(msg);
                }

                serverRunning = false;
                shutdownServer();

            } catch (Exception e) {
                serverRunning = false;
                shutdownServer();
            }
        }
    }

    // ======================================================
    //  Stage1 / Stage2 별 스폰 로직
    // ======================================================

    private void spawnEnemyStage1() {
        int id = nextEnemyId++;
        double x = 100 + rand.nextInt(300);
        double y = -120;

        EnemyState e = new EnemyState(id, x, y, 100);
        e.speedY = 120;

        enemies.put(id, e);

        broadcast("/enemy/spawn/" + id + "/stage1/" + x + "/" + y);
    }

    private void startStage1Spawning() {

        new Thread(() -> {
            try {
                while (serverRunning && currentStage == 1) {
                    spawnEnemyStage1();
                    Thread.sleep(1200);
                }
            } catch (Exception ignore) {}
        }).start();
    }

    // =====================================
    // Stage2 Server Enemy Logic
    // =====================================
    private void spawnEnemyStage2() {

        int id = nextEnemyId++;

        double x = 100 + rand.nextInt(300);
        double y = -130;

        EnemyState e = new EnemyState(id, x, y, 150);
        e.speedY = 120;

        enemies.put(id, e);

        broadcast("/enemy/spawn/" + id + "/stage2/" + x + "/" + y);
    }

    private void startStage2Spawning() {

        new Thread(() -> {
            try {
                while (serverRunning && currentStage == 2) {
                    spawnEnemyStage2();
                    Thread.sleep(1000);
                }
            } catch (Exception ignore) {}
        }).start();
    }
    
    
    // ======================================================
    //  서버 메인 루프
    // ======================================================
    private void startServerLoop() {

        new Thread(() -> {

            long last = System.currentTimeMillis();
            long tickDelay = 1000 / 60;

            while (serverRunning) {

                long now = System.currentTimeMillis();
                long dt = now - last;

                if (dt >= tickDelay) {
                    last = now;
                    updateGame(dt);
                }

                try { Thread.sleep(1); } catch (Exception ignore) {}
            }
        }).start();
    }

    private void updateGame(long dt) {

        double sec = dt / 1000.0;

        if (currentStage == 1) updateEnemies(dt);
        if (currentStage == 2) updateEnemies(dt);
        if (currentStage == 3) {
            updateBoss(dt);
            broadcastBossState();
            checkBossPlayerCollision();// ⭐ 추가
        }
        updateBullets(sec);
        checkBulletEnemyCollision();
        checkEnemyPlayerCollision();
        sendPlayerStates();
        sendBulletStates();

        checkStageTimeout();
    }

    // Stage 타이머 체크
    private void checkStageTimeout() {
        long now = System.currentTimeMillis();

        if (currentStage == 1 && now - stageStartTime > STAGE1_TIME) {
            endStage1();
        }

        if (currentStage == 2 && now - stageStartTime > STAGE2_TIME) {
            endStage2();
        }
    }

    private void endStage1() {

        clearAllEnemies();
        clearAllBullets();
        
        
        players[1].x = P1_START_X;
        players[1].y = PLAYER_START_Y;
        players[2].x = P2_START_X;
        players[2].y = PLAYER_START_Y;
        

        broadcast("/stage/clear/1");
        broadcast("/stage/start/2");

        currentStage = 2;
        stageStartTime = System.currentTimeMillis();

        startStage2Spawning();
    }

    private void endStage2() {

        clearAllEnemies();
        clearAllBullets();

        broadcast("/stage/clear/2");
        broadcast("/stage/start/3");

        currentStage = 3;
        
        spawnBoss();
    }

    private void endStage3() {

        System.out.println("[Server] Boss defeated → Stage3 Clear");

        // 마지막 상태 1회 송신
        broadcastBossState();

        clearAllBullets();
        broadcast("/gameclear");

        boss = null;
        currentStage = 0;
    }


    
    private void spawnBoss() {

        double playAreaX = 150;        // 클라이언트 기준
        double playAreaWidth = 500;

        double bossCenterX = playAreaX + playAreaWidth / 2.0;
        double bossCenterY = 80 + 80;  // 시각적으로 싱글과 맞추기

        boss = new BossState(
            bossId,
            bossCenterX,
            bossCenterY,
            1000
        );

        broadcastBossState();
    }

    
    private void broadcastBossState() {
        if (boss == null) return;

        String msg =
            "/boss/state/" +
            boss.id + "/" +
            boss.x + "/" +
            boss.y + "/" +
            boss.hp + "/" +
            boss.maxHp + "/" +
            (boss.alive ? 1 : 0);

        broadcast(msg);
    }

    
    private void updateEnemies(long dt) {

        double sec = dt / 1000.0;

        for (EnemyState e : enemies.values()) {

            e.y += e.speedY * sec;

            if (e.y > 900) {
                enemies.remove(e.id);
                continue;
            }

            broadcast("/enemy/pos/" + e.id + "/" + e.x + "/" + e.y);
        }
    }
    
    private void updateBoss(long dt) {
        if (boss == null) return;

        // 아직은 가만히 (Stage1의 적처럼 위치만 브로드캐스트)
        //broadcast("/enemy/pos/" + boss.id + "/" + boss.x + "/" + boss.y);
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

        PlayerState p = players[pid];

        double px = p.x;
        double py = p.y - 20;

        int id = nextBulletId++;

        BulletState b = new BulletState(id, pid, px, py, 0, -400);
        bullets.put(id, b);

        broadcast("/bullet/spawn/" + id + "/" + pid + "/" + px + "/" + py);
    }
    
    private void checkEnemyPlayerCollision() {

        for (EnemyState e : enemies.values()) {

            for (int pid = 1; pid <= 2; pid++) {

                PlayerState p = players[pid];
                if (p == null || p.hp <= 0) continue;

                // 싱글 Stage1과 동일한 거리 판정
                if (Math.abs(e.x - p.x) < 40 &&
                    Math.abs(e.y - p.y) < 40) {

                    // 🔥 플레이어 데미지
                    p.hp -= 1;

                    System.out.println("[Server] Player " + pid +
                                       " hit by enemy! HP=" + p.hp);

                    // 적 제거 (싱글과 동일한 처리)
                    enemies.remove(e.id);
                    broadcast("/enemy/dead/" + e.id);

                    // 플레이어 사망 처리
                    if (p.hp <= 0) {
                        broadcast("/player/dead/" + pid);
                    } else {
                        broadcast("/player/hp/" + pid + "/" + p.hp);
                    }

                    return; // 1프레임 1회 충돌
                }
            }
        }
    }

    
    private void checkBulletEnemyCollision() {

        for (BulletState b : bullets.values()) {

            // ─────────────────────────
            // 1) 일반 적 충돌
            // ─────────────────────────
            for (EnemyState e : enemies.values()) {

                if (Math.abs(b.x - e.x) < 40 && Math.abs(b.y - e.y) < 40) {

                    e.hp -= 20;

                    bullets.remove(b.id);
                    broadcast("/bullet/remove/" + b.id);

                    broadcast("/enemy/hp/" + e.id + "/" + e.hp);

                    if (e.hp <= 0) {
                        enemies.remove(e.id);
                        broadcast("/enemy/dead/" + e.id);
                    }
                    break;
                }
            }

            // ─────────────────────────
            // 2) 🔥 보스 충돌 (Stage3)
            // ─────────────────────────
            if (currentStage == 3 && boss != null) {

            	if (Math.abs(b.x - boss.x) < boss.hitW / 2 &&
            		Math.abs(b.y - boss.y) < boss.hitH / 2) {


                    boss.hp -= 10;   // 보스는 덜 깎이게

                    bullets.remove(b.id);
                    broadcast("/bullet/remove/" + b.id);

                    //broadcast("/enemy/hp/" + boss.id + "/" + boss.hp);

                    // 🔥 보스 사망 = Stage3 클리어
                    if (boss.hp <= 0) {
                        boss.hp = 0;
                        boss.alive = false;   // ⭐ 중요
                        endStage3();
                    }
                    break;
                }
            }
        }
    }

    private void checkBossPlayerCollision() {

        if (currentStage != 3 || boss == null || !boss.alive) return;

        for (int pid = 1; pid <= 2; pid++) {

            PlayerState p = players[pid];
            if (p == null || p.hp <= 0) continue;

            if (Math.abs(boss.x - p.x) < boss.hitW / 2 &&
                Math.abs(boss.y - p.y) < boss.hitH / 2) {

                p.hp -= 1;

                System.out.println("[Server] Player " + pid +
                                   " hit by BOSS! HP=" + p.hp);

                if (p.hp <= 0) {
                    broadcast("/player/dead/" + pid);
                } else {
                    broadcast("/player/hp/" + pid + "/" + p.hp);
                }
            }
        }
    }


    private void clearAllEnemies() {
        for (int id : enemies.keySet()) {
            broadcast("/enemy/dead/" + id);
        }
        enemies.clear();
    }

    private void clearAllBullets() {

        for (int id : bullets.keySet()) {
            broadcast("/bullet/remove/" + id);
        }
        bullets.clear();
    }

    private void shutdownServer() {

        try {
            for (Socket s : clients) {
                if (s != null && !s.isClosed()) s.close();
            }

            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }

        } catch (Exception ignore) {}
    }

    public static void main(String[] args) throws Exception {
        new Server(30000);
    }
}
