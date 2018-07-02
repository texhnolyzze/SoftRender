package render;

/**
 *
 * @author Texhnolyze
 */
public interface Vertex {

    Vector4f pos();
    Vector4f norm();
    
    boolean lighted();
    
    float getTempRed();
    float getTempGreen();
    float getTempBlue();
    void setTempRGB(float r, float g, float b);
    
}
