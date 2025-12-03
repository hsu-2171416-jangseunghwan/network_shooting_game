package game.bosseffect;

import game.core.Entity;
import game.enumset.EntityType;
import game.enumset.Team;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * 💥 BigExplosion
 * - 보스 피격 시 발생하는 큰 폭발 이펙트.
 * - 생성 후 잠시 동안 크기가 커지고, 점점 투명해지다가 사라진다.
 */
public class BigExplosion extends Entity {

    private long lifeMs = 1000;          // 폭발 지속 시간 (ms)
    private long elapsed = 0;           // 경과 시간 누적
    private double maxSize = 120;       // 폭발 최대 반경
    private double startSize = 200;      // 시작 크기
    private double endSize = 320;  // 폭발이 최대가 되는 크기

    public BigExplosion(double x, double y) {//이미지가 없을때 쓰는 생성자
        // EntityType은 EFFECT, 팀은 중립
        super(EntityType.EFFECT, Team.NEUTRAL, x, y, 200, 200);
    }
    public BigExplosion(double x, double y, BufferedImage sprite) {//이미지가 있을떄 쓰는 생성자
        super(EntityType.EFFECT, Team.NEUTRAL, x, y,  sprite.getWidth(), sprite.getHeight());
        setSprite(sprite);
    }
    @Override
    public void update(long dt) {
    	 // 🔸 움직이지 않게 super.update(dt) 호출 안 함
        elapsed += dt;
        lifeMs -= dt;

        if (lifeMs <= 0) {
            destroy();
        }
    }

    @Override
    public void render(Graphics2D g) {
    	// 🌀 폭발의 애니메이션 상태 계산
    	// 폭발이 생긴 후 얼마나 시간이 지났는지를 비율(0~1)로 환산
    	float t = Math.max(0f, Math.min(1f, elapsed / 700f));

    	// 💥 폭발 크기 계산 (시간이 지날수록 커짐)
    	// startSize → endSize로 점진적으로 커진다
    	double size = startSize + (endSize - startSize) * t;

    	// 🌫️ 투명도 계산 (시간이 지날수록 희미해짐)
    	// 처음엔 불투명(1.0), 끝날수록 완전 투명(0.0)
    	float alpha = 1f - t;
    	alpha = Math.max(0f, Math.min(1f, alpha));

    	// 🎨 현재 그래픽 상태 저장
    	Composite old = g.getComposite();
    	// AlphaComposite을 사용해 투명도 적용
    	g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

    	// 🔹 폭발이 화면에 그려질 좌표 계산 (폭발 중심을 기준으로 정렬)
    	int drawX = (int) (getX() - size / 2);
    	int drawY = (int) (getY() - size / 2);

    	// 🖼️ 폭발 그리기
    	if (sprite != null) {
    	    // 스프라이트가 있으면 이미지 폭발로 렌더링
    	    g.drawImage(sprite, drawX, drawY, (int) size, (int) size, null);
    	} else {
    	    // 스프라이트가 없으면 단색 원형 폭발로 대체
    	    g.setColor(new Color(255, 180, 60));   // 중심부 밝은 주황
    	    g.fillOval(drawX, drawY, (int) size, (int) size);
    	    g.setColor(new Color(255, 240, 200));  // 외곽선 살짝 밝게
    	    g.drawOval(drawX, drawY, (int) size, (int) size);
    	}

    	// 🔄 투명도 설정 복원
    	g.setComposite(old);
    }
}
