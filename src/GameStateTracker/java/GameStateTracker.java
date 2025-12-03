package GameStateTracker.java;

public class GameStateTracker {
    private static boolean paused = false;

    public static void setPaused(boolean p) {
        paused = p;
    }

    public static boolean isPaused() {
        return paused;
    }
}

