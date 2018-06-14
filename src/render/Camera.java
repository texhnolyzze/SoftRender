package render;

import java.util.Arrays;
import render.Matrix4x4f.mat4;
import render.Vector3f.vec3;


/**
 *
 * @author Texhnolyze
 */
public class Camera {
    
//  ------PERSPECTIVE ATTRIBUTES--------
    private float fov_y_rad;
    private float aspect;
    private float near, far;
//  -------------------------------
    
    private float half_h_near, half_w_near;
    private float half_h_far, half_w_far;
    
    vec3 pos = new vec3();
    vec3 dir = new vec3(0, 0, 1);
    private vec3 up = new vec3(0, 1, 0);
    
    private Frustum frustum = new Frustum();
    
    private vec3 temp_vec1 = new vec3(), temp_vec2 = new vec3();
    
    private mat4 viewMatrix = new mat4();
    private mat4 projectionMatrix = new mat4();
    
    private mat4 temp_mat = new mat4();
    
    public Camera(float near, float far, float verticalFOVDegrees, int viewportWidth, int viewportHeight) {
        if (far <= 0 || near <= 0 || near >= far)
            throw new IllegalArgumentException("0 < near < far");
        this.far = far;
        this.near = near;
        fov_y_rad = (float) Math.toRadians(verticalFOVDegrees);
        aspect = (float) viewportWidth / viewportHeight;
        onPerspectiveAttributesUpdate();
        updateViewMatrix();
        updateProjectionMatrix();
    }
    
    public final void updateViewMatrix() {
        viewMatrix.setToViewMatrix(pos, dir, up);
    }
    
    public final void updateProjectionMatrix() {
        projectionMatrix.setToProjectionMatrix(fov_y_rad, aspect, near, far);
    }
    
    public final void onPerspectiveAttributesUpdate() {
        float tan = (float) Math.tan(fov_y_rad * 0.5);
        half_h_near = tan * near;
        half_w_near = half_h_near * aspect;
        half_h_far = tan * far;
        half_w_far = half_h_far * aspect;
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
        temp_vec1.set(x, y, z).sub(pos, temp_vec1);
        float len2 = temp_vec1.len2();
        if (len2 != 0.0f) {
            temp_vec1.normalize(len2);
            float dp = temp_vec1.dot(up);
            if (Math.abs(dp - 1f) < 0.001f) 
                up.set(-dir.x, -dir.y, -dir.z);
            else if (Math.abs(dp + 1f) < 0.001f)
                up.set(dir.x, dir.y, dir.z);
            dir.set(temp_vec1);
            setUpVector();
        }
        return this;
    }
    
    private void setUpVector() {
        dir.cross(up, temp_vec1);
        temp_vec1.normalize();
        temp_vec1.cross(dir, up);
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
        if (near <= 0 || near >= far)
            throw new IllegalArgumentException();
        this.near = near;
        return this;
    }
    
    public float getFar() {
        return far;
    }
    
    public Camera setFar(float far) {
        if (far <= 0 || far <= near)
            throw new IllegalArgumentException();
        this.far = far;
        return this;
    }
    
    public float getVerticalFOV() {
        return (float) Math.toDegrees(fov_y_rad);
    }
    
    public Camera setVerticalFOV(float fovDegrees) {
        fov_y_rad = (float) Math.toRadians(fovDegrees);
        return this;
    }
    
    final void toViewSpace(Iterable<Vertex> vertices) {
        for (Vertex v : vertices) {
            viewMatrix.toViewSpace(v.pos());
        }
    }
    
    final void project(Iterable<Vertex> vertices, int w, int h) {
        float half_w_minus_one = 0.5f * (w - 1f);
        float half_h_minus_one = 0.5f * (h - 1f);
        for (Vertex v : vertices) {
            projectionMatrix.project(v.pos());
            v.pos().set(
                (1f + v.pos().x()) * half_w_minus_one, 
                (1f + v.pos().y()) * half_h_minus_one, 
                v.pos().z()
            );
        }
    }
    
//  Determines whether the AABB intersects the view space of camera.
//  Calculations take place in the camera's local space.
    boolean testAABB(AABB aabb) {
        return frustum.intersects(aabb);
    }
    
