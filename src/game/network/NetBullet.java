package game.network;

public class NetBullet {
    public int id;
    public double x, y;
    public String owner; // "1", "2", "enemy"

    public NetBullet(int id, double x, double y, String owner) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.owner = owner;
    }

    public boolean isEnemyBullet() {
        return "enemy".equals(owner);
    }

    public int getPlayerId() {
        try {
            return Integer.parseInt(owner);
        } catch (Exception e) {
            return -1;
        }
    }
}
