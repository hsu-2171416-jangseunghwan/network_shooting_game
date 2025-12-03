package game.entity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import game.core.Entity;
import game.enumset.BulletType;
import game.enumset.EntityType;
import game.enumset.PlayerIndex;
import game.enumset.Team;
import game.interfaces.Damageable;
import game.interfaces.Movable;
import game.interfaces.PickupReceiver;
import game.interfaces.Shootable;
import game.item.Item;
import game.manager.AudioManager;
import game.manager.EntityManager;
import game.manager.ResourceManager;
import game.stage.AbstractStage;
import game.status.FeverRegistry;
import game.weapon.Weapon;


public class Player extends Entity implements Movable, Shootable, Damageable, PickupReceiver {

    private PlayerIndex index;       // P1/P2
    private int hp, maxHp, lives;    // 생존t
    private int score, powerLevel;   // 점수/공격레벨
    private boolean shieldActive;    // 실드 상태
    private double comboMultiplier;  // 점수 배수
    private long lastShotTime;       // 마지막 발사 시각
    private long fireCooldown;       // 발사 간격(ms)
    private Weapon weapon;           // 현재 무기
    private List<Item> inventory;    // 지속 아이템
    private boolean isDowned;        // 다운 상태(코옵 가정)
    private long reviveAtMs;         // 부활 예정 시각(ms)
    private long shieldUntilMs;      // 실드 만료 시각(ms)
    private long feverEffectUntilMs = 0; // FEVER HUD용 아이콘 표시 시간

    private long lastHitTime = 0;
    private static final long HIT_FLASH_DURATION = 300; // 0.3초 동안 깜빡임
    
    private AbstractStage stage;
    
    // 스턴/회피 관련
    private boolean stunned = false; // 스턴 여부
    private long stunEndTime = 0;    // 스턴 종료 시간
    private boolean dodging = false; // 회피 무적 상태 여부
    private boolean dodgeSuccessDisplay = false; // 회피 성공 텍스트 표시 여부
    private long dodgeDisplayUntil = 0;
    private long dodgeEndTime = 0;   // 무적 종료 시간

    // 연타용
    private int dodgeTapCount = 0;
    private long dodgeTapExpire = 0;   // 이 시간 지나면 카운트 리셋
    private static final int DODGE_TAP_REQUIRED = 1;     // 몇 번 눌러야 하는지
    private static final long DODGE_TAP_WINDOW = 1200;   // 몇 ms 안에 눌러야 하는지
    
    // 피버 버프
    private long feverUntil ;
    private double scoreMul ;

    // 총 오버레이 필요!
    private static final ResourceManager rm = new ResourceManager();
    private BufferedImage gunSprite;   // 총(팔) 오버레이가 따로 있다면

    // ★ 스피드 버프
    private double speedBonus = 0.0; // px/s
    private long speedBuff = 0L;     // 스피드 버프 만료 시각(ms)

    // HUD 용 - 힐 아이콘 
    private long healEffectUntilMs = 0L;   // 힐 아이콘 표시 종료 시각(ms)

    // HUD 용 - 파워 아이콘
    private long powerBuffUntilMs = 0L;    // 파워 아이콘 표시 종료 시각(ms)
    
    

    // 발사체/이펙트 등, Player가 스폰한 엔티티를 외부가 회수할 수 있도록 버퍼링
    private final List<Entity> spawned = new ArrayList<>();

    // 추가 : 이펙트 등록용
    private transient EntityManager entityManager;

    // 기본 생성자(요구 시그니처 유지). 실제 자원/위치/무기는 with* 로 주입.
    public Player(PlayerIndex idx) {
        super(EntityType.PLAYER, Team.PLAYER, 0, 0, 1, 1); // 임시 크기 → setSprite에서 갱신
        this.index = idx;
        this.maxHp = 100; this.hp = maxHp;
        this.lives = 3;
        this.powerLevel = 1;
        this.comboMultiplier = 1.0;
        this.fireCooldown = 180; // ms (연사 속도)
        this.inventory = new ArrayList<>();
        this.speed = 220; // px/s (Entity.move와 동일 기준)
        this.alive = true;
    }

    // --- 빌더형 편의 주입 메서드 (선택) ---
    public Player withSprite(BufferedImage sprite) { setSprite(sprite); return this; }
    public Player withPlayArea(Rectangle2D area) { setPlayArea(area); return this; }
    public Player withPosition(double x, double y) { setPosition(x, y); return this; }
    public Player withCooldown(long ms) { this.fireCooldown = ms; return this; }

