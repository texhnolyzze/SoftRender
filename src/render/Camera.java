package render;

import render.Matrix4x4f.mat4;
import render.Vector3f.vec3;


/**
 *
 * @author Texhnolyze
 */
public class Camera {
    
    private float fov_y;
    private float aspect;
    private float near, far;
    
    vec3 pos = new vec3();
    vec3 dir = new vec3(0, 0, 1);
    private vec3 up = new vec3(0, 1, 0);
    
    private vec3 temp_vec = new vec3();
    
    private mat4 viewMatrix = new mat4();
    private mat4 projectionMatrix = new mat4();
    
    private mat4 temp_mat = new mat4();
    
    public Camera(float near, float far, float verticalFOV, int viewportWidth, int viewportHeight) {
        this.far = far;
        this.near = near;
        fov_y = verticalFOV;
        aspect = (float) viewportWidth / viewportHeight;
        updateViewMatrix();
        updateProjectionMatrix();
    }
    
    public final void updateViewMatrix() {
        viewMatrix.setToViewMatrix(pos, dir, up);
    }
    
    public final void updateProjectionMatrix() {
        projectionMatrix.setToProjectionMatrix(fov_y, aspect, near, far);
    }
    
    public float getPositionX() {return pos.x;}
    public float getPositionY() {return pos.y;}
    public float getPositionZ() {return pos.z;}
    public Vector3f getPosition(Vector3f dest) {return dest.set(pos.x, pos.y, pos.z);}
    
    public float getDirectionX() {return dir.x;}
    public float getDirectionY() {return dir.y;}
    public float getDirectionZ() {return dir.z;}
    public Vector3f getDirection(Vector3f dest) {return dest.set(dir.x, dir.y, dir.z);}
    
    public Camera lookAt(float x, float y, float z) {
        temp_vec.set(x, y, z).subLocal(pos);
        float len2 = temp_vec.len2();
        if (len2 != 0.0f) {
            temp_vec.normalize(len2);
            float dp = temp_vec.dot(up);
            if (Math.abs(dp - 1f) < 0.001f) 
                up.set(-dir.x, -dir.y, -dir.z);
            else if (Math.abs(dp + 1f) < 0.001f)
                up.set(dir.x, dir.y, dir.z);
            dir.set(temp_vec);
            setUpVector();
        }
        return this;
    }
    
    private void setUpVector() {
        dir.cross(up, temp_vec);
        temp_vec.normalize();
        temp_vec.cross(dir, up);
        up.normalize();
    }
    
    public Camera move(Vector3f v) {
        return move(v.x(), v.y(), v.z());
    }
    
    public Camera move(float dx, float dy, float dz) {
        pos.add(dx, dy, dz);
        return this;
    }
    
    public Camera moveInDirection(float amount) {
        pos.add(dir.x * amount, dir.y * amount, dir.z * amount);
        return this;
    }
    
    public Camera rotateDirectionAround(float axisX, float axisY, float axisZ, float angle) {
        float len2 = MathUtils.len2(axisX, axisY, axisZ);
        if (len2 != 0.0f) {
            float len_inv = (float) (1.0 / Math.sqrt(len2));
            axisX *= len_inv;
            axisY *= len_inv;
            axisZ *= len_inv;
            temp_mat.setToRotation(axisX, axisY, axisZ, angle);
            dir.mul3x3(temp_mat);
            up.mul3x3(temp_mat);
        }
        return this;
    }
    
//  Using this method, make the camera rotate around one point and at the same time
//  "lookAt" - point remains the same
    public Camera rotate(float axisX, float axisY, float axisZ, float axisPosX, float axisPosY, float axisPosZ, float angle) {
        float len2 = MathUtils.len2(axisX, axisY, axisZ);
        if (len2 != 0.0f) {
            float len_inv = (float) (1.0 / Math.sqrt(len2));
            axisX *= len_inv;
            axisY *= len_inv;
            axisZ *= len_inv;
            temp_mat.setToRotationMatrix(axisX, axisY, axisZ, axisPosX, axisPosY, axisPosZ, angle);
            pos.mul4x3(temp_mat);
            dir.mul3x3(temp_mat);
            up.mul3x3(temp_mat);
        }
        return this;
    }
    
    public Camera transformDirection(Matrix4x4f transform) {
        dir.mul3x3(transform);
        up.mul3x3(transform);
        return this;
    }
    
    public Camera transformPosition(Matrix4x4f transform) {
        pos.mul4x3(transform);
        return this;
    }
    
    public Camera apply(Matrix4x4f transform) {
        transformDirection(transform);
        return transformPosition(transform);
    }
    
    public Camera setPosition(Vector3f pos) {
        return setPosition(pos.x(), pos.y(), pos.z());
    }
    
    public Camera setPosition(float x, float y, float z) {
        pos.set(x, y, z);
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
            viewMatrix.toViewSpace(v.pos());
//            MathUtils.normalToViewSpace(v.getNormal(), viewMatrix);
        }
    }
    
    final void project(Iterable<Vertex> vertices, int w, int h) {
        float half_w_minus_one = (float) 0.5f * (w - 1f);
        float half_h_minus_one = (float) 0.5f * (h - 1f);
        for (Vertex v : vertices) {
            projectionMatrix.project(v.pos());
            v.pos().set(
                (1f + v.pos().x()) * half_w_minus_one, 
                h - (1f + v.pos().y()) * half_h_minus_one - 1, 
                v.pos().z()
            );
        }
    }
    
}
