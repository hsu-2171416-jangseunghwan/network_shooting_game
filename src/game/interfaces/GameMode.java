package game.interfaces;

import java.awt.Graphics2D;

import game.entity.Player;
import game.item.Item;
import game.main.Game;

public interface GameMode { //게임 모드(싱글/협동)
    void start(Game game);                        // 모드 초기화(싱글/협동)
    void update(long dt);                         // 모드별 규칙 갱신
    void render(Graphics2D g);                    // 모드별 UI
    void onItemPicked(Player picker, Item item);  // 아이템 획득 처리(공유 등)
}
