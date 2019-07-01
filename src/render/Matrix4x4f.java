package render;

import static render.MathUtils.dot;
import render.Vector3f.vec3;


/**
 *
 * @author Texhnolyze
 */
public interface Matrix4x4f {
    
    public static final int M00 = 0;
    public static final int M01 = 1;
    public static final int M02 = 2;
    public static final int M03 = 3;
    public static final int M10 = 4;
    public static final int M11 = 5;
    public static final int M12 = 6;
    public static final int M13 = 7;
    public static final int M20 = 8;
    public static final int M21 = 9;
    public static final int M22 = 10;
    public static final int M23 = 11;
    public static final int M30 = 12;
    public static final int M31 = 13;
    public static final int M32 = 14;
    public static final int M33 = 15;
    
//  index is the of the constants listed above
    float val(int index);
    Matrix4x4f set(int index, float val);
    
    static class mat4 implements Matrix4x4f { // just for internal usage

        private static final vec3 temp = new vec3();
        
        final float[] values = new float[16];
        mat4() {values[M00] = values[M11] = values[M22] = values[M33] = 1f;}
        
        @Override
        public float val(int index) {
            return values[index];
        }
        
        @Override
        public mat4 set(int index, float val) {
            values[index] = val;
            return this;
        }
        
        void toIdentity() {
            values[M00] = values[M11] = values[M22] = values[M33] = 1f;
            values[M01] = values[M02] = values[M03] = 0f;
            values[M10] = values[M12] = values[M13] = 0f;
            values[M20] = values[M21] = values[M23] = 0f;
            values[M30] = values[M31] = values[M32] = 0f;
        }
        
        void setToViewMatrix(vec3 pos, vec3 dir, vec3 up) { 
            up.cross(dir, temp); // "left" vector
            values[M00] = temp.x;
            values[M01] = up.x;
            values[M02] = dir.x;
            values[M10] = temp.y;
            values[M11] = up.y;
            values[M12] = dir.y;
            values[M20] = temp.z;
            values[M21] = up.z;
            values[M22] = dir.z;
            values[M30] = -dot(pos.x, pos.y, pos.z, temp.x, temp.y, temp.z);
            values[M31] = -dot(pos.x, pos.y, pos.z, up.x, up.y, up.z);
            values[M32] = -dot(pos.x, pos.y, pos.z, dir.x, dir.y, dir.z);
        }
    
        void setToProjectionMatrix(float fov_y_rad, float aspect, 
                                          float near, float far) {
            float f = (float) (1f / (aspect * Math.tan(fov_y_rad * 0.5f)));
            values[M00] = f;
            values[M11] = aspect * f;
        }

        void toViewSpace(Vector3f v) {
            v.set(
                v.x() * values[M00] + v.y() * values[M10] + v.z() * values[M20] + values[M30], 
                v.x() * values[M01] + v.y() * values[M11] + v.z() * values[M21] + values[M31], 
                v.x() * values[M02] + v.y() * values[M12] + v.z() * values[M22] + values[M32]
            );
        }

        void normalToViewSpace(Vector3f n) {
            n.set(
                n.x() * values[M00] + n.y() * values[M10] + n.z() * values[M20], 
                n.x() * values[M01] + n.y() * values[M11] + n.z() * values[M21], 
                n.x() * values[M02] + n.y() * values[M12] + n.z() * values[M22]
            );
        }

        void project(Vector3f v) {
            float w_inv = -1f / v.z();
            v.set(
                v.x() * values[M00] * w_inv, 
                v.y() * values[M11] * w_inv,
                v.z()
            );
        }
        
        void setTranslation(float dx, float dy, float dz) {
            values[M30] = dx;
            values[M31] = dy;
            values[M32] = dz;
        }
        
        // axis is unit vector
        void setToRotation(vec3 axis, float angle) {
            setToRotation(axis.x, axis.y, axis.z, angle);
        }
        
        void setToRotation(float axis_x, float axis_y, float axis_z, float angle) {
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            float one_sub_cos = 1f - cos;
            values[M00] = cos + one_sub_cos * axis_x * axis_x;
            values[M11] = cos + one_sub_cos * axis_y * axis_y;
            values[M22] = cos + one_sub_cos * axis_z * axis_z;
            values[M10] = one_sub_cos * axis_y * axis_x + sin * axis_z;
            values[M01] = one_sub_cos * axis_y * axis_x - sin * axis_z;
            values[M20] = one_sub_cos * axis_z * axis_x - sin * axis_y;
            values[M02] = one_sub_cos * axis_z * axis_x + sin * axis_y;
            values[M21] = one_sub_cos * axis_z * axis_y + sin * axis_x;
            values[M12] = one_sub_cos * axis_z * axis_y - sin * axis_x;
        }
        
        void setToRotationMatrix(vec3 axis, vec3 axis_pos, float angle) {
            setToRotationMatrix(axis.x, axis.y, axis.z, axis_pos.x, axis_pos.y, axis_pos.z, angle);
        }
        
        void setToRotationMatrix(float axis_x, float axis_y, float axis_z, float axis_pos_x, float axis_pos_y, float axis_pos_z, float angle) {
            setToRotation(axis_x, axis_y, axis_z, angle);
            setTranslation(
                -(dot(axis_pos_x, axis_pos_y, axis_pos_z, values[M00], values[M10], values[M20])) + axis_pos_x,
                -(dot(axis_pos_x, axis_pos_y, axis_pos_z, values[M01], values[M11], values[M21])) + axis_pos_y,
                -(dot(axis_pos_x, axis_pos_y, axis_pos_z, values[M02], values[M12], values[M22])) + axis_pos_z
            );
        }
        
    }
    
}