    // 외부 시스템(EntityManager/Game)이 스폰 결과를 회수
    public List<Entity> drainSpawned() {
        List<Entity> out = new ArrayList<>(spawned);
        spawned.clear();
        return out;
    }

    // --- HUD용 버프 상태 조회 ---
    // 스피드 버프: 실제 버프가 살아 있는 동안만 ON
    public boolean isSpeedBuffActive() {
        long now = System.currentTimeMillis();
        return speedBonus > 0 && now < speedBuff;
    }

    // 실드 버프: 기존 실드 로직 재사용
    public boolean isShieldBuffActive() {
        return hasShield();
    }

    // 힐 아이템: 먹고 나서 잠깐만 아이콘 ON
    public boolean isHealBuffActive() {
        long now = System.currentTimeMillis();
        return now < healEffectUntilMs;
    }

    // 파워 아이템: 먹고 나서 일정 시간 동안만 아이콘 ON
    public boolean isPowerBuffActive() {
        long now = System.currentTimeMillis();
        return now < powerBuffUntilMs;
    }

    // ★ EntityManager 주입
    public void setEntityManager(EntityManager em) {
        this.entityManager = em;
    }

    // 스턴 부여
    public void stun(long durationMs) {
        this.stunned = true;
        this.stunEndTime = System.currentTimeMillis() + durationMs;
    }

    public boolean isStunned() {
        return stunned;
    }

    // --- 식별/상태 조회 ---
    public PlayerIndex getIndex() { return index; }
    public boolean isDowned() { return isDowned; }
    public int getHp() { return hp; }
    public long getFireCooldown() { return fireCooldown; }
    public boolean hasShield() { return shieldActive && System.currentTimeMillis() < shieldUntilMs; }
    public int getPowerLevel() { return powerLevel; }

    // --- 리바이브 예약 ---
    public void requestRevive(long atMs) { this.reviveAtMs = atMs; this.isDowned = true; }

    // --- 이동(Movable) : vx, vy는 px/s, dt는 ms ---
    @Override public void move(long dt) { super.move(dt); } // Entity.move가 dt(ms) 처리

    // ★ 입력 기반 이동: 스피드 버프 고려
    private double currentSpeed() { return speed + Math.max(0.0, speedBonus); }

    public void moveLeft()  { this.vx = -currentSpeed(); }
    public void moveRight() { this.vx =  currentSpeed(); }
    public void moveUp()    { this.vy = -currentSpeed(); }
    public void moveDown()  { this.vy =  currentSpeed(); }
    public void stopX()     { this.vx = 0; }
    public void stopY()     { this.vy = 0; }

    // --- 발사(Shootable) ---
    @Override
    public void shoot() {
        // ▼ 무기 없을 때: BASIC 탄 직접 발사 + 쿨타임 적용
        if (weapon == null) {
            long now = System.currentTimeMillis();
            if (now - lastShotTime < fireCooldown) return; // 연사 간격
            lastShotTime = now;

            Bullet b = new Bullet(BulletType.BASIC, this);
            // 기본 무기일 때는 basic.png 사용
            b.setSprite(rm.getImage("basic"));
            b.setDirection(-Math.PI / 2);
            b.setPlayArea(this.playArea);
            spawned.add(b);
            return;
        }

        if (!weapon.canFire()) return;

        List<Bullet> bullets = weapon.createBullets(this);
        
        switch (weapon.getBulletType()) {
        case BASIC -> AudioManager.playSFX("player_shoot.wav");
        case LASER -> AudioManager.playSFX("player_shoot_laser.wav");
        case CANNON -> AudioManager.playSFX("player_shoot_cannon.wav");
        case MISSILE -> AudioManager.playSFX("Laser Impact Light_6.wav"); // 원하는 걸로 교체
        }
        
        if (bullets != null && !bullets.isEmpty()) {
            for (Bullet b : bullets) {
                b.setPlayArea(this.playArea);
                spawned.add(b);
            }
        }
        this.fireCooldown = weapon.getFireCooldown();
    }

