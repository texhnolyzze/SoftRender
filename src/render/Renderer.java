package render;

import java.util.ArrayList;
import java.util.List;
import static render.Graphics.rgb;
import render.Light.AmbientLight;
import render.Light.DirectionLight;
import render.Light.PointLight;
import static render.MathUtils.round;
import render.Vector3f.vec3;

/**
 *
 * @author Texhnolyze
 */
public class Renderer {
    
    private final Rasterizer3D rasterizer;
    
    private final List<AmbientLight> ambLights = new ArrayList<>();
    private final List<DirectionLight> dirLights = new ArrayList<>();
    private final List<PointLight> pointLights = new ArrayList<>();
    
    private int strokeRGB; // if no shade -- this color will be used int triangle stroke
    
    private Camera curr_camera;
    private Scene curr_scene;
    private Model curr_model;
    private Bitmap curr_tex;
    private Material curr_mat;
    private float tex_w, tex_h;
    
    private int curr_face_idx;
    private Face[] temp_faces = new Face[1024];
    
    private final vec3 
            temp_vec1 = new vec3(), 
            temp_vec2 = new vec3(), 
            rgb_vec = new vec3(),
            norm_vec = new vec3(),
            point_vec = new vec3();
    
    
    public Renderer(Graphics g) {
        rasterizer = new Rasterizer3D(g);
    }
    
    public void drawZBuffer(Graphics g) {
        rasterizer.drawZBuffer(g);
    }
    
    public void clearZBuffer() {
        rasterizer.clearZBuffer();
    }
    
    public Graphics getGraphics() {
        return rasterizer.getGraphics();
    }
    
    public List<AmbientLight> getAmbientLights() {return ambLights;}
    public List<DirectionLight> getDirectionLights() {return dirLights;}
    public List<PointLight> getPointLights() {return pointLights;}
    
    public int getStroke() {
        return strokeRGB;
    }
    
    public void setStroke(int rgb) {
        strokeRGB = rgb;
    }
    
