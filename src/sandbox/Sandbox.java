package sandbox;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import javax.imageio.ImageIO;
import render.AABB;
import render.Light.AmbientLight;
import render.Light.DirectionLight;
import render.Camera;
import render.Face;
import render.Graphics.DefaultGraphics;
import render.Light.PointLight;
import render.Material;
import render.Model;
import render.ModelInstance;
import render.Rasterizer3D;
import render.Renderer;
import render.Renderer.ShadeMode;
import render.Vector3f;
import render.Vertex;

/**
 *
 * @author Texhnolyze
 */
public class Sandbox {
    
    public static void main(String[] args) throws IOException {
        DefaultGraphics g = new DefaultGraphics(1024, 768);
        Renderer r = new Renderer(g);
        Rasterizer3D rast = r.getRasterizer();
//        rast.getGraphics().setColor(Graphics.WHITE);
//        rast.fillTriangle(500, -100, 0, 300, 300, 0, -300, 350, 0);
        Camera c = new Camera(10f, 100f, 45, g.getWidth(), g.getHeight());
        c.setPosition(0f, 0, 30);
        c.lookAt(0, 0, 0);
        c.moveInDirection(-43);
        c.rotate(0, 1, 0, 0, 0, 0, (float) Math.toRadians(0));
        c.rotateDirectionAround(0, 1, 0, (float) Math.toRadians(24));
        c.updateViewMatrix();
        M m = fromOBJ(new File("src/sandbox/obj/sphere.obj"));
        MI instance = new MI();
        instance.m = m;
        r.getAmbientLights().add(new AmbientLight(0.3f, 0.3f, 0.3f));
        r.getDirectionLights().add(new DirectionLight(
            0.3f, 0.3f, 0.3f,
            0.3f, 0.3f, 0.3f, 
            0.6f, 0.6f, 0.6f, 
            -1f, -1f, -1f)
        );
        r.getPointLights().add(new PointLight(
            0.5f, 0.5f, 0.5f,
            0.3f, 0.4f, 0.4f,
            0.4f, 0.5f, 0.1f,
            0f, 0f, 1f,
            1f, 0.5f, 0.1f
        ));
//        r.render(c, instance, ShadeMode.GOURAUD);
        long t = System.nanoTime();
        r.render(c, instance, ShadeMode.GOURAUD);
        System.out.println(System.nanoTime() - t);
        ImageIO.write(g.getAsImage(), "jpg", new File("./src/sandbox/out/1.jpg"));
    }
    
    static M fromOBJ(File OBJFile) throws IOException {
        List<Vec> normals = new ArrayList<>();
        M m = new M();
        Scanner s = new Scanner(OBJFile);
        while (s.hasNextLine()) {
            String[] split = s.nextLine().split(" ");
            if (split[0].equals("v")) {
                V v = new V();
                v.pos = new Vec();
                v.pos.x = Float.parseFloat(split[1]);
                v.pos.y = Float.parseFloat(split[2]);
                v.pos.z = Float.parseFloat(split[3]);
                m.vertices.add(v);
            } else if (split[0].equals("vn")) {
                Vec n = new Vec();
                n.x = Float.parseFloat(split[1]);
                n.y = Float.parseFloat(split[2]);
                n.z = Float.parseFloat(split[3]);
                normals.add(n);
            } else if (split[0].equals("f")) {
                F f = new F();
                String[] v0 = split[1].split("/");
                String[] v1 = split[2].split("/");
                String[] v2 = split[3].split("/");
                f.v0 = (V) m.vertices.get(Integer.parseInt(v0[0]) - 1);
                f.v1 = (V) m.vertices.get(Integer.parseInt(v1[0]) - 1);
                f.v2 = (V) m.vertices.get(Integer.parseInt(v2[0]) - 1);
                f.v0.norm = (Vec) normals.get(Integer.parseInt(v0[2]) - 1);
                f.v1.norm = (Vec) normals.get(Integer.parseInt(v1[2]) - 1);
                f.v2.norm = (Vec) normals.get(Integer.parseInt(v2[2]) - 1);                
                f.n = (Vec) normals.get(Integer.parseInt(v0[2]) - 1);
                m.faces.add(f);
            }
        }
        return m;
    }
    
    static class V implements Vertex {

        Vec pos;
        Vec norm;
        float r = -1f, g, b;
        
        @Override
        public Vector3f pos() {
            return pos;
        }

        @Override
        public Vector3f norm() {
            return norm;
        }
        
        @Override
        public boolean lighted() {
            return r != -1f;
        }

        @Override
        public void setTempRGB(float r, float g, float b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }

        @Override
        public float getTempRed() {
            return r;
        }
        
        @Override
        public float getTempGreen() {
            return g;
        }
        
        @Override
        public float getTempBlue() {
            return b;
        }
        
    }
    
    
    static class Vec implements Vector3f {

        float x, y, z;
        
        @Override
        public float x() {
            return x;
        }

        @Override
        public float y() {
            return y;
        }

        @Override
        public float z() {
            return z;
        }

        @Override
        public Vector3f set(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }
        
    }
    
    static class F implements Face {

        V v0, v1, v2;
        Vec n;
        
        float r, g, b;
        
        @Override
        public Vector3f norm() {
            return n;
        }

        @Override
        public Vertex v0() {
            return v0;
        }

        @Override
        public Vertex v1() {
            return v1;
        }

        @Override
        public Vertex v2() {
            return v2;
        }

        @Override
        public boolean isTwoFaced() {
            return false;
        }

        @Override
        public void setTempRGB(float r, float g, float b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }
        
        @Override
        public float getTempRed() {
            return r;
        }
            
        @Override
        public float getTempGreen() {
            return g;
        }
        
        @Override
        public float getTempBlue() {
            return b;
        }
        
    }
    
    static class MI implements ModelInstance {

        M m;
        Material mat;
        
        @Override
        public Model getModel() {
            return m;
        }

        @Override
        public Material getMaterial() {
            return mat;
        }

        @Override
        public void translateModelIntoWorldSpace() {
        }

        @Override
        public AABB getAABB() {
            return null;
        }
        
    }
    
    static class M implements render.Model {

        List<Face> faces = new ArrayList<>();
        List<Vertex> vertices = new ArrayList<>();
        
        
        
        @Override
        public int numFaces() {
            return faces.size();
        }

        @Override
        public Iterable<Face> faces() {
            return faces;
        }
 
        public Iterable<Vertex> vertices() {
            return vertices;
        }

        @Override
        public void reset() {
            
        }

        @Override
        public int numVertices() {
            return vertices.size();
        }
        
    }
    
}
