package render;

import render.Matrix4x4f.mat4;
import static render.Matrix4x4f.*;

/**
 *
 * @author Texhnolyze
 */
public interface Vector3f {

    float x();
    float y();
    float z();
    
    Vector3f set(float x, float y, float z);
    
    static class vec3 implements Vector3f { // just for internal usage
                
        float x, y, z;
        
        vec3() {}
        vec3(float x, float y, float z) {set(x, y, z);}
        
        @Override public float x() {return x;}
        @Override public float y() {return y;}
        @Override public float z() {return z;}

        float len2() {
            return MathUtils.len2(x, y, z);
        } 
       
        @Override
        public vec3 set(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }
        
        vec3 set(vec3 v) {
            x = v.x;
            y = v.y;
            z = v.z;
            return this;
        }
        
        vec3 set(Vector3f v) {
            x = v.x();
            y = v.y();
            z = v.z();
            return this;
        }
        
        vec3 add(float dx, float dy, float dz) {
            x += dx;
            y += dy;
            z += dz;
            return this;
        }
        
        vec3 add(vec3 v) {
            x += v.x;
            y += v.y;
            z += v.z;
            return this;
        }
        
        vec3 add(vec3 v, vec3 dest) {
            return dest.set(x + v.x, x + v.y, y + v.z);
        }
        
        vec3 sub(vec3 v, vec3 dest) {
            return dest.set(x - v.x, y - v.y, z - v.z);
        }
        
        vec3 scale(float s) {
            return set(x * s, y * s, z * s);
        }
                
        vec3 cross(vec3 v, vec3 dest) {
            return dest.set(
                y * v.z - z * v.y,
                z * v.x - x * v.z,
                x * v.y - y * v.x
            );
        }
        
        float dot(vec3 v) {
            return x * v.x + y * v.y + z * v.z;
        }
        
        float dot(float x, float y, float z) {
            return this.x * x + this.y * y + this.z * z;
        }
        
        vec3 normalize_len2_known(float len2) {
            float len_inv = (float) (1.0 / Math.sqrt(len2));
            x *= len_inv;
            y *= len_inv;
            z *= len_inv;
            return this;
        }
        
        vec3 normalize_len_known(float len) {
            float len_inv = 1f / len;
            x *= len_inv;
            y *= len_inv;
            z *= len_inv;
            return this;
        }
        
        vec3 normalize() {
            return normalize_len2_known(len2());
        }
        
        // axis is always unit vector
        vec3 reflect(vec3 axis, vec3 dest) {
            return reflect(axis.x, axis.y, axis.z, dest);
        }
        
        vec3 reflect(float axis_x, float axis_y, float axis_z, vec3 dest) {
            float dp = this.dot(axis_x, axis_y, axis_z);
            return dest.set(
                x + 2f * (axis_x * dp - x), 
                y + 2f * (axis_y * dp - y), 
                z + 2f * (axis_z * dp - z)
            );
        }
        
        vec3 mul3x3(mat4 mat) {
            float[] m = mat.values;
            return set(
                x * m[M00] + y * m[M10] + z * m[M20],
                x * m[M01] + y * m[M11] + z * m[M21],
                x * m[M02] + y * m[M12] + z * m[M22]
            );
        }
        
        vec3 mul3x3(Matrix4x4f m) {
            return set(
                x * m.val(M00) + y * m.val(M10) + z * m.val(M20),
                x * m.val(M01) + y * m.val(M11) + z * m.val(M21),
                x * m.val(M02) + y * m.val(M12) + z * m.val(M22)
            );
        }
        
        vec3 mul4x3(Matrix4x4f m) {
            return set(
                x * m.val(M00) + y * m.val(M10) + z * m.val(M20) + m.val(M30),
                x * m.val(M01) + y * m.val(M11) + z * m.val(M21) + m.val(M31),
                x * m.val(M02) + y * m.val(M12) + z * m.val(M22) + m.val(M32)
            );
        }
        
        vec3 mul4x3(mat4 mat) {
            float[] m = mat.values;
            return set(
                x * m[M00] + y * m[M10] + z * m[M20] + m[M30],
                x * m[M01] + y * m[M11] + z * m[M21] + m[M31],
                x * m[M02] + y * m[M12] + z * m[M22] + m[M32]
            );
        }
        
        vec3 prj(mat4 prjMat) {
            float w_inv = -1f / z;
            return set(
                x * prjMat.values[M00] * w_inv, 
                y * prjMat.values[M11] * w_inv,
                (z * prjMat.values[M22] + prjMat.values[M23]) * w_inv
            );
        }

        @Override
        public String toString() {
            return "(" + x + ", " + y + ", " + z + ')';
        }
        
    }
    
}
