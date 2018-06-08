package render;

/**
 *
 * @author Texhnolyze
 */
public final class FastMath {
    
    static float[] buildSqrtTable(int w, int h) {
        float[] table = new float[w * h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                table[x + y * w] = (float) Math.sqrt(x * x + y * y);
            }
        }
        return table;
    }
    
    static final float SQRT_2 = (float) Math.sqrt(2.0);
    
    static int round(float x) {
        return x < 0.0f ? (int) (x - 0.5f) : (int) (x + 0.5f);
    }
    
    static int roundPositive(float x) {
        return (int) (x + 0.5f);
    }
    
    static int roundNegative(float x) {
        return (int) (x - 0.5f);
    }
    
    static float len(int x1, int y1, int x2, int y2) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    static float sqrt(float x) {
        return (float) Math.sqrt(x);
    }
    
    //  stolen from: https://gist.github.com/leodutra/63ca94fe86dcffee1bab
    static int min(int a, int b) {
        return a - ((a - b) & ((b - a) >> 31));
    }
    
    static int max(int a, int b) {
        return -min(-a, -b);
    }
    
}
