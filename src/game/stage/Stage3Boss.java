package game.stage;

import game.item.ItemDropTable;
import game.entity.Player;
import game.entity.Enemy;
import game.entity.BossSingle;
import game.enumset.BulletType;
import game.enumset.EnemyKind;
import game.manager.EntityManager;
import game.manager.ResourceManager;
import game.movement.LinearMove;
import game.status.RunStats;
import game.ui.UIManager;
import game.weapon.ArcSpreadFire;
import game.weapon.SpreadFirePattern;   // 🔥 분노 확산탄 패턴용
import game.boss.*;
import game.core.Entity;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Stage3Boss extends AbstractStage {

    private BossSingle boss;                 // 보스 본체
    private long lastSpawnTime = 0;          // 잡몹 소환 쿨타임
    private final Random rng = new Random(); // 잡몹 스폰 위치용 RNG
    private boolean bossCounted = false;     // 보스 처치 통계 올렸는지 여부

    // 🔥 3페이즈 잡몹 저장 리스트
    private final List<Enemy> phase3Enemies = new ArrayList<>();

    // 🔥 코옵 QTE 실패 후 분노 패턴 상태
    private boolean rageMode = false;                // QTE 실패 후 분노 모드 진입 여부
    private boolean rageShotDone = false;            // 분노 패턴(확산탄)을 이미 한 번 쐈는지
    //private SpreadFirePattern rageSpreadPattern;     // 분노 때 사용할 확산탄 패턴
    
    // 🔥 분노 모드 전용 이동/공격 상태
    private int rageMoveDir = 1;             // 좌우 이동 방향 (+1 오른쪽, -1 왼쪽)
    private long lastRageShotTime = 0L;      // 마지막 분노탄 발사 시각
    private long rageShotInterval = 700;     // 분노 확산탄 간격(ms) - 0.7초마다

 // 🔥 QTE 실패 후 분노 확산탄 패턴 (이름은 그대로, 타입만 ArcSpreadFire)
    private ArcSpreadFire rageSpreadPattern;
    
    // 🔥 분노 모드 타이머
    private long rageStartTime = 0L;         // 분노 시작 시각
    private long rageDurationMs = 10_000L;   // 분노 지속 시간 (10초)
    
    
    
    
    // 기본 생성자 (RunStats 없는 버전)
    public Stage3Boss(EntityManager em, ResourceManager rm, Player player, UIManager ui) {
        super(em, rm, player, ui);
        // 보스전은 시간 기반 클리어 X → 충분히 크게 잡아둠
        this.stageTimeLimit = 10000;
    }

    // RunStats 포함 생성자
    public Stage3Boss(EntityManager em, ResourceManager rm, Player player, UIManager ui, RunStats runStats) {
        super(em, rm, player, ui, runStats);
        this.stageTimeLimit = 10000;
    }

    @Override
    public int getStageNumber() { 
        return 3; 
    }

    @Override
    public void start() {
        super.start();

        System.out.println("[Stage3Boss] ▶ 보스 스테이지 시작");

        // 1) 보스 이미지 로드
        BufferedImage image = resourceManager.getImage("Boss1");
        if (image == null) {
            throw new RuntimeException("Boss1 이미지가 없습니다!");
        }

        // 2) 스케일 조정 (절반 크기)
        double scale = 0.5;
        int newW = (int)(image.getWidth() * scale);
        int newH = (int)(image.getHeight() * scale);

        Image scaled = image.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
        BufferedImage finalSprite = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = finalSprite.createGraphics();
        g2.drawImage(scaled, 0, 0, null);
        g2.dispose();

        // 3) 보스 위치 계산 (플레이 영역 기준 중앙 상단)
        Rectangle area = player.getPlayArea();
        int centerX = area.x + area.width / 2;

        // 4) 보스 생성
        boss = new BossSingle(centerX - newW / 2.0, 80, finalSprite);

        // ✔ 리소스매니저, 플레이어, 페이즈 정보 주입
        boss.setResourceManager(resourceManager);
        boss.setPlayer(player);

        List<BossPhase> phases = new ArrayList<>();
        phases.add(new BossPhase1());
        phases.add(new BossPhase2());
        phases.add(new BossPhase3());
        boss.setPhases(phases);

        boss.setPlayArea(area);
        boss.setKind(EnemyKind.BOSS);

        // 5) 엔티티 매니저에 등록
        entityManager.add(boss);

        System.out.println("[Stage3Boss] 보스 생성 완료");

     // 🔥 분노 확산탄 패턴 초기화 (BossPhase2와 동일 파라미터)
        var rm = resourceManager;                           // 리소스 매니저
        BufferedImage spreadImg = rm.getImage("bullet_spread"); // 탄 이미지

        rageSpreadPattern = new ArcSpreadFire(
                6,      // 발수
                60,     // 시작 각도 (아래쪽 기준 왼쪽)
                120,    // 끝 각도 (아래쪽 기준 오른쪽)
                300,    // 속도
                12      // 데미지
        );
        
        player.setStage(this);
        ui.getHud().setBossStage(true);
    }

    /** 🔥 3페이즈에서만 잡몹 소환 */
    @Override
    public void spawnEnemies() {

        // 보스 없거나 죽었으면 소환 안 함
        if (boss == null || !boss.isAlive()) return;

        long now = System.currentTimeMillis();
        long COOLDOWN = 3000; // 3초마다 소환

        // 쿨타임 덜 지났으면 리턴
        if (now - lastSpawnTime < COOLDOWN) return;
        lastSpawnTime = now;

        BufferedImage img = resourceManager.getImage("스테이지2잡몸");
        if (img == null) {
            System.out.println("[Stage3Boss] 스테이지2잡몸 이미지 없음");
            return;
        }

        // 적 스프라이트 스케일 조정
        int w = 150, h = 120;
        Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
        BufferedImage sprite = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = sprite.createGraphics();
        g2.drawImage(scaled, 0, 0, null);
        g2.dispose();

        // 🔥 플레이 영역 기준으로 X 계산
        Rectangle area = (Rectangle) boss.getPlayArea();
        int playX = area.x;
        int playW = area.width;
        int margin = 40;

        int available = playW - w - margin * 2;  // 가용 폭

        int x;
        if (available <= 0) {
            // 적이 너무 크거나 margin이 모자라면 중앙에 고정
            x = playX + (playW - w) / 2;
        } else {
            x = playX + margin + rng.nextInt(available);
        }

        int y = -h + 5; // 살짝 위에서 등장

        Enemy e = new Enemy(x, y, sprite)
                .withMovement(new LinearMove(120))         // 아래로 직선 이동
                .withFire(new game.weapon.LinearFire(BulletType.BASIC)) // 기본 탄 발사
                .withTarget(player)
                .withDropTable(new ItemDropTable())        // 아이템 드랍
                .withEntityManager(entityManager)          // 아이템 등록
                .withRunStats(runStats);                   // 통계 연계

        e.setPlayArea(area);
        e.setKind(EnemyKind.STAGE1);
        e.setFireCoolDown(1500);                          // 1.5초마다 공격

        // 리스트에 넣기
        phase3Enemies.add(e);

        // 엔티티 매니저에 등록
        entityManager.add(e);

        System.out.println("[Stage3Boss] 3페이즈 잡몹 스폰됨");
    }

    // 🔥 Game 쪽에서 QTE 실패 시 호출하는 메서드
    public void onCoopQteFailed() {
        if (rageMode) return;          // 이미 분노 상태면 무시
        rageMode = true;               // 분노 모드 ON
        rageShotDone = false;          // 아직 분노 확산탄을 안 쐈다고 표시
        System.out.println("[Stage3Boss] QTE 실패 → 분노 모드 돌입");
        
        // 🔥 분노 모드용 이동/탄 초기화
        rageMoveDir = 1;                              // 처음엔 오른쪽으로
        lastRageShotTime = 0L;                        // 바로 쏠 수 있도록 0으로
        rageShotInterval = 700;                       // 기본 간격 (원하면 조절)
        
        // 🔥 분노 시작 시각 기록
        rageStartTime = System.currentTimeMillis();
    }

    @Override
    public void update(long dt) {

        // 3페이즈 잡몹 소환
        spawnEnemies();

        // 상위(Stage) 공통 업데이트 (타이머, UI 연계 등)
        super.update(dt);

        // 보스 업데이트 + 보스가 spawnEntity로 뱉어낸 탄/이펙트 회수
        if (boss != null) {
            boss.update(dt);
            entityManager.addAll(boss.drainSpawned());
        }

        // 🔥 QTE 실패 후 분노 모드라면, 한 번만 확산탄 패턴 발사
        // 🔥 QTE 실패 후 분노 모드라면, 좌우로 미친 듯이 움직이면서 확산탄 난사
        if (rageMode && boss != null && boss.isAlive()) {
            
        	
        	
        	   long now = System.currentTimeMillis();

               // 0) 분노 시간 다 되면 해제 ---------------------
               if (now - rageStartTime >= rageDurationMs) {
                   rageMode = false;
                   System.out.println("[Stage3Boss] 분노 모드 종료");
               } else {

                   // 1) 좌우 광란 이동 -----------------------------
                   Rectangle area = (Rectangle) boss.getPlayArea();
                   double x = boss.getX();
                   double y = boss.getY();

                   double moveSpeed = 260.0;                         // 속도
                   double dx = rageMoveDir * moveSpeed * (dt / 1000.0);

                   double newX = x + dx;
                   double left = area.x;
                   double right = area.x + area.width - boss.getW();

                   if (newX < left) {
                       newX = left;
                       rageMoveDir = 1;
                   } else if (newX > right) {
                       newX = right;
                       rageMoveDir = -1;
                   }

                   boss.setPosition(newX, y);

                   // 2) 확산탄 난사 -----------------------------
                   if (now - lastRageShotTime >= rageShotInterval) {
                       System.out.println("[Stage3Boss] rageMode 업데이트 - 확산탄 난사!");

                       List<Entity> out = new ArrayList<>();
                       rageSpreadPattern.fire(boss, out);
                       System.out.println("[Stage3Boss] rage bullets = " + out.size());

                       for (Entity e : out) {
                           entityManager.add(e);
                       }

                       lastRageShotTime = now;
                   }
               }
        }




        // 🔥 여기서 잡몹이 spawnEntity로 만든 탄 회수
        for (Enemy e : phase3Enemies) {
            entityManager.addAll(e.drainSpawned());
        }

        // ✔ 보스 HP가 0 → 스테이지 클리어
        if (boss != null && !boss.isAlive()) {

            // ★ 보스 처치 통계 1번만 올리기
            if (!bossCounted && runStats != null) {
                runStats.onEnemyKilled(EnemyKind.BOSS);
                bossCounted = true;
            }

            // 🔥 3페이즈 잡몹 전부 제거
            for (Enemy e : phase3Enemies) {
                e.destroy();       // EntityManager.update()에서 정리됨
            }
            phase3Enemies.clear();

            // ⭐ 결과창 띄우기 (한 번만)
            if (!ui.isResultVisible()) {
                long clearTime = System.currentTimeMillis() - stageStartTime;

                ui.showResult(
                        (player != null ? player.getScore() : 0),
                        (player != null ? player.getCollectedCount() : 0),
                        getStageNumber(),
                        clearTime
                );
            }

            stageEnded = true;
        }
    }

    @Override
    public void render(Graphics2D g) {
        // 엔티티 매니저에 등록된 모든 엔티티 렌더
        entityManager.render(g);
    }

    /** 🔥 시간 제한과 상관없이, 보스가 죽으면 스테이지 클리어 */
    public boolean isCleared() {
        return boss != null && !boss.isAlive();
    }

    @Override
    public BossSingle getBoss() {
        return boss;
    }
}
