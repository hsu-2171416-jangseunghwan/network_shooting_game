package game.bosseffect;


import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import game.core.Entity;
import game.enumset.EntityType;
import game.enumset.Team;

public class SmallExplosion extends Entity {
    private long lifeMs = 300;          // 폭발 지속 시간 (ms)
    private BufferedImage sprite;       // 폭발 이미지 (있으면 사용)
    private float alpha = 1f;           // 투명도
    private double startSize = 80;      // 시작 크기
    private double endSize = 120;        // 최종 크기

    public SmallExplosion(double x, double y) {
        super(EntityType.EFFECT, Team.NEUTRAL, x, y, 32, 32);
    }
    
    public SmallExplosion(double x, double y, BufferedImage sprite) {//이미지 전용 생성자
        this(x, y);
        this.sprite = sprite;
    }
    @Override
    public void update(long dt) {
        lifeMs -= dt;
        if (lifeMs <= 0) destroy();
    }

    @Override
    public void render(Graphics2D g) {
    	long elapsed = Math.max(0, 300 - lifeMs);
        float t = Math.min(1f, elapsed / 300f); // 0~1 사이 비율

        // 점점 커지는 크기
        double size = startSize + (endSize - startSize) * t;

        // 투명도 점점 감소
        alpha = 1f - t;

        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        int drawX = (int)(getX() - size / 2);
        int drawY = (int)(getY() - size / 2);

        if (sprite != null) {
            // 🔸 폭발 이미지 버전
            g.drawImage(sprite, drawX, drawY, (int)size, (int)size, null);
        } else {
            // 🔸 기본 원 버전 (이미지 없을 때)
            g.setColor(new Color(255, 180, 60));
            g.fillOval(drawX, drawY, (int)size, (int)size);
            g.setColor(new Color(255, 240, 200));
            g.drawOval(drawX, drawY, (int)size, (int)size);
        }

        g.setComposite(old);
    }
    
}