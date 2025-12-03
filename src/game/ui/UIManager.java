package game.ui;

import game.entity.Player;
import game.manager.ResourceManager;

import java.awt.*;
import game.status.RunStats;


public class UIManager {
    private HudPanel hud;
    private StageClearOverlay clearOverlay;
    private ResultPanel result;
    
    private ResourceManager resourceManager;
    private boolean resultClosed = false;
    
 // QTE 연출용 필드
    private boolean qteFlashing = false;      // QTE가 현재 진행 중인지
    private long qteFlashStart = 0L;          // 깜빡이기 시작한 시간
    
    private RunStats runStats;//카운트

    public UIManager() {}

    //public UIManager(Player p) {
       // hud = new HudPanel(p);
      //  clearOverlay = new StageClearOverlay();
       // result = new ResultPanel();
   // }
    
 
    
    public UIManager(Player p, RunStats stats, ResourceManager rm) {
        this.hud = new HudPanel(p);
        this.clearOverlay = new StageClearOverlay();
        this.runStats = stats;
        this.resourceManager = rm;

        this.result = new ResultPanel(stats, rm);   // 🔥 아이콘까지 같이 받는 생성자
    }

    public void update(long dt) {}

    // ✅ 수정된 render()
    public void render(Graphics2D g, int width, int height) {
        int leftWidth = 150;
        int rightWidth = 150;
        int centerWidth = width - leftWidth - rightWidth;

        // 🟩 HUD (기본 UI)
        if (hud != null) {
            hud.render(g, width, height); // ✅ 내부에서 Left/Center/Right 모두 그림
        }

        // 🟦 스테이지 클리어 오버레이
        if (clearOverlay != null && clearOverlay.isVisible()) {
            clearOverlay.render(g);
        }

        // 🟥 결과 패널
        if (result != null && result.isVisible()) {
            result.render(g, width, height);
        }
        
     // 예시 : UIManager.render(Graphics2D g) 안

        if (qteFlashing) {
            long elapsed = System.currentTimeMillis() - qteFlashStart; // 흐른 시간
            // 0.2초마다 색 반전
            boolean on = (elapsed / 20) % 2 == 0;                     // 짝수/홀수 구간 나누기
           
            // 깜빡임 색상 결정
            if (on) g.setColor(Color.WHITE);
            else    g.setColor(new Color(255, 200, 200));
            
         // 글씨 크게
            g.setFont(new Font("Dialog", Font.BOLD, 42));

            // 텍스트 내용
            String text = "⚡ QTE! P1:E / P2:L 연타!";
            
         // 중앙 정렬 계산
            FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            int x = (width - textWidth) / 2;
            int y = height / 2;  // 화면 정중앙

            g.drawString(text, x, y);
        }

    }

    // ✅ 결과창 관련 메서드
    public boolean isResultVisible() {
        return result != null && result.isVisible();
    }

    public boolean isResultClosed() {
        return resultClosed;
    }

    public void hideResult() {
        if (result != null) {
            result.hide();
            resultClosed = true;
        }
    }
 // QTE 시작할 때 호출
    public void startQteFlash() {
        qteFlashing = true;                         // 깜빡임 ON
        qteFlashStart = System.currentTimeMillis(); // 시작 시각 저장
    }

    // QTE 끝날 때(성공/실패 둘 다) 호출
    public void stopQteFlash() {
        qteFlashing = false; // 깜빡임 OFF
    }

       
    public void showResult(int score, int items, int combo, long time) {
        if (result != null) {
            System.out.println("[UIManager] 🪄 ResultPanel.show() 호출됨");
            result.show(score, items, combo, time);
            resultClosed = false;
        } else {
            System.out.println("[UIManager] ⚠ result가 null입니다!");
        }
    }

    public HudPanel getHud() { return hud; }
    public void showStageClear() { clearOverlay.show(); }
}
