package game.manager;

import javax.sound.sampled.*;
import java.util.HashMap;

public class AudioManager {

    // ==============================
    // 🔇 시연용 전역 MUTE 스위치
    // ==============================
    private static boolean MUTE = true;   // 🔥 시연할 때 true

    public static void setMute(boolean mute) {
        MUTE = mute;
    }

    // 효과음 전용 캐시
    private static final HashMap<String, Clip> clips = new HashMap<>();

    // 단 하나의 BGM만 유지
    private Clip bgmClip;

    // --- 내부 로드 함수 ---
    private static Clip loadClip(String path) {
        try {
            var url = AudioManager.class.getResource(path);
            if (url == null) {
                System.out.println("[AudioManager] File not found: " + path);
                return null;
            }

            AudioInputStream ais = AudioSystem.getAudioInputStream(url);
            AudioFormat base = ais.getFormat();

            AudioFormat target = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    base.getSampleRate(),
                    16,
                    base.getChannels(),
                    base.getChannels() * 2,
                    base.getSampleRate(),
                    false
            );

            AudioInputStream decodedAis =
                    AudioSystem.getAudioInputStream(target, ais);

            Clip clip = AudioSystem.getClip();
            clip.open(decodedAis);
            return clip;

        } catch (Exception e) {
            System.out.println("[AudioManager] Failed to load: " + path);
            e.printStackTrace();
            return null;
        }
    }

    // --- 효과음 전용 클립 가져오기 ---
    private static Clip getClip(String name) {
        if (!clips.containsKey(name)) {
            Clip c = loadClip("/sounds/" + name);
            clips.put(name, c);
        }
        return clips.get(name);
    }

    // =====================================
    // 🔊 효과음 재생 (SFX)
    // =====================================
    public static void playSFX(String name) {
        if (MUTE) return;   // 🔇 핵심

        Clip clip = getClip(name);
        if (clip == null) return;

        if (clip.isRunning())
            clip.stop();

        clip.setFramePosition(0);
        clip.start();
    }

    // =====================================
    // 🎵 BGM (단 하나만 유지)
    // =====================================
    public void playBGM(String name) {
        if (MUTE) return;   // 🔇 핵심

        String path = "/sounds/" + name;

        try {
            if (bgmClip != null) {
                bgmClip.stop();
                bgmClip.close();
            }

            var url = AudioManager.class.getResource(path);
            if (url == null) {
                System.out.println("[AudioManager] BGM 파일을 찾을 수 없음: " + path);
                return;
            }

            AudioInputStream ais = AudioSystem.getAudioInputStream(url);
            bgmClip = AudioSystem.getClip();
            bgmClip.open(ais);

            bgmClip.setFramePosition(0);
            bgmClip.loop(Clip.LOOP_CONTINUOUSLY);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // BGM 완전 정지
    public void stopBGM() {
        if (MUTE) return;   // 🔇 핵심

        if (bgmClip != null && bgmClip.isRunning()) {
            bgmClip.stop();
        }
    }
}
