package render;

import render.Vector3f.vec3;

/**
 *
 * @author Texhnolyze
 */
public final class MathUtils {
    
    public static final float SQRT_2 = (float) Math.sqrt(2.0);
    
    static float[] buildSqrtTable(int w, int h) {
        float[] table = new float[w * h];
        for (int y = 0, hash = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                table[hash++] = (float) Math.sqrt(x * x + y * y);
            }
        }
        return table;
    }
    
    public static int hash(int x, int y, int w) {
        return x + y * w;
    }
    
    public static int xFromHash(int hash, int w) {
        return hash % w;
    }
    
    public static int yFromHash(int hash, int w) {
        return hash / w;
    }
    
    public static float clamp(float x, float min, float max) {
        if (x < min) return min;
        else if (x > max) return max;
        return x;
    }
    
    public static int round(float x) {
        return x < 0.0f ? (int) (x - 0.5f) : (int) (x + 0.5f);
    }
    
    public static int roundPositive(float x) {
        return (int) (x + 0.5f);
    }
    
    public static int roundNegative(float x) {
        return (int) (x - 0.5f);
    }
    
    public static float dist(int x1, int y1, int x2, int y2) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    public static float dist(float x1, float y1, float z1, float x2, float y2, float z2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    public static float len(float x, float y, float z) {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }
    
    public static float len2(float x, float y, float z) {
        return x * x + y * y + z * z;
    }
    
    public static float dot(float x1, float y1, float z1, float x2, float y2, float z2) {
        return x1 * x2 + y1 * y2 + z1 * z2;
    }
    
//  stolen from: https://gist.github.com/leodutra/63ca94fe86dcffee1bab
    public static int min(int a, int b) {
        return a - ((a - b) & ((b - a) >> 31));
    }
    
    public static int max(int a, int b) {
        return -min(-a, -b);
    }
    
    public static int abs(int a) {
        return (a ^ (a >> 31)) + ((a >> 31) & 1);
    }
    
    public static boolean cubesIntersects(float x000_1, float y000_1, float z000_1, float x111_1, float y111_1, float z111_1, float x000_2, float y000_2, float z000_2, float x111_2, float y111_2, float z111_2) {
        if (!intervalsOverlap(x000_1, x111_1, x000_2, x111_2)) 
            return false;
        if (!intervalsOverlap(y000_1, y111_1, y000_2, y111_2)) 
            return false;
        return intervalsOverlap(z000_1, z111_1, z000_2, z111_2);
    }
    
    public static boolean intervalsOverlap(float start1, float end1, float start2, float end2) {
        return Math.max(start1, start2) < Math.min(end1, end2);
    }
    
    static int make_fixed_16x16(int val) {
        return val << 16;
    }
        
    static int round_fixed_16x16(int fixed) {
        return (fixed + (1 << 15)) >>> 16;
    }
    
    static boolean ray_tri_intersection(vec3 ray_pos, vec3 ray_dir, vec3 v0, vec3 v1, vec3 v2, vec3 dest) {
        float b1 = ray_pos.x - v0.x, b2 = ray_pos.y - v0.y, b3 = ray_pos.z - v0.z;
        float v0v1x = v1.x - v0.x, v0v1y = v1.y - v0.y, v0v1z = v1.z - v0.z;
        float v0v2x = v2.x - v0.x, v0v2y = v2.y - v0.y, v0v2z = v2.z - v0.z;
        float det = v0v1x * v0v2y * -ray_dir.z + -ray_dir.x * v0v1y * v0v2z + v0v2x * -ray_dir.y * v0v1z - -ray_dir.x * v0v2y * v0v1z - v0v1x * -ray_dir.y * v0v2z - v0v2x * v0v1y * -ray_dir.z;
        if (det != 0.0f) {
            det = 1f / det;
            float det1 = b1 * v0v2y * -ray_dir.z + -ray_dir.x * b2 * v0v2z + v0v2x * -ray_dir.y * b3 - -ray_dir.x * v0v2y * b3 - b1 * -ray_dir.y * v0v2z - v0v2x * b2 * -ray_dir.z;
            float u = det1 * det;
            if (u >= 0f && u <= 1f) {
                float det2 = v0v1x * b2 * -ray_dir.z + -ray_dir.x * v0v1y * b3 + b1 * -ray_dir.y * v0v1z - -ray_dir.x * b2 * v0v1z - v0v1x * -ray_dir.y * b3 - b1 * v0v1y * -ray_dir.z;
                float v = det2 * det;
                if (v >= 0f && v <= 1f && u + v <= 1f) {
                    float det3 = v0v1x * v0v2y * b3 + b1 * v0v1y * v0v2z + v0v2x * b2 * v0v1z - b1 * v0v2y * v0v1z - v0v1x * b2 * v0v2z - v0v2x * v0v1y * b3;
                    float k = det3 * det;
                    if (k >= 0f) {
                        dest.set(ray_dir.x * k, ray_dir.y * k, ray_dir.z * k);
                        return true;
                    }
                }
            }   
        }
        return false;
    }
    
    static float ns_to_ms(float ns) {
        return ns / 1000000;
    }
    
}
