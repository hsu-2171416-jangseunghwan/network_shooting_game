package game.main;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import game.bosseffect.ElectricWave;
import game.bosseffect.WavePrompt;
import game.entity.Player;
import game.enumset.BulletType;
import game.enumset.GameState;
import game.enumset.PlayerIndex;
import game.manager.AudioManager;
import game.manager.EntityManager;
import game.manager.ResourceManager;
import game.manager.StageManager;
import game.mode.LocalCoopConfig;
import game.network.NetBullet;
import game.network.NetworkClient;
import game.status.RunStats;  // ★ 한 판 동안의 통계 저장용
import game.ui.GameTheme;
import game.ui.PausePanel;
import game.ui.UIManager;
import game.weapon.Weapon;


class NetEnemy {
    public int id;
    public String type;
    public double x, y;
    public int hp;

    // 🔥 렌더링용
    public int w, h;
    public Image sprite;

    // 보스용
    public int phase = 0;
}



public class Game {

	private HashMap<Integer, NetEnemy> netEnemies = new HashMap<>();
    private GameState state = GameState.MENU;

    private final ResourceManager rm = new ResourceManager();
    private final EntityManager entityManager = new EntityManager();
    private UIManager uiManager;
    
 // 🔥 내 입력 상태 (서버로 보낼 값)
    private int inputX = 0;
    private int inputY = 0;
    private int inputFire = 0;
    
 // 🔥 내 플레이어 ID (서버가 알려줌)
    private int myPlayerId = -1;

    // 🔥 네트워크 클라이언트 객체
    private NetworkClient network;
    
    // ★ 이번 플레이(한 판) 동안의 통계를 저장하는 객체
    private final RunStats runStats = new RunStats();
    
    private PausePanel pausePanel;
    
    private final LocalCoopConfig coopConfig = LocalCoopConfig.defaultCoop(); // ★ 추가
    private StageManager stageManager;
    private Player player;   // P1
    private Player player2;  // P2
    private static boolean coopMode = true;
    private int gameOverSelectedIndex = 0; // 0=Restart, 1=Exit

    private Image bg;
    private int bgY = 0;

    private final Rectangle playArea = new Rectangle(0, 0, 500, 800);

    // READY COUNTDOWN
    private long readyStartTime = 0;
    private int countdown = 3;
    private int stageToStart = 0;
    
    private int gameOverIndex = 0;
    
 // 🔥 멀티 전용 보스 QTE 상태
    private boolean coopQteActive = false;      // QTE 진행 중인지
    private boolean coopQteFinished = false;    // 한 번 끝났는지(재발동 방지)
    private long coopQteStartTime = 0L;         // 시작 시간
    private int coopQteDurationMs = 2000;       // QTE 유지 시간 (2초)

    private int coopQteP1Taps = 0;             // P1 연타 횟수
    private int coopQteP2Taps = 0;             // P2 연타 횟수

    // 조건 값들 (필요하면 나중에 조절 가능)
    private double coopQteHpThreshold = 0.3;   // 보스 HP 30% 이하에서 발동
    private int coopQteRequiredTaps = 3;      // 각 플레이어 최소 연타 수
    
    // 🔥 화면 흔들림 연출용 필드
    private long screenShakeUntil = 0L;          // 흔들림이 끝나는 시각(밀리초)
    private int screenShakeMagnitude = 10;       // 흔들림 세기(픽셀)
    private final Random shakeRng = new Random(); // 랜덤 오프셋 생성용

    private static final boolean MULTI = true;
    
    private boolean finalClear = false;  // 마지막 스테이지 클리어 여부

    private boolean resumeFromPause = false;
    
    private AudioManager audio = new AudioManager();
    
    private List<String> pendingEnemySpawns = new ArrayList<>();
    
    private ConcurrentHashMap<Integer, NetBullet> netBullets = new ConcurrentHashMap<>();
    
    private int serverRemainTime = -1;
    
    private static final int RENDER_OFFSET_X = -25;
    private static final int RENDER_OFFSET_Y = 0;
    