    // --- 피격/회복(Damageable) ---
    @Override
    public void takeDamage(int dmg) {
        if (!alive) return;
        if (hasShield()) return;
        AudioManager.playSFX("player_hit.wav");
        
        hp -= Math.max(0, dmg);
        lastHitTime = System.currentTimeMillis();
        System.out.println("[Player] 피격! -" + dmg + " HP 남음: " + hp);

        if (hp <= 0) {
            hp = 0;
            destroy();
        }
    }

    public Weapon getWeapon() {
        return weapon;
    }

    public Player switchWeapon(Weapon w) {
        this.weapon = w;
        if (w != null) {
            w.setLevel(powerLevel);
            this.fireCooldown = w.getFireCooldown();
        }
        return this;
    }

    public void addScore(int amount) {
        // FEVER 배수 (없으면 1.0, 있으면 2.0)
        double feverMul = FeverRegistry.scoreMul(this);

        int inc = (int) Math.round(amount * feverMul);
        if (inc < 0) inc = 0;

        this.score += inc;
    }


    public void activateShield(long durationMs) {
        this.shieldActive = true;
        this.shieldUntilMs = System.currentTimeMillis() + Math.max(0, durationMs);
    }
    
   //회피시 스턴을 풀어주는 메서드
    public void clearStun() {
       this.stunned = false;
       this.stunEndTime = 0;
     }

    // 회피 입력 (Shift)
    public void tapDodge() {
        long now = System.currentTimeMillis();

        // 윈도우가 지났으면 새로 시작
        if (now > dodgeTapExpire) {
            dodgeTapCount = 0;
            System.out.println("[DODGE] window expired → reset count=0");
        }

        dodgeTapCount++;
        dodgeTapExpire = now + DODGE_TAP_WINDOW;
        
        System.out.println("[Player] tapDodge pressed, count=" + dodgeTapCount
                + " (newExpire=" + dodgeTapExpire + ")");
        // 조건 만족하면 실제 회피 발동
        if (dodgeTapCount >= DODGE_TAP_REQUIRED && !dodging) {
            dodging = true;
            dodgeEndTime = now + 1000;  // 1초간 무적
            dodgeTapCount = 0;
            System.out.println("[Player] DODGE START (dodging=true)");
        }
    }

    public boolean isDodging() {
        return dodging;
    }

    // 회피 성공했을때 부를 메서드
    public void triggerDodgeSuccessDisplay() {
        dodgeSuccessDisplay = true;
        dodgeDisplayUntil = System.currentTimeMillis() + 3000;
    }

    public boolean isShowingDodgeSuccess() {
        return dodgeSuccessDisplay;
    }

    public boolean shouldShowDodgeText() {
        if (dodgeSuccessDisplay && System.currentTimeMillis() <= dodgeDisplayUntil)
            return true;
        dodgeSuccessDisplay = false;
        return false;
    }

    // --- 프레임 갱신 ---
    @Override
    public void update(long dt) {
        if (!alive) return;

        long now = System.currentTimeMillis();

        // 회피 상태 해제
        if (dodging && now >= dodgeEndTime) {
            dodging = false;
        }

        // 회피 성공 텍스트 해제
        if (dodgeSuccessDisplay && now >= dodgeDisplayUntil) {
            dodgeSuccessDisplay = false;
        }

        // 스턴 처리
        if (stunned) {
            if (now >= stunEndTime) {
                stunned = false;
            } else {
                // 스턴 중엔 아예 움직임/공격 안 하게 여기서 return
                return;
            }
        }

        // 다운 상태면 이동/발사 불가
        if (!isDowned) {
            super.update(dt); // = move(dt) 호출
        }

        // 실드 만료 체크
        if (shieldActive && now >= shieldUntilMs) {
            shieldActive = false;
        }

        // 리바이브 시간 도달 시 부활
        if (isDowned && reviveAtMs > 0 && now >= reviveAtMs) {
            isDowned = false;
            reviveAtMs = 0;
            hp = Math.max(maxHp / 2, 1);
        }

        // 스피드 버프 만료
        if (speedBonus > 0 && now >= speedBuff) {
            speedBonus = 0.0;
        }
        // healEffectUntilMs / powerBuffUntilMs 는
        // isXXXBuffActive() 에서 시간 비교만 하므로
        // 따로 0으로 리셋할 필요는 없음.
    }

