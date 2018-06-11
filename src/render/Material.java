package render;

import static render.MathUtils.clamp;

/**
 *
 * @author Texhnolyze
 */
public class Material {
    
    private float ar, ag, ab;
    private float dr, dg, db;
    private float sr, sg, sb;
    private float shininess;
    
    public Material(float ar, float ag, float ab, 
                    float dr, float dg, float db, 
                    float sr, float sg, float sb,
                    float shininess) {
        setAmbient(ar, ag, ab).setDiffuse(dr, dg, db).setSpecular(sr, sg, sb).setShininess(shininess);
    }
    
    public float getAmbientRed() {return ar;}
    public float getAmbientGreen() {return ag;}
    public float getAmbientBlue() {return ab;}
    
    public float getDiffuseRed() {return dr;}
    public float getDiffuseGreen() {return dg;}
    public float getDiffuseBlue() {return db;}
    
    public float getSpecularRed() {return sr;}
    public float getSpecularGreen() {return sg;}
    public float getSpecularBlue() {return sb;}
    
    public float getShininess() {return shininess;}
    
    public final Material setAmbient(float r, float g, float b) {
        ar = clamp(r, 0, 1);
        ag = clamp(g, 0, 1);
        ab = clamp(b, 0, 1);
        return this;
    }
    
    public final Material setDiffuse(float r, float g, float b) {
        dr = clamp(r, 0, 1);
        dg = clamp(g, 0, 1);
        db = clamp(b, 0, 1);
        return this;
    }
    
    public final Material setSpecular(float r, float g, float b) {
        sr = clamp(r, 0, 1);
        sg = clamp(g, 0, 1);
        sb = clamp(b, 0, 1);
        return this;
    }
    
    public final Material setShininess(float shininess) {
        this.shininess = shininess;
        return this;
    }
    
}
