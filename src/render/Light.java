package render;

import static render.MathUtils.clamp;
import render.Vector3f.vec3;

/**
 *
 * @author Texhnolyze
 */
public abstract class Light<T> {
    
    boolean enabled = true;
    
    float r, g, b; 
    
    private Light(float r, float g, float b) {
        setRGB(r, g, b);
    }
    
    public final T setRGB(float r, float g, float b) {
        this.r = clamp(r, 0f, 1f);
        this.g = clamp(g, 0f, 1f);
        this.b = clamp(b, 0f, 1f);
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
    
    public float getRed() {
        return r;
    }
    
    public float getGreen() {
        return g;
    }
    
    public float getBlue() {
        return b;
    }
    
    public static class AmbientLight extends Light<AmbientLight> {
        public AmbientLight(float r, float g, float b) {
            super(r, g, b);
        }
    }
    
    public static class DirectionLight extends Light<DirectionLight> {
        
        vec3 dir = new vec3();

//      this vector also can represents the position of the light source, 
//      if we multiply its components by a large number
        vec3 dir_inv = new vec3(); 

        public DirectionLight(float r, float g, float b, float nx, float ny, float nz) {
            super(r, g, b);
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
            dir_inv.x = -dir.x;
            dir_inv.y = -dir.y;
            dir_inv.z = -dir.z;
            return this;
        }
        
        public float dirX() {return dir.x;}
        public float dirY() {return dir.y;}
        public float dirZ() {return dir.z;}
        
    }
    
    public static class PointLight extends Light<PointLight> {
        
        float radius;
		float radius_inv;
        float radiusSquare;
        vec3 pos = new vec3();
        
        public PointLight(float r, float g, float b, float pos_x, float pos_y, float pos_z, float radius) {
            super(r, g, b);
            setPosition(pos_x, pos_y, pos_z).setRadius(radius);
        }
        
        public final PointLight setPosition(float px, float py, float pz) {
            pos.x = px;
            pos.y = py;
            pos.z = pz;
            return this;
        }
        
        public final PointLight setRadius(float radius) {
			if (radius <= 0f)
				throw new IllegalArgumentException("radius must be > 0");
            this.radius = radius;
			this.radius_inv = 1f / radius;
            this.radiusSquare = radius * radius;
            return this;
        }
        
        public float posX() {return pos.x;}
        public float posY() {return pos.y;}
        public float posZ() {return pos.z;}
        
        public float getRadius() {
            return radius;
        }
        
    }
    
}
