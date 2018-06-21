package render;

import java.util.Arrays;
import static render.Graphics.*;
import static render.MathUtils.*;
import static render.MathUtils.Fixed.*;

/**
 *
 * @author Texhnolyze
 */
public final class Rasterizer3D {
    
    private static final int X_LESS_0 = 8,
                             Y_LESS_0 = 2,
                             X_GREATER_MAX_X = 4,
                             Y_GREATER_MAX_Y = 1;
    
    private final Graphics g;
    
    private int w, h;
    private int x_max, y_max;
    
    private int temp_x, temp_y;
    
    private int temp_int;
    private float temp_float;
    
    private int temp_code1, temp_code2;
    
    private int line_x1, line_y1, line_x2, line_y2;
    private float line_z1, line_z2;
    
    private float[] z_buff;
    private float[] sqrt_table;
    
    public Rasterizer3D(Graphics g) {
        this.g = g;
        updateBounds();
        for (int i = 0; i < tri_points.length; i++)
            tri_points[i] = new TriPoint();
    }
    
    public Graphics getGraphics() {
        return g;
    }
    
    public void updateBounds() {
        if (g.getWidth() != w || g.getHeight() != h) {
            w = g.getWidth();
            h = g.getHeight();
            x_max = w - 1;
            y_max = h - 1;
            z_buff = new float[w * h];
            sqrt_table = buildSqrtTable(w, h);
            clearZBuffer();
        }
    }
    
    public void clearZBuffer() {
        for (int i = 0; i < z_buff.length; i++)
            z_buff[i] = Float.POSITIVE_INFINITY;
    }
    
    public void drawZBuffer(Graphics g) {
        if (w < g.getWidth() || h < g.getHeight())
            return;
        float min = Float.POSITIVE_INFINITY, max = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < z_buff.length; i++) {
            if (z_buff[i] < min) min = z_buff[i];
            if (z_buff[i] > max && z_buff[i] != Float.POSITIVE_INFINITY) max = z_buff[i];
        }
        if (min == Float.POSITIVE_INFINITY) { // z_buffer is empty.
            for (int i = 0; i < z_buff.length; i++) 
                g.plot(i, i, Graphics.BLACK);
        } else {
            float dz = max - min;
            for (int i = 0; i < z_buff.length; i++) {
                int gray = (int) (255f * (z_buff[i] / dz));
                g.plot(i % w, i / w, rgb(gray, gray, gray));
            }
        }
    }
    
    public void strokeTriangle(int x1, int y1, float z1, int x2, int y2, float z2, int x3, int y3, float z3) {
        line(x1, y1, z1, x2, y2, z2);
        line(x2, y2, z2, x3, y3, z3);
        line(x3, y3, z3, x1, y1, z1);
    }
    
    public void fillTriangle(int x1, int y1, float z1, int x2, int y2, float z2, int x3, int y3, float z3) {
        code1 = code(x1, y1);
        code2 = code(x2, y2);
        code3 = code(x3, y3);
        if ((code1 | code2 | code3) == 0) {// all points are inside the viewport
            if (y1 < y3) {
                if (y2 < y3) {
                    fill_tri(
                        x1, y1, z1, 
                        x2, y2, z2, 
                        x3, y3, z3
                    );
                } else {
                    fill_tri(
                        x1, y1, z1, 
                        x3, y3, z3, 
                        x2, y2, z2
                    );
                }
            } else {
                if (y1 > y2) {
                    fill_tri(
                        x3, y3, z3, 
                        x2, y2, z2,
                        x1, y1, z1
                    );
                } else {
                    fill_tri(
                        x3, y3, z3,
                        x1, y1, z1, 
                        x2, y2, z2
                    );
                }
            }
        } else if ((code1 & code2 & code3) != 0) { // triangle is not visible
            return;
        } else {
            this.z1 = z1;
            this.z2 = z2;
            this.z3 = z3;
            clip_triangle(x1, y1, x2, y2, x3, y3, false);
            TriPoint p0 = tri_points[0];
            for (int i = 1; i < n - 1; i++) {
                TriPoint p1 = tri_points[i];
                TriPoint p2 = tri_points[i + 1];
                fill_tri(
                    p2.x, p2.y, p2.z,
                    p1.x, p1.y, p1.z, 
                    p0.x, p0.y, p0.z
                );
            }
        }
    }
    
    public void fillTriangleInterpolateColors(
            int x1, int y1, float z1, float r1, float g1, float b1, 
            int x2, int y2, float z2, float r2, float g2, float b2, 
            int x3, int y3, float z3, float r3, float g3, float b3) {

        code1 = code(x1, y1);
        code2 = code(x2, y2);
        code3 = code(x3, y3);
        if ((code1 | code2 | code3) == 0) { // all points are inside the viewport
            if (y1 < y3) {
                if (y2 < y3) {
                    fill_tri_interpolate_color(
                        x1, y1, z1, make_fixed_8x24(255f * r1), make_fixed_8x24(255f * g1), make_fixed_8x24(255f * b1),
                        x2, y2, z2, make_fixed_8x24(255f * r2), make_fixed_8x24(255f * g2), make_fixed_8x24(255f * b2), 
                        x3, y3, z3, make_fixed_8x24(255f * r3), make_fixed_8x24(255f * g3), make_fixed_8x24(255f * b3)
                    );
                } else {
                    fill_tri_interpolate_color(
                        x1, y1, z1, make_fixed_8x24(255f * r1), make_fixed_8x24(255f * g1), make_fixed_8x24(255f * b1), 
                        x3, y3, z3, make_fixed_8x24(255f * r3), make_fixed_8x24(255f * g3), make_fixed_8x24(255f * b3), 
                        x2, y2, z2, make_fixed_8x24(255f * r2), make_fixed_8x24(255f * g2), make_fixed_8x24(255f * b2)
                    );
                }
            } else {
                if (y1 > y2) {
                    fill_tri_interpolate_color(
                        x3, y3, z3, make_fixed_8x24(255f * r3), make_fixed_8x24(255f * g3), make_fixed_8x24(255f * b3), 
                        x2, y2, z2, make_fixed_8x24(255f * r2), make_fixed_8x24(255f * g2), make_fixed_8x24(255f * b2), 
                        x1, y1, z1, make_fixed_8x24(255f * r1), make_fixed_8x24(255f * g1), make_fixed_8x24(255f * b1)
                    );
                } else {
                    fill_tri_interpolate_color(
                        x3, y3, z3, make_fixed_8x24(255f * r3), make_fixed_8x24(255f * g3), make_fixed_8x24(255f * b3), 
                        x1, y1, z1, make_fixed_8x24(255f * r1), make_fixed_8x24(255f * g1), make_fixed_8x24(255f * b1), 
                        x2, y2, z2, make_fixed_8x24(255f * r2), make_fixed_8x24(255f * g2), make_fixed_8x24(255f * b2)
                    );
                }
            }
        }
        else if ((code1 & code2 & code3) != 0) // triangle is not visible
            return;
        else {
            this.r1 = r1;
            this.r2 = r2;
            this.r3 = r3;
            this.g1 = g1;
            this.g2 = g2;
            this.g3 = g3;
            this.b1 = b1;
            this.b2 = b2;
            this.b3 = b3;
            this.z1 = z1;
            this.z2 = z2;
            this.z3 = z3;
            clip_triangle(x1, y1, x2, y2, x3, y3, true);
            TriPoint p0 = tri_points[0];
            int r0 = make_fixed_8x24(255f * p0.r);
            int g0 = make_fixed_8x24(255f * p0.g);
            int b0 = make_fixed_8x24(255f * p0.b);
            for (int i = 1; i < n - 1; i++) {
                TriPoint p1 = tri_points[i];
                TriPoint p2 = tri_points[i + 1];
                fill_tri_interpolate_color(
                    p2.x, p2.y, p2.z, make_fixed_8x24(255f * p2.r), make_fixed_8x24(255f * p2.g), make_fixed_8x24(255f * p2.b),
                    p1.x, p1.y, p1.z, make_fixed_8x24(255f * p1.r), make_fixed_8x24(255f * p1.g), make_fixed_8x24(255f * p1.b), 
                    p0.x, p0.y, p0.z, r0, g0, b0
                );
            }
        }
    }
    
