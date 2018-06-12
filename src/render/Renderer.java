package render;

import java.util.ArrayList;
import java.util.List;
import static render.Graphics.rgb;
import render.BaseLight.AmbientLight;
import render.BaseLight.DirectionLight;
import render.BaseLight.PointLight;
import static render.MathUtils.clamp;
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
    
    private int curr_face_idx;
    private Face[] temp_faces = new Face[1024];
    
    private vec3 temp_vec1 = new vec3(), temp_vec2 = new vec3(), temp_vec3 = new vec3();
    
    private Camera curr_camera;
    
    public Renderer(Graphics g) {
        rasterizer = new Rasterizer3D(g);
    }
    
    public Rasterizer3D getRasterizer() {
        return rasterizer;
    }
    
    public void addAmbientLight(AmbientLight light) {ambLights.add(light);}
    public void addDirectionLight(DirectionLight light) {dirLights.add(light);}
    public void addPointLight(PointLight light) {pointLights.add(light);}
    
    public enum ShadeMode {
        WIREFRAME, FLAT, GOURAUD
    }
    
    public void render(Camera c, ModelInstance instance, final ShadeMode shadeMode) {
        curr_camera = c;
        Model m = instance.getModel();
        final Material mat = instance.getMaterial();
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
        }
        c.toViewSpace(m.vertices());
        c.project(m.vertices(), rasterizer.getGraphics().getWidth(), rasterizer.getGraphics().getHeight());
        switch (shadeMode) {
            case WIREFRAME:
                int rgb = rgb(mat.ar, mat.ag, mat.ab); // use material ambient color
                for (int i = 0; i < curr_face_idx; i++) {
                    Face f = temp_faces[i];
                    Vector3f v0 = f.v0().pos();
                    Vector3f v1 = f.v1().pos();
                    Vector3f v2 = f.v2().pos();
                    rasterizer.drawTriangleWireframe(
                        round(v0.x()), round(v0.y()), v0.z(), 
                        round(v1.x()), round(v1.y()), v1.z(), 
                        round(v2.x()), round(v2.y()), v2.z(), 
                        rgb
                    );
                }
                break;
            case FLAT:
                for (int i = 0; i < curr_face_idx; i++) {
                    Face f = temp_faces[i];
                    rasterizer.drawTriangleFlatShading(
                        round(f.v0().pos().x()), round(f.v0().pos().y()), f.v0().pos().z(), 
                        round(f.v1().pos().x()), round(f.v1().pos().y()), f.v1().pos().z(), 
                        round(f.v2().pos().x()), round(f.v2().pos().y()), f.v2().pos().z(), 
                        rgb(f.getTempRed(), f.getTempGreen(), f.getTempBlue())
                    );
                }
                break;
            case GOURAUD:
                // not ready yet
                break;
        }
        m.reset();
        curr_face_idx = 0;
    }
    
    // the face is lighting in the world space
    private void lightFace(Face f, final Material m) {
        float nx = f.norm().x();
        float ny = f.norm().y();
        float nz = f.norm().z();
        f.getAVGPoint(temp_vec1);
        float r = 0f, g = 0f, b = 0f;
        for (AmbientLight l : ambLights) {
            if (!l.enabled) 
                continue;
            r += m.ar * l.ar;
            g += m.ag * l.ag;
            b += m.ab * l.ab;
        }
        for (DirectionLight l : dirLights) {
            if (!l.enabled)
                continue;
            r += m.ar * l.ar;
            g += m.ag * l.ag;
            b += m.ab * l.ab;
            float dp = l.dir.dot(nx, ny, nz);
            if (dp < 0f) {
                r += -dp * m.dr * l.dr;
                g += -dp * m.dg * l.dg;
                b += -dp * m.db * l.db;
            }
            l.dir.reflect(nx, ny, nz, temp_vec2);
            temp_vec1.sub(curr_camera.pos, temp_vec3);
            dp = temp_vec2.dot(temp_vec3);
            if (dp > 0f) {
                float pow = (float) Math.pow(dp, m.shininess);
                r += pow * m.sr * l.sr;
                g += pow * m.sg * l.sg;
                b += pow * m.sb * l.sb;
            }
        }
        f.setTempRGB(r > 1f ? 1f : r, g > 1f ? 1f : g, b > 1f ? 1f : b);
    }
    
    private void lightVertex(Vertex v, final Material m) {
        // not ready yet
    }
    
}
