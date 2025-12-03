package game.item;

import game.entity.Enemy;
import game.enumset.ItemType;
import java.util.Random;

/**
 * ItemDropTable
 * - Enemy가 파괴될 때 어떤 아이템을 드랍할지 결정, 현재는 임시작성
 */
public class ItemDropTable {
    private final Random rand = new Random();
    
    private ItemType getRandomItemType() {
        int n = rand.nextInt(8);
        return switch (n) {
            case 0 -> ItemType.HEAL;
            case 1 -> ItemType.POWER_UP;
            case 2 -> ItemType.SHIELD;
            case 3 -> ItemType.SPEED;
            case 4 -> ItemType.FEVER;
            case 5 -> ItemType.CANNON;
            case 6 -> ItemType.LASER;
            default -> ItemType.MISSILE;
        };
    }

    /** 적이 죽을 때 호출됨 */
    public Item tryDrop(Enemy source) {
        double r = rand.nextDouble();
        if (r < 1) { // 20% 확률 // 일단 100%
        	ItemType dropType = getRandomItemType();
            Item drop = new Item(dropType);
            drop.setPosition(source.getX(), source.getY());
            drop.setPlayArea(source.getPlayArea());
            System.out.println("[ItemDropTable] Heal item dropped!");
            return drop;
        }
        return null;
    }
    
}
