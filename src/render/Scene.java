package render;

/**
 *
 * @author Texhnolyze
 */
public interface Scene {
    
    Iterable<ModelInstance> getStaticObjects();
    Iterable<ModelInstance> getDynamicObjects();
    
    Iterable<Light> getLightSources();
    
}
