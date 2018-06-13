package render;

/**
 *
 * @author Texhnolyze
 */
public interface Face {
    
    Vector3f norm();
    
    Vertex v0();
    Vertex v1();
    Vertex v2();
    
    boolean isTwoFaced();
    
    int getTempRGB();
    void setTempRGB(int rgb);
    
    default Vector3f getAVGPoint(Vector3f dest) {
        return dest.set(
            (v0().pos().x() + v1().pos().x() + v2().pos().x()) / 3f, 
            (v0().pos().y() + v1().pos().y() + v2().pos().y()) / 3f, 
            (v0().pos().z() + v1().pos().z() + v2().pos().z()) / 3f
        );
    }
    
}