    public void render(Camera c, ModelInstance instance) {
        if (instance.testAABB()) {
            if (!c.testAABB(instance.getAABB()))
                return;
        }
        curr_camera = c;
        curr_tex = instance.getTexture();
        curr_mat = instance.getMaterial();
        final boolean texture = instance.texture();
        final ShadeMode shadeMode = instance.getShadeMode();
        instance.translateModelIntoWorldSpace();
        curr_model = instance.getModel();
        if (texture) {
            tex_w = curr_tex.getWidth() - 1;
            tex_h = curr_tex.getHeight() - 1;
        }
        if (curr_model.numFaces() > temp_faces.length)
            temp_faces = new Face[2 * curr_model.numFaces()];
        for (Face f : curr_model.faces()) {
            if (!f.isTwoFaced()) { // try to cull face
                Vector3f n = f.norm(); 
                if (MathUtils.dot(c.pos.x - f.vertex1().pos().x(), c.pos.y - f.vertex1().pos().y(), c.pos.z - f.vertex1().pos().z(), n.x(), n.y(), n.z()) <= 0.0f) 
                    continue;   
            }
            temp_faces[curr_face_idx++] = f;
        }
        switch (shadeMode) {
            case FLAT:
                for (int i = 0; i < curr_face_idx; i++) 
                    lightFace(temp_faces[i]);
                break;
            case GOURAUD:
                for (int i = 0; i < curr_face_idx; i++) {
                    Face f = temp_faces[i];
                    Vertex v0 = f.vertex1(), v1 = f.vertex2(), v2 = f.vertex3();
                    if (!v0.lighted()) lightVertex(v0);
                    if (!v1.lighted()) lightVertex(v1);
                    if (!v2.lighted()) lightVertex(v2);
                }
                break;
        }
        c.toViewSpace(curr_model.vertices());
        c.project(curr_model.vertices(), rasterizer.getGraphics().getWidth(), rasterizer.getGraphics().getHeight());
        switch (shadeMode) {
            case NO_SHADE:
                if (texture) {
                    for (int i = 0; i < curr_face_idx; i++) {
                        Face f = temp_faces[i];
                        Vector3f v0 = f.vertex1().pos();
                        Vector3f v1 = f.vertex2().pos();
                        Vector3f v2 = f.vertex3().pos();
                        rasterizer.fillTexturedTriangle(
                            round(v0.x()), round(v0.y()), v0.z(), tex_w * f.u1(), tex_h * f.v1(), 
                            round(v1.x()), round(v1.y()), v1.z(), tex_w * f.u2(), tex_h * f.v2(), 
                            round(v2.x()), round(v2.y()), v2.z(), tex_w * f.u3(), tex_h * f.v3(), 
                            curr_tex, false
                        );
                    }
                } else {
                    rasterizer.getGraphics().setColor(strokeRGB);
                    for (int i = 0; i < curr_face_idx; i++) {
                        Face f = temp_faces[i];
                        Vector3f v0 = f.vertex1().pos();
                        Vector3f v1 = f.vertex2().pos();
                        Vector3f v2 = f.vertex3().pos();
                        rasterizer.strokeTriangle(
                            round(v0.x()), round(v0.y()), v0.z(), 
                            round(v1.x()), round(v1.y()), v1.z(), 
                            round(v2.x()), round(v2.y()), v2.z()
                        );
                    }
                }
                break;
            case FLAT:
                if (texture) {
                    for (int i = 0; i < curr_face_idx; i++) {
                        Face f = temp_faces[i];
                        rasterizer.getGraphics().setColor(rgb(f.getTempRed(), f.getTempGreen(), f.getTempBlue()));
                        rasterizer.fillTexturedTriangle(
                            round(f.vertex1().pos().x()), round(f.vertex1().pos().y()), f.vertex1().pos().z(), tex_w * f.u1(), tex_h * f.v1(), 
                            round(f.vertex2().pos().x()), round(f.vertex2().pos().y()), f.vertex2().pos().z(), tex_w * f.u2(), tex_h * f.v2(), 
                            round(f.vertex3().pos().x()), round(f.vertex3().pos().y()), f.vertex3().pos().z(), tex_w * f.u3(), tex_h * f.v3(),
                            curr_tex, true
                        );
                    }
                } else {
                    for (int i = 0; i < curr_face_idx; i++) {
                        Face f = temp_faces[i];
                        rasterizer.getGraphics().setColor(rgb(f.getTempRed(), f.getTempGreen(), f.getTempBlue()));
                        rasterizer.fillTriangle(
                            round(f.vertex1().pos().x()), round(f.vertex1().pos().y()), f.vertex1().pos().z(), 
                            round(f.vertex2().pos().x()), round(f.vertex2().pos().y()), f.vertex2().pos().z(), 
                            round(f.vertex3().pos().x()), round(f.vertex3().pos().y()), f.vertex3().pos().z()
                        );
                    }
                }
                break;
            case GOURAUD:
                if (texture) {
                    for (int i = 0; i < curr_face_idx; i++) {
                        Face f = temp_faces[i];
                        rasterizer.fillTexturedTriangleInterpolateColor(
                            round(f.vertex1().pos().x()), round(f.vertex1().pos().y()), f.vertex1().pos().z(), tex_w * f.u1(), tex_h * f.v1(), 255f * f.vertex1().getTempRed(), 255f * f.vertex1().getTempGreen(), 255f * f.vertex1().getTempBlue(), 
                            round(f.vertex2().pos().x()), round(f.vertex2().pos().y()), f.vertex2().pos().z(), tex_w * f.u2(), tex_h * f.v2(), 255f * f.vertex2().getTempRed(), 255f * f.vertex2().getTempGreen(), 255f * f.vertex2().getTempBlue(),
                            round(f.vertex3().pos().x()), round(f.vertex3().pos().y()), f.vertex3().pos().z(), tex_w * f.u3(), tex_h * f.v3(), 255f * f.vertex3().getTempRed(), 255f * f.vertex3().getTempGreen(), 255f * f.vertex3().getTempBlue(), 
                            curr_tex 
                        );
                    }
                } else {
                    for (int i = 0; i < curr_face_idx; i++) {
                        Face f = temp_faces[i];
                        rasterizer.fillTriangleInterpolateColor(
                            round(f.vertex1().pos().x()), round(f.vertex1().pos().y()), f.vertex1().pos().z(), 255f * f.vertex1().getTempRed(), 255f * f.vertex1().getTempGreen(), 255f * f.vertex1().getTempBlue(), 
                            round(f.vertex2().pos().x()), round(f.vertex2().pos().y()), f.vertex2().pos().z(), 255f * f.vertex2().getTempRed(), 255f * f.vertex2().getTempGreen(), 255f * f.vertex2().getTempBlue(),
                            round(f.vertex3().pos().x()), round(f.vertex3().pos().y()), f.vertex3().pos().z(), 255f * f.vertex3().getTempRed(), 255f * f.vertex3().getTempGreen(), 255f * f.vertex3().getTempBlue() 
                        );
                    }
                }
                break;
        }
        curr_model.reset();
        curr_face_idx = 0;
    }
    
//  the face is lighting in the world space
    private void lightFace(Face f) {
        light(rgb_vec, point_vec.set(f.getMediPoint()), norm_vec.set(f.norm()), curr_camera.pos);
        f.setTempRGB(rgb_vec.x, rgb_vec.y, rgb_vec.z);
    }
    
