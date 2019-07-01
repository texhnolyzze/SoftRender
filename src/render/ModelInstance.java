package render;

/**
 *
 * @author Texhnolyze
 */
public interface ModelInstance {
    
    Model getModel();
    
    ShadeMode getShadeMode();
    Material getMaterial();
    
    boolean texture();
    Bitmap getTexture();
    
    boolean testAABB();
    AABB getAABB();

    boolean isShadowCaster();
    boolean isShadowReceiver();
    
    void translateModelIntoWorldSpace();
    
}
