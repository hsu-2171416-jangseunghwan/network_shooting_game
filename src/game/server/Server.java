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

    private volatile int currentStage = 0;
    private final Random rand = new Random();

    private ConcurrentHashMap<Integer, BulletState> bullets = new ConcurrentHashMap<>();
    private int nextBulletId = 1;

    private PlayerState[] players = new PlayerState[3];

    private long lastFireTimeP1 = 0;
    private long lastFireTimeP2 = 0;
    private final long FIRE_DELAY = 150;

    private volatile boolean serverRunning = true;

    // ★ 스테이지 스폰 스레드 플래그
    private volatile boolean stage1Running = false;
    private volatile boolean stage2Running = false;

    private volatile boolean stageTransition = false;

    public Server(int port) throws Exception {
        serverSocket = new ServerSocket(port);
        System.out.println("서버 실행 중...");

        clients[0] = serverSocket.accept();
        out[0] = new PrintWriter(clients[0].getOutputStream(), true);
        out[0].println("/setid/1");
        System.out.println("P1 접속");

        clients[1] = serverSocket.accept();
        out[1] = new PrintWriter(clients[1].getOutputStream(), true);
        out[1].println("/setid/2");
        System.out.println("P2 접속");

        players[1] = new PlayerState();
        players[2] = new PlayerState();
        players[1].x = 250; 
        players[1].y = 680;
        players[2].x = 350;
        players[2].y = 680;

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

    // =====================================================================
    // CLIENT HANDLER
    // =====================================================================
    private class ClientHandler extends Thread {
        Socket socket;
        int id;

        public ClientHandler(Socket s, int id) {
            this.socket = s;
            this.id = id;
        }

        @Override
        public void run() {
            try {
                BufferedReader in =
                    new BufferedReader(new InputStreamReader(socket.getInputStream()));

                while (true) {
                    String msg = in.readLine();

                    if (msg == null) {
                        try { Thread.sleep(10); } catch (Exception ignore) {}
                        continue;
                    }

                    System.out.println("[" + id + "] " + msg);

                    // READY
                    if (msg.startsWith("/ready")) {
                        if (id == 1) ready1 = true;
                        if (id == 2) ready2 = true;

                        if (ready1 && ready2) {
                            currentStage = 1;
                            broadcast("/stage/start/1");
                            startStage1Spawning();
                        }
                        continue;
                    }

                    // CLEAR
                    if (msg.startsWith("/clear/")) {

                        int clearedStage = Integer.parseInt(msg.split("/")[2]);

                        if (id == 1) clear1 = true;
                        if (id == 2) clear2 = true;

                        if (clear1 && clear2) {

                            new Thread(() -> {
                            	
                                stageTransition = true;
                                try { Thread.sleep(200); } catch(Exception ignore){}
                                

                                stopAllSpawning();

                                clearAllEnemies();
                                clearAllBullets();

                                try { Thread.sleep(300); } catch(Exception ignore){}

                                if (clearedStage == 1) {
                                    currentStage = 2;
                                    broadcast("/stage/start/2");
                                    startStage2Spawning();
                                }
                                else if (clearedStage == 2) {
                                    currentStage = 3;
                                    broadcast("/stage/start/3");
                                }
                                else if (clearedStage == 3) {
                                    broadcast("/gameclear");
                                }

                                clear1 = clear2 = false;

                            }).start();
                        }
                        continue;
                    }

                    // INPUT
                    if (msg.startsWith("/input/")) {
                        try {
                            String[] t = msg.split("/");
                            int pid = Integer.parseInt(t[2]);
                            int mx = Integer.parseInt(t[3]);
                            int my = Integer.parseInt(t[4]);
                            int fire = Integer.parseInt(t[5]);

                            PlayerState p = players[pid];
                            p.x += mx * 5;
                            p.y += my * 5;

                            if (fire == 1) spawnBulletFromPlayer(pid);

                        } catch (Exception e) { e.printStackTrace(); }

                        continue;
                    }

                }

            } catch (Exception e) {
                System.out.println("[" + id + "] 에러: " + e.getMessage());
            }
        }
    }


    // =====================================================================
    // STAGE 1
    // =====================================================================
    private void startStage1Spawning() {
        stopAllSpawning(); // ★ 기존 스레드 모두 종료
        stage1Running = true;

        new Thread(() -> {
            try {
                while (stage1Running && currentStage == 1 && serverRunning) {
                    spawnEnemyStage1();
                    Thread.sleep(1200);
                }
            } catch(Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void spawnEnemyStage1() {
        int id = nextEnemyId++;
        double x = 100 + rand.nextInt(300);
        double y = -120;

        EnemyState e = new EnemyState(id, x, y, 100);
        enemies.put(id, e);

        broadcast("/enemy/spawn/" + id + "/stage1/" + x + "/" + y);
    }


    // =====================================================================
    // STAGE 2
    // =====================================================================
    private void startStage2Spawning() {

        stopAllSpawning(); 
        stage2Running = true;

        new Thread(() -> {

            long lastSpawn = System.currentTimeMillis();

            try {
                while (stage2Running && currentStage == 2 && serverRunning) {

                    long now = System.currentTimeMillis();

                    if (now - lastSpawn > 1200) {
                        spawnStage2Enemy();
                        lastSpawn = now;
                    }

                    if (rand.nextInt(100) < 20) {
                        spawnStage1EnemyForStage2();
                    }

                    Thread.sleep(50);
                }
            } catch(Exception e){ e.printStackTrace(); }

        }).start();
    }

    private void spawnStage1EnemyForStage2() {
        int id = nextEnemyId++;
        double x = 50 + rand.nextInt(400);
        double y = -120;

        EnemyState e = new EnemyState(id, x, y, 100);
        enemies.put(id, e);

        broadcast("/enemy/spawn/" + id + "/stage1/" + x + "/" + y);
    }

    private void spawnStage2Enemy() {
        int id = nextEnemyId++;
        double x = 50 + rand.nextInt(380);
        double y = -150;

        EnemyState e = new EnemyState(id, x, y, 100);
        e.speedY = 120;
        enemies.put(id, e);

        broadcast("/enemy/spawn/" + id + "/stage2/" + x + "/" + y);
    }



    // =====================================================================
    // 스폰 스레드 STOP
    // =====================================================================
    private void stopAllSpawning() {
        stage1Running = false;
        stage2Running = false;
    }


    // =====================================================================
    // UPDATE LOOP
    // =====================================================================
    private void startServerLoop() {
        new Thread(() -> {
            long last = System.currentTimeMillis();
            long tick = 1000 / 60;

            while (serverRunning) {
                long now = System.currentTimeMillis();
                long dt = now - last;

                if (dt >= tick) {
                    last = now;
                    updateGame(dt);
                }

                try { Thread.sleep(1); } catch(Exception ignore){}
            }
        }).start();
    }

    private void updateGame(long dt) {
    	
    	if (stageTransition)
            return; 
    	
        if (currentStage == 1 || currentStage == 2)
            safeUpdateEnemies(dt);

        updateBullets(dt / 1000.0);

        checkBulletEnemyCollision();
        sendPlayerStates();
        sendBulletStates();
    }

    // =====================================================================
    // SAFE ENEMY UPDATE (예외 발생 방지)
    // =====================================================================
    private void safeUpdateEnemies(long dt) {
        try {
            updateEnemies(dt);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }


    private void updateEnemies(long dt) {
        double sec = dt / 1000.0;
        long now = System.currentTimeMillis();
        double zigzagAmp = 40;

        for (EnemyState e : enemies.values()) {

            if (currentStage == 2) {
                double dx = Math.sin((now + e.id * 500) / 300.0) * zigzagAmp * sec;
                e.x += dx;

                if (rand.nextInt(100) < 5)
                    stage2EnemyAttack(e);
            }

            e.y += e.speedY * sec;

            if (e.y > 900) {
                enemies.remove(e.id);
                continue;
            }

            broadcast("/enemy/pos/" + e.id + "/" + e.x + "/" + e.y);
        }
    }



    // =====================================================================
    // BULLETS
    // =====================================================================
    private void spawnEnemyBullet(EnemyState e, double dir, double speed) {

        int id = nextBulletId++;

        double vx = dir * speed;
        double vy = speed;

        BulletState b = new BulletState(id, -1, e.x, e.y + 20, vx, vy);
        bullets.put(id, b);

        broadcast("/bullet/spawn/" + id + "/enemy/" + e.x + "/" + e.y);
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


    // =====================================================================
    // PLAYER BULLETS
    // =====================================================================
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


    // =====================================================================
    // COLLISION
    // =====================================================================
    private void checkBulletEnemyCollision() {
        try {

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

        } catch(Exception e) {
            e.printStackTrace();
        }
    }


    // =====================================================================
    // CLEAR ALL
    // =====================================================================
    private void clearAllEnemies() {
        for (int id : enemies.keySet())
            broadcast("/enemy/dead/" + id);

        enemies.clear();
    }

    private void clearAllBullets() {
        for (int id : bullets.keySet())
            broadcast("/bullet/remove/" + id);

        bullets.clear();
    }


    // =====================================================================
    // PLAYER SYNC
    // =====================================================================
    private void sendPlayerStates() {
        for (int pid = 1; pid <= 2; pid++) {
            PlayerState p = players[pid];
            broadcast("/player/pos/" + pid + "/" + p.x + "/" + p.y);
        }
    }

    private void stage2EnemyAttack(EnemyState e) {

        int type = rand.nextInt(3);

        if (type == 0) {
            // Linear
            spawnEnemyBullet(e, 0, 200);
        }
        else if (type == 1) {
            // Triple shot
            spawnEnemyBullet(e, -0.3, 200);
            spawnEnemyBullet(e, 0,    200);
            spawnEnemyBullet(e, 0.3,  200);
        }
        else {
            // ArcSpread-like
            for (int i = -2; i <= 2; i++) {
                spawnEnemyBullet(e, i * 0.25, 200);
            }
        }
    }

    // =====================================================================
    public static void main(String[] args) throws Exception {
        new Server(30000);
    }
}
