package game.stage;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import game.entity.BossSingle;
import game.entity.Enemy;
import game.entity.Player;
import game.enumset.BulletType;
import game.enumset.EnemyKind;
import game.item.ItemDropTable;
import game.main.Game;
import game.manager.EntityManager;
import game.manager.ResourceManager;
import game.movement.LinearMove;
import game.movement.MovementPattern;
import game.movement.ZigZagMove;
import game.status.RunStats;
import game.ui.UIManager;
import game.weapon.ArcSpreadFire;
import game.weapon.FirePattern;
import game.weapon.LinearFire;
import game.weapon.TripleCannonFire;

public class Stage2 extends AbstractStage {

    private final List<Enemy> enemies = new ArrayList<>();
    private final Random rng = new Random();
    private long elapsedMs = 0;

    //public Stage2(EntityManager em, ResourceManager rm, Player player, UIManager ui) {
      //  super(em, rm, player, ui);
        //this.stageTimeLimit = 15;//테스트 용
    //}
    public Stage2(EntityManager em, ResourceManager rm, Player player, UIManager ui, RunStats runStats) {
        super(em, rm, player, ui, runStats);
        this.stageTimeLimit = 30; // 테스트용
        }
   
    @Override
    public int getStageNumber() { return 2; }

    @Override
    public void start() {
    	super.start();
    	
        enemies.clear();
        elapsedMs = 0;
    }
 // ✅ Stage1에서 쓰던 잡몹 패턴을 Stage2에서도 쓰기 위한 메서드
    private void spawnStage1Enemies() {

    	if (Game.isMultiplayer()) return;
    			
    	/*
        // ⏱ Stage2도 elapsedMs를 가지고 있다고 가정 (Stage1처럼)
        if (elapsedMs % 2000 < 16) {  // 2초 주기 스폰

            // 🔹 Stage1에서 쓰던 적 이미지 키를 그대로 사용할 수도 있고,
            //   Stage2에서 쓸 다른 이미지 키로 바꿔도 됨.
            BufferedImage src = resourceManager.getImage("스테이지2잡몸"); 
            if (src == null) return;  // 리소스 없으면 바로 종료

            int drawW = 150, drawH = 120;  // 적 스프라이트 크기

            // 원본 이미지를 지정한 크기로 스케일링
            Image scaled = src.getScaledInstance(drawW, drawH, Image.SCALE_SMOOTH);
            BufferedImage sprite = new BufferedImage(drawW, drawH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = sprite.createGraphics();
            g2.drawImage(scaled, 0, 0, null);
            g2.dispose();

            // 플레이어가 움직일 수 있는 영역 기준으로 스폰 범위 계산
            Rectangle area = player.getPlayArea();

            int margin = 20;  // 양 옆 여백
            int minX = (int) area.getX() + margin;
            int maxX = (int) (area.getX() + area.getWidth() - drawW - margin);

            // 🔥 랜덤 X 위치에서 적 스폰
            int spawnX = rng.nextInt(maxX - minX) + minX;

            // 🔫 기본 탄을 쏘는 보통 잡몹 생성
            Enemy e = new Enemy(spawnX, -drawH, sprite)
                    .withTarget(player)                          // 플레이어를 바라보게 타겟 지정
                    .withDropTable(new ItemDropTable())
                    .withEntityManager(entityManager)
                    .withFire(new LinearFire(BulletType.BASIC))  // 기본 탄 발사 패턴
                    .withMovement(new LinearMove(120))          // 아래로 직선 이동
                    .withRunStats(runStats);       
            

            e.setPlayArea(area);           // 움직일 수 있는 영역 지정
            
            e.setKind(EnemyKind.STAGE1);


            enemies.add(e);                // 스테이지가 관리하는 잡몹 리스트에 추가
            entityManager.add(e);          // 엔티티 매니저에도 등록 (실제로 그려지고 업데이트됨)
        }
        */
    }
    
    @Override 
    public void spawnEnemies() {//스테이지2잡몹 소환

    	if (Game.isMultiplayer()) return;
    	
    	/*
        if (elapsedMs % 1000 < 16) {

            BufferedImage src = resourceManager.getImage("스테이지1잡몸");
            if (src == null) return;

            int drawW = 200, drawH = 130;

            Image scaled = src.getScaledInstance(drawW, drawH, Image.SCALE_SMOOTH);
            BufferedImage sprite = new BufferedImage(drawW, drawH, BufferedImage.TYPE_INT_ARGB);

            Graphics2D g2 = sprite.createGraphics();
            g2.drawImage(scaled, 0, 0, null);
            g2.dispose();

            FirePattern pattern;
            int r = rng.nextInt(3);
            if (r == 0) pattern = new LinearFire(BulletType.BASIC);
            else if (r == 1) pattern = new TripleCannonFire(360, 10, 12);
            else pattern = new ArcSpreadFire(5, 60, 120, 280, 6);
            
            MovementPattern moving;
            

            Rectangle area = player.getPlayArea();

            int margin = 20;
            int minX = (int) area.getX() + margin;
            int maxX = (int) (area.getX() + area.getWidth() - drawW - margin);

            int spawnX = rng.nextInt(maxX - minX) + minX;

            Enemy e = new Enemy(spawnX, -drawH, sprite)
                    .withTarget(player)
                    .withDropTable(new ItemDropTable())
                    .withEntityManager(entityManager)
                    .withFire(pattern)
                    .withMovement(new ZigZagMove(120, 40, 1))
                    .withRunStats(runStats);       
         // 🔥 여기!
            

            e.setPlayArea(area);
            
            e.setKind(EnemyKind.STAGE2);

            enemies.add(e);
            entityManager.add(e);
        }
        */
    }

    @Override
    public void update(long dt) {
    	System.out.println("dt = " + dt);
        super.update(dt);
        elapsedMs += dt;

        if (Game.isMultiplayer()) return;

        
        /*
        if (!stageEnded) {

            spawnEnemies();
            
            spawnStage1Enemies();//스테이지1 잡몹 추가

            for (Enemy e : enemies) {
                e.update(dt);
                entityManager.addAll(e.drainSpawned());
            }

            enemies.removeIf(e -> !e.isAlive());

        } else {
            if (ui.isResultVisible()) {
                nextStage = new Stage3Boss(entityManager, resourceManager, player, ui,runStats);
            }
        }
        */
    }

    @Override
    public void render(Graphics2D g) {
    	entityManager.render(g);
        for (Enemy e : enemies) e.render(g);
    }

    @Override
    public boolean isCleared() {
        return elapsedMs > 1500;
    }

    @Override
    public BossSingle getBoss() {
        return null;
    }
}
