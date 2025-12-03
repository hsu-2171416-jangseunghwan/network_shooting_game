package game.entity;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import game.boss.BossPhase;
import game.bosseffect.BigExplosion;
import game.bosseffect.SmallExplosion;
import game.manager.EntityManager;
import game.manager.ResourceManager;

/**
 * BossSingle
 * - 스테이지3에서 등장하는 싱글 전용 보스.
 * - Enemy를 확장해서 다중 페이즈를 관리.
 * - 페이즈1~3 전환 시각, 공격 패턴 호출 등을 담당한다.
 */
public class BossSingle extends Enemy {

    private List<BossPhase> phases = new ArrayList<>();
    private int currentPhase = 0;
    private long phaseStartTime;
    private String bossName = "CORE";

    private int maxHp = 3000;
    private int hp = maxHp;
    private double beamOffsetX = -20;
    private ResourceManager rm;

    private Player player;   // ⭐ 보스 HP바 가운데 정렬을 위해 필요!
    private EntityManager entityManager;
    private boolean dying = false;
    private long deathEndTime = 0;

    public BossSingle(double x, double y, BufferedImage sprite) {
        super(x, y, sprite);
        this.hp = maxHp;
        this.speed = 80;
        setFireCoolDown(2000);
    }

    public double getBeamAnchorX() {
        return getX() + getW() / 2.0 + beamOffsetX;
    }

    public void setResourceManager(ResourceManager rm) {
        this.rm = rm;
    }

    public ResourceManager getResourceManager() {
        return rm;
    }

    @Override
    public void onCollision(game.core.Entity other) {
        if (other instanceof game.entity.Player p) {
            p.takeDamage(20);
        }
    }

    @Override
    public void takeDamage(int dmg) {
        hp -= dmg;

        double ex = getX() + getW()/2.0 - 16;
        double ey = getY() + getH()/2.0 - 16;

        BufferedImage boomSmall = rm.getImage("explosion_small");
        spawnEntity(new SmallExplosion(ex, ey, boomSmall));

        if (hp <= 0) {
            hp = 0;
            startDeathSequence();
        }
    }

    private void startDeathSequence() {
        if (!alive) return;
        alive = false;

        double cx = getX() + getW() / 2.0;
        double cy = getY() + getH() / 2.0;

        BufferedImage boom = rm.getImage("explosion_big");
        spawnEntity(new BigExplosion(cx - 40, cy - 20, boom));
        spawnEntity(new SmallExplosion(cx - 60, cy + 30, boom));
        spawnEntity(new SmallExplosion(cx + 10, cy - 40, boom));

        new Thread(() -> {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException ignored) {}
            destroy();
        }).start();
    }

    @Override
    public void move(long dt) {
        int p = getPhaseIndex();
        if (p == 1 || p == 2 || p == 3) {
            return;
        }
        super.move(dt);
    }

    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }

    public void setPhases(List<BossPhase> list) {
        this.phases = list;
        this.currentPhase = 0;
        if (!phases.isEmpty()) {
            phases.get(0).enter(this);
            phaseStartTime = System.currentTimeMillis();
        }
    }

    public int getPhaseIndex() {
        return currentPhase + 1;
    }

    public void setSpriteKeepingSize(BufferedImage img) {
        if (img == null) return;

        int targetW = (int) getW();
        int targetH = (int) getH();

        Image scaled = img.getScaledInstance(targetW, targetH, Image.SCALE_SMOOTH);
        BufferedImage out = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.drawImage(scaled, 0, 0, null);
        g2.dispose();

        setSprite(out);
    }

    public void nextPhase() {

        if (phases == null || currentPhase + 1 >= phases.size()) {
            System.out.println("[BossSingle] 마지막 페이즈라 nextPhase() 무시");
            return;
        }

        double ex = getX() + getW()/2.0 - 64;
        double ey = getY() + getH()/2.0 - 40;

        if (rm != null) {
            BufferedImage boom = rm.getImage("explosion_big");
            if (boom != null) {
                spawnEntity(new BigExplosion(ex, ey, boom));
            } else {
                spawnEntity(new BigExplosion(ex, ey));
            }
        } else {
            spawnEntity(new BigExplosion(ex, ey));
        }

        if (rm != null) {
            if (currentPhase == 0) {
                BufferedImage img = rm.getImage("Boss2");
                setSpriteKeepingSize(img);
            } else if (currentPhase == 1) {
                BufferedImage img = rm.getImage("Boss3");
                setSpriteKeepingSize(img);
            }
        }

        currentPhase++;
        phases.get(currentPhase).enter(this);
        phaseStartTime = System.currentTimeMillis();
        System.out.println("[BossSingle] ▶ 페이즈 " + (currentPhase + 1) + " 시작");
    }

    @Override
    public void update(long dt) {
        super.update(dt);

        if (phases == null || phases.isEmpty()) return;

        BossPhase p = phases.get(currentPhase);
        p.update(this, dt);

        if (p.isComplete(this)) {
            if (currentPhase + 1 < phases.size()) {
                nextPhase();
            } else {
                System.out.println("[BossSingle] 모든 페이즈 종료 - 보스 파괴");
                destroy();
            }
        }
    }

    // ===============================================================
    //   ⭐⭐ 수정된 부분: Enemy 기본 HP바 제거 + 보스 전용 HP바만 렌더 ⭐⭐
    // ===============================================================
    @Override
    public void render(Graphics2D g) {

        // ⭐ 기본 Enemy HP바를 그리지 않기 위해 super.render() 대신 스프라이트만 그림
    	g.drawImage(sprite, (int)getX(), (int)getY(), null);

        if (player == null) return;

        Rectangle area = player.getPlayArea().getBounds();
        int centerX = area.x + area.width / 2;

        int barWidth = 300;
        int barHeight = 14;
        int barX = centerX - barWidth / 2;
        int barY = 100;
        
        /*
        g.setColor(new Color(200, 200, 200, 180));
        g.drawRect(barX - 2, barY - 2, barWidth + 4, barHeight + 4);

        double ratio = hp / (double) maxHp;
        int currentWidth = (int) (barWidth * ratio);

        g.setColor(Color.RED);
        g.fillRect(barX, barY, currentWidth, barHeight);

        //String text = bossName + " - Phase " + getPhaseIndex();
        //g.setColor(Color.WHITE);

        //FontMetrics fm = g.getFontMetrics();
        //int textWidth = fm.stringWidth(text);
       // int textX = centerX - textWidth / 2;
        //int textY = barY - 8;

        //g.drawString(text, textX, textY);
        ///*/
         
    }

    @Override
    public boolean isAlive() {
        return hp > 0 && super.isAlive();
    }

    // ⭐ HP바 가운데 정렬을 위해 player 전달 필요
    public void setPlayer(Player p) {
        this.player = p;
    }
    
    
    
    
}
