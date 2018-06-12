package render;

import static render.MathUtils.clamp;
import render.Vector3f.vec3;

/**
 *
 * @author Texhnolyze
 */
public abstract class BaseLight<T> {
    
    boolean enabled = true;
    
    float ar, ag, ab; // ambient light intensity, base for all lights.
    
    private BaseLight(float ar, float ag, float ab) {
        setAmbientRGB(ar, ag, ab);
    }
    
    public final T setAmbientRGB(float r, float g, float b) {
        this.ar = clamp(r, 0f, 1f);
        this.ag = clamp(g, 0f, 1f);
        this.ab = clamp(b, 0f, 1f);
        return (T) this;
    } 
    
    public void enable() {
        enabled = true;
    }
    
    public void disable() {
        enabled = false;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public float getAmbientRed() {return ar;}
    public float getAmbientGreen() {return ag;}
    public float getAmbientBlue() {return ab;}
    
    public static class AmbientLight extends BaseLight<AmbientLight> {
        public AmbientLight(float r, float g, float b) {
            super(r, g, b);
        }
    }
    
    private static class NotAmbientLight<T> extends BaseLight<T> {
        
        float dr, dg, db; // diffuse intensity
        float sr, sg, sb; // specular intensity
        
        public NotAmbientLight(float ar, float ag, float ab,
                               float dr, float dg, float db,
                               float sr, float sg, float sb) {
            super(ar, ag, ab);
            setDiffuseRGB(dr, dg, db);
            setSpecularRGB(sr, sg, sb);
        }
        
        public final T setDiffuseRGB(float r, float g, float b) {
            this.dr = clamp(r, 0f, 1f);
            this.dg = clamp(g, 0f, 1f);
            this.db = clamp(b, 0f, 1f);
            return (T) this;
        }
    
        public final T setSpecularRGB(float r, float g, float b) {
            this.sr = clamp(r, 0f, 1f);
            this.sg = clamp(g, 0f, 1f);
            this.sb = clamp(b, 0f, 1f);
            return (T) this;
        }
        
        public float getDiffuseRed() {return dr;}
        public float getDiffuseGreen() {return dg;}
        public float getDiffuseBlue() {return db;}

        public float getSpecularRed() {return sr;}
        public float getSpecularGreen() {return sg;}
        public float getSpecularBlue() {return sb;}
        
    }
    
    public static class DirectionLight extends NotAmbientLight<DirectionLight> {
        
        vec3 dir = new vec3();

        public DirectionLight(float ar, float ag, float ab, 
                              float dr, float dg, float db, 
                              float sr, float sg, float sb,
                              float nx, float ny, float nz) {
            super(ar, ag, ab, dr, dg, db, sr, sg, sb);
            setDirection(nx, ny, nz);
        }
        
        public final DirectionLight setDirection(float nx, float ny, float nz) {
            float len2 = MathUtils.len2(nx, ny, nz);
            if (len2 == 0f)
                throw new IllegalArgumentException();
            float len_inv = (float) (1.0 / Math.sqrt(len2));
            dir.x = nx * len_inv;
            dir.y = ny * len_inv;
            dir.z = nz * len_inv;
            return this;
        }
        
        public float dirX() {return dir.x;}
        public float dirY() {return dir.y;}
        public float dirZ() {return dir.z;}
        
    }
    
    public static class PointLight extends NotAmbientLight<PointLight> {
        
        vec3 pos = new vec3();
        float kc, kl, kq; // constant, linear and quadratic attenuation coefficients

        public PointLight(float ar, float ag, float ab, 
                          float dr, float dg, float db, 
                          float sr, float sg, float sb,
                          float px, float py, float pz,
                          float kc, float kl, float kq) {
            super(ar, ag, ab, dr, dg, db, sr, sg, sb);
            setPosition(px, py, pz).setAttenuationCoefficents(kc, kl, kq);
        }
        
        public final PointLight setPosition(float px, float py, float pz) {
            pos.x = px;
            pos.y = py;
            pos.z = pz;
            return this;
        }
        
        public final PointLight setAttenuationCoefficents(float kc, float kl, float kq) {
            this.kc = clamp(kc, 0, Float.POSITIVE_INFINITY);
            this.kl = clamp(kl, 0, Float.POSITIVE_INFINITY);
            this.kq = clamp(kq, 0, Float.POSITIVE_INFINITY);
            return this;
        }
        
        public float posX() {return pos.x;}
        public float posY() {return pos.y;}
        public float posZ() {return pos.z;}
        
        public float getConstantAttenuationCoefficent() {return kc;}
        public float getLinearAttenuationCoefficent() {return kl;}
        public float getQuadraticAttenuationCoefficent() {return kq;}
        
    }
    
}
