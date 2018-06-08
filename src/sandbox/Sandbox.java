package sandbox;

import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import render.Graphics;
import render.Graphics.DefaultGraphics;
import render.Rasterizer3D;

/**
 *
 * @author Texhnolyze
 */
public class Sandbox {
    
    public static void main(String[] args) throws IOException {
        DefaultGraphics g = new DefaultGraphics(640, 480);
        Rasterizer3D r = new Rasterizer3D(g);
        r.drawTriangleFlatShading(543, 345, 3, 3, 4, 7567, 567, 65, 0, Graphics.WHITE);
        ImageIO.write(g.getAsImage(), "jpg", new File("1.jpg"));
    }
    
}
