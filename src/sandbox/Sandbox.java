package sandbox;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import render.Graphics;
import render.Rasterizer3D;

/**
 *
 * @author Texhnolyze
 */
public class Sandbox {
    
    static class G implements Graphics {

        private int rgb;
        BufferedImage img = new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB);
        
        @Override
        public int getWidth() {
            return img.getWidth();
        }

        @Override
        public int getHeight() {
            return img.getHeight();
        }

        @Override
        public void setColor(int rgb) {
            this.rgb = rgb;
        }

        @Override
        public void plot(int x, int y) {
            img.setRGB(x, y, rgb);
        }

        @Override
        public void plot(int x, int y, int rgb) {
            img.setRGB(x, y, rgb);
        }
        
    }
    
    public static void main(String[] args) throws IOException {
        G g = new G();
        Rasterizer3D r = new Rasterizer3D(g);
        r.drawTriangleFlatShading(-1000, 0, 5, -1000, 300, 152, 543, 123, 0, 16581000);
        ImageIO.write(g.img, "jpg", new File("1.jpg"));
    }
    
}
