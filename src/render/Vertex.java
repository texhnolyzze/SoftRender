package render;

/**
 *
 * @author Texhnolyze
 */
public interface Vertex {

    Vector3f pos();
    Vector3f norm();
    
    boolean lighted();
    void markAsLighted();
    void markAsNotLighted();
    
    void setTempRGB(int rgb);
    int getTempRGB();
    
}
