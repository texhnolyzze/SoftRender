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
    
    void setTempColor(float r, float g, float b);
    
    float getTempColorRed();
    float getTempColorGreen();
    float getTempColorBlue();
    
}
