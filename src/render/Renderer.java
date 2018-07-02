package render;

import java.util.ArrayList;
import java.util.List;
import static render.Graphics.rgb;
import render.Light.AmbientLight;
import render.Light.DirectionLight;
import render.Light.PointLight;
import static render.MathUtils.round;
import render.Vector4f.vec4;

/**
 *
 * @author Texhnolyze
 */
public class Renderer {

    public enum ShadeMode {
        NO_SHADE, FLAT, GOURAUD
    }
    
    private final Rasterizer3D rasterizer;
    
    private final List<AmbientLight> ambLights = new ArrayList<>();
    private final List<DirectionLight> dirLights = new ArrayList<>();
    private final List<PointLight> pointLights = new ArrayList<>();
    
    private int strokeRGB; // if no shade -- this color will be used int triangle stroke
    
    private Camera curr_camera;
    private Bitmap curr_tex;
    private Material curr_mat;
    private float tex_w, tex_h;
    
    private int curr_face_idx;
    private Face[] temp_faces = new Face[1024];
    
    private final vec4 
            temp_vec1 = new vec4(), 
            temp_vec2 = new vec4(), 
            rgb_vec = new vec4(),
            norm_vec = new vec4(),
            point_vec = new vec4();
    
    
    public Renderer(Graphics g) {
        rasterizer = new Rasterizer3D(g);
    }
    
