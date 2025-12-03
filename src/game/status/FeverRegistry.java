package game.status;

import java.util.Map;
import java.util.WeakHashMap;
import game.entity.Player;

public final class FeverRegistry {
    private static final class State {
        long untilMs = 0L;
        double scoreMul = 2.0; // 점수 2배
    }
    private static final Map<Player, State> MAP = new WeakHashMap<>();
    private static State S(Player p){ return MAP.computeIfAbsent(p, k -> new State()); }

    public static void startScoreFever(Player p, long durationMs){
        S(p).untilMs = System.currentTimeMillis() + Math.max(0, durationMs);
    }
    public static boolean isOn(Player p){
        return System.currentTimeMillis() < S(p).untilMs;
    }
    public static double scoreMul(Player p){
        return isOn(p) ? S(p).scoreMul : 1.0;
    }
}