    public Game() {
        bg = rm.getImage("background");
        entityManager.setCoopConfig(coopConfig);   // ★ 충돌 시스템에 코옵 설정 연결
        
        audio.playBGM("menu_music.wav");
        pausePanel = new PausePanel(runStats, rm);
        //network = new NetworkClient(this, "127.0.0.1", 30000);
    }
    
    
 // ============================================================
 // UPDATE
 // ============================================================
 public void update(long dt) {

     // ───────── PAUSED 상태 ─────────
     if (state == GameState.PAUSED) {

         // 카운트다운 중이면 숫자만 렌더링, 게임 논리 업데이트는 모두 중지
         if (pausePanel.isCounting()) {

        	 if (pausePanel.isCountdownFinished()) {
        		    pausePanel.stopCountdown();
        		    pausePanel.hide();

        		    // 🔥 ESC → RESUME 후 재개해야 하는 경우
        		    if (resumeFromPause) {
        		        uiManager.getHud().resumeTimer(); // ★ 타이머 재개
        		        resumeFromPause = false;          // 플래그 초기화
        		    }

        		    state = GameState.RUNNING;   // 게임 재개
        		    return;
        		}

             return; // 카운트다운 중엔 모든 업데이트 중지
         }

         return; // 선택 메뉴 상태 — 업데이트 중지
     }

     // ───────── READY 상태 (3,2,1 카운트) ─────────
     if (state == GameState.READY) {

         long elapsed = (System.currentTimeMillis() - readyStartTime) / 1000;
         countdown = Math.max(0, 3 - (int) elapsed);

         // 3초 이하에서는 게임 진행 X
         if (elapsed < 4) {
             return;
         }
         

         // 스테이지 시작 (처음 플레이 or 스테이지 넘어갈 때)
         if (elapsed >= 4) {
             if (stageToStart == 1) startStage1();
             else if (stageToStart == 2) startStage2();
             else if (stageToStart == 3) startStage3();

             state = GameState.RUNNING;
         }

         return;
     }

     // ───────── RUNNING 상태만 게임 논리 업데이트 ─────────
     if (state == GameState.RUNNING) {

    	 if (network != null && myPlayerId != -1) {
    	        network.sendInput(myPlayerId, inputX, inputY, inputFire);
    	    }
    	 
         // 결과창 떠있으면 멈춤
         if (uiManager != null && uiManager.isResultVisible())
             return;

         // 🔥 QTE 처리
         handleCoopQte(dt);

         // 스테이지 진행 (QTE 중엔 멈춤)
         if (!isMultiplayer() && stageManager != null && !coopQteActive) {
        	    stageManager.update(dt);
        	}

         // P1 업데이트
         if (player != null) {
             player.update(dt);
             
             /*
             List<Bullet> spawned = (List<Bullet>)(List<?>) player.drainSpawned();
             if (!spawned.isEmpty())
                 entityManager.addAll(spawned);
                 */
         }

         // P2 업데이트
         if (coopMode && player2 != null && player2.isAlive()) {
             player2.update(dt);

             /*
             List<Bullet> spawned2 = (List<Bullet>)(List<?>) player2.drainSpawned();
             if (!spawned2.isEmpty())
                 entityManager.addAll(spawned2);
                 */
         }

         // 엔티티 전체 업데이트 (Bullets, Enemies, Item 등)
         entityManager.update(dt);

         // 생존 체크
         boolean p1Alive = (player != null && player.isAlive());
         boolean p2Alive = (coopMode && player2 != null && player2.isAlive());

         if (!coopMode) {
             if (!p1Alive) {
                 state = GameState.GAME_OVER;
                 return;
             }
         } else {
        	 if (!isMultiplayer()) {
        		    if (!p1Alive) {
        		        state = GameState.GAME_OVER;
        		        return;
        		    }
        		}
         }

     }
 }

 
//🔥 멀티 전용 보스 QTE 관리 (HP 30% 이하에서 한 번 발동)
 private void handleCoopQte(long dt) {

	    // 코옵 아니면 종료
	    if (!coopMode) return;

	    // 플레이어 둘 다 살아있어야 의미 있음
	    if (player == null || player2 == null) return;
	    if (!player.isAlive() || !player2.isAlive()) return;

	    // 🔥 서버에서 받은 보스(NetEnemy) 찾기
	    NetEnemy boss = null;
	    for (NetEnemy ne : netEnemies.values()) {
	        if ("boss".equals(ne.type)) {
	            boss = ne;
	            break;
	        }
	    }
	    if (boss == null) return;

	    // 이미 QTE 한 번 끝났으면 재실행 금지
	    if (coopQteFinished) return;

	    long now = System.currentTimeMillis();

	    // ─────────────────────────
	    // QTE 시작 조건 체크
	    // ─────────────────────────
	    if (!coopQteActive) {

	        // ⚠️ 서버 보스 maxHp = 1000 기준 (약속된 값)
	        double hpRate = boss.hp / 1000.0;

	        if (hpRate <= coopQteHpThreshold) {
	            coopQteActive = true;
	            coopQteStartTime = now;
	            coopQteP1Taps = 0;
	            coopQteP2Taps = 0;

	            if (uiManager != null) {
	                uiManager.startQteFlash();
	            }

	            System.out.println("[CoopQTE] 합동 QTE 시작");
	        }
	        return;
	    }

	    // ─────────────────────────
	    // QTE 진행 중 → 시간 종료 체크
	    // ─────────────────────────
	    if (now - coopQteStartTime < coopQteDurationMs) {
	        return;
	    }

	    // ─────────────────────────
	    // QTE 종료 처리
	    // ─────────────────────────
	    coopQteActive = false;
	    coopQteFinished = true;

	    if (uiManager != null) {
	        uiManager.stopQteFlash();
	    }

	    boolean p1Ok = coopQteP1Taps >= coopQteRequiredTaps;
	    boolean p2Ok = coopQteP2Taps >= coopQteRequiredTaps;

	    System.out.println("[CoopQTE] 결과 → P1:" + coopQteP1Taps + " / P2:" + coopQteP2Taps);

	    // ─────────────────────────
	    // 성공 / 실패 분기
	    // ─────────────────────────
	    if (p1Ok && p2Ok) {
	        // ✅ 성공 → 서버에 통보
	        System.out.println("[CoopQTE] 성공 → 서버 통보");

	        if (network != null) {
	            network.send("/boss/qte/success/" + coopQteP1Taps + "/" + coopQteP2Taps);
	        }

	        // 🔥 성공 연출 (클라 전용)
	        entityManager.add(new game.bosseffect.WavePrompt(
	                "✅ QTE 성공! 강력한 합동 공격!", 1500));

	        screenShakeUntil = System.currentTimeMillis() + 1500;
	        screenShakeMagnitude = 14;

	    } else {
	        // ❌ 실패 → 서버에 통보
	        System.out.println("[CoopQTE] 실패 → 서버 통보");

	        if (network != null) {
	            network.send("/boss/qte/fail/" + coopQteP1Taps + "/" + coopQteP2Taps);
	        }

	        entityManager.add(new game.bosseffect.WavePrompt(
	                "❌ QTE 실패... 보스가 분노합니다!", 1500));

	        // 실패 패널티 (연출 + 체감용)
	        if (player.isAlive()) player.takeDamage(1);
	        if (player2.isAlive()) player2.takeDamage(1);

	        screenShakeUntil = System.currentTimeMillis() + 2000;
	        screenShakeMagnitude = 12;
	    }
	}


 