    public Rasterizer3D getRasterizer() {
        return rasterizer;
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
    
    public void render(Camera c, ModelInstance instance, final ShadeMode shadeMode, final boolean texture) {
        curr_camera = c;
        if (instance.testAABB()) {
            if (!c.testAABB(instance.getAABB()))
                return;
        }
        instance.translateModelIntoWorldSpace();
        Model m = instance.getModel();
        curr_mat = instance.getMaterial();
        curr_tex = instance.getTexture();
        if (texture) {
            tex_w = curr_tex.getWidth() - 1;
            tex_h = curr_tex.getHeight() - 1;
        }
        if (m.numFaces() > temp_faces.length)
            temp_faces = new Face[2 * m.numFaces()];
        for (Face f : m.faces()) {
            if (!f.isTwoFaced()) { // try to cull face
                Vector4f n = f.norm(); 
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
        c.toViewSpace(m.vertices());
        c.project(m.vertices(), rasterizer.getGraphics().getWidth(), rasterizer.getGraphics().getHeight());
        switch (shadeMode) {
            case NO_SHADE:
                if (texture) {
                    for (int i = 0; i < curr_face_idx; i++) {
                        Face f = temp_faces[i];
                        Vector4f v0 = f.vertex1().pos();
                        Vector4f v1 = f.vertex2().pos();
                        Vector4f v2 = f.vertex3().pos();
                        rasterizer.fillTexturedTriangle(
                            round(v0.x()), round(v0.y()), v0.w(), tex_w * f.u1(), tex_h * f.v1(), 
                            round(v1.x()), round(v1.y()), v1.w(), tex_w * f.u2(), tex_h * f.v2(), 
                            round(v2.x()), round(v2.y()), v2.w(), tex_w * f.u3(), tex_h * f.v3(), 
                            curr_tex, false
                        );
                    }
                } else {
                    rasterizer.getGraphics().setColor(strokeRGB);
                    for (int i = 0; i < curr_face_idx; i++) {
                        Face f = temp_faces[i];
                        Vector4f v0 = f.vertex1().pos();
                        Vector4f v1 = f.vertex2().pos();
                        Vector4f v2 = f.vertex3().pos();
                        rasterizer.strokeTriangle(
                            round(v0.x()), round(v0.y()), v0.w(), 
                            round(v1.x()), round(v1.y()), v1.w(), 
                            round(v2.x()), round(v2.y()), v2.w()
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
                            round(f.vertex1().pos().x()), round(f.vertex1().pos().y()), f.vertex1().pos().w(), tex_w * f.u1(), tex_h * f.v1(), 
                            round(f.vertex2().pos().x()), round(f.vertex2().pos().y()), f.vertex2().pos().w(), tex_w * f.u2(), tex_h * f.v2(), 
                            round(f.vertex3().pos().x()), round(f.vertex3().pos().y()), f.vertex3().pos().w(), tex_w * f.u3(), tex_h * f.v3(),
                            curr_tex, true
                        );
                    }
                } else {
                    for (int i = 0; i < curr_face_idx; i++) {
                        Face f = temp_faces[i];
                        rasterizer.getGraphics().setColor(rgb(f.getTempRed(), f.getTempGreen(), f.getTempBlue()));
                        rasterizer.fillTriangle(
                            round(f.vertex1().pos().x()), round(f.vertex1().pos().y()), f.vertex1().pos().w(), 
                            round(f.vertex2().pos().x()), round(f.vertex2().pos().y()), f.vertex2().pos().w(), 
                            round(f.vertex3().pos().x()), round(f.vertex3().pos().y()), f.vertex3().pos().w()
                        );
                    }
                }
                break;
            case GOURAUD:
                if (texture) {
                    for (int i = 0; i < curr_face_idx; i++) {
                        Face f = temp_faces[i];
                        rasterizer.fillTexturedTriangleInterpolateColor(
                            round(f.vertex1().pos().x()), round(f.vertex1().pos().y()), f.vertex1().pos().w(), tex_w * f.u1(), tex_h * f.v1(), 255f * f.vertex1().getTempRed(), 255f * f.vertex1().getTempGreen(), 255f * f.vertex1().getTempBlue(), 
                            round(f.vertex2().pos().x()), round(f.vertex2().pos().y()), f.vertex2().pos().w(), tex_w * f.u2(), tex_h * f.v2(), 255f * f.vertex2().getTempRed(), 255f * f.vertex2().getTempGreen(), 255f * f.vertex2().getTempBlue(),
                            round(f.vertex3().pos().x()), round(f.vertex3().pos().y()), f.vertex3().pos().w(), tex_w * f.u3(), tex_h * f.v3(), 255f * f.vertex3().getTempRed(), 255f * f.vertex3().getTempGreen(), 255f * f.vertex3().getTempBlue(), 
                            curr_tex 
                        );
                    }
                } else {
                    for (int i = 0; i < curr_face_idx; i++) {
                        Face f = temp_faces[i];
                        rasterizer.fillTriangleInterpolateColor(
                            round(f.vertex1().pos().x()), round(f.vertex1().pos().y()), f.vertex1().pos().w(), 255f * f.vertex1().getTempRed(), 255f * f.vertex1().getTempGreen(), 255f * f.vertex1().getTempBlue(), 
                            round(f.vertex2().pos().x()), round(f.vertex2().pos().y()), f.vertex2().pos().w(), 255f * f.vertex2().getTempRed(), 255f * f.vertex2().getTempGreen(), 255f * f.vertex2().getTempBlue(),
                            round(f.vertex3().pos().x()), round(f.vertex3().pos().y()), f.vertex3().pos().w(), 255f * f.vertex3().getTempRed(), 255f * f.vertex3().getTempGreen(), 255f * f.vertex3().getTempBlue() 
                        );
                    }
                }
                break;
        }
        m.reset();
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
    
    private void light(vec4 rgb, vec4 point, vec4 norm, vec4 view_pos) {
        rgb.set(0f, 0f, 0f);
        view_pos.sub(point, temp_vec1).normalize();
        vec4 view_dir = temp_vec1; // vector directed at the observer
        for (AmbientLight l : ambLights) {
            if (!l.enabled) 
                continue;
            rgb.x += curr_mat.getAmbientRed() * l.ar;
            rgb.y += curr_mat.getAmbientGreen() * l.ag;
            rgb.z += curr_mat.getAmbientBlue() * l.ab;
        }
        for (DirectionLight l : dirLights) {
            if (!l.enabled)
                continue;
            rgb.x += curr_mat.getAmbientRed() * l.ar;
            rgb.y += curr_mat.getAmbientGreen() * l.ag;
            rgb.z += curr_mat.getAmbientBlue() * l.ab;
            float dp = l.dir_inv.dot(norm);
            if (dp > 0f) {
                rgb.x += dp * curr_mat.getDiffuseRed() * l.dr;
                rgb.y += dp * curr_mat.getDiffuseGreen() * l.dg;
                rgb.z += dp * curr_mat.getDiffuseBlue() * l.db;
            }
//          note that, for example, for a cube whose side is 
//          represented by two triangles, the specular lighting 
//          component may be look ridiculous, because one of the triangles 
//          will have a color different from the other
            l.dir_inv.reflect(norm, temp_vec2);
            vec4 light_dir_reflected = temp_vec2; 
            dp = light_dir_reflected.dot(view_dir);
            if (dp > 0f) {
                float pow = (float) Math.pow(dp, curr_mat.getShininess());
                rgb.x += pow * curr_mat.getSpecularRed() * l.sr;
                rgb.y += pow * curr_mat.getSpecularGreen() * l.sg;
                rgb.z += pow * curr_mat.getSpecularBlue() * l.sb;
            }
        }
        for (PointLight l : pointLights) {
            if (!l.enabled)
                continue;
            temp_vec2.set(l.pos).sub(point, temp_vec2);
            vec4 light_dir = temp_vec2; // vector directed at the point light source
            float d_sqr = light_dir.len2(), d = (float) Math.sqrt(d_sqr);
            float attenuation = 1f / (l.kc + l.kl * d + l.kq * d_sqr);
            if (attenuation <= PointLight.ATTENUATION_EPS)
                continue;
            rgb.x += attenuation * curr_mat.getAmbientRed() * l.ar;
            rgb.y += attenuation * curr_mat.getAmbientGreen() * l.ag;
            rgb.z += attenuation * curr_mat.getAmbientBlue() * l.ab;
            light_dir.normalize_len_known(d);
            float dp = light_dir.dot(norm);
            if (dp > 0f) {
                rgb.x += attenuation * dp * curr_mat.getDiffuseRed() * l.dr;
                rgb.y += attenuation * dp * curr_mat.getDiffuseGreen() * l.dg;
                rgb.z += attenuation * dp * curr_mat.getDiffuseBlue() * l.db;
            }
            light_dir.reflect(norm, light_dir); 
            dp = light_dir.dot(view_dir);
            if (dp > 0f) {
                float pow = (float) Math.pow(dp, curr_mat.getShininess());
                rgb.x += attenuation * pow * curr_mat.getSpecularRed() * l.sr;
                rgb.y += attenuation * pow * curr_mat.getSpecularGreen() * l.sg;
                rgb.z += attenuation * pow * curr_mat.getSpecularBlue() * l.sb;
            }
        }
        rgb.set(rgb.x > 1f ? 1f: rgb.x, rgb.y > 1f ? 1f: rgb.y, rgb.z > 1f ? 1f: rgb.z);
    }
    
}
