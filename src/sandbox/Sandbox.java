package sandbox;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import javax.imageio.ImageIO;
import render.BaseLight.AmbientLight;
import render.BaseLight.DirectionLight;
import render.Camera;
import render.Face;
import render.Graphics;
import render.Graphics.DefaultGraphics;
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
//        rast.drawTriangleFlatShading(570, 348, ca, 512, 378, ca, 453, 348, ca, Graphics.WHITE);
        Camera c = new Camera(1f, 100f, 45, g.getWidth(), g.getHeight());
        c.setPosition(0f, 30, -50);
        c.lookAt(0, 0, 0);
        c.moveInDirection(30);
        c.rotate(0, 1, 0, 0, 0, 0, (float) Math.toRadians(115));
        c.updateViewMatrix();
        M m = fromOBJ(new File("src/sandbox/obj/cube.obj"));
        MI instance = new MI();
        instance.m = m;
        instance.mat = new Material(0.24725f, 0.1995f, 0.0745f, 0.75164f, 0.60648f, 0.22648f, 0.628281f, 0.555802f, 0.366065f, 0.4f);
        r.addAmbientLight(new AmbientLight(1f, 1f, 1f));
        r.addDirectionLight(new DirectionLight(0.1f, 0.3f, 0.2f, 0.5f, 0.5f, 0.5f, 0.3f, 0.2f, 0.1f, 64f, 43f, 5f));
        long t = System.nanoTime();
        r.render(c, instance, ShadeMode.FLAT);
        System.out.println(System.nanoTime() - t);
        ImageIO.write(g.getAsImage(), "jpg", new File("./src/sandbox/out/1.jpg"));
    }
    
    static float angle = (float) Math.toRadians(0);
    static float ca = (float) Math.cos(angle);
    static float sa = (float) Math.sin(angle);
    static float temp_x, temp_z;
    
    static M fromOBJ(File OBJFile) throws IOException {
        List<Vec> normals = new ArrayList<>();
        M m = new M();
        Scanner s = new Scanner(OBJFile);
        while (s.hasNextLine()) {
            String[] split = s.nextLine().split(" ");
            if (split[0].equals("v")) {
                V v = new V();
                v.pos = new Vec();
                v.pos.x = Float.parseFloat(split[1]) * 10;
                v.pos.y = Float.parseFloat(split[2]) * 10;
                v.pos.z = Float.parseFloat(split[3]) * 10;
                temp_x = v.pos.x * ca - v.pos.z * sa;
                temp_z = v.pos.x * sa + v.pos.z * ca;
                v.pos.x = temp_x;
                v.pos.z = temp_z;
                m.vertices.add(v);
            } else if (split[0].equals("vn")) {
                Vec n = new Vec();
                n.x = Float.parseFloat(split[1]);
                n.y = Float.parseFloat(split[2]);
                n.z = Float.parseFloat(split[3]);
                temp_x = n.x * ca - n.z * sa;
                temp_z = n.x * sa + n.z * ca;
                n.x = temp_x;
                n.z = temp_z;
                normals.add(n);
            } else if (split[0].equals("f")) {
                F f = new F();
                String[] v0 = split[1].split("/");
                String[] v1 = split[2].split("/");
                String[] v2 = split[3].split("/");
                f.v0 = (V) m.vertices.get(Integer.parseInt(v0[0]) - 1);
                f.v1 = (V) m.vertices.get(Integer.parseInt(v1[0]) - 1);
                f.v2 = (V) m.vertices.get(Integer.parseInt(v2[0]) - 1);
                f.n = (Vec) normals.get(Integer.parseInt(v0[2]) - 1);
                m.faces.add(f);
            }
        }
        return m;
    }
    
    static class V implements Vertex {

        Vec pos;
        
        @Override
        public Vector3f pos() {
            return pos;
        }

        @Override
        public Vector3f norm() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void markAsLighted() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void markAsNotLighted() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void setTempColor(float r, float g, float b) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public float getTempColorRed() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public float getTempColorGreen() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public float getTempColorBlue() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean lighted() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
        
    }
    
}