    // ============================================================
    // RENDER
    // ============================================================
    public void render(Graphics2D g,  int width, int height) {
    	// 🔥 화면 흔들림용 변환 저장
        AffineTransform oldTx = g.getTransform();

        // 🔥 현재 시간이 흔들림 유지 시간 안이면 랜덤 오프셋 계산
        int shakeX = 0;
        int shakeY = 0;
        long now = System.currentTimeMillis();
        if (now < screenShakeUntil) {
            // -magnitude ~ +magnitude 사이 랜덤 값
            shakeX = shakeRng.nextInt(screenShakeMagnitude * 2 + 1) - screenShakeMagnitude;
            shakeY = shakeRng.nextInt(screenShakeMagnitude * 2 + 1) - screenShakeMagnitude;

            // 살짝만 흔들고 싶으면 Y는 줄이고 X만 크게 해도 됨
            // shakeY /= 2;
        }

        // 🔥 실제 좌표계 이동 (배경 + 스테이지 + 플레이어 + UI 전부 같이 흔들림)
        g.translate(shakeX, shakeY);
        // 배경
        int h = bg.getHeight(null);
        g.drawImage(bg, 0, bgY - h, null);
        g.drawImage(bg, 0, bgY, null);

        // 메뉴 화면
        if (state == GameState.MENU) {
            drawMenu(g, width, height);
            return;
        }

        // READY 화면
        if (state == GameState.READY) {
            drawReady(g, width, height);
            return;
        }

        // 스테이지 렌더링
        if (!isMultiplayer() && stageManager != null) {
            stageManager.render(g);
        }

        
     // 🔥 서버에서 받은 적 렌더링
     // 🔥 서버에서 받은 적 렌더링
        for (NetEnemy e : netEnemies.values()) {

            BufferedImage img;

            switch (e.type) {
                case "stage1" -> img = rm.getImage("스테이지1잡몸");
                case "stage2" -> img = rm.getImage("스테이지2잡몸");
                case "boss"   -> {
                    // 🔥 페이즈별 보스 이미지
                    img = switch (e.phase) {
                        case 2 -> rm.getImage("Boss2");
                        case 3 -> rm.getImage("Boss3");
                        default -> rm.getImage("Boss1");
                    };
                }
                default -> img = rm.getImage("스테이지1잡몸");
            }

            // ===============================
            // 🔥 BOSS 전용 렌더링 (싱글과 동일)
            // ===============================
            if ("boss".equals(e.type)) {

                // 페이즈별 이미지
                switch (e.phase) {
                    case 1 -> img = rm.getImage("Boss1");
                    case 2 -> img = rm.getImage("Boss2");
                    case 3 -> img = rm.getImage("Boss3");
                    default -> img = rm.getImage("Boss1");
                }

                // 🔥 싱글과 동일한 스케일
                double scale = 0.5;

                int drawW = (int)(img.getWidth() * scale);
                int drawH = (int)(img.getHeight() * scale);

                // 🔥 서버 좌표 = 보스 중심 좌표
                int drawX = (int)(e.x - drawW / 2.0);
                int drawY = (int)(e.y - drawH / 2.0);

                g.drawImage(img, drawX, drawY, drawW, drawH, null);
                continue;
            }


            // ===============================
            // 일반 적 렌더링 (기존 방식 유지)
            // ===============================
            int enemyW = 70;
            int enemyH = 70;

            g.drawImage(img, (int)e.x, (int)e.y, enemyW, enemyH, null);

            // HP BAR
            int barW = 50;
            int barH = 6;
            int barX = (int)e.x + (enemyW - barW) / 2;
            int barY = (int)e.y - 10;

            float ratio = Math.max(0f, e.hp / 100f);

            g.setColor(new Color(60, 0, 0, 150));
            g.fillRect(barX, barY, barW, barH);

            g.setColor(Color.RED);
            g.fillRect(barX, barY, (int)(barW * ratio), barH);
        }



        
        // 플레이어 렌더링
        if (player != null)
            player.render(g);
        if (coopMode && player2 != null)
            player2.render(g);

        // UI 렌더링
        if (uiManager != null)
            uiManager.render(g, width, height);

     // GAME OVER or GAME CLEAR
        if (state == GameState.GAME_OVER) {
            if (finalClear) {
                drawGameClear(g);   // 🔹 새로 만들 함수
            } else {
                drawGameOver(g);    // 🔹 기존 실패용 화면
            }
        }
        
        // 🔥 일시정지 UI
        if (state == GameState.PAUSED && pausePanel != null) {
            int score = (player != null) ? player.getScore() : 0;
            pausePanel.setCurrentScore(score);           // 점수 전달
            pausePanel.render(g, width, height);         // 패널 렌더
        }
        
        for (NetBullet b : netBullets.values()) {

            // ===============================
            // 🔥 적 총알 (싱글과 동일: 이미지 없음)
            // ===============================
            if (b.isEnemyBullet()) {

                g.setColor(new Color(255, 200, 60)); // 싱글 BASIC 느낌
                g.fillRoundRect(
                    (int) b.x,
                    (int) b.y,
                    8,   // 싱글 Bullet 기본 width
                    16,  // 싱글 Bullet 기본 height
                    4, 4
                );

            }
            // ===============================
            // 🔥 플레이어 총알 (기존 이미지 유지)
            // ===============================
            else {

                Image img = rm.getImage("bullet_basic");

                if (img != null) {
                    g.drawImage(img, (int) b.x, (int) b.y, null);
                } else {
                    // 혹시 이미지 없을 때 안전장치
                    g.setColor(Color.YELLOW);
                    g.fillRoundRect((int) b.x, (int) b.y, 8, 16, 4, 4);
                }
            }
        }



     // 🔥 마지막에 항상 원래 변환으로 되돌리기
        g.setTransform(oldTx);
    }

