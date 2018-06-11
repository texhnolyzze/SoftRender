package render;

import java.util.ArrayList;
import java.util.List;
import render.Light.AmbientLight;
import render.Light.DirectionLight;
import render.Light.PointLight;
import static render.MathUtils.round;

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
    private Face[] faces = new Face[1024];
    
    public Renderer(Graphics g) {
        rasterizer = new Rasterizer3D(g);
    }
    
    public Rasterizer3D getRasterizer() {
        return rasterizer;
    }
    
    public void addAmbientLight(AmbientLight light) {ambLights.add(light);}
    public void addDirectionLight(DirectionLight light) {dirLights.add(light);}
    public void addPointLight(PointLight light) {pointLights.add(light);}
    
//  simple wireframe rendering
    public void render(Camera c, ModelInstance instance) {
        Model m = instance.getModel();
        if (m.numFaces() > faces.length)
            faces = new Face[2 * m.numFaces()];
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
            faces[curr_face_idx++] = f;
        }
        c.toViewSpace(m.vertices());
        c.project(m.vertices(), rasterizer.getGraphics().getWidth(), rasterizer.getGraphics().getHeight());
        for (int i = 0; i < curr_face_idx; i++) {
            Face f = faces[i];
            Vector3f v0 = f.v0().pos();
            Vector3f v1 = f.v1().pos();
            Vector3f v2 = f.v2().pos();
            rasterizer.drawTriangleWireframe(
                round(v0.x()), round(v0.y()), v0.z(), 
                round(v1.x()), round(v1.y()), v1.z(), 
                round(v2.x()), round(v2.y()), v2.z(), 
                Graphics.WHITE
            );
        }
        m.reset();
        curr_face_idx = 0;
    }
    
    private void lightVertex(Vertex v, Material m) {
        float r = 0f, g = 0f, b = 0f;
        v.setTempColor(r > 1f ? 1f : r, g > 1f ? 1f : g, b > 1f ? 1f : b);
        v.markAsLighted();
    }
    
    
}
