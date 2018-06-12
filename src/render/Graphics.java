package render;

import java.awt.image.BufferedImage;

/**
 *
 * @author Texhnolyze
 */
public interface Graphics {
    
    // Color format is:
    // most bit -> (--------RRRRRRRRGGGGGGGGBBBBBBBB) <- least bit
    
    int getWidth();
    int getHeight();
    
    void setColor(int rgb);
    default void setColor(int r, int g, int b) {
        setColor(rgb(r, g, b));
    }
    
    void plot(int x, int y);
    void plot(int x, int y, int rgb);
    
    public static final int BLACK = rgb(0, 0, 0);
    public static final int WHITE = rgb(255, 255, 255);
    
    public static int rgb(int r, int g, int b) {
        return ((r << 16) | (g << 8)) | b;
    }
    
    public static int rgb(float r, float g, float b) {
        return rgb((int) (r * 255f), (int) (g * 255f), (int) (b * 255f));
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
        public void plot(int x, int y) {
            data[x + w * y] = rgb;
        }

        @Override
        public void plot(int x, int y, int rgb) {
            data[x + w * y] = rgb;
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
