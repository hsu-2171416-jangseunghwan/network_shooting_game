package game.item;

import java.awt.Color;
import java.awt.Graphics2D;

import game.core.Entity;
import game.enumset.EntityType;
import game.enumset.ItemType;
import game.enumset.Team;
//추가 Pickup interface
import game.interfaces.PickupReceiver;
import game.manager.ResourceManager;
import game.status.RunStats;

/**
 * Item : 현재는 기본 형만 정의
 * - 적이 드랍하거나 스테이지에 등장하는 아이템
 * - 플레이어가 획득 시 효과 발동 (회복, 파워업, 실드 등), 
 */
public class Item extends Entity {

    private ItemType type;      // 아이템 종류
    private double fallSpeed = 150;   // 낙하 속도(px/s)
    private double lifeMs = 10000; // 아이템 생존 시간
    private double ageMs = 0; // 아이템 생존 즉시 시간
    private static final ResourceManager  rm = new ResourceManager();
    private RunStats runStats;
    
    public Item(ItemType type) {
        super(EntityType.ITEM, Team.NEUTRAL, 0, 0, 24, 24);
        this.type = type;
        this.vx = 0;
        this.vy = fallSpeed;
    }

    public ItemType getItemType() { return type; }

    public void setRunStats(RunStats stats) {
        this.runStats = stats;
    }
    
    @Override
    public void update(long dt) {
        if (!alive) return;
        double dtSec = dt / 1000.0;
        position.y += fallSpeed * dtSec;
        // 아이템 수명 증가
        ageMs += dt;

        //수명 다 하면 자동 소멸
        if (ageMs >= lifeMs) {
            destroy();
            return;
        }

        // 화면 밖으로 나가면 제거
        if (playArea != null && !playArea.intersects(getBounds())) {
            destroy();
        }
    }

    @Override
    public void render(Graphics2D g) {
        if (!alive) return;

        // 1) 먼저 이미지가 있으면 그리기
        String key = switch (type) {
            case HEAL      -> "item_heal";
            case POWER_UP  -> "item_power";
            case SHIELD    -> "item_shield";
            case SPEED     -> "item_speed";
            case FEVER     -> "item_fever";
            default        -> "item_levelup";
        };

        if (key != null) {
            java.awt.Image img = rm.getImage(key);
            if (img != null) {
                g.drawImage(img, (int)getX(), (int)getY(), (int)getW(), (int)getH(), null);
                return;
            }
        }

        // 2) 이미지가 없거나 로드 실패 시, 색상 도형으로 폴백
        Color color = switch (type) {
            case HEAL      -> new Color(80, 255, 120);
            case POWER_UP  -> new Color(120, 200, 255);
            case SHIELD    -> new Color(255, 230, 90);
            case SPEED     -> new Color(120, 220, 255);
            default        -> Color.WHITE;
        };

        g.setColor(color);
        g.fillOval((int)getX(), (int)getY(), (int)getW(), (int)getH());
        g.setColor(Color.DARK_GRAY);
        g.drawOval((int)getX(), (int)getY(), (int)getW(), (int)getH());
    }

    /** 누구와 충돌했는지 확인*/
    @Override
    public void onCollision(Entity other) {
        if(!alive) return;
        if(other.getTeam() != Team.PLAYER) return;

        if(other instanceof PickupReceiver p) {
            applyTo(p);

            destroy();
        }
    }

    /** 플레이어가 획득했을 때 효과 적용 */
    public void applyTo(PickupReceiver p) {
    	if (runStats != null) {
            runStats.onItemAcquired(type);
        }
    	
        switch (type) {
            case HEAL      -> p.heal(20);
            case POWER_UP  -> p.addPower(1);              // 무기 레벨 +1
            case SHIELD    -> p.addShield(1);             // 보호막 1 스택
            case SPEED     -> p.addSpeed(2000, 80.0);     // 2초간 +80 px/s
            case FEVER     -> p.addFever(1000);
        }
    }
}
