package render;

/**
 *
 * @author Texhnolyze
 */
public interface Vertex {

    Vector3f pos();
    Vector3f norm();
    
    boolean lighted();
    
    float getTempRed();
    float getTempGreen();
    float getTempBlue();
    void setTempRGB(float r, float g, float b);
    
}