    private void lightVertex(Vertex v) {
        light(rgb_vec, point_vec.set(v.pos()), norm_vec.set(v.norm()), curr_camera.pos);
        v.setTempRGB(rgb_vec.x, rgb_vec.y, rgb_vec.z);
    }
    
    void light(vec3 rgb, vec3 point, vec3 norm, vec3 view_pos) {
        rgb.set(0f, 0f, 0f);
        view_pos.sub(point, temp_vec1).normalize();
        vec3 view_dir = temp_vec1; // vector directed at the observer
        for (AmbientLight l : ambLights) {
            if (!l.enabled) 
                continue;
            rgb.x += curr_mat.getAmbientRed() * l.r;
            rgb.y += curr_mat.getAmbientGreen() * l.g;
            rgb.z += curr_mat.getAmbientBlue() * l.b;
        }
        for (DirectionLight l : dirLights) {
            if (!l.enabled)
                continue;
            rgb.x += curr_mat.getAmbientRed() * l.r;
            rgb.y += curr_mat.getAmbientGreen() * l.g;
            rgb.z += curr_mat.getAmbientBlue() * l.b;
            float dp = l.dir_inv.dot(norm);
            if (dp > 0f) {
                rgb.x += dp * curr_mat.getDiffuseRed() * l.r;
                rgb.y += dp * curr_mat.getDiffuseGreen() * l.g;
                rgb.z += dp * curr_mat.getDiffuseBlue() * l.b;
            }
//          note that, for example, for a cube whose side is 
//          represented by two triangles, the specular lighting 
//          component in Gouraud or flat shade mode
//          may be look ridiculous, because one of the triangles 
//          will have a color different from the other
            l.dir_inv.reflect(norm, temp_vec2);
            vec3 light_dir_reflected = temp_vec2; 
            dp = light_dir_reflected.dot(view_dir);
            if (dp > 0f) {
                float pow = (float) Math.pow(dp, curr_mat.getShininess());
                rgb.x += pow * curr_mat.getSpecularRed() * l.r;
                rgb.y += pow * curr_mat.getSpecularGreen() * l.g;
                rgb.z += pow * curr_mat.getSpecularBlue() * l.b;
            }
        }
        for (PointLight l : pointLights) {
            if (!l.enabled)
                continue;
            temp_vec2.set(l.pos).sub(point, temp_vec2);
            vec3 light_dir = temp_vec2; // vector directed at the point light source
            float d_sqr = light_dir.len2();
            if (d_sqr > l.radiusSquare)
                continue;
            float d = (float) Math.sqrt(d_sqr);
            float attenuation = 1f - d * l.radius_inv; // varies linearly
            rgb.x += attenuation * curr_mat.getAmbientRed() * l.r;
            rgb.y += attenuation * curr_mat.getAmbientGreen() * l.g;
            rgb.z += attenuation * curr_mat.getAmbientBlue() * l.b;
            light_dir.normalize_len_known(d);
            float dp = light_dir.dot(norm);
            if (dp > 0f) {
                rgb.x += attenuation * dp * curr_mat.getDiffuseRed() * l.r;
                rgb.y += attenuation * dp * curr_mat.getDiffuseGreen() * l.g;
                rgb.z += attenuation * dp * curr_mat.getDiffuseBlue() * l.b;
            }
            light_dir.reflect(norm, light_dir); 
            dp = light_dir.dot(view_dir);
            if (dp > 0f) {
                float pow = (float) Math.pow(dp, curr_mat.getShininess());
                rgb.x += attenuation * pow * curr_mat.getSpecularRed() * l.r;
                rgb.y += attenuation * pow * curr_mat.getSpecularGreen() * l.g;
                rgb.z += attenuation * pow * curr_mat.getSpecularBlue() * l.b;
            }
        }
        rgb.set(rgb.x > 1f ? 1f: rgb.x, rgb.y > 1f ? 1f: rgb.y, rgb.z > 1f ? 1f: rgb.z);
    }
    
    
    
}
