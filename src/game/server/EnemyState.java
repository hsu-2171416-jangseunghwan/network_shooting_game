package game.server;

public class EnemyState {
    int id;
    double x, y;
    double speedY;
    int hp;

    long spawnTime;
    long lastFireTime;
    long fireDelay;

    MoveType moveType;
    FireType fireType;

    // 🔥 ZIGZAG 전용
    double baseX;        // 중심 X
    double zigzagTime;   // 누적 시간 (초)
    double amplitude;    // 진폭 (px)
    double frequency;    // 각속도 (rad/s)

    public EnemyState(int id, double x, double y, int hp,
                      MoveType moveType, FireType fireType) {

        this.id = id;
        this.x = x;
        this.y = y;
        this.baseX = x;

        this.hp = hp;
        this.moveType = moveType;
        this.fireType = fireType;

        this.speedY = 120;
        this.spawnTime = System.currentTimeMillis();
        this.lastFireTime = 0;
        this.fireDelay = 1500;
        int score = 100;
        // 기본값
        this.zigzagTime = 0;

        if (moveType == MoveType.ZIGZAG) {
            this.amplitude = 40;     // 싱글과 동일
            this.frequency = 2.5;    // 체감용 (≈ 1.25초 왕복)
        }
    }
}