//  these variables need for triangle clipping
    private float r1, g1, b1;
    private float r2, g2, b2;
    private float r3, g3, b3;
    private float z1, z2, z3;
    private int code1, code2, code3;
    
    private int n;
    private final TriPoint[] tri_points = new TriPoint[10];
    
//  stores in the "tri_points" array the clipped points belonging to the triangle 
//  in the counterclockwise order
    private void clip_triangle(int x1, int y1, int x2, int y2, int x3, int y3, final boolean interpolate_colors) {
        n = 0;
        int max_y_index = -1;
        temp_code1 = code1;
        temp_code2 = code2;
        clip0(x1, y1, x2, y2);
        if (line_x1 != -1) {
            float dist_inv = -1f;
            tri_points[0].x = line_x1;
            tri_points[0].y = line_y1;
            tri_points[1].x = line_x2;
            tri_points[1].y = line_y2;
            
            if (line_x1 != x1 || line_y1 != y1) { // need to recalculate depth and color
                dist_inv = 1f / MathUtils.dist(x1, y1, x2, y2);
                float coeff = dist_inv * MathUtils.dist(x1, y1, line_x1, line_y1);
                tri_points[0].z = z1 + (z2 - z1) * coeff;
                if (interpolate_colors) {
                    tri_points[0].r = r1 + (r2 - r1) * coeff;
                    tri_points[0].g = g1 + (g2 - g1) * coeff;
                    tri_points[0].b = b1 + (b2 - b1) * coeff;
                }
            } else {
                tri_points[0].z = z1;
                if (interpolate_colors) {
                    tri_points[0].r = r1;
                    tri_points[0].g = g1;
                    tri_points[0].b = b1;
                }
            }
            
            if (line_x2 != x2 || line_y2 != y2) {
                float coeff;
                if (dist_inv == -1f)
                    coeff = MathUtils.dist(x2, y2, line_x2, line_y2) / MathUtils.dist(x1, y1, x2, y2);
                else
                    coeff = dist_inv * MathUtils.dist(x2, y2, line_x2, line_y2);
                tri_points[1].z = z2 + (z1 - z2) * coeff;
                if (interpolate_colors) {
                    tri_points[1].r = r2 + (r1 - r2) * coeff;
                    tri_points[1].g = g2 + (g1 - g2) * coeff;
                    tri_points[1].b = b2 + (b1 - b2) * coeff;
                }
            } else {
                tri_points[1].z = z2;
                if (interpolate_colors) {
                    tri_points[1].r = r2;
                    tri_points[1].g = g2;
                    tri_points[1].b = b2;
                }
            }
            
            n = 2;
        } 
        
        temp_code1 = code2;
        temp_code2 = code3;
        clip0(x2, y2, x3, y3);
        if (line_x1 != -1) {
            if (n == 0) {
                
                float dist_inv = -1f;
                
                tri_points[0].x = line_x1;
                tri_points[0].y = line_y1;
                tri_points[1].x = line_x2;
                tri_points[1].y = line_y2;
                
                if (line_x1 != x2 || line_y1 != y2) { 
                    dist_inv = 1f / MathUtils.dist(x2, y2, x3, y3);
                    float coeff = dist_inv * MathUtils.dist(x2, y2, line_x1, line_y1);
                    tri_points[0].z = z2 + (z3 - z2) * coeff;
                    if (interpolate_colors) {
                        tri_points[0].r = r2 + (r3 - r2) * coeff;
                        tri_points[0].g = g2 + (g3 - g2) * coeff;
                        tri_points[0].b = b2 + (b3 - b2) * coeff;
                    }
                } else {
                    tri_points[0].z = z2;
                    if (interpolate_colors) {
                        tri_points[0].r = r2;
                        tri_points[0].g = g2;
                        tri_points[0].b = b2;
                    }
                }
            
                if (line_x2 != x3 || line_y2 != y3) {
                    float coeff;
                    if (dist_inv == -1f)
                        coeff = MathUtils.dist(x3, y3, line_x2, line_y2) / MathUtils.dist(x2, y2, x3, y3);
                    else
                        coeff = dist_inv * MathUtils.dist(x3, y3, line_x2, line_y2);
                    tri_points[1].z = z3 + (z2 - z3) * coeff;
                    if (interpolate_colors) {
                        tri_points[1].r = r3 + (r2 - r3) * coeff;
                        tri_points[1].g = g3 + (g2 - g3) * coeff;
                        tri_points[1].b = b3 + (b2 - b3) * coeff;
                    }
                } else {
                    tri_points[1].z = z3;
                    if (interpolate_colors) {
                        tri_points[1].r = r3;
                        tri_points[1].g = g3;
                        tri_points[1].b = b3;
                    }
                }
                
                n = 2;
                
            } else {
                if (tri_points[1].x == line_x1 && tri_points[1].y == line_y1) {

                    tri_points[2].x = line_x2;
                    tri_points[2].y = line_y2;
                    
                    if (line_x2 != x3 || line_y2 != y3) {
                        float coeff = MathUtils.dist(x3, y3, line_x2, line_y2) / MathUtils.dist(x2, y2, x3, y3);
                        tri_points[2].z = z3 + (z2 - z3) * coeff;
                        if (interpolate_colors) {
                            tri_points[2].r = r3 + (r2 - r3) * coeff;
                            tri_points[2].g = g3 + (g2 - g3) * coeff;
                            tri_points[2].b = b3 + (b2 - b3) * coeff;
                        }
                    }
                    
                    n = 3;
                    
                } else {

                    tri_points[2].x = line_x1;
                    tri_points[2].y = line_y1;
                    tri_points[3].x = line_x2;
                    tri_points[3].y = line_y2;
                    
                    float dist_inv = -1f;
                    
                    if (line_x1 != x2 || line_y1 != y2) { 
                        dist_inv = 1f / MathUtils.dist(x2, y2, x3, y3);
                        float coeff = dist_inv * MathUtils.dist(x2, y2, line_x1, line_y1);
                        tri_points[2].z = z2 + (z3 - z2) * coeff;
                        if (interpolate_colors) {
                            tri_points[2].r = r2 + (r3 - r2) * coeff;
                            tri_points[2].g = g2 + (g3 - g2) * coeff;
                            tri_points[2].b = b2 + (b3 - b2) * coeff;
                        }
                    } else {
                        tri_points[2].z = z2;
                        if (interpolate_colors) {
                            tri_points[2].r = r2;
                            tri_points[2].g = g2;
                            tri_points[2].b = b2;
                        }
                    }

                    if (line_x2 != x3 || line_y2 != y3) {
                    
                        float coeff;
                        if (dist_inv == -1f)
                            coeff = MathUtils.dist(x3, y3, line_x2, line_y2) / MathUtils.dist(x2, y2, x3, y3);
                        else
                            coeff = dist_inv * MathUtils.dist(x3, y3, line_x2, line_y2);
                        tri_points[3].z = z3 + (z2 - z3) * coeff;
                        if (interpolate_colors) {
                            tri_points[3].r = r3 + (r2 - r3) * coeff;
                            tri_points[3].g = g3 + (g2 - g3) * coeff;
                            tri_points[3].b = b3 + (b2 - b3) * coeff;
                        }
                    } else {

                        tri_points[3].z = z3;
                        if (interpolate_colors) {
                            tri_points[3].r = r3;
                            tri_points[3].g = g3;
                            tri_points[3].b = b3;
                        }
                    }
                    
                    n = 4;
                    
                }
                
            }
        }
        
        temp_code1 = code3;
        temp_code2 = code1;
        clip0(x3, y3, x1, y1);
        if (line_x1 != -1) {

            if (n == 0) {
                float dist_inv = -1f;
                tri_points[0].x = line_x1;
                tri_points[0].y = line_y1;
                tri_points[1].x = line_x2;
                tri_points[1].y = line_y2;
                
                if (line_x1 != x3 || line_y1 != y3) { 
                    dist_inv = 1f / MathUtils.dist(x3, y3, x1, y1);
                    float coeff = dist_inv * MathUtils.dist(x3, y3, line_x1, line_y1);
                    tri_points[0].z = z3 + (z1 - z3) * coeff;
                    if (interpolate_colors) {
                        tri_points[0].r = r3 + (r1 - r3) * coeff;
                        tri_points[0].g = g3 + (g1 - g3) * coeff;
                        tri_points[0].b = b3 + (b1 - b3) * coeff;
                    }
                } else {
                    tri_points[0].z = z3;
                    if (interpolate_colors) {
                        tri_points[0].r = r3;
                        tri_points[0].g = g3;
                        tri_points[0].b = b3;
                    }
                }
            
                if (line_x2 != x1 || line_y2 != y1) {
                    float coeff;
                    if (dist_inv == -1f)
                        coeff = MathUtils.dist(x1, y1, line_x2, line_y2) / MathUtils.dist(x3, y3, x1, y1);
                    else
                        coeff = dist_inv * MathUtils.dist(x1, y1, line_x2, line_y2);
                    tri_points[1].z = z1 + (z3 - z1) * coeff;
                    if (interpolate_colors) {
                        tri_points[1].r = r1 + (r3 - r1) * coeff;
                        tri_points[1].g = g1 + (g3 - g1) * coeff;
                        tri_points[1].b = b1 + (b3 - b1) * coeff;
                    }
                } else {
                    tri_points[1].z = z1;
                    if (interpolate_colors) {
                        tri_points[1].r = r1;
                        tri_points[1].g = g1;
                        tri_points[1].b = b1;
                    }
                }
                
                n = 2;
                
            } else {
                float dist_inv = -1f;
                if (tri_points[n - 1].x != line_x1 || tri_points[n - 1].y != line_y1) {

                    dist_inv = 1f / MathUtils.dist(x3, y3, x1, y1);
                    float coeff = dist_inv * MathUtils.dist(x3, y3, line_x1, line_y1);
                    tri_points[n].x = line_x1;
                    tri_points[n].y = line_y1;
                    tri_points[n].z = z3 + (z1 - z3) * coeff;
                    if (interpolate_colors) {
                        tri_points[n].r = r3 + (r1 - r3) * coeff;
                        tri_points[n].g = g3 + (g1 - g3) * coeff;
                        tri_points[n].b = b3 + (b1 - b3) * coeff;
                    }
                    n++;
                } 
                if (tri_points[0].x != line_x2 || tri_points[0].y != line_y2) {
                    float coeff;
                    if (dist_inv == -1f)
                        coeff = MathUtils.dist(x1, y1, line_x2, line_y2) / MathUtils.dist(x3, y3, x1, y1);
                    else
                        coeff = dist_inv * MathUtils.dist(x1, y1, line_x2, line_y2);
                    tri_points[n].x = line_x2;
                    tri_points[n].y = line_y2;
                    tri_points[n].z = z1 + (z3 - z1) * coeff;
                    if (interpolate_colors) {
                        tri_points[n].r = r1 + (r3 - r1) * coeff;
                        tri_points[n].g = g1 + (g3 - g1) * coeff;
                        tri_points[n].b = b1 + (b3 - b1) * coeff;
                    }
                    n++;
                }
            }
        }
//      now "tri_points" array contains all intersection points of triangle
//      with the viewport, and "n" variable contains the number of this points.
        v1v2_x = x2 - x1;
        v1v2_y = y2 - y1;
        v1v3_x = x3 - x1;
        v1v3_y = y3 - y1;
        temp_det = v1v2_x * v1v3_y - v1v2_y * v1v3_x;

        if (test_point(-x1, -y1)) { // point (0, 0) belongs to triangle
            tri_points[n].x = 0;
            tri_points[n].y = 0;
            tri_points[n].z = (z1 + (z2 - z1) * temp_k1) + (z1 + (z3 - z1) * temp_k2);
            if (interpolate_colors) {
                tri_points[n].r = (r1 + (r2 - r1) * temp_k1) + (r1 + (r3 - r1) * temp_k2);
                tri_points[n].g = (g1 + (g2 - g1) * temp_k1) + (g1 + (g3 - g1) * temp_k2);
                tri_points[n].b = (b1 + (b2 - b1) * temp_k1) + (b1 + (b3 - b1) * temp_k2);                
            }
            n++;
        }

        if (test_point(-x1, h - y1 - 1f)) { // point (0, h - 1) belongs to triangle
            max_y_index = n; // small optimisation
            tri_points[n].x = 0;
            tri_points[n].y = h - 1;
            tri_points[n].z = (z1 + (z2 - z1) * temp_k1) + (z1 + (z3 - z1) * temp_k2);
            if (interpolate_colors) {
                tri_points[n].r = (r1 + (r2 - r1) * temp_k1) + (r1 + (r3 - r1) * temp_k2);
                tri_points[n].g = (g1 + (g2 - g1) * temp_k1) + (g1 + (g3 - g1) * temp_k2);
                tri_points[n].b = (b1 + (b2 - b1) * temp_k1) + (b1 + (b3 - b1) * temp_k2);                
            }
            n++;
        }

        if (test_point(w - x1 - 1f, -y1)) { // point (w - 1, 0) belongs to triangle
            tri_points[n].x = w - 1;
            tri_points[n].y = 0;
            tri_points[n].z = (z1 + (z2 - z1) * temp_k1) + (z1 + (z3 - z1) * temp_k2);
            if (interpolate_colors) {
                tri_points[n].r = (r1 + (r2 - r1) * temp_k1) + (r1 + (r3 - r1) * temp_k2);
                tri_points[n].g = (g1 + (g2 - g1) * temp_k1) + (g1 + (g3 - g1) * temp_k2);
                tri_points[n].b = (b1 + (b2 - b1) * temp_k1) + (b1 + (b3 - b1) * temp_k2);                
            }
            n++;
        }

        if (test_point(w - x1 - 1f, h - y1 - 1f)) { // point (w - 1, h - 1) belongs to triangle
            tri_points[n].x = w - 1;
            tri_points[n].y = h - 1;
            tri_points[n].z = (z1 + (z2 - z1) * temp_k1) + (z1 + (z3 - z1) * temp_k2);
            if (interpolate_colors) {
                tri_points[n].r = (r1 + (r2 - r1) * temp_k1) + (r1 + (r3 - r1) * temp_k2);
                tri_points[n].g = (g1 + (g2 - g1) * temp_k1) + (g1 + (g3 - g1) * temp_k2);
                tri_points[n].b = (b1 + (b2 - b1) * temp_k1) + (b1 + (b3 - b1) * temp_k2);                
            }
            n++;
        }
        
        if (max_y_index == -1) {
            max_y_index = 0;
            for (int i = 1; i < n; i++) {
                if ((tri_points[i].y > tri_points[max_y_index].y) || (tri_points[i].y == tri_points[max_y_index].y && tri_points[i].x < tri_points[max_y_index].x)) {
                    max_y_index = i;
                }
            }
        }
        
        TriPoint temp = tri_points[0];
        tri_points[0] = tri_points[max_y_index];
        tri_points[max_y_index] = temp;
        
        final TriPoint max_y = tri_points[0];
        try {
        Arrays.sort(tri_points, 1, n, (TriPoint p1, TriPoint p2) -> {
            return Double.compare(p1.angle_rel_to(max_y), p2.angle_rel_to(max_y));
        });
        } catch (Exception ex) {
        }
        
        for (int i = 0; i < n; i++) tri_points[i].angle_calc = false;
        
    }
    
    private float temp_det;
    private float temp_k1, temp_k2; 
    private float v1v2_x, v1v2_y, v1v3_x, v1v3_y;

    private boolean test_point(float px, float py) { 
        float det1 = v1v2_x * py - v1v2_y * px;
        temp_k1 = det1 / temp_det;
        if (temp_k1 >= 0f && temp_k1 <= 1f) {
            float det2 = v1v3_y * px - v1v3_x * py;
            temp_k2 = det2 / temp_det;
            if (temp_k2 >= 0f && temp_k2 <= 1f && temp_k1 + temp_k2 <= 1f) {
                return true;
            }
        }
        return false;
    }
    
