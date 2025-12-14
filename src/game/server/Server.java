package game.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;


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
    boolean alive = true;
}

class BossState {

    // ===== 기존 필드 =====
    boolean p1Telegraphing = false;
    long lastNormalShot = 0;

    int id;
    double x, y;
    int hp, maxHp;
    boolean alive = true;

    int hitW = 340;
    int hitH = 180;

    int phase;
    long phaseStart;

    // Phase1
    long lastCoreBeam = 0;
    boolean telegraphing = false;

    // ===== Phase2 (🔥 추가) =====
    long p2LastCenterShot = 0;
    long p2LastBeamFired = 0;
    boolean p2Telegraphing = false;
    boolean p2CanMove = true;

    double p2MoveCenterX = 0;
    double p2MoveRange = 120;
    double p2MoveSpeed = 80;

    long lastSpread = 0;
    int moveDir = 1;

    // ===== Phase3 =====
    long lastWave = 0;
    long lastOrb = 0;

    // 공통
    long lastBossEnemySpawn = 0;
    double fireAnchorOffsetX = 0;

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
    
    private static final long STAGE1_TIME = 20000;
    private static final long STAGE2_TIME = 20000; // 테스트용
    
    private static final double P1_START_X = 250;
    private static final double P2_START_X = 450;
    private static final double PLAYER_START_Y = 680;
    
    // ======================
    // DAMAGE TUNING
    // ======================
    /*
    private static final int ENEMY_CONTACT_DAMAGE = 10;
    private static final int ENEMY_BULLET_DAMAGE  = 5;
    private static final int BOSS_CONTACT_DAMAGE  = 20;
    */
    
   
    private static final int ENEMY_CONTACT_DAMAGE = 1;
    private static final int ENEMY_BULLET_DAMAGE  = 1;
    private static final int BOSS_CONTACT_DAMAGE  = 1;

    
 // ======================
 // PLAY AREA (CLIENT SYNC)
 // ======================
    private static final double PLAY_AREA_X = 150;
    private static final double PLAY_AREA_WIDTH = 500;
    private static final double ENEMY_WIDTH = 70;
    
    private BossState boss = null;
    private int bossId = 999; // 적과 겹치지 않게 고정 ID 추천

    private static final int ENEMY_W = 150; // stage1 기준
    private static final int ENEMY_H = 120;

    private static final int PLAYER_W = 110; // 싱글 값
    private static final int PLAYER_H = 60;
    
    private static final int BULLET_W = 8;
    private static final int BULLET_H = 16;
    
    private int stageDurationSec = 15;   // Stage1 = 15초 (예시)
    private boolean stageRunning = false;
    
 // Server.java 필드 영역
    private long lastStageTimeBroadcast = 0;
    private static final long STAGE_TIME_PACKET_INTERVAL = 1000; // 1초
    
    boolean qteActive = false;
    boolean qteP1Pressed = false;
    boolean qteP2Pressed = false;
    long qteStartTime = 0;
    
    private static final long P1_CORE_INTERVAL   = 5500;
    private static final long P1_TELEGRAPH_TIME  = 600;
    private static final long P1_NORMAL_INTERVAL = 1200;
    private static final long P1_SPREAD_INTERVAL = 10000;

    
    static final long QTE_TIME_LIMIT = 2000; // 2
    
    private int teamScore = 0;
    
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

                            currentStage = 1;
                            stageStartTime = System.currentTimeMillis();
                            lastStageTimeBroadcast = 0;

                            broadcast("/stage/start/1");

                            startStage1Spawning();   // 🔥 Stage1 스폰 시작
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

        double x = PLAY_AREA_X
                 + rand.nextDouble() * (PLAY_AREA_WIDTH - ENEMY_WIDTH);
        double y = -120;

        EnemyState e = new EnemyState(
            id,
            x,
            y,
            100,
            MoveType.LINEAR,
            Math.random() < 0.3 ? FireType.LINEAR : FireType.NONE
        );

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

        double x = PLAY_AREA_X
                 + rand.nextDouble() * (PLAY_AREA_WIDTH - ENEMY_WIDTH);
        double y = -130;

        boolean reuseStage1 = rand.nextDouble() < 0.3; // 30% 확률

