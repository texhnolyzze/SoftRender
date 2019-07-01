package render;

import render.Vector3f.vec3;

/**
 *
 * @author Texhnolyze
 */
public interface Face {
    
    static final vec3 TEMP_NORM = new vec3();
    static final vec3 TEMP_MEDIPOINT = new vec3();
    
    default Vector3f norm() {
        float x02 = vertex3().pos().x() - vertex1().pos().x();
        float y02 = vertex3().pos().y() - vertex1().pos().y();
        float z02 = vertex3().pos().z() - vertex1().pos().z();
        float x01 = vertex2().pos().x() - vertex1().pos().x();
        float y01 = vertex2().pos().y() - vertex1().pos().y();
        float z01 = vertex2().pos().z() - vertex1().pos().z();
        return TEMP_NORM.set(
            y01 * z02 - z01 * y02,
            z01 * x02 - x01 * z02,
            x01 * y02 - y01 * x02
        ).normalize();
    }    
    
    Vertex vertex1();
    Vertex vertex2();
    Vertex vertex3();
    
//-----------Texture coordinates-------------
    float u1();
    float v1();
    
    float u2();
    float v2();
    
    float u3();
    float v3();
//-------------------------------------------
    
    boolean isTwoFaced();
    
    float getTempRed();
    float getTempGreen();
    float getTempBlue();
    void setTempRGB(float r, float g, float b);
    
    default Vector3f getMediPoint() {
        return TEMP_MEDIPOINT.set((vertex1().pos().x() + vertex2().pos().x() + vertex3().pos().x()) / 3f, 
            (vertex1().pos().y() + vertex2().pos().y() + vertex3().pos().y()) / 3f, 
            (vertex1().pos().z() + vertex2().pos().z() + vertex3().pos().z()) / 3f
        );
    }
    
}
