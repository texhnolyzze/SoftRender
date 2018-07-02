package render;

import java.awt.image.BufferedImage;
import static render.MathUtils.*;

/**
 *
 * @author Texhnolyze
 */
public interface Graphics {
    
    public static final int BLACK = rgb(0, 0, 0);
    public static final int WHITE = rgb(255, 255, 255);
    
    // Color format is:
    // most bit -> (--------RRRRRRRRGGGGGGGGBBBBBBBB) <- least bit
    
    int getWidth();
    int getHeight();
    
    int getRGBInHash(int hash);
    int getRGBInXY(int x, int y);
    
    void setColor(int rgb);
    default void setColor(int r, int g, int b) {
        setColor(rgb(r, g, b));
    }
    
    void plotToHash(int hash);
    void plotToHash(int hash, int rgb);
    
    void plotToXY(int x, int y);
    void plotToXY(int x, int y, int rgb);
    
    default void modulateInHash(int hash, int rgb) {
        int h_rgb = getRGBInHash(hash);
        plotToHash(
            hash, 
            rgb(
                (red(h_rgb) * red(rgb)) / 255, 
                (green(h_rgb) * green(rgb)) / 255, 
                (blue(h_rgb) * blue(rgb)) / 255
            )
        );
    }
    
    default void modulateInXY(int x, int y, int rgb) {
        modulateInHash(hash(x, y, getWidth()), rgb);
    }
    
    public static int rgb(int r, int g, int b) {
        return ((r << 16) | (g << 8)) | b;
    }
    
    public static int rgb(float r, float g, float b) {
        return rgb(roundPositive(255f * r), roundPositive(255f * g), roundPositive(255f * b));
    }
    
    public static int red(int rgb) {
        return (rgb & 0xff0000) >> 16;
    }
    
    public static int green(int rgb) {
        return (rgb & 0xff00) >> 8;
    }
    
    public static int blue(int rgb) {
        return (rgb & 0xff);
    }
    
    public static class DefaultGraphics implements Graphics {

        private int defaultRGB;
        
        private int w;
        private int h;
        private int rgb;
        
        private int[] data;
        
        public DefaultGraphics(int w, int h) {
            this.w = w;
            this.h = h;
            data = new int[w * h];
        }
        
        public void clear() {
            for (int i = 0; i < data.length; i++) 
                data[i] = defaultRGB;
        }
        
        @Override
        public int getWidth() {
            return w;
        }

        @Override
        public int getHeight() {
            return h;
        }
        
        @Override
        public int getRGBInHash(int hash) {
            return data[hash];
        }

        @Override
        public int getRGBInXY(int x, int y) {
            return data[hash(x, y, w)];
        }

        @Override
        public void setColor(int rgb) {
            this.rgb = rgb;
        }
        
        public int getDefaultColor() {
            return defaultRGB;
        }
        
        public void setDefaultColor(int rgb) {
            this.defaultRGB = rgb;
        }

        @Override
        public void plotToHash(int hash) {
            data[hash] = rgb;
        }
        
        @Override
        public void plotToHash(int hash, int rgb) {
            data[hash] = rgb;
        }
        
        @Override
        public void plotToXY(int x, int y) {
            data[hash(x, y, w)] = rgb;
        }

        @Override
        public void plotToXY(int x, int y, int rgb) {
            data[hash(x, y, w)] = rgb;
        }
        
        public BufferedImage getAsImage() {
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            img.setRGB(0, 0, w, h, data, 0, w);
            return img;
        }
        
        public void inflict(BufferedImage img) {
            img.setRGB(0, 0, w, h, data, 0, w);
        }
        
    }
    
}
