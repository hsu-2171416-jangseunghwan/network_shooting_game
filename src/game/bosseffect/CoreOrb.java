package game.bosseffect;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import game.core.Entity;
import game.entity.Player;
import game.enumset.EntityType;
import game.enumset.Team;
//페아즈 3보스 대형 구체
public class CoreOrb extends Entity {

    private long lifeMs;
    private int damage;

    public CoreOrb(double x, double y, double size, double speed, long lifeMs, int damage) {
       
        super(EntityType.BULLET, Team.ENEMY, x, y, size, size);
        this.vy = speed;      // 아래로 내려가게
        this.lifeMs = lifeMs;
        this.damage = damage;
    }
    
    // 🟣 이미지 버전 생성자 (스프라이트 추가)
    public CoreOrb(double x, double y, double size, double speed, long lifeMs, int damage, BufferedImage sprite) {
        super(EntityType.BULLET, Team.ENEMY, x, y, size, size);
        this.vy = speed;
        this.lifeMs = lifeMs;
        this.damage = damage;
        this.sprite = sprite; // 이미지 지정
    }
    
    @Override
    public void update(long dt) {
    	// 1) 먼저 이동
        super.update(dt);

        // 2) 수명 먼저 깎고 끝났으면 바로 제거
        lifeMs -= dt;
        if (lifeMs <= 0) {
            destroy();
            return;
        }

        // 3) 플레이영역이 설정되어 있으면 화면 밖 체크
        if (playArea != null) {
            double bottom = playArea.getMaxY();   // 예: 800
            double myBottom = getY() + getH();    // 구체의 아랫부분

            // 아랫부분이 화면보다 충분히 내려갔으면 제거
            if (myBottom > bottom -10) {
                destroy();
                return;
            }
        } 
    }

    @Override
    public void render(Graphics2D g) {
    	
    	// 🔹 스프라이트가 설정돼 있다면 이미지로 렌더
        if (sprite != null) {
            // 약간의 반투명 효과 주기 (알파 블렌딩)
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f));
            
            g.drawImage(sprite, (int) getX(), (int) getY(), (int) getW(), (int) getH(), null);

            g.setComposite(old);
            return;
        }
        //없다면 그냥 파란 구체
        // 반투명 파란 구체
        g.setColor(new Color(90, 200, 255, 180));
        g.fillOval((int)getX(), (int)getY(), (int)getW(), (int)getH());

        g.setColor(new Color(255, 255, 255, 220));
        g.drawOval((int)getX(), (int)getY(), (int)getW(), (int)getH());
    }

    @Override
    public void onCollision(game.core.Entity other) {
        if (other instanceof Player p) {
           p.takeDamage(damage);
            destroy();
        }
    }
}
