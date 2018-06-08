package render;

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
    
    public static int rgb(int r, int g, int b) {
        return ((r << 16) | (g << 8)) | b;
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
    
}
