package render;

/**
 *
 * @author Texhnolyze
 */
public final class MathUtils {
    
    public static final float SQRT_2 = (float) Math.sqrt(2.0);
    
    static float[] buildSqrtTable(int w, int h) {
        float[] table = new float[w * h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                table[x + y * w] = (float) Math.sqrt(x * x + y * y);
            }
        }
        return table;
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
    
}
