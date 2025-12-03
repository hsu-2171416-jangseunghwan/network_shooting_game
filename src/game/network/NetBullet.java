package game.network;

public class NetBullet {
    public int id;
    public double x, y;
    public int ownerId;

    public NetBullet(int id, double x, double y, int ownerId) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.ownerId = ownerId;
    }
}