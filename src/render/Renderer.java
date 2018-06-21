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

    public enum ShadeMode {
        WIREFRAME, FLAT, GOURAUD
    }
    
    private final Rasterizer3D rasterizer;
    
    private final List<AmbientLight> ambLights = new ArrayList<>();
    private final List<DirectionLight> dirLights = new ArrayList<>();
    private final List<PointLight> pointLights = new ArrayList<>();
    
    private int curr_face_idx;
    private Face[] temp_faces = new Face[1024];
    
    private vec3 
            temp_vec1 = new vec3(), 
            temp_vec2 = new vec3(), 
            rgb_vec = new vec3(),
            norm_vec = new vec3(),
            point_vec = new vec3();
    
    private Camera curr_camera;
    
    public Renderer(Graphics g) {
        rasterizer = new Rasterizer3D(g);
    }
    
    public Rasterizer3D getRasterizer() {
        return rasterizer;
    }
    
    public List<AmbientLight> getAmbientLights() {return ambLights;}
    public List<DirectionLight> getDirectionLights() {return dirLights;}
    public List<PointLight> getPointLights() {return pointLights;}
    
    public void render(Camera c, ModelInstance instance, final ShadeMode shadeMode) {
        curr_camera = c;
        AABB aabb = instance.getAABB();
        if (aabb != null) {
            if (!c.testAABB(aabb))
                return;
        }
        instance.translateModelIntoWorldSpace();
        Model m = instance.getModel();
        final Material mat = instance.getMaterial() == null ? Material.DEFAULT : instance.getMaterial();
        if (m.numFaces() > temp_faces.length)
            temp_faces = new Face[2 * m.numFaces()];
        float dir_x = c.getDirectionX();
        float dir_y = c.getDirectionY();
        float dir_z = c.getDirectionZ();
        for (Face f : m.faces()) {
            if (!f.isTwoFaced()) { // try to cull face
                Vector3f n = f.norm();
                float nx = n.x();
                float ny = n.y();
                float nz = n.z();
                if (MathUtils.dot(dir_x, dir_y, dir_z, nx, ny, nz) >= 0.0f) 
                    continue;   
            }
            temp_faces[curr_face_idx++] = f;
        }
        switch (shadeMode) {
            case FLAT:
                for (int i = 0; i < curr_face_idx; i++) 
                    lightFace(temp_faces[i], mat);
                break;
            case GOURAUD:
                for (int i = 0; i < curr_face_idx; i++) {
                    Face f = temp_faces[i];
                    Vertex v0 = f.v0(), v1 = f.v1(), v2 = f.v2();
                    if (!v0.lighted()) lightVertex(v0, mat);
                    if (!v1.lighted()) lightVertex(v1, mat);
                    if (!v2.lighted()) lightVertex(v2, mat);
                }
                break;
        }
        c.toViewSpace(m.vertices());
        c.project(m.vertices(), rasterizer.getGraphics().getWidth(), rasterizer.getGraphics().getHeight());
        switch (shadeMode) {
            case WIREFRAME:
                int rgb = rgb(mat.ar, mat.ag, mat.ab); // use material ambient color
                rasterizer.getGraphics().setColor(rgb);
                for (int i = 0; i < curr_face_idx; i++) {
                    Face f = temp_faces[i];
                    Vector3f v0 = f.v0().pos();
                    Vector3f v1 = f.v1().pos();
                    Vector3f v2 = f.v2().pos();
                    rasterizer.strokeTriangle(
                        round(v0.x()), round(v0.y()), v0.z(), 
                        round(v1.x()), round(v1.y()), v1.z(), 
                        round(v2.x()), round(v2.y()), v2.z()
                    );
                }
                break;
            case FLAT:
                for (int i = 0; i < curr_face_idx; i++) {
                    Face f = temp_faces[i];
                    rasterizer.getGraphics().setColor(rgb(f.getTempRed(), f.getTempGreen(), f.getTempBlue()));
                    rasterizer.fillTriangle(
                        round(f.v0().pos().x()), round(f.v0().pos().y()), f.v0().pos().z(), 
                        round(f.v1().pos().x()), round(f.v1().pos().y()), f.v1().pos().z(), 
                        round(f.v2().pos().x()), round(f.v2().pos().y()), f.v2().pos().z()
                    );
                }
                break;
            case GOURAUD:
                for (int i = 0; i < curr_face_idx; i++) {
                    Face f = temp_faces[i];
                    rasterizer.fillTriangleInterpolateColors(
                        round(f.v0().pos().x()), round(f.v0().pos().y()), f.v0().pos().z(), f.v0().getTempRed(), f.v0().getTempGreen(), f.v0().getTempBlue(), 
                        round(f.v1().pos().x()), round(f.v1().pos().y()), f.v1().pos().z(), f.v1().getTempRed(), f.v1().getTempGreen(), f.v1().getTempBlue(),
                        round(f.v2().pos().x()), round(f.v2().pos().y()), f.v2().pos().z(), f.v2().getTempRed(), f.v2().getTempGreen(), f.v2().getTempBlue() 
                    );
                }
                break;
        }
        m.reset();
        curr_face_idx = 0;
    }
    