    // ============================================================
    // READY 화면
    // ============================================================
    private void drawReady(Graphics2D g, int width, int height) {

        if (countdown > 0) {
            g.setFont(GameTheme.COUNTDOWN);
            g.setColor(GameTheme.ENERGY_YELLOW);
            GameTheme.drawCentered(g, String.valueOf(countdown), width, height / 2);
        } else {
            g.setFont(GameTheme.READY);

            if (coopMode && stageToStart == 3) {
                g.setColor(GameTheme.NEON_BLUE);
                GameTheme.drawCentered(g, "BOSS STAGE START!", width, height / 2);
            } else {
                g.setColor(GameTheme.NEON_CYAN);
                GameTheme.drawCentered(
                        g,
                        "STAGE " + stageToStart + " START!",
                        width,
                        height / 2
                );
            }
        }
    }

    // ============================================================
    // 메뉴 UI
    // ============================================================
    private void drawMenu(Graphics2D g, int width, int height) {

        g.setColor(new Color(0, 0, 0, 160));
        g.fillRect(0, 0, width, height);

        g.setFont(GameTheme.TITLE);
        g.setColor(GameTheme.NEON_CYAN);
        GameTheme.drawCentered(g, "STELLAR IMPACT", width, 220);

        g.setFont(GameTheme.MENU_OPTION);
        g.setColor(GameTheme.NEON_CYAN);
        GameTheme.drawCentered(g, "Coop Mode (2P)", width, 360);

        g.setFont(GameTheme.RESULT_TEXT);
        g.setColor(new Color(220, 240, 255, 160));
        GameTheme.drawCentered(g, "Press ENTER to Start", width, 520);
    }


    // ============================================================
    // GAME OVER UI
    // ============================================================
    private void drawGameOver(Graphics2D g) {

        int width = 800;
        int height = 800;

        // 배경 오버레이
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, width, height);

        // "GAME OVER"
        g.setFont(GameTheme.TITLE);
        g.setColor(GameTheme.ALERT_RED);
        GameTheme.drawCentered(g, "GAME OVER", width, height / 2 - 140);

        // 옵션
        String[] options = {"Restart", "Exit"};
        int baseY = height / 2 - 20;
        int gap = 70;

        g.setFont(GameTheme.SUBTITLE);

