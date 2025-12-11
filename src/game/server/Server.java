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

        updateBullets(sec);
        checkBulletEnemyCollision();
        sendPlayerStates();
        sendBulletStates();

        checkStageTimeout();
    }

    // Stage 타이머 체크
    private void checkStageTimeout() {
        long now = System.currentTimeMillis();

        if (currentStage == 1 && now - stageStartTime > 15000) {
            endStage1();
        }

        if (currentStage == 2 && now - stageStartTime > 30000) {
            endStage2();
        }
    }

    private void endStage1() {

        clearAllEnemies();
        clearAllBullets();

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

    private void checkBulletEnemyCollision() {

        for (BulletState b : bullets.values()) {

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
