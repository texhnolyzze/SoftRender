package render;

import render.MathUtils.Matrix4x4;
import static render.MathUtils.roundPositive;

/**
 *
 * @author Texhnolyze
 */
public class Camera {
    
    private float fov_y;
    private float aspect;
    private float near, far;
    
    private float pos_x = 0f, pos_y = 0f, pos_z = 0f;
    private float dir_x = 0f, dir_y = 0f, dir_z = 1f;
    private float up_x = 0f, up_y = 1f, up_z = 0f;
    
    private float temp_x, temp_y, temp_z;
    
    private Matrix4x4 viewMatrix = new Matrix4x4();
    private Matrix4x4 projectionMatrix = new Matrix4x4();
    
    public Camera(float near, float far, float verticalFOV, int viewportWidth, int viewportHeight) {
        this.far = far;
        this.near = near;
        fov_y = verticalFOV;
        aspect = (float) viewportWidth / viewportHeight;
        updateViewMatrix();
        updateProjectionMatrix();
    }
    
    public final void updateViewMatrix() {
        MathUtils.setToViewMatrix(pos_x, pos_y, pos_z, dir_x, dir_y, dir_z, up_x, up_y, up_z, viewMatrix);
    }
    
    public final void updateProjectionMatrix() {
        MathUtils.setToProjectionMatrix(fov_y, aspect, near, far, projectionMatrix);
    }
    
    public float getPositionX() {return pos_x;}
    public float getPositionY() {return pos_y;}
    public float getPositionZ() {return pos_z;}
    public Vector3f getPosition(Vector3f dest) {return dest.set(pos_x, pos_y, pos_z);}
    
    public float getDirectionX() {return dir_x;}
    public float getDirectionY() {return dir_y;}
    public float getDirectionZ() {return dir_z;}
    public Vector3f getDirection(Vector3f dest) {return dest.set(dir_x, dir_y, dir_z);}
    
    public Camera lookAt(float x, float y, float z) {
        temp_x = x - pos_x;
        temp_y = y - pos_y;
        temp_z = z - pos_z;
        float len2 = MathUtils.len2(temp_x, temp_y, temp_z);
        if (len2 != 0.0f) {
            float len = (float) Math.sqrt(len2);
            temp_x /= len;
            temp_y /= len;
            temp_z /= len;
            float dp = MathUtils.dot(temp_x, temp_y, temp_z, up_x, up_y, up_z);
            if (Math.abs(dp - 1f) < 0.001f) {
                up_x = -dir_x;
                up_y = -dir_y;
                up_z = -dir_z;
            } else if (Math.abs(dp + 1f) < 0.001f) {
                up_x = dir_x;
                up_y = dir_y;
                up_z = dir_z;
            }
            dir_x = temp_x;
            dir_y = temp_y;
            dir_z = temp_z;
            setUp();
        }
        return this;
    }
    
    private void setUp() {
        temp_x = dir_y * up_z - dir_z * up_y;
        temp_y = dir_z * up_x - dir_x * up_z;
        temp_z = dir_x * up_y - dir_y * up_x;
        float len = MathUtils.len(temp_x, temp_y, temp_z);
        temp_x /= len;
        temp_y /= len;
        temp_z /= len;
        up_x = temp_y * dir_z - temp_z * dir_y;
        up_y = temp_z * dir_x - temp_x * dir_z;
        up_z = temp_x * dir_y - temp_y * dir_x;
        len = MathUtils.len(up_x, up_y, up_z);
        up_x /= len;
        up_y /= len;
        up_z /= len;
    }
    
    public Camera move(Vector3f v) {
        return move(v.x(), v.y(), v.z());
    }
    
    public Camera move(float dx, float dy, float dz) {
        pos_x += dx;
        pos_y += dy;
        pos_z += dz;
        return this;
    }
    
    public Camera moveInDirection(float amount) {
        pos_x += dir_x * amount;
        pos_y += dir_y * amount;
        pos_z += dir_z * amount;
        return this;
    }
    
    public Camera setPosition(Vector3f pos) {
        return setPosition(pos.x(), pos.y(), pos.z());
    }
    
    public Camera setPosition(float x, float y, float z) {
        pos_x = x;
        pos_y = y;
        pos_z = z;
        return this;
    }
    
    public float getNear() {
        return near;
    }
    
    public Camera setNear(float near) {
        this.near = near;
        return this;
    }
    
    public float getFar() {
        return far;
    }
    
    public Camera setFar(float far) {
        this.far = far;
        return this;
    }
    
    public float getVerticalFOV() {
        return fov_y;
    }
    
    public Camera setVerticalFOV(float fov) {
        fov_y = fov;
        return this;
    }
    
    final void toViewSpace(Iterable<Vertex> vertices) {
        for (Vertex v : vertices) {
            MathUtils.toViewSpace(v.pos(), viewMatrix);
//            MathUtils.normalToViewSpace(v.getNormal(), viewMatrix);
        }
    }
    
    final void project(Iterable<Vertex> vertices, int w, int h) {
        float half_w_minus_one = (float) w / 2f - 0.5f;
        float half_h_minus_one = (float) h / 2f - 0.5f;
        for (Vertex v : vertices) {
            MathUtils.project(v.pos(), projectionMatrix);
            v.pos().set(
                (1f + v.pos().x()) * half_w_minus_one, 
                (1f + v.pos().y()) * half_h_minus_one - 1, 
                v.pos().z()
            );
        }
    }
    
}