        String type;
        MoveType move;
        FireType fire;
        int hp;

        if (reuseStage1) {
            // 🔁 Stage1 적 재사용
            type = "stage1";
            move = MoveType.LINEAR;
            fire = Math.random() < 0.3 ? FireType.LINEAR : FireType.NONE;
            hp = 100;
        } else {
            // 🔥 Stage2 적
            type = "stage2";
            move = MoveType.ZIGZAG;
            fire = switch (rand.nextInt(3)) {
                case 0 -> FireType.LINEAR;
                case 1 -> FireType.TRIPLE;
                default -> FireType.ARC;
            };
            hp = 150;
        }

        EnemyState e = new EnemyState(id, x, y, hp, move, fire);
        enemies.put(id, e);

        broadcast("/enemy/spawn/" + id + "/" + type + "/" + x + "/" + y);
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
    
    private void fireLinear(EnemyState e) {

        int id = nextBulletId++;

        double bx = e.x + ENEMY_W / 2 - BULLET_W / 2;
        double by = e.y + ENEMY_H;

        double vx = 0;
        double vy = 250;

        BulletState b = new BulletState(id, -1, bx, by, vx, vy);
        bullets.put(id, b);

        broadcast("/bullet/spawn/" + id + "/enemy/" + bx + "/" + by);
    }

    private void fireTriple(EnemyState e) {

        double bx = e.x + 35;
        double by = e.y + 60;

        double speed = 250;
        double[] angles = {
            Math.toRadians(90),
            Math.toRadians(75),
            Math.toRadians(105)
        };

        for (double a : angles) {
            int id = nextBulletId++;

            double vx = Math.cos(a) * speed;
            double vy = Math.sin(a) * speed;

            BulletState b = new BulletState(id, -1, bx, by, vx, vy);
            bullets.put(id, b);

            broadcast("/bullet/spawn/" + id + "/enemy/" + bx + "/" + by);
        }
    }

    
    private void fireArc(EnemyState e) {

        double bx = e.x + 35;
        double by = e.y + 60;

        int count = 5;
        double start = Math.toRadians(60);
        double end   = Math.toRadians(120);
        double speed = 180;

        for (int i = 0; i < count; i++) {
            double t = i / (double)(count - 1);
            double angle = start + (end - start) * t;

            int id = nextBulletId++;

            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed;

            BulletState b = new BulletState(id, -1, bx, by, vx, vy);
            bullets.put(id, b);

            broadcast("/bullet/spawn/" + id + "/enemy/" + bx + "/" + by);
        }
    }

    private void fireByType(EnemyState e) {
        switch (e.fireType) {
            case LINEAR -> fireLinear(e);
            case TRIPLE -> fireTriple(e);
            case ARC    -> fireArc(e);
            case NONE   -> {}
        }
    }

    private void updateBoss(long dt) {
        if (boss == null || !boss.alive) return;

        long now = System.currentTimeMillis();
        double sec = dt / 1000.0;

        updateBossPhaseTransition(now);

        switch (boss.phase) {
            case 1 -> updateBossPhase1(now);
            case 2 -> updateBossPhase2(now, sec);
            case 3 -> updateBossPhase3(now, sec);
        }
    }

    private void spawnBoss() {

        boss = new BossState(
            bossId,
            PLAY_AREA_X + PLAY_AREA_WIDTH / 2,
            160,
            1000
        );

        long now = System.currentTimeMillis();

        boss.phase = 1;
        boss.phaseStart = now;

        // 🔥 FSM 타이머 초기화
        boss.lastCoreBeam = now;
        boss.lastSpread = now;
        boss.lastWave = now;
        boss.lastOrb = now;
        boss.lastBossEnemySpawn = now;

        boss.telegraphing = false;
        boss.moveDir = 1;
        boss.fireAnchorOffsetX = 0;
        broadcastBossState();
        
        boss.p2LastCenterShot = now;
        boss.p2LastBeamFired = now;
        boss.p2Telegraphing = false;
        boss.p2CanMove = true;
        boss.p2MoveCenterX = boss.x;
    }


   
    private void updateBossPhaseTransition(long now) {

        double rate = boss.hp / (double) boss.maxHp;

        int newPhase =
            (rate <= 0.33) ? 3 :
            (rate <= 0.66) ? 2 : 1;

        if (newPhase != boss.phase) {
            boss.phase = newPhase;
            boss.phaseStart = now;

            // 공통 초기화
            boss.telegraphing = false;
            boss.lastCoreBeam = now;
            boss.lastSpread   = now;

            // 🔥 Phase3 전용 타이머 반드시 초기화
            if (newPhase == 3) {
                boss.lastWave = now - 15000;          // 즉시 1회 발동 가능
                boss.lastOrb  = now - 7000;
                boss.lastBossEnemySpawn = now - 8000;
            }

            broadcast("/boss/phase/" + newPhase);
        }
    }
    