//  y1 <= y3 AND y2 <= y3, all point are fully inside viewport
    private void fill_tri(
            int x1, int y1, float z1, 
            int x2, int y2, float z2, 
            int x3, int y3, float z3) {
        
        if (y2 < y1) {
            temp_x = x2;
            temp_y = y2;
            temp_float = z2;
            x2 = x1;
            y2 = y1;
            z2 = z1;
            x1 = temp_x;
            y1 = temp_y;
            z1 = temp_float;
        }
        int dx13 = 0, dx12 = 0, dx23 = 0;
        float dz13 = 0f, dz12 = 0f, dz23 = 0f;
        if (y3 != y1) {
            dx13 = make_fixed_16x16(x3 - x1) / (y3 - y1);
            dz13 = (z3 - z1) / (y3 - y1);
        }
        if (y2 != y1) {
            dx12 = make_fixed_16x16(x2 - x1) / (y2 - y1);
            dz12 = (z2 - z1) / (y2 - y1);
        }
        if (y3 != y2) {
            dx23 = make_fixed_16x16(x3 - x2) / (y3 - y2);
            dz23 = (z3 - z2) / (y3 - y2);
        }
        int _dx13 = dx13;
        float _dz13 = dz13;
        if (dx13 > dx12) {
            temp_int = dx13;
            dx13 = dx12;
            dx12 = temp_int;
            temp_float = dz13;
            dz13 = dz12;
            dz12 = temp_float;
        }
        float lz = z1, rz = z1;
        int lx = make_fixed_16x16(x1), rx = lx;
        for (int y = y1; y < y2; y++) {
            hor_line(
                round_fixed_16x16(lx), lz, 
                round_fixed_16x16(rx), rz, y
            );
            lx += dx13;
            rx += dx12;
            lz += dz13;
            rz += dz12;
        }
        if (y1 == y2) {
            if (x2 < x1) {
                rx = lx;
                lx = make_fixed_16x16(x2);
                rz = z1;
                lz = z2;
                temp_int = _dx13;
                _dx13 = dx23;
                dx23 = temp_int;
                temp_float = _dz13;
                _dz13 = dz23;
                dz23 = temp_float;
            } else {
                rx = make_fixed_16x16(x2);
                rz = z2;
            }
        } else {
            if (_dx13 < dx23) {
                temp_int = _dx13;
                _dx13 = dx23;
                dx23 = temp_int;
                temp_float = _dz13;
                _dz13 = dz23;
                dz23 = temp_float;
            }
        }
        for (int y = y2; y <= y3; y++) {
            hor_line(
                round_fixed_16x16(lx), lz, 
                round_fixed_16x16(rx), rz, y
            );
            lx += _dx13;
            rx += dx23;
            lz += _dz13;
            rz += dz23;
        }
    }
    
