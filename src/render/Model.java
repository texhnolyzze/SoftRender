package render;

/**
 *
 * @author Texhnolyze
 */
public interface Model {
    
    int numFaces();
    Iterable<Face> faces();

    Iterable<Vertex> vertices();    
    
//  This method is called after the ModelInstance drawing ends
    void reset(); 
    
}