    private void startStage3Spawning() {

        new Thread(() -> {
            try {
                while (serverRunning && currentStage == 3) {

                    double r = rand.nextDouble();

                    if (r < 0.5) {
                        spawnEnemyStage1();   // 50%
                    } else {
                        spawnEnemyStage2();   // 50%
                    }

                    Thread.sleep(1500); // 스폰 간격
                }
            } catch (Exception ignore) {}
        }).start();
    }
    
    private void updateBossPhase1(long now) {

        /* =========================
         * ① Idle → Telegraph
         * ========================= */
        if (!boss.p1Telegraphing &&
            now - boss.lastCoreBeam >= P1_CORE_INTERVAL - P1_TELEGRAPH_TIME) {

            broadcast("/boss/pattern/phase1/telegraph");
            boss.p1Telegraphing = true;
            return;
        }

        /* =========================
         * ② Telegraph → CoreBeam
         * ========================= */
        if (boss.p1Telegraphing &&
            now - boss.lastCoreBeam >= P1_CORE_INTERVAL) {

            broadcast("/boss/pattern/phase1/corebeam");

            boss.lastCoreBeam = now;
            boss.p1Telegraphing = false;
        }

        /* =========================
         * ③ 일반 탄막 (Linear)
         * ========================= */
        if (now - boss.lastNormalShot >= P1_NORMAL_INTERVAL) {
            fireBossLinear();   // 서버용 직선탄
            boss.lastNormalShot = now;
        }

        /* =========================
         * ④ 확산 탄막 (Spread)
         * ========================= */
        if (now - boss.lastSpread >= P1_SPREAD_INTERVAL) {
            fireBossSpread();
            boss.lastSpread = now;
        }
    }

    private void fireBossLinear() {

        double cx = boss.x + boss.fireAnchorOffsetX;
        double cy = boss.y + boss.hitH;

        int id = nextBulletId++;

        BulletState b =
            new BulletState(id, -1, cx, cy, 0, 260);

        bullets.put(id, b);
        broadcast("/bullet/spawn/" + id + "/enemy/" + cx + "/" + cy);
    }


    
    private static final long P2_CORE_INTERVAL   = 6000;
    private static final long P2_TELEGRAPH_TIME  = 700;
    private static final long P2_BEAM_DURATION   = 600;
    private static final long P2_SPREAD_INTERVAL = 4000;

    private void updateBossPhase2(long now, double dt) {

        /* =========================
         * 1️⃣ 좌우 이동
         * ========================= */
        if (boss.p2CanMove) {
            boss.x += boss.moveDir * boss.p2MoveSpeed * dt;

            if (boss.x > boss.p2MoveCenterX + boss.p2MoveRange) {
                boss.x = boss.p2MoveCenterX + boss.p2MoveRange;
                boss.moveDir = -1;
            } else if (boss.x < boss.p2MoveCenterX - boss.p2MoveRange) {
                boss.x = boss.p2MoveCenterX - boss.p2MoveRange;
                boss.moveDir = 1;
            }
        }

        /* =========================
         * 2️⃣ Telegraph
         * ========================= */
        if (!boss.p2Telegraphing &&
            now - boss.p2LastCenterShot >= P2_CORE_INTERVAL - P2_TELEGRAPH_TIME) {

            broadcast("/boss/pattern/phase2/telegraph");
            boss.p2Telegraphing = true;
            boss.p2CanMove = false;
            return;
        }

        /* =========================
         * 3️⃣ CoreBeam
         * ========================= */
        if (boss.p2Telegraphing &&
            now - boss.p2LastCenterShot >= P2_CORE_INTERVAL) {

            broadcast("/boss/pattern/phase2/corebeam");

            boss.p2LastCenterShot = now;
            boss.p2LastBeamFired = now;
            boss.p2Telegraphing = false;
        }

        // 빔 끝나면 다시 이동 가능
        if (!boss.p2CanMove &&
            !boss.p2Telegraphing &&
            now - boss.p2LastBeamFired > P2_BEAM_DURATION) {

            boss.p2CanMove = true;
        }

        /* =========================
         * 4️⃣ Spread 탄막
         * ========================= */
        if (now - boss.lastSpread >= P2_SPREAD_INTERVAL) {
            fireBossSpread();
            broadcast("/boss/pattern/phase2/spread");
            boss.lastSpread = now;
        }
    }



