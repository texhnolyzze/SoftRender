package render;

/**
 *
 * @author Texhnolyze
 */
public interface ModelInstance {
    
    Model getModel();
    Material getMaterial();
    
    AABB getAABB();
    
    void translateModelIntoWorldSpace();
    
}
