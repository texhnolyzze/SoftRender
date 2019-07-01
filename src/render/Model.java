package render;

/**
 *
 * @author Texhnolyze
 */
public interface Model {
    
    int numFaces();
    Iterable<Face> faces();

    int numVertices();
    Iterable<Vertex> vertices();    
    
    boolean dirty();
    void markAsDirty();
    
//  This method is called if model was marked as dirty.
    void reset(); 
    
}
