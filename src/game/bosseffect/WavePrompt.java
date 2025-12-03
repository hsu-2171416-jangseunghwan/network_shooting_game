package game.bosseffect;

import game.core.Entity;
import game.enumset.EntityType;
import game.enumset.Team;
//Boss phase 전용 임시 이펙트
import java.awt.*;
//화면에 qte 띄우는 거
public class WavePrompt extends Entity {

    private long lifeMs;
    private String message;

    public WavePrompt(String message, long lifeMs) {
        // 화면 위쪽  40, 60 근처
        super(EntityType.EFFECT, Team.NEUTRAL, 40, 50, 1, 1);
        this.message = message;
        this.lifeMs = lifeMs;
    }

    @Override
    public void update(long dt) {
        lifeMs -= dt;
        if (lifeMs <= 0) {
            destroy();
        }
    }

    @Override
    public void render(Graphics2D g) {
    	 // 폰트 설정 (크고 굵게)
        g.setFont(new Font("Dialog", Font.BOLD, 28));
        FontMetrics fm = g.getFontMetrics();

        // 문자열 가로폭 계산
        int textWidth = fm.stringWidth(message);
        int textHeight = fm.getAscent();

        // 화면 중심 좌표 (네 해상도 400x800 기준)
        int screenW = 400;
        int screenH = 800;
        int x = 150+(screenW - textWidth) / 2;
        int y = (screenH / 2) + textHeight / 4;  // 세로 중앙 약간 위쪽

        // 반투명 배경 박스 (문자 크기에 맞춰)
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRoundRect(x - 20, y - textHeight, textWidth + 40, textHeight + 30, 15, 15);

        // 텍스트 그리기
        g.setColor(Color.WHITE);
        g.drawString(message, x, y);
    }
}
