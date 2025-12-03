package game.boss;


import game.entity.BossSingle;
import game.bosseffect.CoreBeam;
import game.bosseffect.TelegraphBeam;
import game.core.Entity;
import game.enumset.BulletType;

import game.weapon.LinearFire;
import game.weapon.SpreadFirePattern;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * BossPhase1
 * - 1페이즈: 제자리에서 공격만 하는 구간
 * - 중앙 코어 직선 빔(=직선탄) + 주기적으로 확산탄 쏘는 느낌으로 구성
 * - HP가 70% 이하가 되면 페이즈 종료해서 BossSingle이 nextPhase() 하도록 함
 */
public class BossPhase1 implements BossPhase {

    // 빔(직선탄) 쿨타임
    private long lastCenterShot = 0;
    private long centerShotInterval = 5500; // 2.5초마다
   
   
    private boolean telegraphing = false; // 지금 경고 상태냐(요네 궁)
    private long telegraphTime = 600;     // 경고 유지 시간(ms)
    
    private long lastNormalShot = 0;
    private long normalShotInterval = 1200;   //일반 탄환
    
    // 날개 확산탄 쿨타임
    private BufferedImage spreadImg;
    private long lastSpreadShot = 0;
    private long spreadInterval = 10_000; // 10초마다 확산탄
    private SpreadFirePattern centerSpread;
   // private final CoreBeamFirePattern corePattern = new CoreBeamFirePattern();//코이 빔 패턴
    private BufferedImage beamImg; // 빔 이미지
    

    @Override
    public void enter(BossSingle boss) {
        System.out.println("[BossPhase1] 진입");

        // 이 페이즈 동안 기본 탄막은 직선으로 나가게 세팅
        // Enemy에 있던 withFire(...) 메서드 그대로 활용
        var rm = boss.getResourceManager();
        
            // 리소스 키는 네가 가진 파일명에 맞춰서: "beam_blue", "laser_core" 등
        beamImg = rm.getImage("corebeam");
  
        spreadImg = boss.getResourceManager().getImage("bullet_spread"); // 리소스 키는 네 자원 이름에 맞춰
        boss.withFire(new LinearFire(BulletType.CANNON));
        
        // 타이머 초기화
        long now = System.currentTimeMillis();
        lastCenterShot = now;
        lastSpreadShot = 0;

        // 보스 중앙에서 살짝 아래에서 쏘게 세팅
        centerSpread = new SpreadFirePattern(
                6,          // 발수
                60, 120,     // 각도 범위
                boss.getW()/2.0,  // 보스 중앙 X 오프셋
                boss.getH(),      // 보스 하단 Y 오프셋
                220,         // 속도
                spreadImg,   // 🔹 이미지
                30, 30       // 🔹 화면에서 보일 픽셀 크기
        );
    }

    @Override
    public void update(BossSingle boss, long dt) {
        long now = System.currentTimeMillis();
        boss.setPosition(boss.getX(), boss.getY()); // 고정 위치 유지
     // 1) 경고 상태가 아닐 때
        if (!telegraphing) {//요네 궁 깔아주는 거
            if (now - lastCenterShot >= centerShotInterval - telegraphTime) {
            	double cx = boss.getBeamAnchorX();         // ← 앵커 사용
                double warnW = 50;//굵기
                double warnX = cx - warnW / 2.0+21;
                double warnY = boss.getY() + boss.getH();
                double warnH = 800;

                boss.spawnEntity(new TelegraphBeam(warnX, warnY, warnW, warnH, telegraphTime));
                telegraphing = true;
            }
        } else {
            // 2) 코어 빔 쏘는 단계
            if (now - lastCenterShot >= centerShotInterval) {
            	double centerX = boss.getBeamAnchorX();
                double beamW   = 500; // 굵기
                double beamX   = centerX - beamW/2.0+6;
                double beamY   = boss.getY() + boss.getH()-60;
                double beamH   = 800;
                long   keepMs  = 600;

                CoreBeam beam = (beamImg != null)
                    ? new CoreBeam(beamX, beamY, beamW, beamH, keepMs, beamImg,15)
                    : new CoreBeam(beamX, beamY, beamW, beamH, keepMs,15);

                boss.spawnEntity(beam);
                lastCenterShot = now;
                telegraphing = false;
                System.out.println("[BossPhase2] 중앙 빔(스프라이트) 발사");
            }
        }
        
       
        // ② 👇 여기서 일반 탄막 쏘는 부분 ----------------
        if (now - lastNormalShot >= normalShotInterval) {

            // 보스가 가진 기본 FirePattern을 꺼내온다
            var fp = boss.getFirePattern();
            if (fp != null) {
                // 패턴이 총알을 담을 리스트가 필요하니까 하나 만든다
                java.util.List<game.core.Entity> out = new java.util.ArrayList<>();
                fp.fire(boss, out);    // 보스 기준으로 탄 생성

                // 생성된 탄들을 실제 게임으로 올린다
                for (var e : out) {
                    boss.spawnEntity(e);
                }
            }

            lastNormalShot = now;
        }
        

        // ② 확산탄 로직
        if (now - lastSpreadShot >= spreadInterval) {
        	 List<Entity> out = new ArrayList<>();
             centerSpread.fire(boss, out);
             for (Entity e : out) {
                 boss.spawnEntity(e);
             }
             lastSpreadShot = now;
             System.out.println("[BossPhase2] 확산탄 발사(파동)");
        }

        // 3) 1페이즈는 “제자리에서 공격만”이라서 이동은 안 시킴
        //    boss.setX(...) / boss.setY(...) 안 함
    }

    @Override
    public boolean isComplete(BossSingle boss) {
        // HP가 70% 이하로 내려가면 페이즈1 종료
        return boss.getHp() <= boss.getMaxHp() * 0.7;
    }
}