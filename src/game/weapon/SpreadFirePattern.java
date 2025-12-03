package game.weapon;

import game.core.Entity;
import game.entity.Bullet;
import game.entity.Enemy;
import game.enumset.BulletType;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * 보스/적 중심에서 아래쪽으로 퍼지는 확산탄
 */
public class SpreadFirePattern implements FirePattern {

    private final int count;
    private final double startDeg;
    private final double endDeg;
    private final double speed;
    private final double offsetX;
    private final double offsetY;
    
 // 🔹 추가: 확산탄 스프라이트와 크기
    private final BufferedImage sprite;
    private final int bulletW, bulletH;//높이 ,크기

    public SpreadFirePattern(int count,
                             double startDeg,
                             double endDeg,
                             double offsetX,
                             double offsetY,
                             double speed) {
        this.count = count;
        this.startDeg = startDeg;
        this.endDeg = endDeg;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.speed = speed;
		this.sprite = null;
		this.bulletW = 0;
		this.bulletH = 0;
    }
 // 기존 생성자에 이미지/크기 파라미터만 추가
    public SpreadFirePattern(int count, double startDeg, double endDeg,
                             double offsetX, double offsetY, double speed,
                             BufferedImage sprite, int bulletW, int bulletH) {
        this.count = count;
        this.startDeg = startDeg;
        this.endDeg = endDeg;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.speed = speed;
        this.sprite = sprite;     // 확산탄 이미지
        this.bulletW = bulletW;   // 렌더링할 가로
        this.bulletH = bulletH;   // 렌더링할 세로
    }
    
    
    
    @Override
    public void fire(Enemy owner, List<Entity> outBullets) {
        // 💡 반드시 적 위치를 더해서 실제 화면 좌표로 만든다
    	  final double originX = owner.getX() + offsetX;
          final double originY = owner.getY() + offsetY;

          final double step = (count > 1) ? (endDeg - startDeg) / (count - 1) : 0;

          for (int i = 0; i < count; i++) {
        	// 화면 ‘아래’를 90도로 정한 뒤, 거기서 좌우로 벌리는 각도
              double visualDeg = startDeg + step * i;    // 네가 의도한 시각적 각도(예: -40~+40)
              double engineDeg = 90 + visualDeg;         // 수학 좌표계(오른쪽=0°)로 변환
              double rad = Math.toRadians(startDeg + step * i);

              // 🔹 확산탄 타입으로 생성
              Bullet b = new Bullet(BulletType.SPREAD, owner);
              
              

              // 🔹 위치/크기/모양 지정
              b.setPosition(originX - bulletW / 2.0, originY); // 보스 아래쪽 중앙에서 발사
              if (sprite != null) b.setSprite(sprite);         // 스프라이트 적용
              
              double bx = originX - b.getW() / 2.0;
              double by = originY - b.getH()/2.0;;
              b.setPosition(bx, by);

              // 🔹 이동/방향
              b.setSpeed(speed);
              b.setDirection(rad);

              outBullets.add(b);
        }
    }
}
