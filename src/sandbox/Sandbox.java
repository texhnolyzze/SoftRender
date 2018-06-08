package sandbox;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import render.Graphics;
import static render.Graphics.rgb;
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
        long t = System.nanoTime();
        r.drawTriangleGouraudShading(320, -100, 0, rgb(255, 0, 0), -100, 400, 0, rgb(0, 255, 0), 700, 500, 0, rgb(0, 0, 255));
        long dt = System.nanoTime() - t;
        System.out.println(dt);
        ImageIO.write(g.img, "jpg", new File("1.jpg"));
    }
    
}