    private void updateBossPhase3(long now, double dt) {

        // 이동
        boss.x += boss.moveDir * 140 * dt;
        if (boss.x < PLAY_AREA_X ||
            boss.x > PLAY_AREA_X + PLAY_AREA_WIDTH) {
            boss.moveDir *= -1;
        }

        // ⚡ 전기파동
        if (now - boss.lastWave >= 15000) {
            broadcast("/boss/pattern/phase3/electricwave");
            applyElectricWaveDamage(); 
            boss.lastWave = now;
        }

        // 🔮 구체
        if (now - boss.lastOrb >= 7000) {
            fireBossOrb();  // 실제 총알 스폰
            broadcast("/boss/pattern/phase3/orb");
            boss.lastOrb = now;
        }

        // 잡몹 소환
        if (now - boss.lastBossEnemySpawn >= 8000) {
            spawnStage3Enemy();
            boss.lastBossEnemySpawn = now;
        }
    }

    private void applyElectricWaveDamage() {
        for (int pid = 1; pid <= 2; pid++) {
            PlayerState p = players[pid];
            if (p == null || !p.alive) continue;

            // 전기파동은 전체 판정
            p.hp -= 10;

            if (p.hp <= 0) {
                p.hp = 0;
                p.alive = false;
                broadcast("/player/dead/" + pid);
                checkGameOver();
            } else {
                broadcast("/player/hp/" + pid + "/" + p.hp);
            }
        }
    }

    private void fireBossOrb() {

        double cx = boss.x + boss.hitW / 2;
        double cy = boss.y + boss.hitH;

        int id = nextBulletId++;

        double vx = 0;
        double vy = 180; // 느린 대형 탄

        BulletState b =
            new BulletState(id, -1, cx, cy, vx, vy);

        bullets.put(id, b);
        broadcast("/bullet/spawn/" + id + "/enemy/" + cx + "/" + cy);
    }
    
    private void spawnStage3Enemy() {

        int id = nextEnemyId++;

        double x = PLAY_AREA_X
                 + rand.nextDouble() * (PLAY_AREA_WIDTH - ENEMY_WIDTH);
        double y = -120;

        EnemyState e =
            new EnemyState(
                id,
                x,
                y,
                120,
                MoveType.LINEAR,
                FireType.LINEAR
            );

        enemies.put(id, e);
        broadcast("/enemy/spawn/" + id + "/stage3/" + x + "/" + y);
    }

    private void fireBossSpread() {

        double cx = boss.x;
        double cy = boss.y + boss.hitH / 2;

        int count = 6;
        double start = Math.toRadians(60);
        double end   = Math.toRadians(120);
        double speed = 300;

        for (int i = 0; i < count; i++) {
            double t = i / (double)(count - 1);
            double angle = start + (end - start) * t;

            int id = nextBulletId++;

            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed;

            BulletState b =
                new BulletState(id, -1, cx, cy, vx, vy);

            bullets.put(id, b);
            broadcast("/bullet/spawn/" + id + "/enemy/" + cx + "/" + cy);
        }
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
        	updateEnemies(dt);
            updateBoss(dt);
            broadcastBossState();
            checkBossPlayerCollision();// ⭐ 추가
        }
        updateBullets(sec);
        checkBulletEnemyCollision();
        checkBulletPlayerCollision();
        checkEnemyPlayerCollision();
        sendPlayerStates();
        sendBulletStates();