//  y1 <= y3 AND y2 <= y3, all points are fully inside viewport
//  color values are fixed point numbers
    private void fill_tri_interpolate_color(
            int x1, int y1, float z1, int r1, int g1, int b1, 
            int x2, int y2, float z2, int r2, int g2, int b2, 
            int x3, int y3, float z3, int r3, int g3, int b3) {
        
        if (y2 < y1) {
            temp_x = x2;
            temp_y = y2;
            temp_float = z2;
            x2 = x1;
            y2 = y1;
            z2 = z1;
            x1 = temp_x;
            y1 = temp_y;
            z1 = temp_float;
            temp_int = r2;
            r2 = r1;
            r1 = temp_int;
            temp_int = g2;
            g2 = g1;
            g1 = temp_int;
            temp_int = b2;
            b2 = b1;
            b1 = temp_int;
        }
        int dx13 = 0, dx12 = 0, dx23 = 0;
        int dr13 = 0, dr12 = 0, dr23 = 0;
        int dg13 = 0, dg12 = 0, dg23 = 0;
        int db13 = 0, db12 = 0, db23 = 0;
        float dz13 = 0f, dz12 = 0f, dz23 = 0f;
        if (y3 != y1) {
            int dy = y3 - y1;
            dx13 = make_fixed_16x16(x3 - x1) / dy;
            dr13 = (r3 - r1) / dy;
            dg13 = (g3 - g1) / dy;
            db13 = (b3 - b1) / dy;
            dz13 = (z3 - z1) / dy;
        }
        if (y2 != y1) {
            int dy = y2 - y1;
            dx12 = make_fixed_16x16(x2 - x1) / dy;
            dr12 = (r2 - r1) / dy;
            dg12 = (g2 - g1) / dy;
            db12 = (b2 - b1) / dy;
            dz12 = (z2 - z1) / dy;
        }
        if (y3 != y2) {
            int dy = y3 - y2;
            dx23 = make_fixed_16x16(x3 - x2) / dy;
            dr23 = (r3 - r2) / dy;
            dg23 = (g3 - g2) / dy;
            db23 = (b3 - b2) / dy;
            dz23 = (z3 - z2) / dy;
        }
        float _dz13 = dz13;
        int _dx13 = dx13, _dr13 = dr13, _dg13 = dg13, _db13 = db13;
        if (dx13 > dx12) {
            temp_int = dx13;
            dx13 = dx12;
            dx12 = temp_int;
            temp_int = dr13;
            dr13 = dr12;
            dr12 = temp_int;
            temp_int = dg13;
            dg13 = dg12;
            dg12 = temp_int;
            temp_int = db13;
            db13 = db12;
            db12 = temp_int;
            temp_float = dz13;
            dz13 = dz12;
            dz12 = temp_float;
        }
        float lz = z1, rz = z1;
        int lx = make_fixed_16x16(x1), rx = lx;
        int lr = r1, 
            lg = g1, 
            lb = b1;
        int rr = lr, rg = lg, rb = lb;
        for (int y = y1; y < y2; y++) {
            hor_line_interpolate_color(
                y, 
                round_fixed_16x16(lx), lz, lr, lg, lb, 
                round_fixed_16x16(rx), rz, rr, rg, rb
            );
            lx += dx13;
            rx += dx12;
            lz += dz13;
            rz += dz12;
            lr += dr13;
            lg += dg13;
            lb += db13;
            rr += dr12;
            rg += dg12;
            rb += db12;
        }
        if (y1 == y2) {
            if (x2 < x1) {
                rx = lx;
                lx = make_fixed_16x16(x2);
                rz = z1;
                lz = z2;
                rr = lr;
                lr = r2;
                rg = lg;
                lg = g2;
                rb = lb;
                lb = b2;
                temp_int = _dx13;
                _dx13 = dx23;
                dx23 = temp_int;
                temp_int = _dr13;
                _dr13 = dr23;
                dr23 = temp_int;
                temp_int = _dg13;
                _dg13 = dg23;
                dg23 = temp_int;
                temp_int = _db13;
                _db13 = db23;
                db23 = temp_int;
                temp_float = _dz13;
                _dz13 = dz23;
                dz23 = temp_float;
            } else {
                rx = make_fixed_16x16(x2);
                rz = z2;
                rr = r2;
                rg = g2;
                rb = b2;
            }
        } else {
            if (_dx13 < dx23) {
                temp_int = _dx13;
                _dx13 = dx23;
                dx23 = temp_int;
                temp_int = _dr13;
                _dr13 = dr23;
                dr23 = temp_int;
                temp_int = _dg13;
                _dg13 = dg23;
                dg23 = temp_int;
                temp_int = _db13;
                _db13 = db23;
                db23 = temp_int;
                temp_float = _dz13;
                _dz13 = dz23;
                dz23 = temp_float;
            }
        }
        for (int y = y2; y <= y3; y++) {
            hor_line_interpolate_color(
                y, 
                round_fixed_16x16(lx), lz, lr, lg, lb, 
                round_fixed_16x16(rx), rz, rr, rg, rb
            );
            lx += _dx13;
            rx += dx23;
            lz += _dz13;
            rz += dz23;
            lr += _dr13;
            lg += _dg13;
            lb += _db13;
            rr += dr23;
            rg += dg23;
            rb += db23;
        }
    }
    
