package game.manager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ResourceManager {
    private Map<String, BufferedImage> imageCache = new HashMap<>();

    // 예: getImage("back2") → resources/image/back2.png
    //     getImage("ui/SPEED_OFF") → resources/image/ui/SPEED_OFF.png
    public BufferedImage getImage(String key) {
        if (imageCache.containsKey(key)) return imageCache.get(key);

        String path = "/image/" + key + ".png";

        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                System.err.println("[ResourceManager] 이미지 리소스를 찾을 수 없습니다: " + path);
                return null; // 아이콘 같은 건 null이면 그냥 안 그려지게
            }
            BufferedImage img = ImageIO.read(is);
            imageCache.put(key, img);
            return img;
        } catch (Exception e) {
            System.err.println("[ResourceManager] 이미지 로드 실패: " + path);
            e.printStackTrace();
            return null;
        }
    }
}
