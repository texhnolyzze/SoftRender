package render;

import static render.MathUtils.clamp;

/**
 *
 * @author Texhnolyze
 */
public abstract class Light {
    
    private boolean enabled;
    
    private float ar, ag, ab; // [0..1]
    private float dr, dg, db;
    private float sr, sg, sb;
    
    public Light(float ar, float ag, float ab, 
                 float dr, float dg, float db, 
                 float sr, float sg, float sb) {
        setAmbientRGB(ar, ag, ab).setDiffuseRGB(dr, dg, db).setSpecularRGB(sr, sg, sb);
    }
    
    public final Light setAmbientRGB(float r, float g, float b) {
        this.ar = clamp(r, 0f, 1f);
        this.ag = clamp(g, 0f, 1f);
        this.ab = clamp(b, 0f, 1f);
        return this;
    } 
    
    public final Light setDiffuseRGB(float r, float g, float b) {
        this.dr = clamp(r, 0f, 1f);
        this.dg = clamp(g, 0f, 1f);
        this.db = clamp(b, 0f, 1f);
        return this;
    }
    
    public final Light setSpecularRGB(float r, float g, float b) {
        this.sr = clamp(r, 0f, 1f);
        this.sg = clamp(g, 0f, 1f);
        this.sb = clamp(b, 0f, 1f);
        return this;
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
    
    public float getDiffuseRed() {return dr;}
    public float getDiffuseGreen() {return dg;}
    public float getDiffuseBlue() {return db;}
    
    public float getSpecularRed() {return sr;}
    public float getSpecularGreen() {return sg;}
    public float getSpecularBlue() {return sb;}
    
    public static class AmbientLight extends Light {
        
        public AmbientLight(float ar, float ag, float ab, 
                            float dr, float dg, float db, 
                            float sr, float sg, float sb) {
            super(ar, ag, ab, dr, dg, db, sr, sg, sb);
        }
        
    }
    
    public static class DirectionLight extends Light {
        
        private float nx, ny, nz;

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
            float len = (float) Math.sqrt(len2);
            nx /= len;
            ny /= len;
            nz /= len;
            return this;
        }
        
        public float dirX() {return nx;}
        public float dirY() {return ny;}
        public float dirZ() {return nz;}
        
    }
    
    public static class PointLight extends Light {
        
        private float px, py, pz;
        private float kc, kl, kq; // constant, linear and quadratic attenuation coefficients

        public PointLight(float ar, float ag, float ab, 
                          float dr, float dg, float db, 
                          float sr, float sg, float sb,
                          float px, float py, float pz,
                          float kc, float kl, float kq) {
            super(ar, ag, ab, dr, dg, db, sr, sg, sb);
            setPosition(px, py, pz).setAttenuationCoefficents(kc, kl, kq);
        }
        
        
        public final PointLight setPosition(float px, float py, float pz) {
            this.px = px;
            this.py = py;
            this.pz = pz;
            return this;
        }
        
        public final PointLight setAttenuationCoefficents(float kc, float kl, float kq) {
            this.kc = clamp(kc, 0, Float.POSITIVE_INFINITY);
            this.kl = clamp(kl, 0, Float.POSITIVE_INFINITY);
            this.kq = clamp(kq, 0, Float.POSITIVE_INFINITY);
            return this;
        }
        
        public float posX() {return px;}
        public float posY() {return py;}
        public float posZ() {return pz;}
        
        public float getConstantAttenuationCoefficent() {return kc;}
        public float getLinearAttenuationCoefficent() {return kl;}
        public float getQuadraticAttenuationCoefficent() {return kq;}
        
    }
    
}