//  color values are fixed point numbers (8x24)
    private void hor_line_interpolate_color(int y, int x1, float z1, int r1, int g1, int b1, int x2, float z2, int r2, int g2, int b2) {
        int i = index(x1, y);
        float z = z1;
        int dx = max(1, x2 - x1);
        final float dz = (z2 - z1) / dx;
        final int dr = (r2 - r1) / dx;
        final int dg = (g2 - g1) / dx;
        final int db = (b2 - b1) / dx;
        int r = r1, g = g1, b = b1;
        for (int x = x1;;) {
            if (z_buff[i] > z) {
                this.g.plot(
                    x, y, 
                    rgb(round_fixed_8x24(r), round_fixed_8x24(g), round_fixed_8x24(b)));
                z_buff[i] = z;
            }
            if (++x > x2)
                break;
            z += dz;
            r += dr;
            g += dg;
            b += db;
            i++;
        }
    }
    
    public void line(int x1, int y1, float z1, int x2, int y2, float z2) {
        clip(x1, y1, x2, y2);
        if (line_x1 != -1) {
            line_z1 = z1;
            line_z2 = z2;
            float src_len = -1.0f;
            if (line_x1 != x1 || line_y1 != y1) {
                src_len = dist(x1, y1, x2, y2);
                line_z1 = new_val(x1, y1, line_x1, line_y1, z1, z2, src_len);
            }
            if (line_x2 != x2 || line_y2 != y2) {
                if (src_len == -1.0f) 
                    src_len = dist(x1, y1, x2, y2);
                line_z2 = new_val(x2, y2, line_x2, line_y2, z2, z1, src_len);
            }
            bresenham(line_x1, line_y1, line_z1, line_x2, line_y2, line_z2);
        } 
    }
    
    private float new_val(int old_x, int old_y, int clipped_x, int clipped_y, float v1, float v2, float old_len) {
        return v1 + (v2 - v1) * dist(old_x, old_y, clipped_x, clipped_y) / old_len;
    }
    
    private void bresenham(int x1, int y1, float z1, int x2, int y2, float z2) {
        if (x1 == x2) { // vertical line
            if (y1 == y2) {
                int i = index(x1, y1);
                if (z_buff[i] > z1 || z_buff[i] > z2) {
                    g.plot(x1, y1);
                    z_buff[i] = Math.min(z1, z2);
                }
            } else {
                if (y1 < y2) ver_line(x1, y1, z1, y2, z2);
                else         ver_line(x1, y2, z2, y1, z1);
            }
        } else if (y1 == y2) { // horizontal line
            if (x1 < x2) hor_line(x1, z1, x2, z2, y1);
            else         hor_line(x2, z2, x1, z1, y1);
        } else {
            if (x1 > x2) {
                temp_x = x1;
                temp_y = y1;
                temp_float = z1;
                x1 = x2;
                y1 = y2;
                z1 = z2;
                x2 = temp_x;
                y2 = temp_y;
                z2 = temp_float;
            }
            int dx = x2 - x1, dy;
            if (y1 < y2) {
                dy = y2 - y1;
                if (dx > dy) x_line(x1, y1, z1, x2, z2, dx, dy, 1);
                else if (dx < dy) y_line(x1, y1, z1, y2, z2, dx, dy, 1);
                else line_45(x1, y1, z1, x2, z2, 1);
            } else {
                dy = y1 - y2;
                if (dx > dy) x_line(x1, y1, z1, x2, z2, dx, dy, -1);
                else if (dx < dy) y_line(x1, y1, z1, y2, z2, dx, dy, -1);
                else line_45(x1, y1, z1, x2, z2, -1);
            }
        }
    }
    
