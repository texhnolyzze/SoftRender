package render;

import static render.MathUtils.clamp;

/**
 *
 * @author Texhnolyze
 */
public interface Material {
    
//  Materials table from http://devernay.free.fr/cours/opengl/materials.html
    public static final Material 
        GOLD            = new BaseMaterial(0.24725f, 0.1995f, 0.0745f, 0.75164f, 0.60648f, 0.22648f, 0.628281f, 0.555802f, 0.366065f, 0.4f),
        EMERALD         = new BaseMaterial(0.0215f, 0.1745f, 0.0215f, 0.07568f, 0.61424f, 0.07568f, 0.633f, 0.727811f, 0.633f, 0.6f),
        JADE            = new BaseMaterial(0.135f, 0.2225f, 0.1575f, 0.54f, 0.89f, 0.63f, 0.316228f, 0.316228f, 0.316228f, 0.1f),
        OBSIDIAN        = new BaseMaterial(0.05375f, 0.05f, 0.06625f, 0.18275f, 0.17f, 0.22525f, 0.332741f, 0.328634f, 0.346435f, 0.3f),
        PEARL           = new BaseMaterial(0.25f, 0.20725f, 0.20725f, 1f, 0.829f, 0.829f, 0.296648f, 0.296648f, 0.296648f, 0.088f),
        RUBY            = new BaseMaterial(0.1745f, 0.01175f, 0.01175f, 0.61424f, 0.04136f, 0.04136f, 0.727811f, 0.626959f, 0.626959f, 0.6f),
        TURQUOISE       = new BaseMaterial(0.1f, 0.18725f, 0.1745f, 0.396f, 0.74151f, 0.69102f, 0.297254f, 0.30829f, 0.306678f, 0.1f),
        BRASS           = new BaseMaterial(0.329412f, 0.223529f, 0.027451f, 0.780392f, 0.568627f, 0.113725f, 0.992157f, 0.941176f, 0.807843f, 0.21794872f),
        BRONZE          = new BaseMaterial(0.2125f, 0.1275f, 0.054f, 0.714f, 0.4284f, 0.18144f, 0.393548f, 0.271906f, 0.166721f, 0.2f),
        CHROME          = new BaseMaterial(0.25f, 0.25f, 0.25f, 0.4f, 0.4f, 0.4f, 0.774597f, 0.774597f, 0.774597f, 0.6f),
        COPPER          = new BaseMaterial(0.19125f, 0.0735f, 0.0225f, 0.7038f, 0.27048f, 0.0828f, 0.256777f, 0.137622f, 0.086014f, 0.1f),
        SILVER          = new BaseMaterial(0.19225f, 0.19225f, 0.19225f, 0.50754f, 0.50754f, 0.50754f, 0.508273f, 0.508273f, 0.508273f, 0.4f),
        BLACK_PLASTIC   = new BaseMaterial(0.0f, 0.0f, 0.0f, 0.01f, 0.01f, 0.01f, 0.50f, 0.50f, 0.50f, .25f),
        CYAN_PLASTIC    = new BaseMaterial(0.0f, 0.1f, 0.06f, 0.0f, 0.50980392f, 0.50980392f, 0.50196078f, 0.50196078f, 0.50196078f, .25f),
        GREEN_PLASTIC   = new BaseMaterial(0.0f, 0.0f, 0.0f, 0.1f, 0.35f, 0.1f, 0.45f, 0.55f, 0.45f, .25f),
        RED_PLASTIC     = new BaseMaterial(0.0f, 0.0f, 0.0f, 0.5f, 0.0f, 0.0f, 0.7f, 0.6f, 0.6f, .25f),
        WHITE_PLASTIC   = new BaseMaterial(0.0f, 0.0f, 0.0f, 0.55f, 0.55f, 0.55f, 0.70f, 0.70f, 0.70f, .25f),
        YELLOW_PLASTIC  = new BaseMaterial(0.0f, 0.0f, 0.0f, 0.5f, 0.5f, 0.0f, 0.60f, 0.60f, 0.50f, .25f),
        BLACK_RUBBER    = new BaseMaterial(0.02f, 0.02f, 0.02f, 0.01f, 0.01f, 0.01f, 0.4f, 0.4f, 0.4f, .078125f),
        CYAN_RUBBER     = new BaseMaterial(0.0f, 0.05f, 0.05f, 0.4f, 0.5f, 0.5f, 0.04f, 0.7f, 0.7f, .078125f),
        GREEN_RUBBER    = new BaseMaterial(0.0f, 0.05f, 0.0f, 0.4f, 0.5f, 0.4f, 0.04f, 0.7f, 0.04f, .078125f),
        RED_RUBBER      = new BaseMaterial(0.05f, 0.0f, 0.0f, 0.5f, 0.4f, 0.4f, 0.7f, 0.04f, 0.04f, .078125f),
        WHITE_RUBBER    = new BaseMaterial(0.05f, 0.05f, 0.05f, 0.5f, 0.5f, 0.5f, 0.7f, 0.7f, 0.7f, .078125f),
        YELLOW_RUBBER   = new BaseMaterial(0.05f, 0.05f, 0.0f, 0.5f, 0.5f, 0.4f, 0.7f, 0.7f, 0.04f, .078125f);
    
    float getAmbientRed();
    float getAmbientGreen();
    float getAmbientBlue();
    
    float getDiffuseRed();
    float getDiffuseGreen();
    float getDiffuseBlue();
    
    float getSpecularRed();
    float getSpecularGreen();
    float getSpecularBlue();
    
    float getShininess();
    
    public static class BaseMaterial implements Material {
        
        float ar, ag, ab;
        float dr, dg, db;
        float sr, sg, sb;
        float shininess;
    
        public BaseMaterial(float ar, float ag, float ab, 
                            float dr, float dg, float db, 
                            float sr, float sg, float sb,
                            float shininess) {
            setAmbient(ar, ag, ab).setDiffuse(dr, dg, db).setSpecular(sr, sg, sb).setShininess(shininess);
        }
        
        @Override public float getAmbientRed() {return ar;}
        @Override public float getAmbientGreen() {return ag;}
        @Override public float getAmbientBlue() {return ab;}

        @Override public float getDiffuseRed() {return dr;}
        @Override public float getDiffuseGreen() {return dg;}
        @Override public float getDiffuseBlue() {return db;}

        @Override public float getSpecularRed() {return sr;}
        @Override public float getSpecularGreen() {return sg;}
        @Override public float getSpecularBlue() {return sb;}

        @Override public float getShininess() {return shininess;}

        public final BaseMaterial setAmbient(float r, float g, float b) {
            ar = clamp(r, 0, 1);
            ag = clamp(g, 0, 1);
            ab = clamp(b, 0, 1);
            return this;
        }

        public final BaseMaterial setDiffuse(float r, float g, float b) {
            dr = clamp(r, 0, 1);
            dg = clamp(g, 0, 1);
            db = clamp(b, 0, 1);
            return this;
        }

        public final BaseMaterial setSpecular(float r, float g, float b) {
            sr = clamp(r, 0, 1);
            sg = clamp(g, 0, 1);
            sb = clamp(b, 0, 1);
            return this;
        }

        public final BaseMaterial setShininess(float shininess) {
            this.shininess = shininess;
            return this;
        }
    }
    
    
    
}
