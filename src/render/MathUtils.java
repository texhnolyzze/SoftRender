package render;

import static render.MathUtils.Matrix4x4.*;

/**
 *
 * @author Texhnolyze
 */
public final class MathUtils {
    
    public static float[] buildSqrtTable(int w, int h) {
        float[] table = new float[w * h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                table[x + y * w] = (float) Math.sqrt(x * x + y * y);
            }
        }
        return table;
    }
    
    public static final float SQRT_2 = (float) Math.sqrt(2.0);
    
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
    
    static void setToViewMatrix(float pos_x, float pos_y, float pos_z,
                                float dir_x, float dir_y, float dir_z,
                                float up_x, float up_y, float up_z,
                                Matrix4x4 dest) { // dest is already view matrix
        float right_x = up_y * dir_z + up_z * dir_y;
        float right_y = up_z * dir_x + up_x * dir_z;
        float right_z = up_x * dir_y + up_y * dir_x;
        float[] val = dest.val;
        val[M00] = right_x;
        val[M01] = up_x;
        val[M02] = dir_x;
        val[M10] = right_y;
        val[M11] = up_y;
        val[M12] = dir_y;
        val[M20] = right_z;
        val[M21] = up_z;
        val[M22] = dir_z;
        val[M30] = -dot(pos_x, pos_y, pos_z, right_x, right_y, right_z);
        val[M31] = -dot(pos_x, pos_y, pos_z, up_x, up_y, up_z);
        val[M32] = -dot(pos_x, pos_y, pos_z, dir_x, dir_y, dir_z);
    }
    
    static void setToProjectionMatrix(float fov_y, float aspect, 
                                      float near, float far, Matrix4x4 dest) { // dest is already projection matrix
        float[] val = dest.val;
        float f = (float) Math.tan(Math.toRadians(fov_y) / 2.0);
        val[M00] = 1f / (f * aspect);
        val[M11] = 1f / f;
        val[M22] = -(far + near) / (far - near);
        val[M23] = -(2f * far * near) / (far - near);
    }
    
    static void toViewSpace(Vector3f v, Matrix4x4 m) {
        float[] val = m.val;
        float x = v.x();
        float y = v.y();
        float z = v.z();
        v.set(
            x * val[M00] + y * val[M10] + z * val[M20] + val[M30], 
            x * val[M01] + y * val[M11] + z * val[M21] + val[M31], 
            x * val[M02] + y * val[M12] + z * val[M22] + val[M32]
        );
    }
    
    static void normalToViewSpace(Vector3f n, Matrix4x4 m) {
        float[] val = m.val;
        float x = n.x();
        float y = n.y();
        float z = n.z();
        n.set(
            x * val[M00] + y * val[M10] + z * val[M20], 
            x * val[M01] + y * val[M11] + z * val[M21], 
            x * val[M02] + y * val[M12] + z * val[M22]
        );
    }
    
    static void project(Vector3f v, Matrix4x4 m) {
        float[] val = m.val;
        float x = v.x();
        float y = v.y();
        float z = v.z();
        float w = z * val[M23];
        v.set(x * val[M00] / w, y * val[M11] / w, (val[M22] * z - 1f) / w);
    }
    
    static class Matrix4x4 { // just for internal usage
        
        static final int M00 = 0;
        static final int M01 = 1;
        static final int M02 = 2;
        static final int M03 = 3;
        static final int M10 = 4;
        static final int M11 = 5;
        static final int M12 = 6;
        static final int M13 = 7;
        static final int M20 = 8;
        static final int M21 = 9;
        static final int M22 = 10;
        static final int M23 = 11;
        static final int M30 = 12;
        static final int M31 = 13;
        static final int M32 = 14;
        static final int M33 = 15;
        
        final float[] val = new float[16];
        Matrix4x4() {val[M00] = val[M11] = val[M22] = val[M33] = 1f;}
        
    }
    
}