        broadcastStageTime(); 
        
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
        lastStageTimeBroadcast = 0;
        startStage2Spawning();
    }

    private void endStage2() {

        clearAllEnemies();
        clearAllBullets();

        broadcast("/stage/clear/2");
        broadcast("/stage/start/3");

        currentStage = 3;
        stageStartTime = System.currentTimeMillis();
        lastStageTimeBroadcast = 0;

        spawnBoss();
        startStage3Spawning();   // 🔥 여기서만 호출
    }


    private void endStage3() {

        System.out.println("[Server] Boss defeated → Stage3 Clear");

        clearAllEnemies();   // 🔥 추가
        clearAllBullets();

        broadcast("/gameclear");

        boss = null;
        currentStage = 0;
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
        long now = System.currentTimeMillis();

        for (EnemyState e : enemies.values()) {

            // 🔥 이동
            updateEnemyMovement(e, sec);

            // 화면 밖 제거
            if (e.y > 900) {
                enemies.remove(e.id);
                continue;
            }

            // 🔫 공격 (기존 로직 유지)
            if (e.fireType != FireType.NONE) {

                if (now - stageStartTime < 2000) continue;

                if (e.y > 80 &&
                    now - e.spawnTime > 1000 &&
                    now - e.lastFireTime > e.fireDelay) {

                    e.lastFireTime = now;
                    fireByType(e);
                }
            }

            // 위치 동기화
            broadcast("/enemy/pos/" + e.id + "/" + e.x + "/" + e.y);
        }
    }

    
    private void updateEnemyMovement(EnemyState e, double sec) {

        // 공통: 아래 이동
        e.y += e.speedY * sec;

        // 패턴별 가로 이동
        if (e.moveType == MoveType.ZIGZAG) {
            e.zigzagTime += sec;
            e.x = e.baseX + Math.sin(e.zigzagTime * e.frequency) * e.amplitude;
        }
    }

    
    
    private void spawnEnemyBullet(EnemyState e) {

        int id = nextBulletId++;

        // 싱글과 동일하게 아래 방향
        double bx = e.x + 35;  // 적 스프라이트 절반
        double by = e.y + 60;

        double vx = 0;
        double vy = 250; // 적 총알 속도 (싱글 값 맞추기)

        BulletState b = new BulletState(id, -1, bx, by, vx, vy);
        bullets.put(id, b);

        broadcast("/bullet/spawn/" + id + "/enemy/" + bx + "/" + by);
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

        double px = p.x + PLAYER_W / 2.0;
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
                if (p.x < e.x + ENEMY_W &&
                	    p.x + PLAYER_W > e.x &&
                	    p.y < e.y + ENEMY_H &&
                	    p.y + PLAYER_H > e.y) {

                    // 🔥 플레이어 데미지
                	p.hp -= ENEMY_CONTACT_DAMAGE;

                    System.out.println("[Server] Player " + pid +
                                       " hit by enemy! HP=" + p.hp);

                    // 적 제거 (싱글과 동일한 처리)
                    enemies.remove(e.id);
                    broadcast("/enemy/dead/" + e.id);

                    // 플레이어 사망 처리
                    if (p.hp <= 0) {
                    	p.hp = 0;
                        p.alive = false;
                        broadcast("/player/dead/" + pid);
                        checkGameOver(); // 🔥 추가
                    } else {
                        broadcast("/player/hp/" + pid + "/" + p.hp);
                    }

                    return; // 1프레임 1회 충돌
                }
            }
        }
    }
    
    private void checkGameOver() {

        boolean p1Dead = (players[1] == null || !players[1].alive);
        boolean p2Dead = (players[2] == null || !players[2].alive);

        if (p1Dead && p2Dead) {
            System.out.println("[Server] ALL PLAYERS DEAD → GAME OVER");
            broadcast("/gameover");
            currentStage = 0;

            clearAllEnemies();
            clearAllBullets();
        }

    }

    
    private void checkBulletEnemyCollision() {

        for (BulletState b : bullets.values()) {

            // 🔥 플레이어 총알만 적을 맞출 수 있음
            if (b.ownerId == -1) continue;

            for (EnemyState e : enemies.values()) {

            	if (b.x < e.x + ENEMY_W &&
            		    b.x + BULLET_W > e.x &&
            		    b.y < e.y + ENEMY_H &&
            		    b.y + BULLET_H > e.y) {

                    e.hp -= 20;

                    bullets.remove(b.id);
                    broadcast("/bullet/remove/" + b.id);
                    broadcast("/enemy/hp/" + e.id + "/" + e.hp);

                    if (e.hp <= 0) {
                        enemies.remove(e.id);
                        broadcast("/enemy/dead/" + e.id);
                        addTeamScore(100);
                    }
                    break;
                }
            }

            // ─────────────────────────
            // 2) 🔥 보스 충돌 (Stage3)
            // ─────────────────────────
            if (currentStage == 3 && boss != null) {

            	if (b.x < boss.x + boss.hitW &&
            		    b.x + BULLET_W > boss.x &&
            		    b.y < boss.y + boss.hitH &&
            		    b.y + BULLET_H > boss.y) {

            		    boss.hp -= 10;

            		    bullets.remove(b.id);
            		    broadcast("/bullet/remove/" + b.id);

            		    if (boss.hp <= 0) {
            	            boss.hp = 0;
            	            boss.alive = false;
            	            addTeamScore(1000);
            	            endStage3();
            	        }
            	        break;
            		}
                    //break;
                }
            }
        }
    
    
    private void checkBulletPlayerCollision() {

        for (BulletState b : bullets.values()) {

            // 🔥 적 총알만 플레이어를 공격
            if (b.ownerId != -1) continue;

            for (int pid = 1; pid <= 2; pid++) {

                PlayerState p = players[pid];
                if (p == null || p.hp <= 0) continue;

                // 싱글과 동일한 판정 크기
                if (b.x < p.x + PLAYER_W &&
                	    b.x + BULLET_W > p.x &&
                	    b.y < p.y + PLAYER_H &&
                	    b.y + BULLET_H > p.y) {

                    // 💥 데미지
                	p.hp -= ENEMY_BULLET_DAMAGE;

                    System.out.println(
                        "[Server] Player " + pid + " hit by ENEMY BULLET! HP=" + p.hp
                    );

                    // 총알 제거
                    bullets.remove(b.id);
                    broadcast("/bullet/remove/" + b.id);

                    // HP 동기화
                    if (p.hp <= 0) {
                        p.hp = 0;
                        p.alive = false;
                        broadcast("/player/dead/" + pid);
                        checkGameOver();   // 🔥 반드시 추가
                    } else {
                        broadcast("/player/hp/" + pid + "/" + p.hp);
                    }

                    return; // 1프레임 1회 처리
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

            	p.hp -= BOSS_CONTACT_DAMAGE;

                System.out.println("[Server] Player " + pid +
                                   " hit by BOSS! HP=" + p.hp);

                if (p.hp <= 0) {
                    p.hp = 0;
                    p.alive = false;
                    broadcast("/player/dead/" + pid);
                    checkGameOver();   // 🔥 반드시 추가
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
    
 // Server.java
    private void broadcastStageTime() {

        if (currentStage == 0) return;

        long now = System.currentTimeMillis();
        long elapsedMs = now - stageStartTime;

        long stageLimitMs =
            (currentStage == 1) ? STAGE1_TIME :
            (currentStage == 2) ? STAGE2_TIME :
            -1;

        if (stageLimitMs <= 0) return; // 보스전은 TIME HUD 없음

        int remainSec = (int)Math.max(0, (stageLimitMs - elapsedMs) / 1000);

        // 1초에 한 번만 전송
        if (now - lastStageTimeBroadcast >= STAGE_TIME_PACKET_INTERVAL) {
            broadcast("/stage/time/" + remainSec);
            lastStageTimeBroadcast = now;
        }
    }

    private void addTeamScore(int amount) {
        teamScore += amount;
        broadcast("/score/" + teamScore);
    }
    public static void main(String[] args) throws Exception {
        new Server(30000);
    }
}
