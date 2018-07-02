package render;

/**
 *
 * @author Texhnolyze
 */
public interface ModelInstance {
    
    Model getModel();
    Material getMaterial();
    Bitmap getTexture();
    
    AABB getAABB();
    boolean testAABB();
    
    void translateModelIntoWorldSpace();
    
}
