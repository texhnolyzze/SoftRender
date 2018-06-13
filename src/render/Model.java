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
    
//  This method is called after the ModelInstance drawing ends.
//  It is needed to translate the model back into the local space.
    void reset(); 
    
}
