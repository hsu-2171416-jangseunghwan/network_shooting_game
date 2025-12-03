// src/game/boss/BossPhase3.java
package game.boss;

import game.bosseffect.CoreBeam;
import game.bosseffect.CoreOrb;
import game.bosseffect.ElectricWave;
import game.bosseffect.ElectricWaveTelegraph;
import game.bosseffect.TelegraphBeam;
import game.bosseffect.WavePrompt;
import game.core.Entity;
import game.entity.BossSingle;
import game.entity.Enemy;
import game.enumset.BulletType;
import game.movement.LinearMove;
import game.weapon.LinearFire;
import game.weapon.SpreadFirePattern;
import game.enumset.PlayerIndex;


import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class BossPhase3 implements BossPhase {

	// 좌우 이동 더 빠르게
	private double moveCenterX;
	private double moveRange = 150;
	private double moveSpeed = 140; // 2페이즈보다 빠르게
	private int moveDir = 1;

	// 빔 시간 제언(여기선 그냥 굵은 빔/구형탄으로 재활용)
	private long lastCoreShot = 0;
	private long coreShotInterval = 5000;
	private boolean telegraphing = false;// 빔 전용 예고(요네궁) 껏다켰다 할때 쓰는 변수
	private long telegraphTime = 700;
	private BufferedImage beamImg; // 빔 이미지

	// 거대한 구체
	private long lastOrbShot = 0;
	private long orbInterval = 7000; // 빔이랑 살짝 다르게
	private boolean orbTelegraphing = false;// 구체 전용 예고(요네궁) 껏다켰다 할때 쓰는 변수
	private long orbTelegraphTime = 600;
	private BufferedImage orbImg;//구체 이미지

	// 확산/1페이즈 몹 등장용
	private long lastSpawnTime = 0;
	private long spawnInterval = 8000; // 8초마다 1페이즈 애들 소환
	// 필요하면 여기서 Stage2/SpawnPattern 호출하면 됨

	// 전기 파동
	private long lastWave = 0;
	private long waveInterval = 15000; // 30초마다

	@Override
	public void enter(BossSingle boss) {
		System.out.println("[BossPhase3] 진입");
		long now = System.currentTimeMillis();
		lastCoreShot = now;
		lastOrbShot = now;
		lastWave = now; // 시작하고 좀 있다가 오게 하려면 now - 25_000 이런 식으로
		moveCenterX = boss.getX();
		var rm = boss.getResourceManager();
        beamImg = rm.getImage("corebeam");//코어빔 이미지
        orbImg = boss.getResourceManager().getImage("CoreOrb");//구슬이미지 저장
		System.out.println("[BossPhase3] 진입");

	}

	@Override
	public void update(BossSingle boss, long dt) {
		long now = System.currentTimeMillis();
		
		
		// 1) 이동 (예고/파동 중에는 멈춰도 됨) 예고중에 멈추고 빔 쏘는 순간 다시 움직임
		boolean canMove = !telegraphing && !orbTelegraphing;// 구체예고와 빔 예고가 켜질떄 멈춤
		if (canMove) {// 예고가 나올때 멈추기
			double dtSec = dt / 1000.0;
			double nextX = boss.getX() + moveDir * moveSpeed * dtSec;
			if (nextX > moveCenterX + moveRange) {
				nextX = moveCenterX + moveRange;
				moveDir = -1;
			} else if (nextX < moveCenterX - moveRange) {
				nextX = moveCenterX - moveRange;
				moveDir = 1;
			}
			boss.setPosition(nextX, boss.getY());
		}

		// 2) 코어에서 거대한 탄/빔
		if (!telegraphing) {// 요네 궁
			if (now - lastCoreShot >= coreShotInterval - telegraphTime) {
				double centerX = boss.getBeamAnchorX() + 15; // ←
				double warnW = 70; // 예고선 두께
				double warnX = centerX - warnW / 2.0;
				double warnY = boss.getY() + boss.getH();
				double warnH = 800;

				TelegraphBeam warn = new TelegraphBeam(warnX, warnY, warnW, warnH, telegraphTime);
				boss.spawnEntity(warn);
				;
				telegraphing = true;// 다시 움직이기 시작
			}
		} else {
			if (now - lastCoreShot >= coreShotInterval) {
				double centerX = boss.getBeamAnchorX();
                double beamW   = 720; // 굵기
                double beamX   = centerX - beamW/2.0;
                double beamY   = boss.getY() + boss.getH()-50;
                double beamH   = 800;
                long   keepMs  = 600;

                CoreBeam beam = (beamImg != null)
                    ? new CoreBeam(beamX, beamY, beamW, beamH, keepMs, beamImg,15)
                    : new CoreBeam(beamX, beamY, beamW, beamH, keepMs,15);
                
                boss.spawnEntity(beam);

				lastCoreShot = now;
				telegraphing = false;
				System.out.println("[BossPhase3] 코어 대형 빔 발사");
			}
		}
		
		

		// 맵 전체 전기 파동
		// 파동 쿨 다 됐을 때 안내 먼저
		if (now - lastWave >= waveInterval - 1500 && now - lastWave < waveInterval) {
			WavePrompt prompt = new WavePrompt("⚡ 전기 파동! 회피키 입력!!(P1:SHIFT P2:P)", 1500);
			boss.spawnEntity(prompt);
		} // 나중에 UI 나오게 변경
			// 실제 파동
		if (now - lastWave >= waveInterval) {
			Rectangle area = boss.getPlayArea() != null ? boss.getPlayArea().getBounds()
					: new Rectangle(0, 0, 400, 800);
			ElectricWave wave = new ElectricWave(area.x, area.y, area.width, area.height, 700);
			boss.spawnEntity(wave);
			lastWave = now;
			System.out.println("[BossPhase3] 전기 파동 발사");
		}

		// ── 거대 구체 ──
		if (!orbTelegraphing) {
			if (now - lastOrbShot >= orbInterval - orbTelegraphTime) {
				double cx = boss.getBeamAnchorX() + 15;
				double warnW = 50; // 구체는 좀 더 얇게
				double warnX = cx - warnW / 2.0;
				double warnY = boss.getY() + boss.getH();

				boss.spawnEntity(new TelegraphBeam(warnX, warnY, warnW, 800, orbTelegraphTime));
				orbTelegraphing = true;
			}
		} else {
			if (now - lastOrbShot >= orbInterval) {
				double cx = boss.getBeamAnchorX() + 15;
				double orbSize = 80;
				double orbX = cx - orbSize / 2.0;
				double orbY = boss.getY() + boss.getH();

				
				CoreOrb orb = new CoreOrb(orbX, orbY, orbSize, 200, 4000,20,orbImg);
				
				orb.setPlayArea(boss.getPlayArea());
				boss.spawnEntity(orb);

				lastOrbShot = now;
				orbTelegraphing = false;
				System.out.println("[BossPhase3] 구체 발사");
			}
		}

	}

	@Override
	public boolean isComplete(BossSingle boss) {
		// 0되면 끝
		return boss.getHp() <= 0;
	}
}