    private class Frustum {
        
        private vec3 bounding_pos000 = new vec3(), bounding_pos111 = new vec3();
        private vec3 bounding_w = new vec3();
        private vec3 bounding_h = new vec3();
        private vec3 bounding_d = new vec3();
        
        private final vec3[] temp_bounding_points = new vec3[8];
        
        Frustum() {
            for (int i = 0; i < temp_bounding_points.length; i++) temp_bounding_points[i] = new vec3();
        }
        
        boolean intersects(AABB aabb) {
            
            bounding_pos000.set(aabb.posX(), aabb.posY(), aabb.posZ());
            bounding_w.set(aabb.width(), 0f, 0f);
            bounding_h.set(0f, aabb.height(), 0f);
            bounding_d.set(0f, 0f, aabb.depth());
            
            bounding_pos000.mul4x3(viewMatrix);
            bounding_w.mul3x3(viewMatrix);
            bounding_h.mul3x3(viewMatrix);
            bounding_d.mul3x3(viewMatrix);
            
            float min_x = Float.POSITIVE_INFINITY, max_x = Float.NEGATIVE_INFINITY; 
            float min_y = Float.POSITIVE_INFINITY, max_y = Float.NEGATIVE_INFINITY;
            float min_z = Float.POSITIVE_INFINITY, max_z = Float.NEGATIVE_INFINITY;
            
            temp_bounding_points[0].set(bounding_pos000);
            temp_bounding_points[1].set(bounding_pos000).add(bounding_w);
            temp_bounding_points[2].set(bounding_pos000).add(bounding_h);
            temp_bounding_points[3].set(bounding_pos000).add(bounding_d);
            temp_bounding_points[4].set(bounding_pos000).add(bounding_w).add(bounding_h);
            temp_bounding_points[5].set(bounding_pos000).add(bounding_h).add(bounding_d);
            temp_bounding_points[6].set(bounding_pos000).add(bounding_w).add(bounding_d);
            temp_bounding_points[7].set(bounding_pos000).add(bounding_w).add(bounding_h).add(bounding_d);

            for (int i = 0; i < temp_bounding_points.length; i += 2) {
                vec3 v1 = temp_bounding_points[i], v2 = temp_bounding_points[i + 1];
                
                if (v1.x < v2.x) {
                    if (v1.x < min_x) min_x = v1.x;
                    if (v2.x > max_x) max_x = v2.x;
                } else {
                    if (v1.x > max_x) max_x = v1.x;
                    if (v2.x < min_x) min_x = v2.x;
                }
                
                if (v1.y < v2.y) {
                    if (v1.y < min_y) min_y = v1.y;
                    if (v2.y > max_y) max_y = v2.y;
                } else {
                    if (v1.y > max_y) max_y = v1.y;
                    if (v2.y < min_y) min_y = v2.y;
                }
                
                if (v1.z < v2.z) {
                    if (v1.z < min_z) min_z = v1.z;
                    if (v2.z > max_z) max_z = v2.z;
                } else {
                    if (v1.z > max_z) max_z = v1.z;
                    if (v2.z < min_z) min_z = v2.z;
                }
                
            }
            
            bounding_pos000.set(min_x, min_y, min_z);
            bounding_pos111.set(max_x, max_y, max_z);
            bounding_pos000.project(projectionMatrix);
            bounding_pos111.project(projectionMatrix);
            
            return MathUtils.cubesIntersects(
                -1f, -1f, -1f, 1f, 1f, 1f, 
                // axises are inverted
                bounding_pos111.x, bounding_pos111.y, bounding_pos000.z, 
                bounding_pos000.x, bounding_pos000.y, bounding_pos111.z
            );
            
        }
        
    }
    
}