//  the face is lighting in the world space
    private void lightFace(Face f, final Material m) {
        norm_vec.set(f.norm());
        f.getMediPoint(point_vec);
        light(rgb_vec, point_vec, norm_vec, curr_camera.pos, m);
        f.setTempRGB(rgb_vec.x, rgb_vec.y, rgb_vec.z);
    }
    
    private void lightVertex(Vertex v, Material m) {
        norm_vec.set(v.norm());
        point_vec.set(v.pos());
        light(rgb_vec, point_vec, norm_vec, curr_camera.pos, m);
        v.setTempRGB(rgb_vec.x, rgb_vec.y, rgb_vec.z);
    }
    
    private void light(vec3 rgb, vec3 point, vec3 norm, vec3 view_pos, Material m) {
        rgb.set(0f, 0f, 0f);
        view_pos.sub(point, temp_vec1).normalize();
        vec3 view_dir = temp_vec1; // vector directed at the observer
        for (AmbientLight l : ambLights) {
            if (!l.enabled) 
                continue;
            rgb.x += m.ar * l.ar;
            rgb.y += m.ag * l.ag;
            rgb.z += m.ab * l.ab;
        }
        for (DirectionLight l : dirLights) {
            if (!l.enabled)
                continue;
            rgb.x += m.ar * l.ar;
            rgb.y += m.ag * l.ag;
            rgb.z += m.ab * l.ab;
            float dp = l.dir_inv.dot(norm);
            if (dp > 0f) {
                rgb.x += dp * m.dr * l.dr;
                rgb.y += dp * m.dg * l.dg;
                rgb.z += dp * m.db * l.db;
            }
//          note that, for example, for a cube whose side is 
//          represented by two triangles, the specular lighting 
//          component may be look ridiculous, because one of the triangles 
//          will have a color different from the other
            l.dir_inv.reflect(norm, temp_vec2);
            vec3 light_dir_reflected = temp_vec2; 
            dp = light_dir_reflected.dot(view_dir);
            if (dp > 0f) {
                float pow = (float) Math.pow(dp, m.shininess);
                rgb.x += pow * m.sr * l.sr;
                rgb.y += pow * m.sg * l.sg;
                rgb.z += pow * m.sb * l.sb;
            }
        }
        for (PointLight l : pointLights) {
            if (!l.enabled)
                continue;
            temp_vec2.set(l.pos).sub(point, temp_vec2);
            vec3 light_dir = temp_vec2; // vector directed at the point light source
            float d_sqr = light_dir.len2(), d = (float) Math.sqrt(d_sqr);
            float attenuation = 1f / (l.kc + l.kl * d + l.kq * d_sqr);
            if (attenuation < 0.0001f)
                continue;
            rgb.x += attenuation * m.ar * l.ar;
            rgb.y += attenuation * m.ag * l.ag;
            rgb.z += attenuation * m.ab * l.ab;
            light_dir.normalize_len_known(d);
            float dp = light_dir.dot(norm);
            if (dp > 0f) {
                rgb.x += attenuation * dp * m.dr * l.dr;
                rgb.y += attenuation * dp * m.dg * l.dg;
                rgb.z += attenuation * dp * m.db * l.db;
            }
            light_dir.reflect(norm, light_dir); 
            dp = light_dir.dot(view_dir);
            if (dp > 0f) {
                float pow = (float) Math.pow(dp, m.shininess);
                rgb.x += attenuation * pow * m.sr * l.sr;
                rgb.y += attenuation * pow * m.sg * l.sg;
                rgb.z += attenuation * pow * m.sb * l.sb;
            }
        }
        rgb.set(rgb.x > 1f ? 1f: rgb.x, rgb.y > 1f ? 1f: rgb.y, rgb.z > 1f ? 1f: rgb.z);
    }
    
    private static int toRGB(vec3 rgb) {
        return rgb(rgb.x, rgb.y, rgb.z);
    }
    
}
