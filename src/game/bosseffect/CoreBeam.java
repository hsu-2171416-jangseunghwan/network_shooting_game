// CoreBeam.java
package game.bosseffect;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import game.core.Entity;
import game.entity.Player;
import game.enumset.EntityType;
import game.enumset.Team;

/**
 * CoreBeam
 * - 보스 코어에서 발사되는 ‘레이저/빔’.
 * - 이미지 스프라이트가 있으면 그걸 늘려서 렌더, 없으면 기본 도형으로 렌더.
 * - 충돌 판정은 항상 사각형 hitbox(=width/height)로 처리.
 */
public class CoreBeam extends Entity {

    private long lifeMs;                 // 빔 지속 시간(ms)
    private BufferedImage sprite;        // 빔 텍스처(선택)
    private float alpha = 1f;            // 투명도(끝나갈수록 서서히 낮추고 싶을 때 사용)
    private boolean fadeOut = false;      // 종료 직전 페이드아웃 여부
    private int damage;
    
    
    
    // 중복 히트 방지
    private boolean hitOnce = false;

    public CoreBeam(double x, double y, double w, double h, long lifeMs,int damage) {
        super(EntityType.EFFECT, Team.ENEMY, x, y, w, h);
        this.lifeMs = lifeMs;
        this.damage=damage;
        // ← 플레이어와 충돌 판정 활성화
    }

    /** 스프라이트 지정 버전 (이미지 있을 때) */
    public CoreBeam(double x, double y, double w, double h, long lifeMs, BufferedImage sprite,int damage) {
        this(x, y, w, h, lifeMs, damage);
        this.sprite = sprite;
    }
    public Rectangle2D getBounds() {//판정폭을 얇게 설정
        double hitW = Math.min(getW(), 80);      // 예: 판정 폭 80px
        double offsetX = (getW() - hitW) / 2.0;  // 중앙 정렬
        return new Rectangle2D.Double(getX() + offsetX, getY(), hitW, getH());
    }
    @Override
    public void update(long dt) {
        lifeMs -= dt;
        if (lifeMs <= 0) {
            destroy();
            return;
        }
      
        // 남은 비율로 페이드아웃(옵션)
        if (fadeOut) {
            float t = Math.max(0f, Math.min(1f, lifeMs / 600f)); // 마지막 0.6초만 서서히 줄어들게 예시
            alpha = 0.3f + 0.7f * t; // 0.3 ~ 1.0 사이
        }
    }
    @Override
    public void onCollision(game.core.Entity other) {
        if (other instanceof Player p) {
            p.takeDamage(damage);
            destroy();
        }
    }
    @Override
    public void render(Graphics2D g) {
        // 투명도 적용
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        int dx = (int)getX();
        int dy = (int)getY();
        int dw = (int)getW();
        int dh = (int)getH();

        if (sprite != null) {
            // 🔸 이미지가 있으면 그대로 “늘려서” 그린다 (성능 안정 + 간단)
        	 g.drawImage(sprite, (int)getX(), (int)getY(), (int)getW(), (int)getH(), null);
        } else {
            // 🔸 이미지 없으면 기본 도형 레이저(기존 폴백)
            g.setColor(new Color(80, 180, 255, 200)); // 외곽
            g.fillRect(dx, dy, dw, dh);
            g.setColor(new Color(200, 240, 255, 220)); // 중앙 하이라이트
            g.fillRect(dx + dw/4, dy, dw/2, dh);
        }

        g.setComposite(old);
    }
}