//  x1 < x2
    private void x_line(int x1, int y1, float z1, int x2, float z2, int dx, int dy, final int y_inc) {
        int i = index(x1, y1);
        int _dx = 0, _dy = 0;
        float z = z1;
        final float dz = (z2 - z1) / sqrt_table[index(dx, dy)];
        for (int x = x1, y = y1, err = 0;;) {
            if (z_buff[i] > z) {
                g.plot(x, y);
                z_buff[i] = z;
            }
            if (++x > x2)
                break;
            i++;
            _dx++;
            err += dy;
            if (err << 1 >= dx) {
                _dy++;
                err -= dx;
                y += y_inc;
                i += y_inc * w;
            } 
            z = z1 + dz * sqrt_table[index(_dx, _dy)];
        } 
    }
    
    private void y_line(int x1, int y1, float z1, int y2, float z2, int dx, int dy, final int y_inc) {
        int i = index(x1, y1);
        int _dy = 0, _dx = 0;
        float z = z1;
        final float dz = (z2 - z1) / sqrt_table[index(dx, dy)];
        for (int y = y1, x = x1, err = 0;;) {
            if (z_buff[i] > z) {
                g.plot(x, y);
                z_buff[i] = z;
            }
            y += y_inc;
            if (y == y2) {
                _dy++;
                err += dx;
                i += y_inc * w;
                if (err << 1 >= dy) {
                    x++;
                    i++;
                    _dx++;
                }
                z = z1 + dz * sqrt_table[index(_dx, _dy)];
                if (z_buff[i] > z) {
                    g.plot(x, y);
                    z_buff[i] = z;
                }
                return;
            }
            _dy++;
            err += dx;
            i += y_inc * w;
            if (err << 1 >= dy) {
                x++;
                i++;
                _dx++;
                err -= dy;
            }
            z = z1 + dz * sqrt_table[index(_dx, _dy)];
        }
    }
    
    private void ver_line(int x, int y1, float z1, int y2, float z2) {
        int i = index(x, y1);
        float z = z1;
        final float dz = (z2 - z1) / (y2 - y1);
        for (int y = y1;;) {
            if (z_buff[i] > z) {
                g.plot(x, y);
                z_buff[i] = z;
            }
            if (++y > y2)
                break;
            i += w;
            z += dz;
        }
    }
    
    private void hor_line(int x1, float z1, int x2, float z2, int y) {
        int i = index(x1, y);
        float z = z1;
        final float dz = (z2 - z1) / (x2 - x1);
        for (int x = x1;;) {
            if (z_buff[i] > z) {
                g.plot(x, y);
                z_buff[i] = z;
            } 
            if (++x > x2)
                break;
            z += dz;
            i++;
        }
    }
    
    private void line_45(int x1, int y1, float z1, int x2, float z2, final int dy) {
        int i = index(x1, y1);
        float z = z1;
        int dx = x2 - x1;
        final float dz = SQRT_2 * (z2 - z1) / sqrt_table[index(dx, dx)];
        for (int x = x1, y = y1;;) {
            if (z_buff[i] > z) {
                g.plot(x, y);
                z_buff[i] = z;
            }
            if (++x > x2)
                break;
            y += dy;
            z += dz;
            i += 1 + dy * w;
        }
    }
    
    private void clip(int x1, int y1, int x2, int y2) {
        temp_code1 = code(x1, y1);
        temp_code2 = code(x2, y2);
        clip0(x1, y1, x2, y2);
    }
    
    private void clip0(int x1, int y1, int x2, int y2) {
        for (;;) {
            if ((temp_code1 | temp_code2) == 0) {
                line_x1 = x1;
                line_x2 = x2;
                line_y1 = y1;
                line_y2 = y2;
                break;
            } else {
                if ((temp_code1 & temp_code2) == 0) {
                    if (temp_code1 != 0) {
                        transfer(x1, y1, x2, y2, temp_code1);
                        x1 = temp_x;
                        y1 = temp_y;
                        temp_code1 = code(x1, y1);
                    } else { // code_2 != 0
                        transfer(x2, y2, x1, y1, temp_code2);
                        x2 = temp_x;
                        y2 = temp_y;
                        temp_code2 = code(x2, y2);
                    }
                } else {
                    line_x1 = -1; // line is outside the screen
                    break;
                }
            }
        }
    }
    
    private void transfer(int x, int y, int rx, int ry, int code) {
        switch (code) {
            case X_LESS_0:
                x_less_0(x, y, rx, ry);
                break;
            case X_GREATER_MAX_X:
                x_greater_max_x(x, y, rx, ry);
                break;
            case Y_LESS_0:
                y_less_0(x, y, rx, ry);
                break;
            case Y_GREATER_MAX_Y:
                y_greater_max_y(x, y, rx, ry);
                break;
            case (X_LESS_0 | Y_LESS_0):
                x_less_0(x, y, rx, ry);
                break;
            case (X_GREATER_MAX_X | Y_GREATER_MAX_Y):
                x_greater_max_x(x, y, rx, ry);
                break;
            case (X_LESS_0 | Y_GREATER_MAX_Y):
                y_greater_max_y(x, y, rx, ry);
                break;
            case (X_GREATER_MAX_X | Y_LESS_0):
                y_less_0(x, y, rx, ry);
                break;
            default: // can't be
                break;
        }
    }
    
    private void x_less_0(float x, float y, float rx, float ry) {
        temp_x = 0;
        temp_y = round(ry + (y - ry) * -(rx / (x - rx)));
    }

    private void y_less_0(float x, float y, float rx, float ry) {
        temp_x = round(rx + (x - rx) * -(ry / (y - ry)));
        temp_y = 0;
    }

    private void x_greater_max_x(float x, float y, float rx, float ry) {
        temp_x = x_max;
        temp_y = round(ry + (y - ry) * ((x_max - rx) / (x - rx)));
    }

    private void y_greater_max_y(float x, float y, float rx, float ry) {
        temp_x = round(rx + (x - rx) * ((y_max - ry) / (y - ry)));
        temp_y = y_max;
    }
    
    private int code(int x, int y) {
        int code = 0;
        if (x < 0) 
            code = X_LESS_0;
        else if (x > x_max)
            code = X_GREATER_MAX_X;
        if (y < 0)
            code |= Y_LESS_0;
        else if (y > y_max)
            code |= Y_GREATER_MAX_Y;
        return code;
    }
    
    private int index(int x, int y) {
        return x + y * w;
    }
    
    private static class TriPoint {
        
        float z;
        int x, y;
        
        int rgb;
        float r, g, b;
        
        double angle;
        boolean angle_calc;
        
        boolean eq(TriPoint p) {
            return x == p.x && y == p.y;
        }
        
        double angle_rel_to(TriPoint p) {
            if (angle_calc)
                return angle;
            float px = x - p.x;
            float py = y - p.y;
            angle = Math.atan2(-py, px);
            angle_calc = true;
            return angle;
        }
        
    }
    
}