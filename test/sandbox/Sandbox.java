package sandbox;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.util.Pair;
import javax.imageio.ImageIO;
import render.AABB;
import render.Bitmap;
import render.Camera;
import render.Face;
import render.Graphics;
import render.Graphics.DefaultGraphics;
import render.Light.DirectionLight;
import render.Material;
import render.Model;
import render.ModelInstance;
import render.Renderer;
import render.ShadeMode;
import render.Vertex;
import render.Vector3f;

/**
 *
 * @author Texhnolyze
 */
public class Sandbox {
    
    public static void main(String[] args) throws IOException {
        DefaultGraphics g = new DefaultGraphics(1920, 1080);
        Renderer r = new Renderer(g);
        r.setStroke(Graphics.WHITE);
        Camera c = new Camera(0.1f, 100f, 45, g.getWidth(), g.getHeight());
        c.setPosition(1, 1, 1);
        c.lookAt(0, 0, 0);
        c.rotate(0, 1, 0, 0, 0, 0, (float) Math.toRadians(45));
        c.rotate(0, 1, 0, 0, 0, 0, (float) Math.toRadians(45));
        float f = -0.5f;
        c.moveInDirection(f);
        c.updateViewMatrix();
        M m1 = fromOBJ(new File("test/sandbox/obj/cube.obj"), 0);
        MI instance1 = new MI();
        instance1.m = m1;
        r.getDirectionLights().add(new DirectionLight(
            1f, 1f, 1f, -1f, -1f, -1f
        ));
        r.render(c, instance1);
        long t = System.nanoTime();
//        r.render(c, instance2, ShadeMode.FLAT, true);
        System.out.println(System.nanoTime() - t);
        r.drawZBuffer(r.getGraphics());
        ImageIO.write(g.getAsImage(), "jpg", new File("./test/sandbox/out/" + 1 + ".jpg"));
       
    }
    
    static M fromOBJ(File OBJFile, float alpha) throws IOException {
        float temp_x, temp_z;
        float ca = (float) Math.cos(alpha), sa = (float) Math.sin(alpha);
        List<Vec> normals = new ArrayList<>();
        List<Pair<Float, Float>> tex_coords = new ArrayList<>();
        M m = new M();
        Scanner s = new Scanner(OBJFile);
        while (s.hasNextLine()) {
            String[] split = s.nextLine().split(" ");
            switch (split[0]) {
                case "v":
                    {
                        V v = new V();
                        v.pos = new Vec();
                        v.pos.x = Float.parseFloat(split[1]);
                        v.pos.y = Float.parseFloat(split[2]);
                        v.pos.z = Float.parseFloat(split[3]);
                        temp_x = v.pos.x * ca - v.pos.z * sa;
                        temp_z = v.pos.x * sa + v.pos.z * ca;
                        v.pos.x = temp_x;
                        v.pos.z = temp_z;
                        m.vertices.add(v);
                        break;
                    }
                case "vn":
                    Vec n = new Vec();
                    n.x = Float.parseFloat(split[1]);
                    n.y = Float.parseFloat(split[2]);
                    n.z = Float.parseFloat(split[3]);
                    temp_x = n.x * ca - n.z * sa;
                    temp_z = n.x * sa + n.z * ca;
                    n.x = temp_x;
                    n.z = temp_z;
                    normals.add(n);
                    break;
                case "f":
                    F f = new F();
                    String[] v0 = split[1].split("/");
                    String[] v1 = split[2].split("/");
                    String[] v2 = split[3].split("/");
                    f.vertex0 = (V) m.vertices.get(Integer.parseInt(v0[0]) - 1);
                    f.vertex1 = (V) m.vertices.get(Integer.parseInt(v1[0]) - 1);
                    f.vertex2 = (V) m.vertices.get(Integer.parseInt(v2[0]) - 1);
                    f.u1 = tex_coords.get(Integer.parseInt(v0[1]) - 1).getKey();
                    f.v1 = tex_coords.get(Integer.parseInt(v0[1]) - 1).getValue();
                    f.u2 = tex_coords.get(Integer.parseInt(v1[1]) - 1).getKey();
                    f.v2 = tex_coords.get(Integer.parseInt(v1[1]) - 1).getValue();
                    f.u3 = tex_coords.get(Integer.parseInt(v2[1]) - 1).getKey();
                    f.v3 = tex_coords.get(Integer.parseInt(v2[1]) - 1).getValue();
                    f.vertex0.norm = (Vec) normals.get(Integer.parseInt(v0[2]) - 1);
                    f.vertex1.norm = (Vec) normals.get(Integer.parseInt(v1[2]) - 1);
                    f.vertex2.norm = (Vec) normals.get(Integer.parseInt(v2[2]) - 1);
                    f.n = (Vec) normals.get(Integer.parseInt(v0[2]) - 1);
                    m.faces.add(f);
                    break;
                case "vt":
                    {
                        float u = Float.parseFloat(split[1]);
                        float v = Float.parseFloat(split[2]);
                        tex_coords.add(new Pair<>(u, v));
                        break;
                    }
                default:
                    break;
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

        float x, y, z, w;
        
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

        V vertex0, vertex1, vertex2;
        float u1, v1, u2, v2, u3, v3;
        Vec n;
        
        float r, g, b;

        @Override
        public Vertex vertex1() {
            return vertex0;
        }

        @Override
        public Vertex vertex2() {
            return vertex1;
        }

        @Override
        public Vertex vertex3() {
            return vertex2;
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

        @Override
        public float u1() {
            return u1;
        }

        @Override
        public float v1() {
            return v1;
        }

        @Override
        public float u2() {
            return u2;
        }

        @Override
        public float v2() {
            return v2;
        }

        @Override
        public float u3() {
            return u3;
        }

        @Override
        public float v3() {
            return v3;
        }
        
    }
    
    static class MI implements ModelInstance {

        M m;
        Material mat = Material.CHROME;
        
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

        @Override
        public Bitmap getTexture() {
            return new Bitmap() {
                BufferedImage img;
                {
                    try {
                        img = ImageIO.read(new File("2.jpg"));
                    } catch (IOException ex) {
                        Logger.getLogger(Sandbox.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                @Override
                public int getWidth() {
                    return img.getWidth();
                }

                @Override
                public int getHeight() {
                    return img.getHeight();
                }

                @Override
                public int getRGB(int x, int y) {
                    return img.getRGB(x, y);
                }
            };
        }

        @Override
        public boolean testAABB() {
            return false;
        }

        @Override
        public ShadeMode getShadeMode() {
            return ShadeMode.GOURAUD;
        }

        @Override
        public boolean texture() {  
            return false;
        }

        @Override
        public boolean isShadowCaster() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean isShadowReceiver() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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

        @Override
        public boolean dirty() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void markAsDirty() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        
    }
    
}
