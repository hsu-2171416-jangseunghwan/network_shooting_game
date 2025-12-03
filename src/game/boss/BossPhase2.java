package game.boss;

import game.bosseffect.CoreBeam;
import game.bosseffect.TelegraphBeam;
import game.core.Entity;
import game.entity.BossSingle;
import game.weapon.SpreadFirePattern;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * BossPhase2 - 2페이즈부터는 좌우로 움직인다 - 중앙 빔은 1페이즈보다 더 굵게 - 주변으로 확산탄을 더 많이 뿌린다 - HP
 * 40% 이하가 되면 다음 페이즈로
 */
public class BossPhase2 implements BossPhase {

	// 중앙 빔 관련
	private long lastCenterShot = 0;
	private BufferedImage beamImg; // 한 번 로드해서 보관

	// 빔이 나간 시점 기록용
	private long lastBeamFired = 0;
	private long centerShotInterval = 6000; // 6초마다 쏘게 약간 늘림
	private boolean telegraphing = false;
	private long telegraphTime = 700; // 예고는 살짝 더 길게
	private boolean canMove = true;// 빔 쏠때 멈추기 위한 플래그
	private long beamDuration = 600; // 빔이 유지되는 시간

	// 확산탄 관련
	private long lastSpreadShot = 0;
	private long spreadInterval = 4000; // 4초마다
	private SpreadFirePattern spreadPattern;
	private BufferedImage spreadImg;

	// 좌우 이동 관련
	private double moveCenterX; // 기준점
	private double moveRange = 120; // 좌우로 흔들릴 범위
	private double moveSpeed = 80; // px/s
	private int moveDir = 1; // 1: 오른쪽, -1: 왼쪽

	@Override
	public void enter(BossSingle boss) {
		System.out.println("[BossPhase2] 진입");

		long now = System.currentTimeMillis();
		lastCenterShot = now;
		lastSpreadShot = 0;
		telegraphing = false;

		var rm = boss.getResourceManager();
		beamImg = rm.getImage("corebeam");
		spreadImg = rm.getImage("bullet_spread");
		// 이동 기준점을 지금 위치로
		moveCenterX = boss.getX();

		// 2페이즈는 확산탄을 더 많이
		// (1페이즈가 7발이었다면 여기선 11발 정도)
		spreadPattern = new SpreadFirePattern(6, // 발수
				60, 120, // 각도 범위
				boss.getW() / 2.0, // 보스 중앙 X 오프셋
				boss.getH(), // 보스 하단 Y 오프셋
				220, // 속도
				spreadImg, // 🔹 이미지
				30, 30 // 🔹 화면에서 보일 픽셀 크기
		);
	}

	@Override
	public void update(BossSingle boss, long dt) {
		long now = System.currentTimeMillis();
		// 기준점에서 moveRange만큼만 왔다갔다
		if (canMove) {
			// 1) 좌우 이동 ------------------------------------------------
			// dt는 ms니까 초단위로 바꿔서 이동
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

		// 2) 중앙 빔 (1페이즈보다 더 굵게) ----------------------------
		if (!telegraphing) {
			if (now - lastCenterShot >= centerShotInterval - telegraphTime) {
				double x = boss.getBeamAnchorX() + 14 - 5; // 예고선도 조금 굵게
				double y = boss.getY() + boss.getH();
				double h = 800;
				TelegraphBeam warn = new TelegraphBeam(x, y, 16, h, telegraphTime);
				boss.spawnEntity(warn);
				telegraphing = true;
				canMove = false; // 이때 잠깐 멈추게
			}
		} else {
			if (now - lastCenterShot >= centerShotInterval) {
				double centerX = boss.getBeamAnchorX();
				double beamW = 720; // 굵기
				double beamX = centerX - beamW / 2.0;
				double beamY = boss.getY() + boss.getH() - 50;
				double beamH = 800;
				long keepMs = 600;

				CoreBeam beam = (beamImg != null) ? new CoreBeam(beamX, beamY, beamW, beamH, keepMs, beamImg, 15)
						: new CoreBeam(beamX, beamY, beamW, beamH, keepMs, beamImg, 15);

				boss.spawnEntity(beam);
				lastCenterShot = now;
				telegraphing = false;
				// 빔 발사 시각 기록
				lastBeamFired = now;
				lastCenterShot = now;
				telegraphing = false;
				System.out.println("[BossPhase2] 중앙 빔 발사(굵게)");

			}
		}
		if (!canMove && !telegraphing && (now - lastBeamFired > beamDuration)) {
			canMove = true;
		}

		// 3) 확산탄 (파동 느낌) ---------------------------------------
		if (now - lastSpreadShot >= spreadInterval) {
			List<Entity> out = new ArrayList<>();
			spreadPattern.fire(boss, out);
			for (Entity e : out) {
				boss.spawnEntity(e);
			}
			lastSpreadShot = now;
			System.out.println("[BossPhase2] 확산탄 발사(파동)");
		}
	}

	@Override
	public boolean isComplete(BossSingle boss) {
		// HP 40% 이하로 내려가면 페이즈2 종료
		return boss.getHp() <= boss.getMaxHp() * 0.4;
	}
}