    // --- 렌더 : 기본 스프라이트 + 간단 HP바 ---
    @Override
    public void render(Graphics2D g) {
        if (!alive) return;

        long now = System.currentTimeMillis();
        boolean flashing = (now - lastHitTime < HIT_FLASH_DURATION);
        boolean flashVisible = !flashing || ((now / 100) % 2 == 0);

        if (flashVisible) {
            super.render(g);
        }

        // HP bar
        final int barW = (int) getW();
        final int barH = 6;
        final int bx = (int) getX();
        final int by = (int) (getY() + getH() + 8);

        g.setColor(new Color(0, 0, 0, 120));
        g.fillRect(bx - 1, by - 1, barW + 2, barH + 2);
        float ratio = Math.max(0f, Math.min(1f, hp / (float) maxHp));
        g.setColor(new Color(60, 200, 90));
        g.fillRect(bx, by, (int) (barW * ratio), barH);
        g.setColor(Color.DARK_GRAY);
        g.drawRect(bx - 1, by - 1, barW + 2, barH + 2);
    }

    // ───────────────────────────────
    // PickupReceiver 구현부 (아이템 효과)
    // ───────────────────────────────
    @Override
    public void addPower(int levels) {
        if (levels <= 0) return;
        powerLevel += levels;

        // 무기 연동
        if (weapon != null) {
            for (int i = 0; i < levels; i++) weapon.levelUp();
            this.fireCooldown = weapon.getFireCooldown();
        }

        // 총 스프라이트 및 HUD 파워 아이콘 둘 다 갱신
        updateGunSpriteByLevel();
        powerBuffUntilMs = System.currentTimeMillis() + 3000; // 3초 동안 POWER 아이콘 ON

        System.out.println("[Player] POWER UP! level=" + powerLevel);
    }

    // 총 이미지 변경 메소드
    private void updateGunSpriteByLevel() {
        int lv = Math.max(1, Math.min(powerLevel, 6));
        try {
            this.gunSprite = rm.getImage("gun_lv" + lv);
        } catch (RuntimeException ignore) {
            try {
                this.setSprite(rm.getImage("player_lv" + lv));
            } catch (RuntimeException ignore2) {
                // 리소스 없으면 그냥 넘어감
            }
        }
    }

    @Override
    public void addShield(int stacks) {
        long dur = 3000; // 3초
        activateShield(dur);

        // 실드 이펙트 생성
        if (entityManager != null) {
            entityManager.add(new ShieldEffect(this, dur));
        }

        System.out.println("[Player] SHIELD ON (" + dur + "ms)");
    }

    @Override
    public void addSpeed(long durationMs, double bonusPxPerSec) {
        if (durationMs <= 0 || bonusPxPerSec <= 0) return;
        speedBonus = Math.max(speedBonus, bonusPxPerSec);
        speedBuff = System.currentTimeMillis() + durationMs;
        System.out.println("[Player] SPEED UP +" + bonusPxPerSec + " for " + durationMs + "ms");
    }

    @Override
    public void heal(int amount) {
        if (!alive) return;
        hp = Math.min(maxHp, hp + Math.max(0, amount));
        System.out.println("[Player] HEAL +" + amount + " → " + hp + "/" + maxHp);

        // HUD에 1초 동안 HEAL 아이콘 ON
        healEffectUntilMs = System.currentTimeMillis() + 1000;
    }

    // --- 점수/아이템 관련 Getter ---
    public int getScore() {
        return score;
    }

    public int getCollectedCount() {
        return inventory.size();
    }

    public boolean hasItem(String itemName) {
        for (Item item : inventory) {
            if (item.getType().name().equalsIgnoreCase(itemName)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public void addFever(int durationMs) {
        FeverRegistry.startScoreFever(this, durationMs);
        System.out.println("[Player] FEVER MODE! +" + durationMs + "ms");
    }




    // UI용 플레이영역(Rectangle로 반환)
    public Rectangle getPlayArea() {
        if (this.playArea instanceof Rectangle) {
            return (Rectangle) this.playArea;
        } else {
            return new Rectangle(
                    (int) this.playArea.getX(),
                    (int) this.playArea.getY(),
                    (int) this.playArea.getWidth(),
                    (int) this.playArea.getHeight()
            );
        }
    }
    
    public void setStage(AbstractStage stage) {
        this.stage = stage;
    }

    public AbstractStage getStage() {
        return stage;
    }
    
    public void setNetworkPosition(double x, double y) {
    	this.setPosition(x, y);
    }
}
