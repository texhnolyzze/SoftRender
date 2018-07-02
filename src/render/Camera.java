package render;

import render.Matrix4x4f.mat4;
import render.Vector4f.vec4;


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
    
    vec4 pos = new vec4();
    vec4 dir = new vec4(0, 0, 1);
    private vec4 up = new vec4(0, 1, 0);
    
    private Frustum frustum = new Frustum();
    
    private vec4 temp_vec = new vec4();
    
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
        updateViewMatrix();
        updateProjectionMatrix();
        onPerspectiveAttributesUpdate();
        updateFrustum();
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
    
    public final void updateFrustum() {
        frustum.update();
    }
    
    public float getPositionX() {return pos.x;}
    public float getPositionY() {return pos.y;}
    public float getPositionZ() {return pos.z;}
    public Vector4f getPosition(Vector4f dest) {return dest.set(pos.x, pos.y, pos.z);}
    
    public float getDirectionX() {return dir.x;}
    public float getDirectionY() {return dir.y;}
    public float getDirectionZ() {return dir.z;}
    public Vector4f getDirection(Vector4f dest) {return dest.set(dir.x, dir.y, dir.z);}
    
    public Camera lookAt(float x, float y, float z) {
        temp_vec.set(x, y, z).sub(pos, temp_vec);
        float len2 = temp_vec.len2();
        if (len2 != 0.0f) {
            temp_vec.normalize_len2_known(len2);
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
    
    public Camera move(Vector4f v) {
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
    
    public Camera setPosition(Vector4f pos) {
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
    
    final void normalsToViewSpace(Iterable<Face> faces) {
        for (Face f : faces) {
            viewMatrix.toViewSpace(f.norm());
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
        
        private final vec4 near_left_bot  = new vec4(), 
                     near_left_top  = new vec4(), 
                     near_right_top = new vec4(), 
                     near_right_bot = new vec4(),
                     far_left_bot   = new vec4(), 
                     far_left_top   = new vec4(), 
                     far_right_top  = new vec4(), 
                     far_right_bot  = new vec4();;
        
        private final vec4 temp_vec1 = new vec4(), 
                     temp_vec2       = new vec4(),
                     on_near_plane   = new vec4(), 
                     on_far_plane    = new vec4();
        
        private static final int NEAR_PLANE     = 0,
                                 FAR_PLANE      = 1,
                                 LEFT_PLANE     = 2,
                                 RIGHT_PLANE    = 3,
                                 TOP_PLANE      = 4,
                                 BOTTOM_PLANE   = 5;
                                 
        
        private final vec4[] planes = new vec4[6]; // (normal, d)
        private final vec4 aabb_min = new vec4(), aabb_max = new vec4();
        
        Frustum() {
            for (int i = 0; i < planes.length; i++) planes[i] = new vec4();
        }
        
        void update() {
            vec4 left = up.cross(dir, temp_vec1);
            
            on_near_plane.set(pos).add(dir.x * near, dir.y * near, dir.z * near);
            
            near_left_bot.set(on_near_plane).add(left.x * half_w_near, left.y * half_w_near, left.z * half_w_near).add(-up.x * half_h_near, -up.y * half_h_near, -up.z * half_h_near);
            near_left_top.set(on_near_plane).add(left.x * half_w_near, left.y * half_w_near, left.z * half_w_near).add(up.x * half_h_near, up.y * half_h_near, up.z * half_h_near);
            near_right_top.set(on_near_plane).add(-left.x * half_w_near, -left.y * half_w_near, -left.z * half_w_near).add(up.x * half_h_near, up.y * half_h_near, up.z * half_h_near);
            near_right_bot.set(on_near_plane).add(-left.x * half_w_near, -left.y * half_w_near, -left.z * half_w_near).add(-up.x * half_h_near, -up.y * half_h_near, -up.z * half_h_near);
            
            on_far_plane.set(on_near_plane).add(dir.x * (far - near), dir.y * (far - near), dir.z * (far - near));
            
            far_left_bot.set(on_far_plane).add(left.x * half_w_far, left.y * half_w_far, left.z * half_w_far).add(-up.x * half_h_far, -up.y * half_h_far, -up.z * half_h_far);
            far_left_top.set(on_far_plane).add(left.x * half_w_far, left.y * half_w_far, left.z * half_w_far).add(up.x * half_h_far, up.y * half_h_far, up.z * half_h_far);
            far_right_top.set(on_far_plane).add(-left.x * half_w_far, -left.y * half_w_far, -left.z * half_w_far).add(up.x * half_h_far, up.y * half_h_far, up.z * half_h_far);
            far_right_bot.set(on_far_plane).add(-left.x * half_w_far, -left.y * half_w_far, -left.z * half_w_far).add(-up.x * half_h_far, -up.y * half_h_far, -up.z * half_h_far);
            
            planes[NEAR_PLANE].set(dir);
            planes[NEAR_PLANE].w = dir.dot(on_near_plane);
            
            planes[FAR_PLANE].set(-dir.x, -dir.y, -dir.z);
            planes[FAR_PLANE].w = on_far_plane.dot(-dir.x, -dir.y, -dir.z);
            
            temp_vec1.set(near_left_bot).sub(near_left_top).cross(temp_vec2.set(far_left_top).sub(near_left_top));
            temp_vec1.normalize();
            planes[LEFT_PLANE].set(temp_vec1);
            planes[LEFT_PLANE].w = near_left_bot.dot(planes[LEFT_PLANE]);
            
            planes[RIGHT_PLANE].set(temp_vec1).reflect(dir, planes[RIGHT_PLANE]);
            planes[RIGHT_PLANE].w = near_right_bot.dot(planes[RIGHT_PLANE]);
            
            temp_vec1.set(far_left_top).sub(near_left_top).cross(temp_vec2.set(near_right_top).sub(near_left_top));
            temp_vec1.normalize();
            planes[TOP_PLANE].set(temp_vec1);
            planes[TOP_PLANE].w = far_left_top.dot(planes[TOP_PLANE]);
            
            planes[BOTTOM_PLANE].set(temp_vec1).reflect(dir, planes[BOTTOM_PLANE]);
            planes[BOTTOM_PLANE].w = near_left_bot.dot(planes[BOTTOM_PLANE]);
            
        }
        
        boolean intersects(AABB aabb) {
            aabb_min.set(aabb.posX(), aabb.posY(), aabb.posZ());
            aabb_max.set(aabb_min).add(aabb.width(), aabb.height(), aabb.depth());
            for (vec4 plane : planes) {
                float d = Math.max(aabb_min.x * plane.x, aabb_max.x * plane.x) + 
                          Math.max(aabb_min.y * plane.y, aabb_max.y * plane.y) +
                          Math.max(aabb_min.z * plane.z, aabb_max.z * plane.z) + 
                          plane.w;
                if (d < 0)
                    return false;
            }
            return true;
        }
        
    }
    
}
