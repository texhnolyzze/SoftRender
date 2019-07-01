package render;

/**
 *
 * @author Texhnolyze
 */
public enum ShadeMode {
    
//  The Phong shade model is used: color = ambient + diffuse + specular
    
    NO_SHADE, 
    FLAT,    // per face
    GOURAUD, // per vertex
    PHONG    // per pixel
    
}