        for (int i = 0; i < options.length; i++) {

            boolean highlight = (i == gameOverIndex);
            int y = baseY + i * gap;

            if (highlight) {
                // 🔥 메뉴와 동일한 네온 글로우 박스
                g.setColor(new Color(0, 255, 255, 70));    // 반투명 CYAN
                g.fillRoundRect(
                        width / 2 - 160,     // x
                        y - 40,              // y
                        320,                 // w
                        55,                  // h
                        14, 14
                );

                g.setColor(GameTheme.ENERGY_YELLOW);       // 강조 텍스트
            } else {
                g.setColor(GameTheme.SOFT_WHITE);
            }

            GameTheme.drawCentered(g, options[i], width, y);
        }
    }

    
 // ============================================================
    // GAME CLEAR UI (클리어 전용 화면)
    // ============================================================
    private void drawGameClear(Graphics2D g) {
        int width = 800;
        int height = 800;

        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, width, height);

        // TITLE
        g.setFont(GameTheme.TITLE);
        g.setColor(GameTheme.NEON_BLUE);
        GameTheme.drawCentered(g, "GAME CLEAR!", width, height / 2 - 140);

        // SCORE
        int finalScore = (player != null) ? player.getScore() : 0;
        g.setFont(GameTheme.SUBTITLE);
        g.setColor(GameTheme.SOFT_WHITE);
        GameTheme.drawCentered(g, "SCORE : " + finalScore, width, height / 2 - 40);

        // 안내
        g.setFont(GameTheme.RESULT_TEXT);
        g.setColor(GameTheme.NEON_CYAN);
        GameTheme.drawCentered(g, "Press ENTER to return to Title", width, height / 2 + 40);
    }

    // ============================================================
    // INPUT
    // ============================================================
    public void onKeyPressed(KeyEvent e) {
        int code = e.getKeyCode();

        // ESC → 일시정지
        if (code == KeyEvent.VK_ESCAPE) {
            if (state == GameState.RUNNING) {
                state = GameState.PAUSED;
                uiManager.getHud().pauseTimer(); 
                pausePanel.show();
                return;
            }
            else if (state == GameState.PAUSED) {
                resumeFromPause = true;
                pausePanel.startResumeCountdown();
                return;
            }
        }

        // ───────── PAUSED 상태 ─────────
        if (state == GameState.PAUSED) {

            // ↑ ↓ 이동
            if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W) {
                pausePanel.moveUp();
                return;
            }
            if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_S) {
                pausePanel.moveDown();
                return;
            }

            // Enter → 실행
            if (code == KeyEvent.VK_ENTER) {

                int selected = pausePanel.getSelectedIndex();

                // 0 = RESUME
                if (selected == 0) { // RESUME
                    resumeFromPause = true;
                    pausePanel.startResumeCountdown();  // 🔥 패널 카운트다운 시작
                    return;
                }

                // 1 = MENU
                if (selected == 1) {
                    entityManager.clearAll();
                    stageManager = null;
                    player = null;
                    player2 = null;
                    runStats.reset();

                    pausePanel.hide();
                    state = GameState.MENU;
                    audio.playBGM("menu_music.wav");
                    return;
                }
            }
        }

        // ───────── 메뉴 ─────────
        if (state == GameState.MENU) {

            // ENTER → 항상 COOP 모드로 시작
        	 if (code == KeyEvent.VK_ENTER) {

        	        // 🔥 서버에게 준비 완료 신호를 보낸다
        	        if (network != null) {
        	            network.send("/ready");
        	        }

        	        System.out.println("[Client] READY 전송");
        	    }

        	    return;
        	}


     // ───────── 결과창 ─────────
        if (uiManager != null && uiManager.isResultVisible()) {

            if (code == KeyEvent.VK_ENTER) {

                uiManager.hideResult();

                // 현재 스테이지 번호 확인
                uiManager.hideResult();
                return;

                // 🚫 startReadyCountdown(next) 호출 금지!!
                // 서버가 "/stage/start/{next}" 패킷을 보내줄 때까지 대기
            }

            return;
        }


        // ───────── 게임 중 ─────────
        if (state == GameState.RUNNING) {

            // QTE 처리 (그대로 유지)
            if (coopQteActive) {
                handleCoopQteKeyInput(code);
            }

            // =======================================
            // 🔥 1) 내 입력 상태 갱신 (서버 전송용)
            // =======================================
            if (code == KeyEvent.VK_LEFT)  inputX = -1;
            if (code == KeyEvent.VK_RIGHT) inputX = 1;
            if (code == KeyEvent.VK_UP)    inputY = -1;
            if (code == KeyEvent.VK_DOWN)  inputY = 1;

            if (code == KeyEvent.VK_SPACE || code == KeyEvent.VK_O)
                inputFire = 1;

            // 서버로 전송
            if (network != null && myPlayerId != -1)
                network.sendInput(myPlayerId, inputX, inputY, inputFire);

            // =======================================
            // 🔥 2) 로컬 내 플레이어에도 즉시 반영
            // =======================================
            if (myPlayerId == 1 && player != null) {
                if (code == KeyEvent.VK_LEFT)  player.moveLeft();
                if (code == KeyEvent.VK_RIGHT) player.moveRight();
                if (code == KeyEvent.VK_UP)    player.moveUp();
                if (code == KeyEvent.VK_DOWN)  player.moveDown();
                //if (code == KeyEvent.VK_SPACE) player.shoot();
            }

            if (myPlayerId == 2 && player2 != null) {
                if (code == KeyEvent.VK_LEFT)  player2.moveLeft();
                if (code == KeyEvent.VK_RIGHT) player2.moveRight();
                if (code == KeyEvent.VK_UP)    player2.moveUp();
                if (code == KeyEvent.VK_DOWN)  player2.moveDown();
                //if (code == KeyEvent.VK_SPACE) player2.shoot();
            }

            return;
        }


        // ───────── GAME OVER ─────────
        if (state == GameState.GAME_OVER) {

        	
        	// ✅ 클리어 상태일 때: ENTER 한 번으로 타이틀 복귀
            if (finalClear) {
                if (code == KeyEvent.VK_ENTER) {
                    // 싹 정리하고 메뉴로
                    entityManager.clearAll();
                    stageManager = null;
                    player = null;
                    player2 = null;
                    runStats.reset();

                    finalClear = false;       // 다음 판을 위해 초기화
                    state = GameState.MENU;
                }
                return;
            }
           
            // ↑/↓ 또는 W/S 로 선택 변경



            if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W ||
                code == KeyEvent.VK_DOWN || code == KeyEvent.VK_S) {

                gameOverIndex = (gameOverIndex == 0 ? 1 : 0);
            }

            else if (code == KeyEvent.VK_ENTER) {

            	if (gameOverIndex == 0) { // Restart
            	    entityManager.clearAll();
            	    netEnemies.clear();
            	    netBullets.clear();

            	    player = null;
            	    player2 = null;
            	    runStats.reset();

            	    state = GameState.MENU;

            	    if (network != null) {
            	        network.send("/ready"); // 🔥 다시 준비
            	    }
            	} else {
                    System.exit(0);
                }
            }

            return;
        }
    }
    
 // 🔥 QTE 중일 때 키 입력 처리 (P1: E, P2: L)
    private void handleCoopQteKeyInput(int code) {
        if (code == KeyEvent.VK_E) {
            // P1용 QTE 키
            coopQteP1Taps++;
            // System.out.println("P1 탭: " + coopQteP1Taps);
        } else if (code == KeyEvent.VK_L) {
            // P2용 QTE 키
            coopQteP2Taps++;
            // System.out.println("P2 탭: " + coopQteP2Taps);
        }
    }

    public void onKeyReleased(KeyEvent e) {
        int code = e.getKeyCode();

        if (state != GameState.RUNNING) return;

        // =======================================
        // 🔥 1) 내 입력 상태 초기화
        // =======================================
        if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_RIGHT)
            inputX = 0;

        if (code == KeyEvent.VK_UP || code == KeyEvent.VK_DOWN)
            inputY = 0;

        if (code == KeyEvent.VK_SPACE || code == KeyEvent.VK_O)
            inputFire = 0;

        // 서버로 전송
        if (network != null && myPlayerId != -1)
            network.sendInput(myPlayerId, inputX, inputY, inputFire);

        // =======================================
        // 🔥 2) 로컬에서도 내 캐릭터 정지 처리
        // =======================================
        if (myPlayerId == 1 && player != null) {
            if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_RIGHT)
                player.stopX();

            if (code == KeyEvent.VK_UP || code == KeyEvent.VK_DOWN)
                player.stopY();
        }

        if (myPlayerId == 2 && player2 != null) {
            if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_RIGHT)
                player2.stopX();

            if (code == KeyEvent.VK_UP || code == KeyEvent.VK_DOWN)
                player2.stopY();
        }
    }


    // ============================================================
    // STAGE READY / START
    // ============================================================
    private void startReadyCountdown(int stageNum) {
        countdown = 3;
        readyStartTime = System.currentTimeMillis();
        state = GameState.READY;
        stageToStart = stageNum;
        
        System.out.println("[Game] Stage " + stageNum + " 카운트다운 시작");
    }

    // ============================================================
    // STAGE START (Stage1 / Stage2 / Stage3)
    // ============================================================
    private void startStage1() {

        // 🔥 서버에서 받은 적 스폰 패킷 처리만
        if (!pendingEnemySpawns.isEmpty()) {
            List<String> copy = new ArrayList<>(pendingEnemySpawns);
            pendingEnemySpawns.clear();
            for (String packet : copy) {
                onNetworkPacket(packet);
            }
        }

        setupPlayer();
        uiManager = new UIManager(player, runStats, rm);

        audio.playBGM("game_music.wav");

        System.out.println("[Game] Stage1 시작 (MULTI)");
    }


    private void startStage2() {
    	//runStats.reset();             
    	
        setupPlayer();
        uiManager = new UIManager(player, runStats,rm);
        
        if (isMultiplayer()) {
            System.out.println("[Client] Multiplayer Stage2 → server authoritative");
            audio.playBGM("game_music.wav");
            return;   // ❗ Stage2 로컬 로직 생성 금지
        }
        
        audio.playBGM("game_music.wav");

        System.out.println("[Game] Stage2 시작");
    }

    private void startStage3() {

        audio.stopBGM();

        setupPlayer();
        uiManager = new UIManager(player, runStats, rm);

        // 🔥 멀티플레이면 Stage 로직 생성 금지
        if (isMultiplayer()) {
            System.out.println("[Client] Multiplayer Stage3 → server authoritative");

            // 서버가 /enemy/spawn (boss) 보내줄 것임
            audio.playBGM("boss_stage_music.wav");
            return;
        }


        audio.playBGM("boss_stage_music.wav");

        System.out.println("[Game] Stage3 시작 (Single)");
    }


    // ============================================================
    // PLAYER SETUP
    // ============================================================
    private void setupPlayer() {

        entityManager.clearAll();

        int totalWidth = 800;
        int totalHeight = 800;

        int leftWidth = 150;
        int rightWidth = 150;
        int centerWidth = totalWidth - leftWidth - rightWidth;

        playArea.setBounds(leftWidth, 0, centerWidth, totalHeight);

        int startY = totalHeight - 120;

        // ─────────────────────────────
        // 코옵 모드 (P1 + P2)
        // ─────────────────────────────
        int gap = 80; // 가운데 기준으로 좌우 간격
        int p1X = leftWidth + centerWidth / 2 - gap - 24;
        int p2X = leftWidth + centerWidth / 2 + gap - 24;

        // P1
        if (player == null) {
            player = new Player(PlayerIndex.P1)
                    .withSprite(rm.getImage("player"))
                    .withPlayArea(playArea)
                    .withPosition(p1X, startY)
                    .switchWeapon(new Weapon(BulletType.BASIC));
        } else {
            player.setPlayArea(playArea);
            player.setPosition(p1X, startY);
        }

        // P2
        if (player2 == null) {
            player2 = new Player(PlayerIndex.P2)
                    .withSprite(rm.getImage("player2"))   // player2.png
                    .withPlayArea(playArea)
                    .withPosition(p2X, startY)
                    .switchWeapon(new Weapon(BulletType.BASIC));
        } else {
            player2.setPlayArea(playArea);
            player2.setPosition(p2X, startY);
        }
        
        // ★ 두 플레이어 모두에 EntityManager 주입
        player.setEntityManager(entityManager);
        player2.setEntityManager(entityManager);

        entityManager.add(player);
        entityManager.add(player2);
    }
    
    // ★ 다른 클래스들(Enemy, UI 등)에서 통계에 접근할 때 쓸 getter
    public RunStats getRunStats() {
        return runStats;
    }
    
 // ============================================================
 // 🔥 멀티플레이 패킷 처리 (서버 → 클라이언트)
 // ============================================================
 public void onNetworkPacket(String p) {
	 
	 if (p.equals("/gameover")) {

		    System.out.println("[NET] GAME OVER (ALL PLAYERS DEAD)");

		    finalClear = false;           // 클리어 아님
		    state = GameState.GAME_OVER;  // 🔥 기존 UI 재사용

		    // 입력 방지 / 정리
		    inputX = inputY = inputFire = 0;

		    return;
		}

	 
     // -------------------------
     // 플레이어 ID 배정
     // -------------------------
	 if (p.startsWith("/setid/")) {
 	    try {
 	        int id = Integer.parseInt(p.split("/")[2]);
 	        myPlayerId = id;  // 🔥 플레이어 ID 저장
 	        System.out.println("[NET] My Player ID = " + myPlayerId);
 	    } catch (Exception e) { }
 	    return;
 	}

     // -------------------------
     // 입력 패킷 처리
     //   /input/{pid}/{mx}/{my}/{fire}
     // -------------------------
     if (p.startsWith("/input/")) {
         try {
             String[] t = p.split("/");
             int pid  = Integer.parseInt(t[2]);
             int mx   = Integer.parseInt(t[3]);
             int my   = Integer.parseInt(t[4]);
             int fire = Integer.parseInt(t[5]);

             Player target = (pid == 1 ? player : player2);
             if (target != null) {
                 if (mx < 0) target.moveLeft();
                 else if (mx > 0) target.moveRight();
                 else target.stopX();

                 if (my < 0) target.moveUp();
                 else if (my > 0) target.moveDown();
                 else target.stopY();

                 //if (fire == 1) target.shoot();
             }
         } catch (Exception e) {
             System.out.println("[NET] INPUT parse error: " + p);
         }
         return;
     }
     
  // -------------------------
  // 적 스폰 패킷 처리
  // /enemy/spawn/{type}/{x}/{y}
  // -------------------------

     if (p.startsWith("/enemy/spawn/")) {
    	    try {
    	        if (player == null) {
    	            pendingEnemySpawns.add(p);
    	            return;
    	        }

    	        String[] t = p.split("/");

    	        int id = Integer.parseInt(t[3]);
    	        String type = t[4];
    	        double x = Double.parseDouble(t[5]);
    	        double y = Double.parseDouble(t[6]);

    	        NetEnemy ne = new NetEnemy();
    	        ne.id = id;
    	        ne.type = type;
    	        ne.x = x;
    	        ne.y = y;
    	        ne.hp = 100;

    	        // =====================================
    	        // 🔥 타입별 크기 / 이미지 결정
    	        // =====================================
    	        switch (type) {

    	            case "stage1" -> {
    	                ne.w = 150;
    	                ne.h = 120;
    	                ne.sprite = rm.getImage("스테이지2잡몸");
    	            }

    	            case "stage2" -> {
    	                ne.w = 200;
    	                ne.h = 130;
    	                ne.sprite = rm.getImage("스테이지1잡몸");
    	            }

    	            case "boss" -> {
    	                ne.w = 160;
    	                ne.h = 160;
    	                ne.sprite = rm.getImage("boss");
    	                ne.phase = 1;
    	            }
    	        }

    	        netEnemies.put(id, ne);

    	    } catch (Exception ex) {
    	        ex.printStackTrace();
    	    }
    	    return;
    	}


     if (p.startsWith("/boss/phase/")) {
    	    int phase = Integer.parseInt(p.split("/")[3]);

    	    for (NetEnemy ne : netEnemies.values()) {
    	        if ("boss".equals(ne.type)) {
    	            ne.phase = phase;
    	            System.out.println("[NET] Boss phase changed → " + phase);
    	        }
    	    }
    	    return;
    	}

     



     // -------------------------
     // 상태 패킷 처리
     // -------------------------
     if (p.startsWith("/state/")) {
         String stateName = p.substring(7);

         switch (stateName) {
             case "MENU" -> state = GameState.MENU;
             case "READY" -> state = GameState.READY;
             case "PLAY" -> state = GameState.RUNNING;
         }
         return;
     }

     // -------------------------
     // 스테이지 시작 패킷
     // -------------------------
     if (p.startsWith("/stage/start/")) {
    	    coopMode = true;
    	    try {
    	        String[] t = p.split("/");

    	        int stage = Integer.parseInt(t[3]); // ✅ 여기까지만 있음

    	        // HUD 보스 여부만 처리
    	        if (uiManager != null && uiManager.getHud() != null) {
    	            uiManager.getHud().setBossStage(stage == 3);
    	        }

    	        // READY 카운트다운 시작
    	        startReadyCountdown(stage);

    	        System.out.println("[NET] Stage " + stage + " start");

    	    } catch (Exception e) {
    	        System.out.println("[NET] stage/start parse error: " + p);
    	    }
    	    return;
    	}
     	
     if (p.startsWith("/score/")) {
    	    try {
    	        String[] t = p.split("/");
    	        int score = Integer.parseInt(t[2]);

    	        // 🔥 두 플레이어 모두 동일 점수
    	        if (player != null)  player.setScore(score);
    	        if (player2 != null) player2.setScore(score);

    	    } catch (Exception e) {
    	        System.out.println("[NET] score parse error: " + p);
    	    }
    	    return;
    	}
     

     
     if (p.startsWith("/gameclear")) {
    	    finalClear = true;
    	    state = GameState.GAME_OVER;  // 또는 별도의 CLEAR 화면 로직
    	    return;
    	}

     if (p.startsWith("/enemy/pos/")) {
    	    String[] t = p.split("/");

    	    int id = Integer.parseInt(t[3]);
    	    double x = Double.parseDouble(t[4]);
    	    double y = Double.parseDouble(t[5]);

    	    NetEnemy ne = netEnemies.get(id);
    	    if (ne != null) {
    	        ne.x = x;
    	        ne.y = y;
    	    }
    	    return;
    	}

     if (p.startsWith("/bullet/spawn/")) {
    	    String[] t = p.split("/");

    	    int id = Integer.parseInt(t[3]);
    	    String owner = t[4];        // ★ 핵심
    	    double x = Double.parseDouble(t[5]);
    	    double y = Double.parseDouble(t[6]);

    	    NetBullet b = new NetBullet(id, x, y, owner);
    	    netBullets.put(id, b);
    	    return;
    	}


     
     if (p.startsWith("/bullet/pos/")) {
    	    String[] t = p.split("/");

    	    int id = Integer.parseInt(t[3]);
    	    double x = Double.parseDouble(t[4]);
    	    double y = Double.parseDouble(t[5]);

    	    NetBullet b = netBullets.get(id);
    	    if (b != null) {
    	        b.x = x;
    	        b.y = y;
    	    }
    	    return;
    	}
     
     if (p.startsWith("/enemy/hp/")) {
    	    String[] t = p.split("/");
    	    int id = Integer.parseInt(t[3]);
    	    int hp = Integer.parseInt(t[4]);

    	    NetEnemy ne = netEnemies.get(id);
    	    if (ne != null) ne.hp = hp;

    	    return;
    	}
     
     if (p.startsWith("/enemy/dead/")) {
    	    int id = Integer.parseInt(p.split("/")[3]);
    	    netEnemies.remove(id);
    	    return;
    	}
     
     if (p.startsWith("/bullet/remove/")) {
    	    int id = Integer.parseInt(p.split("/")[3]);
    	    netBullets.remove(id);
    	    return;
    	}
     
     if (p.startsWith("/player/pos/")) {
    	    String[] t = p.split("/");
    	    int pid = Integer.parseInt(t[3]);
    	    double x = Double.parseDouble(t[4]);
    	    double y = Double.parseDouble(t[5]);

    	    Player target = (pid == 1 ? player : player2);
    	    if (target != null) {
    	        target.setNetworkPosition(x, y); // 새로 만들기
    	    }
    	    return;
    	}
     
     if (p.startsWith("/player/hp/")) {
    	    String[] t = p.split("/");
    	    int pid = Integer.parseInt(t[3]);
    	    int hp  = Integer.parseInt(t[4]);

    	    Player target = (pid == 1 ? player : player2);
    	    if (target != null) {
    	        target.setHp(hp);   // 또는 takeDamage 기반
    	    }
    	    return;
    	}

    	if (p.startsWith("/player/dead/")) {
    	    int pid = Integer.parseInt(p.split("/")[3]);

    	    Player target = (pid == 1 ? player : player2);
    	    if (target != null) {
    	        target.kill();
    	    }
    	    return;
    	}
    	
    	if (p.startsWith("/stage/time/")) {
    	    try {
    	        String[] t = p.split("/");

    	        int remain = Integer.parseInt(t[3]); // ✅ 남은 시간만 있음

    	        serverRemainTime = remain;

    	        if (uiManager != null && uiManager.getHud() != null) {
    	            uiManager.getHud().setRemainingTimeFromServer(remain);
    	        }

    	    } catch (Exception e) {
    	        System.out.println("[NET] stage/time parse error: " + p);
    	    }
    	    return;
    	}



     
  // -------------------------
  // 🔥 Boss State 패킷 처리
  // /boss/state/{id}/{x}/{y}/{hp}/{maxHp}/{alive}
  // -------------------------
  if (p.startsWith("/boss/state/")) {
      try {
          String[] t = p.split("/");

          int id = Integer.parseInt(t[3]);
          double x = Double.parseDouble(t[4]);
          double y = Double.parseDouble(t[5]);
          int hp = Integer.parseInt(t[6]);
          int maxHp = Integer.parseInt(t[7]);
          boolean alive = Integer.parseInt(t[8]) == 1;

          NetEnemy boss = netEnemies.get(id);
          if (boss == null) {
              boss = new NetEnemy();
              boss.id = id;
              boss.type = "boss";
              boss.phase = 1;
              netEnemies.put(id, boss);
          }

          boss.x = x;
          boss.y = y;
          boss.hp = hp;

          // 🔥🔥🔥 여기 추가 🔥🔥🔥
          if (uiManager != null && uiManager.getHud() != null) {
              uiManager.getHud().setBossStage(true); // TIME 대신 보스 HUD
              uiManager.getHud().setBossHp(
                  boss.hp,
                  maxHp,
                  boss.phase
              );
          }

          if (!alive) {
              netEnemies.remove(id);
          }


      } catch (Exception e) {
          System.out.println("[NET] boss/state parse error: " + p);
      }
      return;
  }
  
  if (p.startsWith("/boss/pattern/phase3/electricwave")) {
	    entityManager.add(new ElectricWave(
	        playArea.x,
	        playArea.y,
	        playArea.width,
	        playArea.height,
	        700
	    ));
	    return;
	}

	if (p.startsWith("/boss/pattern/phase3/orb")) {
	    // 서버에서 총알은 이미 spawn됨
	    // 여기서는 연출만 추가 가능
	    entityManager.add(
	        new WavePrompt("⚡ 거대 구체 발사!", 800)
	    );
	    return;
	}



     
     
     System.out.println("[NET] Unknown packet: " + p);
 }
 
 	public void setNetwork(NetworkClient network) {
	    this.network = network;
	}
 	
 	public static boolean isMultiplayer() {
 	    return true;
 	}

    
    
}