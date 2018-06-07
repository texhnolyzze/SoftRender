package render;

/**
 *
 * @author Texhnolyze
 */
public interface Graphics {
    
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
    
}
