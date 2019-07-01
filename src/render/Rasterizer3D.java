package render;

import java.util.Arrays;
import static render.Graphics.*;
import static render.MathUtils.*;

/**
 *
 * @author Texhnolyze
 */
class Rasterizer3D {
    
    private static class BufferData {
        
        private int rgb;
        private float z_inv;
        
        private Material mat;
        private float nx, ny, nz;
        private boolean need_light;
        
    } 
    
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
    private float line_z1_inv, line_z2_inv;
    
//----------These variables stores attributes of current rasterized triangle---------
    private float r1, g1, b1;
    private float r2, g2, b2;
    private float r3, g3, b3;
    private float z1, z2, z3;
    private float z1_inv, z2_inv, z3_inv;
    private int code1, code2, code3;
    private Bitmap tex;
    private boolean modulate; // if true then texture rgb will be modulate with current graphics rgb
    private float u1, v1;
    private float u2, v2;
    private float u3, v3;
//-----------------------------------------------------------------------------------
    
    private float[] z_buff; // z buffer contains inversed z values
    private float[] sqrt_table;
	
    private BufferData[] buffer;
    
    Rasterizer3D(Graphics g) {
        this.g = g;
        updateBounds();
        for (int i = 0; i < tri_points.length; i++)
            tri_points[i] = new TriPoint();
    }
    
    Graphics getGraphics() {
        return g;
    }
    
    void updateBounds() {
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
    
    void clearZBuffer() {
        for (int i = 0; i < z_buff.length; i++)
            z_buff[i] = Float.NEGATIVE_INFINITY;
    }
    
    void drawZBuffer(Graphics g) {
        if (w < g.getWidth() || h < g.getHeight())
            return;
        float min = Float.POSITIVE_INFINITY, max = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < z_buff.length; i++) {
            if (z_buff[i] < min && z_buff[i] != Float.NEGATIVE_INFINITY) min = z_buff[i];
            if (z_buff[i] > max) max = z_buff[i];
        }
        if (max == Float.NEGATIVE_INFINITY) { // z_buffer is empty.
            for (int i = 0; i < z_buff.length; i++) 
                g.plotToHash(i, Graphics.WHITE);
        } else {
            float dz_inv;
            if (max - min == 0.0f)
                dz_inv = 1f;
            else
                dz_inv = 1f / (max - min);
            for (int i = 0; i < z_buff.length; i++) {
                int gray = z_buff[i] == Float.NEGATIVE_INFINITY ? Graphics.BLACK : roundPositive((255f * (z_buff[i] - min) * dz_inv));
                g.plotToHash(i, rgb(gray, gray, gray));
            }
        }
    }
    
    void strokeTriangle(int x1, int y1, float z1, int x2, int y2, float z2, int x3, int y3, float z3) {
        line(x1, y1, z1, x2, y2, z2);
        line(x2, y2, z2, x3, y3, z3);
        line(x3, y3, z3, x1, y1, z1);
    }
    
    void fillTriangle(int x1, int y1, float z1, int x2, int y2, float z2, int x3, int y3, float z3) {
        code1 = code(x1, y1);
        code2 = code(x2, y2);
        code3 = code(x3, y3);
        if ((code1 | code2 | code3) == 0) {
            tri_points[0].x = x1;
            tri_points[0].y = y1;
            tri_points[0].z_inv = 1f / z1;
            tri_points[1].x = x2;
            tri_points[1].y = y2;
            tri_points[1].z_inv = 1f / z2;
            tri_points[2].x = x3;
            tri_points[2].y = y3;
            tri_points[2].z_inv = 1f / z3;
            sort_first_tri_points();
            fill_tri(tri_points[0], tri_points[1], tri_points[2]);
        } else if ((code1 & code2 & code3) == 0) {
            this.z1 = z1;
            this.z2 = z2;
            this.z3 = z3;
            this.z1_inv = 1f / z1;
            this.z2_inv = 1f / z2;
            this.z3_inv = 1f / z3;
            boolean visible = clip_triangle(x1, y1, x2, y2, x3, y3, false, false);
            if (visible) {
                TriPoint p0 = tri_points[0];
                for (int i = 1; i < n - 1; i++) {
                    TriPoint p1 = tri_points[i];
                    TriPoint p2 = tri_points[i + 1];
                    if (p1.y > p2.y) 
                        fill_tri(p2, p1, p0);
                    else 
                        fill_tri(p1, p2, p0);
                }
            }
        }
    }
    
    void fillTriangleInterpolateColor(
            int x1, int y1, float z1, float r1, float g1, float b1, 
            int x2, int y2, float z2, float r2, float g2, float b2,
            int x3, int y3, float z3, float r3, float g3, float b3) {
        code1 = code(x1, y1);
        code2 = code(x2, y2);
        code3 = code(x3, y3);
        if ((code1 | code2 | code3) == 0) {
            tri_points[0].x = x1;
            tri_points[0].y = y1;
            tri_points[0].z_inv = 1f / z1;
            tri_points[0].r = r1;
            tri_points[0].g = g1;
            tri_points[0].b = b1;
            tri_points[1].x = x2;
            tri_points[1].y = y2;
            tri_points[1].z_inv = 1f / z2;
            tri_points[1].r = r2;
            tri_points[1].g = g2;
            tri_points[1].b = b2;
            tri_points[2].x = x3;
            tri_points[2].y = y3;
            tri_points[2].z_inv = 1f / z3;
            tri_points[2].r = r3;
            tri_points[2].g = g3;
            tri_points[2].b = b3;
            sort_first_tri_points();
            fill_tri_interpolate_color(tri_points[0], tri_points[1], tri_points[2]);
        } else if ((code1 & code2 & code3) == 0) {
            this.z1 = z1;
            this.z2 = z2;
            this.z3 = z3;
            this.z1_inv = 1f / z1;
            this.z2_inv = 1f / z2;
            this.z3_inv = 1f / z3;
            this.r1 = r1;
            this.g1 = g1;
            this.b1 = b1;
            this.r2 = r2;
            this.g2 = g2;
            this.b2 = b2;
            this.r3 = r3;
            this.g3 = g3;
            this.b3 = b3;
            boolean visible = clip_triangle(x1, y1, x2, y2, x3, y3, true, false);
            if (visible) {
                TriPoint p0 = tri_points[0];
                for (int i = 1; i < n - 1; i++) {
                    TriPoint p1 = tri_points[i];
                    TriPoint p2 = tri_points[i + 1];
                    if (p1.y > p2.y) 
                        fill_tri_interpolate_color(p2, p1, p0);
                    else 
                        fill_tri_interpolate_color(p1, p2, p0);
                }
            }
        }
    }
    
    void fillTexturedTriangle(int x1, int y1, float z1, float u1, float v1, int x2, int y2, float z2, float u2, float v2, int x3, int y3, float z3, float u3, float v3, Bitmap texture, boolean modulate) {
        this.tex = texture;
        this.modulate = modulate;
        code1 = code(x1, y1);
        code2 = code(x2, y2);
        code3 = code(x3, y3);
        if ((code1 | code2 | code3) == 0) {
            tri_points[0].x = x1;
            tri_points[0].y = y1;
            tri_points[0].z_inv = 1f / z1;
            tri_points[0].u = u1;
            tri_points[0].v = v1;
            tri_points[1].x = x2;
            tri_points[1].y = y2;
            tri_points[1].z_inv = 1f / z2;
            tri_points[1].u = u2;
            tri_points[1].v = v2;
            tri_points[2].x = x3;
            tri_points[2].y = y3;
            tri_points[2].z_inv = 1f / z3;
            tri_points[2].u = u3;
            tri_points[2].v = v3;
            sort_first_tri_points();
            fill_textured_tri(tri_points[0], tri_points[1], tri_points[2]);
        } else if ((code1 & code2 & code3) == 0) {
            this.z1 = z1;
            this.z2 = z2;
            this.z3 = z3;
            this.z1_inv = 1f / z1;
            this.z2_inv = 1f / z2;
            this.z3_inv = 1f / z3;
            this.u1 = u1;
            this.u2 = u2;
            this.u3 = u3;
            this.v1 = v1;
            this.v2 = v2;
            this.v3 = v3;
            boolean visible = clip_triangle(x1, y1, x2, y2, x3, y3, false, true);
            if (visible) {
                TriPoint p0 = tri_points[0];
                for (int i = 1; i < n - 1; i++) {
                    TriPoint p1 = tri_points[i];
                    TriPoint p2 = tri_points[i + 1];
                    if (p1.y > p2.y) 
                        fill_textured_tri(p2, p1, p0);
                    else 
                        fill_textured_tri(p1, p2, p0);
                }
            }
        }
    }
    
    void fillTexturedTriangleInterpolateColor(int x1, int y1, float z1, float u1, float v1, float r1, float g1, float b1, 
                                                     int x2, int y2, float z2, float u2, float v2, float r2, float g2, float b2,
                                                     int x3, int y3, float z3, float u3, float v3, float r3, float g3, float b3, 
                                                     Bitmap texture) {
        this.tex = texture;
        code1 = code(x1, y1);
        code2 = code(x2, y2);
        code3 = code(x3, y3);
        if ((code1 | code2 | code3) == 0) {
            tri_points[0].x = x1;
            tri_points[0].y = y1;
            tri_points[0].z_inv = 1f / z1;
            tri_points[0].u = u1;
            tri_points[0].v = v1;
            tri_points[0].r = r1;
            tri_points[0].g = g1;
            tri_points[0].b = b1;
            tri_points[1].x = x2;
            tri_points[1].y = y2;
            tri_points[1].z_inv = 1f / z2;
            tri_points[1].u = u2;
            tri_points[1].v = v2;
            tri_points[1].r = r2;
            tri_points[1].g = g2;
            tri_points[1].b = b2;
            tri_points[2].x = x3;
            tri_points[2].y = y3;
            tri_points[2].z_inv = 1f / z3;
            tri_points[2].u = u3;
            tri_points[2].v = v3;
            tri_points[2].r = r3;
            tri_points[2].g = g3;
            tri_points[2].b = b3;
            sort_first_tri_points();
            fill_textured_tri_interpolate_color(tri_points[0], tri_points[1], tri_points[2]);
        } else if ((code1 & code2 & code3) == 0) {
            this.z1 = z1;
            this.z2 = z2;
            this.z3 = z3;
            this.z1_inv = 1f / z1;
            this.z2_inv = 1f / z2;
            this.z3_inv = 1f / z3;
            this.u1 = u1;
            this.u2 = u2;
            this.u3 = u3;
            this.v1 = v1;
            this.v2 = v2;
            this.v3 = v3;
            this.r1 = r1;
            this.r2 = r2;
            this.r3 = r3;
            this.g1 = g1;
            this.g2 = g2;
            this.g3 = g3;
            this.b1 = b1;
            this.b2 = b2;
            this.b3 = b3;
            boolean visible = clip_triangle(x1, y1, x2, y2, x3, y3, true, true);
            if (visible) {
                TriPoint p0 = tri_points[0];
                for (int i = 1; i < n - 1; i++) {
                    TriPoint p1 = tri_points[i];
                    TriPoint p2 = tri_points[i + 1];
                    if (p1.y > p2.y) 
                        fill_textured_tri_interpolate_color(p2, p1, p0);
                    else 
                        fill_textured_tri_interpolate_color(p1, p2, p0);
                }
            }
        }
    }
    
//  makes tri_points[2].y >= tri_points[1].y AND tri_points[1].y >= tri_points[0].y
    private void sort_first_tri_points() {
        if (tri_points[0].y > tri_points[2].y) {
            if (tri_points[0].y > tri_points[1].y) {
                if (tri_points[1].y > tri_points[2].y) {
                    swap(0, 2);
                } else {
                    swap(0, 2);
                    swap(0, 1);
                }
            } else {
                swap(0, 1);
                swap(0, 2);  
            }
        } else {
            if (tri_points[0].y > tri_points[1].y) {
                swap(0, 1);
            } else {
                if (tri_points[1].y > tri_points[2].y) {
                    swap(1, 2);
                }
            }
        }
    }
    
    private void swap(int i, int j) {
        TriPoint temp = tri_points[i];
        tri_points[i] = tri_points[j];
        tri_points[j] = temp;
    }
    
//  p3.y >= p2.y AND p2.y >= p1.y
    private void fill_tri(TriPoint p1, TriPoint p2, TriPoint p3) {
        int dx13 = 0, dx12 = 0, dx23 = 0;
        float dz_inv13 = 0f, dz_inv12 = 0f, dz_inv23 = 0f;
        if (p3.y != p1.y) {
            dz_inv13 = (p3.z_inv - p1.z_inv) / (p3.y - p1.y);
            dx13 = make_fixed_16x16(p3.x - p1.x) / (p3.y - p1.y);
        }
        if (p2.y != p1.y) {
            dz_inv12 = (p2.z_inv - p1.z_inv) / (p2.y - p1.y);
            dx12 = make_fixed_16x16(p2.x - p1.x) / (p2.y - p1.y);
        }
        if (p2.y != p3.y) {
            dz_inv23 = (p3.z_inv - p2.z_inv) / (p3.y - p2.y);
            dx23 = make_fixed_16x16(p3.x - p2.x) / (p3.y - p2.y);
        }
        int _dx13 = dx13;
        float _dz_inv13 = dz_inv13;
        if (dx13 > dx12) {
            temp_int = dx13;
            dx13 = dx12;
            dx12 = temp_int;
            temp_float = dz_inv13;
            dz_inv13 = dz_inv12;
            dz_inv12 = temp_float;
        }
        float lz_inv = p1.z_inv, rz_inv = lz_inv;
        int lx = make_fixed_16x16(p1.x), rx = lx;
        for (int y = p1.y; y < p2.y; y++) {
            scanline(
                round_fixed_16x16(lx), lz_inv, 
                round_fixed_16x16(rx), rz_inv, y
            );
            lx += dx13;
            rx += dx12;
            lz_inv += dz_inv13;
            rz_inv += dz_inv12;
        }
        if (p1.y == p2.y) {
            if (p2.x < p1.x) {
                lx = make_fixed_16x16(p2.x);
                lz_inv = p2.z_inv;
                temp_int = _dx13;
                _dx13 = dx23;
                dx23 = temp_int;
                temp_float = _dz_inv13;
                _dz_inv13 = dz_inv23;
                dz_inv23 = temp_float;
            } else {
                rx = make_fixed_16x16(p2.x);
                rz_inv = p2.z_inv;
            }
        } else {
            if (_dx13 < dx23) {
                temp_int = _dx13;
                _dx13 = dx23;
                dx23 = temp_int;
                temp_float = _dz_inv13;
                _dz_inv13 = dz_inv23;
                dz_inv23 = temp_float;
            }
        }
        for (int y = p2.y; y <= p3.y; y++) {
            scanline(
                round_fixed_16x16(lx), lz_inv, 
                round_fixed_16x16(rx), rz_inv, y
            );
            lx += _dx13;
            rx += dx23;
            lz_inv += _dz_inv13;
            rz_inv += dz_inv23;
        }
    }
    
    private void fill_tri_interpolate_color(TriPoint p1, TriPoint p2, TriPoint p3) {
        int dx13 = 0, dx12 = 0, dx23 = 0;
        float dz_inv13 = 0f, dz_inv12 = 0f, dz_inv23 = 0f;
        float drdz13 = 0f, drdz12 = 0f, drdz23 = 0f;
        float dgdz13 = 0f, dgdz12 = 0f, dgdz23 = 0f;
        float dbdz13 = 0f, dbdz12 = 0f, dbdz23 = 0f;
        if (p3.y != p1.y) {
            dz_inv13 = (p3.z_inv - p1.z_inv) / (p3.y - p1.y);
            dx13 = make_fixed_16x16(p3.x - p1.x) / (p3.y - p1.y);
            drdz13 = (p3.r * p3.z_inv - p1.r * p1.z_inv) / (p3.y - p1.y);
            dgdz13 = (p3.g * p3.z_inv - p1.g * p1.z_inv) / (p3.y - p1.y);
            dbdz13 = (p3.b * p3.z_inv - p1.b * p1.z_inv) / (p3.y - p1.y);
        }
        if (p2.y != p1.y) {
            dz_inv12 = (p2.z_inv - p1.z_inv) / (p2.y - p1.y);
            dx12 = make_fixed_16x16(p2.x - p1.x) / (p2.y - p1.y);
            drdz12 = (p2.r * p2.z_inv - p1.r * p1.z_inv) / (p2.y - p1.y);
            dgdz12 = (p2.g * p2.z_inv - p1.g * p1.z_inv) / (p2.y - p1.y);
            dbdz12 = (p2.b * p2.z_inv - p1.b * p1.z_inv) / (p2.y - p1.y);
        }
        if (p2.y != p3.y) {
            dz_inv23 = (p3.z_inv - p2.z_inv) / (p3.y - p2.y);
            dx23 = make_fixed_16x16(p3.x - p2.x) / (p3.y - p2.y);
            drdz23 = (p3.r * p3.z_inv - p2.r * p2.z_inv) / (p3.y - p2.y);
            dgdz23 = (p3.g * p3.z_inv - p2.g * p2.z_inv) / (p3.y - p2.y);
            dbdz23 = (p3.b * p3.z_inv - p2.b * p2.z_inv) / (p3.y - p2.y);
        }
        int _dx13 = dx13;
        float _dz_inv13 = dz_inv13;
        float _drdz13 = drdz13, _dgdz13 = dgdz13, _dbdz13 = dbdz13;
        if (dx13 > dx12) {
            temp_int = dx13;
            dx13 = dx12;
            dx12 = temp_int;
            temp_float = dz_inv13;
            dz_inv13 = dz_inv12;
            dz_inv12 = temp_float;
            temp_float = drdz13;
            drdz13 = drdz12;
            drdz12 = temp_float;
            temp_float = dgdz13;
            dgdz13 = dgdz12;
            dgdz12 = temp_float;
            temp_float = dbdz13;
            dbdz13 = dbdz12;
            dbdz12 = temp_float;
        }
        int lx = make_fixed_16x16(p1.x), rx = lx;
        float lz_inv = p1.z_inv, rz_inv = lz_inv;
        float lrdz = p1.z_inv * p1.r, rrdz = lrdz;
        float lgdz = p1.z_inv * p1.g, rgdz = lgdz;
        float lbdz = p1.z_inv * p1.b, rbdz = lbdz;
        for (int y = p1.y; y < p2.y; y++) {
            scanline_interpolate_color(
                y,
                round_fixed_16x16(lx), lrdz, lgdz, lbdz, lz_inv, 
                round_fixed_16x16(rx), rrdz, rgdz, rbdz, rz_inv
            );
            lx += dx13;
            rx += dx12;
            lz_inv += dz_inv13;
            rz_inv += dz_inv12;
            lrdz += drdz13;
            rrdz += drdz12;
            lgdz += dgdz13;
            rgdz += dgdz12;
            lbdz += dbdz13;
            rbdz += dbdz12;
        }
        if (p1.y == p2.y) {
            if (p2.x < p1.x) {
                lx = make_fixed_16x16(p2.x);
                lz_inv = p2.z_inv;
                lrdz = p2.z_inv * p2.r;
                lgdz = p2.z_inv * p2.g;
                lbdz = p2.z_inv * p2.b;
                temp_int = _dx13;
                _dx13 = dx23;
                dx23 = temp_int;
                temp_float = _dz_inv13;
                _dz_inv13 = dz_inv23;
                dz_inv23 = temp_float;
                temp_float = _drdz13;
                _drdz13 = drdz23;
                drdz23 = temp_float;
                temp_float = _dgdz13;
                _dgdz13 = dgdz23;
                dgdz23 = temp_float;
                temp_float = _dbdz13;
                _dbdz13 = dbdz23;
                dbdz23 = temp_float;
            } else {
                rx = make_fixed_16x16(p2.x);
                rz_inv = p2.z_inv;
                rrdz = p2.z_inv * p2.r;
                rgdz = p2.z_inv * p2.g;
                rbdz = p2.z_inv * p2.b;
            }
        } else {
            if (_dx13 < dx23) {
                temp_int = _dx13;
                _dx13 = dx23;
                dx23 = temp_int;
                temp_float = _dz_inv13;
                _dz_inv13 = dz_inv23;
                dz_inv23 = temp_float;
                temp_float = _drdz13;
                _drdz13 = drdz23;
                drdz23 = temp_float;
                temp_float = _dgdz13;
                _dgdz13 = dgdz23;
                dgdz23 = temp_float;
                temp_float = _dbdz13;
                _dbdz13 = dbdz23;
                dbdz23 = temp_float;
            }
        }
        for (int y = p2.y; y <= p3.y; y++) {
            scanline_interpolate_color(
                y,
                round_fixed_16x16(lx), lrdz, lgdz, lbdz, lz_inv, 
                round_fixed_16x16(rx), rrdz, rgdz, rbdz, rz_inv
            );
            lx += _dx13;
            rx += dx23;
            lz_inv += _dz_inv13;
            rz_inv += dz_inv23;
            lrdz += _drdz13;
            rrdz += drdz23;
            lgdz += _dgdz13;
            rgdz += dgdz23;
            lbdz += _dbdz13;
            rbdz += dbdz23;
        }
    }
    
    private void fill_textured_tri(TriPoint p1, TriPoint p2, TriPoint p3) {
        int dx13 = 0, dx12 = 0, dx23 = 0;
        float dz_inv13 = 0f, dz_inv12 = 0f, dz_inv23 = 0f;
        float dudz13 = 0f, dudz12 = 0f, dudz23 = 0f;
        float dvdz13 = 0f, dvdz12 = 0f, dvdz23 = 0f;
        if (p3.y != p1.y) {
            dz_inv13 = (p3.z_inv - p1.z_inv) / (p3.y - p1.y);
            dx13 = make_fixed_16x16(p3.x - p1.x) / (p3.y - p1.y);
            dudz13 = (p3.u * p3.z_inv - p1.u * p1.z_inv) / (p3.y - p1.y);
            dvdz13 = (p3.v * p3.z_inv - p1.v * p1.z_inv) / (p3.y - p1.y);
        }
        if (p2.y != p1.y) {
            dz_inv12 = (p2.z_inv - p1.z_inv) / (p2.y - p1.y);
            dx12 = make_fixed_16x16(p2.x - p1.x) / (p2.y - p1.y);
            dudz12 = (p2.u * p2.z_inv - p1.u * p1.z_inv) / (p2.y - p1.y);
            dvdz12 = (p2.v * p2.z_inv - p1.v * p1.z_inv) / (p2.y - p1.y);
        }
        if (p2.y != p3.y) {
            dz_inv23 = (p3.z_inv - p2.z_inv) / (p3.y - p2.y);
            dx23 = make_fixed_16x16(p3.x - p2.x) / (p3.y - p2.y);
            dudz23 = (p3.u * p3.z_inv - p2.u * p2.z_inv) / (p3.y - p2.y);
            dvdz23 = (p3.v * p3.z_inv - p2.v * p2.z_inv) / (p3.y - p2.y);
        }
        int _dx13 = dx13;
        float _dz_inv13 = dz_inv13;
        float _dudz13 = dudz13, _dvdz13 = dvdz13;
        if (dx13 > dx12) {
            temp_int = dx13;
            dx13 = dx12;
            dx12 = temp_int;
            temp_float = dz_inv13;
            dz_inv13 = dz_inv12;
            dz_inv12 = temp_float;
            temp_float = dudz13;
            dudz13 = dudz12;
            dudz12 = temp_float;
            temp_float = dvdz13;
            dvdz13 = dvdz12;
            dvdz12 = temp_float;
        }
        float lz_inv = p1.z_inv, rz_inv = lz_inv;
        int lx = make_fixed_16x16(p1.x), rx = lx;
        float ludz = p1.u * p1.z_inv, rudz = ludz;
        float lvdz = p1.v * p1.z_inv, rvdz = lvdz;
        for (int y = p1.y; y < p2.y; y++) {
            scanline_texture(
                round_fixed_16x16(lx), lz_inv, ludz, lvdz, 
                round_fixed_16x16(rx), rz_inv, rudz, rvdz, 
                y
            );
            lx += dx13;
            rx += dx12;
            lz_inv += dz_inv13;
            rz_inv += dz_inv12;
            ludz += dudz13;
            rudz += dudz12;
            lvdz += dvdz13;
            rvdz += dvdz12;
        }
        if (p1.y == p2.y) {
            if (p2.x < p1.x) {
                lx = make_fixed_16x16(p2.x);
                lz_inv = p2.z_inv;
                ludz = p2.u * p2.z_inv;
                lvdz = p2.v * p2.z_inv;
                temp_int = _dx13;
                _dx13 = dx23;
                dx23 = temp_int;
                temp_float = _dz_inv13;
                _dz_inv13 = dz_inv23;
                dz_inv23 = temp_float;
                temp_float = _dudz13;
                _dudz13 = dudz23;
                dudz23 = temp_float;
                temp_float = _dvdz13;
                _dvdz13 = dvdz23;
                dvdz23 = temp_float;
            } else {
                rx = make_fixed_16x16(p2.x);
                rz_inv = p2.z_inv;
                rudz = p2.u * p2.z_inv;
                rvdz = p2.v * p2.z_inv;
            }
        } else {
            if (_dx13 < dx23) {
                temp_int = _dx13;
                _dx13 = dx23;
                dx23 = temp_int;
                temp_float = _dz_inv13;
                _dz_inv13 = dz_inv23;
                dz_inv23 = temp_float;
                temp_float = _dudz13;
                _dudz13 = dudz23;
                dudz23 = temp_float;
                temp_float = _dvdz13;
                _dvdz13 = dvdz23;
                dvdz23 = temp_float;
            }
        }
        for (int y = p2.y; y <= p3.y; y++) {
            scanline_texture(
                round_fixed_16x16(lx), lz_inv, ludz, lvdz, 
                round_fixed_16x16(rx), rz_inv, rudz, rvdz, 
                y
            );
            lx += _dx13;
            rx += dx23;
            lz_inv += _dz_inv13;
            rz_inv += dz_inv23;
            ludz += _dudz13;
            rudz += dudz23;
            lvdz += _dvdz13;
            rvdz += dvdz23;
        }
    }
    
    private void fill_textured_tri_interpolate_color(TriPoint p1, TriPoint p2, TriPoint p3) {
        int dx13 = 0, dx12 = 0, dx23 = 0;
        float dz_inv13 = 0f, dz_inv12 = 0f, dz_inv23 = 0f;
        float dudz13 = 0f, dudz12 = 0f, dudz23 = 0f;
        float dvdz13 = 0f, dvdz12 = 0f, dvdz23 = 0f;
        float drdz13 = 0f, drdz12 = 0f, drdz23 = 0f;
        float dgdz13 = 0f, dgdz12 = 0f, dgdz23 = 0f;
        float dbdz13 = 0f, dbdz12 = 0f, dbdz23 = 0f;
        if (p3.y != p1.y) {
            dz_inv13 = (p3.z_inv - p1.z_inv) / (p3.y - p1.y);
            dx13 = make_fixed_16x16(p3.x - p1.x) / (p3.y - p1.y);
            dudz13 = (p3.u * p3.z_inv - p1.u * p1.z_inv) / (p3.y - p1.y);
            dvdz13 = (p3.v * p3.z_inv - p1.v * p1.z_inv) / (p3.y - p1.y);
            drdz13 = (p3.r * p3.z_inv - p1.r * p1.z_inv) / (p3.y - p1.y);
            dgdz13 = (p3.g * p3.z_inv - p1.g * p1.z_inv) / (p3.y - p1.y);
            dbdz13 = (p3.b * p3.z_inv - p1.b * p1.z_inv) / (p3.y - p1.y);
        }
        if (p2.y != p1.y) {
            dz_inv12 = (p2.z_inv - p1.z_inv) / (p2.y - p1.y);
            dx12 = make_fixed_16x16(p2.x - p1.x) / (p2.y - p1.y);
            dudz12 = (p2.u * p2.z_inv - p1.u * p1.z_inv) / (p2.y - p1.y);
            dvdz12 = (p2.v * p2.z_inv - p1.v * p1.z_inv) / (p2.y - p1.y);
            drdz12 = (p2.r * p2.z_inv - p1.r * p1.z_inv) / (p2.y - p1.y);
            dgdz12 = (p2.g * p2.z_inv - p1.g * p1.z_inv) / (p2.y - p1.y);
            dbdz12 = (p2.b * p2.z_inv - p1.b * p1.z_inv) / (p2.y - p1.y);
        }
        if (p2.y != p3.y) {
            dz_inv23 = (p3.z_inv - p2.z_inv) / (p3.y - p2.y);
            dx23 = make_fixed_16x16(p3.x - p2.x) / (p3.y - p2.y);
            dudz23 = (p3.u * p3.z_inv - p2.u * p2.z_inv) / (p3.y - p2.y);
            dvdz23 = (p3.v * p3.z_inv - p2.v * p2.z_inv) / (p3.y - p2.y);
            drdz23 = (p3.r * p3.z_inv - p2.r * p2.z_inv) / (p3.y - p2.y);
            dgdz23 = (p3.g * p3.z_inv - p2.g * p2.z_inv) / (p3.y - p2.y);
            dbdz23 = (p3.b * p3.z_inv - p2.b * p2.z_inv) / (p3.y - p2.y);
        }
        int _dx13 = dx13;
        float _dz_inv13 = dz_inv13;
        float _dudz13 = dudz13, _dvdz13 = dvdz13;
        float _drdz13 = drdz13, _dgdz13 = dgdz13, _dbdz13 = dbdz13;
        if (dx13 > dx12) {
            temp_int = dx13;
            dx13 = dx12;
            dx12 = temp_int;
            temp_float = dz_inv13;
            dz_inv13 = dz_inv12;
            dz_inv12 = temp_float;
            temp_float = dudz13;
            dudz13 = dudz12;
            dudz12 = temp_float;
            temp_float = dvdz13;
            dvdz13 = dvdz12;
            dvdz12 = temp_float;
            temp_float = drdz13;
            drdz13 = drdz12;
            drdz12 = temp_float;
            temp_float = dgdz13;
            dgdz13 = dgdz12;
            dgdz12 = temp_float;
            temp_float = dbdz13;
            dbdz13 = dbdz12;
            dbdz12 = temp_float;
        }
        float lz_inv = p1.z_inv, rz_inv = lz_inv;
        int lx = make_fixed_16x16(p1.x), rx = lx;
        float ludz = p1.u * p1.z_inv, rudz = ludz;
        float lvdz = p1.v * p1.z_inv, rvdz = lvdz;
        float lrdz = p1.z_inv * p1.r, rrdz = lrdz;
        float lgdz = p1.z_inv * p1.g, rgdz = lgdz;
        float lbdz = p1.z_inv * p1.b, rbdz = lbdz;
        for (int y = p1.y; y < p2.y; y++) {
            scanline_texture_interpolate_color(
                round_fixed_16x16(lx), lz_inv, ludz, lvdz, lrdz, lgdz, lbdz, 
                round_fixed_16x16(rx), rz_inv, rudz, rvdz, rrdz, rgdz, rbdz, 
                y
            );
            lx += dx13;
            rx += dx12;
            lz_inv += dz_inv13;
            rz_inv += dz_inv12;
            ludz += dudz13;
            rudz += dudz12;
            lvdz += dvdz13;
            rvdz += dvdz12;
            lrdz += drdz13;
            rrdz += drdz12;
            lgdz += dgdz13;
            rgdz += dgdz12;
            lbdz += dbdz13;
            rbdz += dbdz12;
        }
        if (p1.y == p2.y) {
            if (p2.x < p1.x) {
                lx = make_fixed_16x16(p2.x);
                lz_inv = p2.z_inv;
                ludz = p2.u * p2.z_inv;
                lvdz = p2.v * p2.z_inv;
                lrdz = p2.z_inv * p2.r;
                lgdz = p2.z_inv * p2.g;
                lbdz = p2.z_inv * p2.b;
                temp_int = _dx13;
                _dx13 = dx23;
                dx23 = temp_int;
                temp_float = _dz_inv13;
                _dz_inv13 = dz_inv23;
                dz_inv23 = temp_float;
                temp_float = _dudz13;
                _dudz13 = dudz23;
                dudz23 = temp_float;
                temp_float = _dvdz13;
                _dvdz13 = dvdz23;
                dvdz23 = temp_float;
                temp_float = _drdz13;
                _drdz13 = drdz23;
                drdz23 = temp_float;
                temp_float = _dgdz13;
                _dgdz13 = dgdz23;
                dgdz23 = temp_float;
                temp_float = _dbdz13;
                _dbdz13 = dbdz23;
                dbdz23 = temp_float;
            } else {
                rx = make_fixed_16x16(p2.x);
                rz_inv = p2.z_inv;
                rudz = p2.u * p2.z_inv;
                rvdz = p2.v * p2.z_inv;
                rrdz = p2.z_inv * p2.r;
                rgdz = p2.z_inv * p2.g;
                rbdz = p2.z_inv * p2.b;
            }
        } else {
            if (_dx13 < dx23) {
                temp_int = _dx13;
                _dx13 = dx23;
                dx23 = temp_int;
                temp_float = _dz_inv13;
                _dz_inv13 = dz_inv23;
                dz_inv23 = temp_float;
                temp_float = _dudz13;
                _dudz13 = dudz23;
                dudz23 = temp_float;
                temp_float = _dvdz13;
                _dvdz13 = dvdz23;
                dvdz23 = temp_float;
                temp_float = _drdz13;
                _drdz13 = drdz23;
                drdz23 = temp_float;
                temp_float = _dgdz13;
                _dgdz13 = dgdz23;
                dgdz23 = temp_float;
                temp_float = _dbdz13;
                _dbdz13 = dbdz23;
                dbdz23 = temp_float;
            }
        }
        for (int y = p2.y; y <= p3.y; y++) {
            scanline_texture_interpolate_color(
                round_fixed_16x16(lx), lz_inv, ludz, lvdz, lrdz, lgdz, lbdz, 
                round_fixed_16x16(rx), rz_inv, rudz, rvdz, rrdz, rgdz, rbdz, 
                y
            );
            lx += _dx13;
            rx += dx23;
            lz_inv += _dz_inv13;
            rz_inv += dz_inv23;
            ludz += _dudz13;
            rudz += dudz23;
            lvdz += _dvdz13;
            rvdz += dvdz23;
            lrdz += _drdz13;
            rrdz += drdz23;
            lgdz += _dgdz13;
            rgdz += dgdz23;
            lbdz += _dbdz13;
            rbdz += dbdz23;
        }
    }
    
    private int n;
    private final TriPoint[] tri_points = new TriPoint[10];
    
//  stores in the "tri_points" array the clipped points belonging to the triangle 
//  in the counterclockwise order
    private boolean clip_triangle(int x1, int y1, int x2, int y2, int x3, int y3, final boolean interpolate_rgb, final boolean interpolate_uv) {
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
                dist_inv = 1f / dist(x1, y1, x2, y2);
                float coeff = dist_inv * dist(x1, y1, line_x1, line_y1);
                tri_points[0].z_inv = z1_inv + (z2_inv - z1_inv) * coeff;
                tri_points[0].z = 1f / tri_points[0].z_inv; 
                if (interpolate_rgb) {
                    tri_points[0].r = tri_points[0].z * (r1 * z1_inv + (r2 * z2_inv - r1 * z1_inv) * coeff);
                    tri_points[0].g = tri_points[0].z * (g1 * z1_inv + (g2 * z2_inv - g1 * z1_inv) * coeff);
                    tri_points[0].b = tri_points[0].z * (b1 * z1_inv + (b2 * z2_inv - b1 * z1_inv) * coeff);
                }
                if (interpolate_uv) {
                    tri_points[0].u = tri_points[0].z * (u1 * z1_inv + (u2 * z2_inv - u1 * z1_inv) * coeff);
                    tri_points[0].v = tri_points[0].z * (v1 * z1_inv + (v2 * z2_inv - v1 * z1_inv) * coeff);
                }
            } else {
                tri_points[0].z = z1;
                tri_points[0].z_inv = 1f / z1;
                if (interpolate_rgb) {
                    tri_points[0].r = r1;
                    tri_points[0].g = g1;
                    tri_points[0].b = b1;
                }
                if (interpolate_uv) {
                    tri_points[0].u = u1;
                    tri_points[0].v = v1;
                }
            }
            if (line_x2 != x2 || line_y2 != y2) {
                float coeff;
                if (dist_inv == -1f)
                    coeff = dist(x2, y2, line_x2, line_y2) / dist(x1, y1, x2, y2);
                else
                    coeff = dist_inv * dist(x2, y2, line_x2, line_y2);
                tri_points[1].z_inv = z2_inv + (z1_inv - z2_inv) * coeff;
                tri_points[1].z = 1f / tri_points[1].z_inv;
                if (interpolate_rgb) {
                    tri_points[1].r = tri_points[1].z * (r2 * z2_inv + (r1 * z1_inv - r2 * z2_inv) * coeff);
                    tri_points[1].g = tri_points[1].z * (g2 * z2_inv + (g1 * z1_inv - g2 * z2_inv) * coeff);
                    tri_points[1].b = tri_points[1].z * (b2 * z2_inv + (b1 * z1_inv - b2 * z2_inv) * coeff);
                }
                if (interpolate_uv) {
                    tri_points[1].u = tri_points[1].z * (u2 * z2_inv + (u1 * z1_inv - u2 * z2_inv) * coeff);
                    tri_points[1].v = tri_points[1].z * (v2 * z2_inv + (v1 * z1_inv - v2 * z2_inv) * coeff);
                }
            } else {
                tri_points[1].z = z2;
                tri_points[1].z_inv = 1f / z2;
                if (interpolate_rgb) {
                    tri_points[1].r = r2;
                    tri_points[1].g = g2;
                    tri_points[1].b = b2;
                }
                if (interpolate_uv) {
                    tri_points[1].u = u2;
                    tri_points[1].v = v2;
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
                    dist_inv = 1f / dist(x2, y2, x3, y3);
                    float coeff = dist_inv * dist(x2, y2, line_x1, line_y1);
                    tri_points[0].z_inv = z2_inv + (z3_inv - z2_inv) * coeff;
                    tri_points[0].z = 1f / tri_points[0].z_inv;
                    if (interpolate_rgb) {
                        tri_points[0].r = tri_points[0].z * (r2 * z2_inv + (r3 * z3_inv - r2 * z2_inv) * coeff);
                        tri_points[0].g = tri_points[0].z * (g2 * z2_inv + (g3 * z3_inv - g2 * z2_inv) * coeff);
                        tri_points[0].b = tri_points[0].z * (b2 * z2_inv + (b3 * z3_inv - b2 * z2_inv) * coeff);
                    }
                    if (interpolate_uv) {
                        tri_points[0].u = tri_points[0].z * (u2 * z2_inv + (u3 * z3_inv - u2 * z2_inv) * coeff);
                        tri_points[0].v = tri_points[0].z * (v2 * z2_inv + (v3 * z3_inv - v2 * z2_inv) * coeff);
                    }
                } else {
                    tri_points[0].z = z2;
                    tri_points[0].z_inv = 1f / z2;
                    if (interpolate_rgb) {
                        tri_points[0].r = r2;
                        tri_points[0].g = g2;
                        tri_points[0].b = b2;
                    }
                    if (interpolate_uv) {
                        tri_points[0].u = u2;
                        tri_points[0].v = v2;
                    }
                }
            
                if (line_x2 != x3 || line_y2 != y3) {
                    float coeff;
                    if (dist_inv == -1f)
                        coeff = dist(x3, y3, line_x2, line_y2) / dist(x2, y2, x3, y3);
                    else
                        coeff = dist_inv * dist(x3, y3, line_x2, line_y2);
                    tri_points[1].z_inv = z3_inv + (z2_inv - z3_inv) * coeff;
                    tri_points[1].z = 1f / tri_points[1].z;
                    if (interpolate_rgb) {
                        tri_points[1].r = tri_points[1].z * (r3 * z3_inv + (r2 * z2_inv - r3 * z3_inv) * coeff);
                        tri_points[1].g = tri_points[1].z * (g3 * z3_inv + (g2 * z2_inv - g3 * z3_inv) * coeff);
                        tri_points[1].b = tri_points[1].z * (b3 * z3_inv + (b2 * z2_inv - b3 * z3_inv) * coeff);
                    }
                    if (interpolate_uv) {
                        tri_points[1].u = tri_points[1].z * (u3 * z3_inv + (u2 * z2_inv - u3 * z3_inv) * coeff);
                        tri_points[1].v = tri_points[1].z * (v3 * z3_inv + (v2 * z2_inv - v3 * z3_inv) * coeff);
                    }
                } else {
                    tri_points[1].z = z3;
                    tri_points[1].z_inv = 1f / z3;
                    if (interpolate_rgb) {
                        tri_points[1].r = r3;
                        tri_points[1].g = g3;
                        tri_points[1].b = b3;
                    }
                    if (interpolate_uv) {
                        tri_points[1].u = u3;
                        tri_points[1].v = v3;
                    }
                }
                
                n = 2;
                
            } else {
                if (tri_points[1].x == line_x1 && tri_points[1].y == line_y1) {

                    tri_points[2].x = line_x2;
                    tri_points[2].y = line_y2;
                    
                    if (line_x2 != x3 || line_y2 != y3) {
                        float coeff = dist(x3, y3, line_x2, line_y2) / dist(x2, y2, x3, y3);
                        tri_points[2].z_inv = z3_inv + (z2_inv - z3_inv) * coeff;
                        tri_points[2].z = 1f / tri_points[2].z_inv;
                        if (interpolate_rgb) {
                            tri_points[2].r = tri_points[2].z * (r3 * z3_inv + (r2 * z2_inv - r3 * z3_inv) * coeff);
                            tri_points[2].g = tri_points[2].z * (g3 * z3_inv + (g2 * z2_inv - g3 * z3_inv) * coeff);
                            tri_points[2].b = tri_points[2].z * (b3 * z3_inv + (b2 * z2_inv - b3 * z3_inv) * coeff);
                        }
                        if (interpolate_uv) {
                            tri_points[2].u = tri_points[2].z * (u3 * z3_inv + (u2 * z2_inv - u3 * z3_inv) * coeff);
                            tri_points[2].v = tri_points[2].z * (v3 * z3_inv + (v2 * z2_inv - v3 * z3_inv) * coeff);
                        }
                    } else {
                        tri_points[2].z = z3;
                        tri_points[2].z_inv = 1f / z3;
                        if (interpolate_rgb) {
                            tri_points[2].r = r3;
                            tri_points[2].g = g3;
                            tri_points[2].b = b3;
                        }
                        if (interpolate_uv) {
                            tri_points[2].u = u3;
                            tri_points[2].v = v3;
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
                        dist_inv = 1f / dist(x2, y2, x3, y3);
                        float coeff = dist_inv * dist(x2, y2, line_x1, line_y1);
                        tri_points[2].z_inv = z2_inv + (z3_inv - z2_inv) * coeff;
                        tri_points[2].z = 1f / tri_points[2].z_inv;
                        if (interpolate_rgb) {
                            tri_points[2].r = tri_points[2].z * (r2 * z2_inv + (r3 * z3_inv - r2 * z2_inv) * coeff);
                            tri_points[2].g = tri_points[2].z * (g2 * z2_inv + (g3 * z3_inv - g2 * z2_inv) * coeff);
                            tri_points[2].b = tri_points[2].z * (b2 * z2_inv + (b3 * z3_inv - b2 * z2_inv) * coeff);
                        }
                        if (interpolate_uv) {
                            tri_points[2].u = tri_points[2].z * (u2 * z2_inv + (u3 * z3_inv - u2 * z2_inv) * coeff);
                            tri_points[2].v = tri_points[2].z * (v2 * z2_inv + (v3 * z3_inv - v2 * z2_inv) * coeff);
                        }
                    } else {
                        tri_points[2].z = z2;
                        tri_points[2].z_inv = 1f / z2;
                        if (interpolate_rgb) {
                            tri_points[2].r = r2;
                            tri_points[2].g = g2;
                            tri_points[2].b = b2;
                        }
                        if (interpolate_uv) {
                            tri_points[2].u = u2;
                            tri_points[2].v = v2;
                        }
                    }

                    if (line_x2 != x3 || line_y2 != y3) {
                    
                        float coeff;
                        if (dist_inv == -1f)
                            coeff = dist(x3, y3, line_x2, line_y2) / dist(x2, y2, x3, y3);
                        else
                            coeff = dist_inv * dist(x3, y3, line_x2, line_y2);
                        tri_points[3].z_inv = z3_inv + (z2_inv - z3_inv) * coeff;
                        tri_points[3].z = 1f / tri_points[3].z_inv;
                        if (interpolate_rgb) {
                            tri_points[3].r = tri_points[3].z * (r3 * z3_inv + (r2 * z2_inv - r3 * z3_inv) * coeff);
                            tri_points[3].g = tri_points[3].z * (g3 * z3_inv + (g2 * z2_inv - g3 * z3_inv) * coeff);
                            tri_points[3].b = tri_points[3].z * (b3 * z3_inv + (b2 * z2_inv - b3 * z3_inv) * coeff);
                        }
                        if (interpolate_uv) {
                            tri_points[3].u = tri_points[3].z * (u3 * z3_inv + (u2 * z2_inv - u3 * z3_inv) * coeff);
                            tri_points[3].v = tri_points[3].z * (v3 * z3_inv + (v2 * z2_inv - v3 * z3_inv) * coeff);
                        }
                    } else {

                        tri_points[3].z = z3;
                        tri_points[3].z_inv = 1f / z3;
                        if (interpolate_rgb) {
                            tri_points[3].r = r3;
                            tri_points[3].g = g3;
                            tri_points[3].b = b3;
                        }
                        if (interpolate_uv) {
                            tri_points[3].u = u3;
                            tri_points[3].v = v3;
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
                    dist_inv = 1f / dist(x3, y3, x1, y1);
                    float coeff = dist_inv * dist(x3, y3, line_x1, line_y1);
                    tri_points[0].z_inv = z3_inv + (z1_inv - z3_inv) * coeff;
                    tri_points[0].z = 1f / tri_points[0].z_inv;
                    if (interpolate_rgb) {
                        tri_points[0].r = tri_points[0].z * (r3 * z3_inv + (r1 * z1_inv - r3 * z3_inv) * coeff);
                        tri_points[0].g = tri_points[0].z * (g3 * z3_inv + (g1 * z1_inv - g3 * z3_inv) * coeff);
                        tri_points[0].b = tri_points[0].z * (b3 * z3_inv + (b1 * z1_inv - b3 * z3_inv) * coeff);
                    }
                    if (interpolate_uv) {
                        tri_points[0].u = tri_points[0].z * (u3 * z3_inv + (u1 * z1_inv - u3 * z3_inv) * coeff);
                        tri_points[0].v = tri_points[0].z * (v3 * z3_inv + (v1 * z1_inv - v3 * z3_inv) * coeff);
                    }
                } else {
                    tri_points[0].z = z3;
                    tri_points[0].z_inv = 1f / z3;
                    if (interpolate_rgb) {
                        tri_points[0].r = r3;
                        tri_points[0].g = g3;
                        tri_points[0].b = b3;
                    }
                    if (interpolate_uv) {
                        tri_points[0].u = u3;
                        tri_points[0].v = v3;
                    }
                }
            
                if (line_x2 != x1 || line_y2 != y1) {
                    float coeff;
                    if (dist_inv == -1f)
                        coeff = dist(x1, y1, line_x2, line_y2) / dist(x3, y3, x1, y1);
                    else
                        coeff = dist_inv * dist(x1, y1, line_x2, line_y2);
                    tri_points[1].z_inv = z1_inv + (z3_inv - z1_inv) * coeff;
                    tri_points[1].z = 1f / tri_points[1].z_inv;
                    if (interpolate_rgb) {
                        tri_points[1].r = tri_points[1].z * (r1 * z1_inv + (r3 * z3_inv - r1 * z1_inv) * coeff);
                        tri_points[1].g = tri_points[1].z * (g1 * z1_inv + (g3 * z3_inv - g1 * z1_inv) * coeff);
                        tri_points[1].b = tri_points[1].z * (b1 * z1_inv + (b3 * z3_inv - b1 * z1_inv) * coeff);
                    }
                    if (interpolate_uv) {
                        tri_points[1].u = tri_points[1].z * (u1 * z1_inv + (u3 * z3_inv - u1 * z1_inv) * coeff);
                        tri_points[1].v = tri_points[1].z * (v1 * z1_inv + (v3 * z3_inv - v1 * z1_inv) * coeff);
                    }
                } else {
                    tri_points[1].z = z1;
                    tri_points[1].z_inv = 1f / z1;
                    if (interpolate_rgb) {
                        tri_points[1].r = r1;
                        tri_points[1].g = g1;
                        tri_points[1].b = b1;
                    }
                    if (interpolate_uv) {
                        tri_points[1].u = u1;
                        tri_points[1].v = v1;
                    }
                }
                
                n = 2;
                
            } else {
                float dist_inv = -1f;
                if (tri_points[n - 1].x != line_x1 || tri_points[n - 1].y != line_y1) {

                    dist_inv = 1f / dist(x3, y3, x1, y1);
                    float coeff = dist_inv * dist(x3, y3, line_x1, line_y1);
                    tri_points[n].x = line_x1;
                    tri_points[n].y = line_y1;
                    tri_points[n].z_inv = z3_inv + (z1_inv - z3_inv) * coeff;
                    tri_points[n].z = 1f / tri_points[n].z_inv;
                    if (interpolate_rgb) {
                        tri_points[n].r = tri_points[n].z * (r3 * z3_inv + (r1 * z1_inv - r3 * z3_inv) * coeff);
                        tri_points[n].g = tri_points[n].z * (g3 * z3_inv + (g1 * z1_inv - g3 * z3_inv) * coeff);
                        tri_points[n].b = tri_points[n].z * (b3 * z3_inv + (b1 * z1_inv - b3 * z3_inv) * coeff);
                    }
                    if (interpolate_uv) {
                        tri_points[n].u = tri_points[n].z * (u3 * z3_inv + (u1 * z1_inv - u3 * z3_inv) * coeff);
                        tri_points[n].v = tri_points[n].z * (v3 * z3_inv + (v1 * z1_inv - v3 * z3_inv) * coeff);
                    }
                    n++;
                } 
                if (tri_points[0].x != line_x2 || tri_points[0].y != line_y2) {
                    
                    float coeff;
                    if (dist_inv == -1f)
                        coeff = dist(x1, y1, line_x2, line_y2) / dist(x3, y3, x1, y1);
                    else
                        coeff = dist_inv * dist(x1, y1, line_x2, line_y2);
                    tri_points[n].x = line_x2;
                    tri_points[n].y = line_y2;
                    tri_points[n].z_inv = z1_inv + (z3_inv - z1_inv) * coeff;
                    tri_points[n].z = 1f / tri_points[n].z_inv;
                    if (interpolate_rgb) {
                        tri_points[n].r = tri_points[n].z * (r1 * z1_inv + (r3 * z3_inv - r1 * z1_inv) * coeff);
                        tri_points[n].g = tri_points[n].z * (g1 * z1_inv + (g3 * z3_inv - g1 * z1_inv) * coeff);
                        tri_points[n].b = tri_points[n].z * (b1 * z1_inv + (b3 * z3_inv - b1 * z1_inv) * coeff);
                    }
                    if (interpolate_uv) {
                        tri_points[n].u = tri_points[n].z * (u1 * z1_inv + (u3 * z3_inv - u1 * z1_inv) * coeff);
                        tri_points[n].v = tri_points[n].z * (v1 * z1_inv + (v3 * z3_inv - v1 * z1_inv) * coeff);
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
        temp_det_inv = 1f / (v1v2_x * v1v3_y - v1v2_y * v1v3_x);

        if (test_point(-x1, -y1)) { // point (0, 0) belongs to triangle
            tri_points[n].x = 0;
            tri_points[n].y = 0;
            tri_points[n].z_inv = z1_inv + (z2_inv - z1_inv) * u + (z3_inv - z1_inv) * v;
            tri_points[n].z = 1f / tri_points[n].z_inv;
            if (interpolate_rgb) {
                tri_points[n].r = tri_points[n].z * (r1 * z1_inv + (r2 * z2_inv - r1 * z1_inv) * u + (r3 * z3_inv - r1 * z1_inv) * v);
                tri_points[n].g = tri_points[n].z * (g1 * z1_inv + (g2 * z2_inv - g1 * z1_inv) * u + (g3 * z3_inv - g1 * z1_inv) * v);
                tri_points[n].b = tri_points[n].z * (b1 * z1_inv + (b2 * z2_inv - b1 * z1_inv) * u + (b3 * z3_inv - b1 * z1_inv) * v);              
            }
            if (interpolate_uv) {
                tri_points[n].u = tri_points[n].z * (u1 * z1_inv + (u2 * z2_inv - u1 * z1_inv) * u + (u3 * z3_inv - u1 * z1_inv) * v);
                tri_points[n].v = tri_points[n].z * (v1 * z1_inv + (v2 * z2_inv - v1 * z1_inv) * u + (v3 * z3_inv - v1 * z1_inv) * v);
            }
            n++;
        }

        if (test_point(-x1, y_max - y1)) { // point (0, h - 1) belongs to triangle
            max_y_index = n; // small optimisation
            tri_points[n].x = 0;
            tri_points[n].y = y_max;
            tri_points[n].z_inv = z1_inv + (z2_inv - z1_inv) * u + (z3_inv - z1_inv) * v;
            tri_points[n].z = 1f / tri_points[n].z_inv;
            if (interpolate_rgb) {
                tri_points[n].r = tri_points[n].z * (r1 * z1_inv + (r2 * z2_inv - r1 * z1_inv) * u + (r3 * z3_inv - r1 * z1_inv) * v);
                tri_points[n].g = tri_points[n].z * (g1 * z1_inv + (g2 * z2_inv - g1 * z1_inv) * u + (g3 * z3_inv - g1 * z1_inv) * v);
                tri_points[n].b = tri_points[n].z * (b1 * z1_inv + (b2 * z2_inv - b1 * z1_inv) * u + (b3 * z3_inv - b1 * z1_inv) * v);              
            }
            if (interpolate_uv) {
                tri_points[n].u = tri_points[n].z * (u1 * z1_inv + (u2 * z2_inv - u1 * z1_inv) * u + (u3 * z3_inv - u1 * z1_inv) * v);
                tri_points[n].v = tri_points[n].z * (v1 * z1_inv + (v2 * z2_inv - v1 * z1_inv) * u + (v3 * z3_inv - v1 * z1_inv) * v);
            }
            n++;
        }

        if (test_point(x_max - x1, -y1)) { // point (w - 1, 0) belongs to triangle
            tri_points[n].x = x_max;
            tri_points[n].y = 0;
            tri_points[n].z_inv = z1_inv + (z2_inv - z1_inv) * u + (z3_inv - z1_inv) * v;
            tri_points[n].z = 1f / tri_points[n].z_inv;
            if (interpolate_rgb) {
                tri_points[n].r = tri_points[n].z * (r1 * z1_inv + (r2 * z2_inv - r1 * z1_inv) * u + (r3 * z3_inv - r1 * z1_inv) * v);
                tri_points[n].g = tri_points[n].z * (g1 * z1_inv + (g2 * z2_inv - g1 * z1_inv) * u + (g3 * z3_inv - g1 * z1_inv) * v);
                tri_points[n].b = tri_points[n].z * (b1 * z1_inv + (b2 * z2_inv - b1 * z1_inv) * u + (b3 * z3_inv - b1 * z1_inv) * v);              
            }
            if (interpolate_uv) {
                tri_points[n].u = tri_points[n].z * (u1 * z1_inv + (u2 * z2_inv - u1 * z1_inv) * u + (u3 * z3_inv - u1 * z1_inv) * v);
                tri_points[n].v = tri_points[n].z * (v1 * z1_inv + (v2 * z2_inv - v1 * z1_inv) * u + (v3 * z3_inv - v1 * z1_inv) * v);
            }
            n++;
        }

        if (test_point(x_max - x1, y_max - y1)) { // point (w - 1, h - 1) belongs to triangle
            tri_points[n].x = x_max;
            tri_points[n].y = y_max;
            tri_points[n].z_inv = z1_inv + (z2_inv - z1_inv) * u + (z3_inv - z1_inv) * v;
            tri_points[n].z = 1f / tri_points[n].z_inv;
            if (interpolate_rgb) {
                tri_points[n].r = tri_points[n].z * (r1 * z1_inv + (r2 * z2_inv - r1 * z1_inv) * u + (r3 * z3_inv - r1 * z1_inv) * v);
                tri_points[n].g = tri_points[n].z * (g1 * z1_inv + (g2 * z2_inv - g1 * z1_inv) * u + (g3 * z3_inv - g1 * z1_inv) * v);
                tri_points[n].b = tri_points[n].z * (b1 * z1_inv + (b2 * z2_inv - b1 * z1_inv) * u + (b3 * z3_inv - b1 * z1_inv) * v);              
            }
            if (interpolate_uv) {
                tri_points[n].u = tri_points[n].z * (u1 * z1_inv + (u2 * z2_inv - u1 * z1_inv) * u + (u3 * z3_inv - u1 * z1_inv) * v);
                tri_points[n].v = tri_points[n].z * (v1 * z1_inv + (v2 * z2_inv - v1 * z1_inv) * u + (v3 * z3_inv - v1 * z1_inv) * v);
            }
            n++;
        }
        
        if (n < 3) //triangle is not visible
            return false;
        
        
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
        Arrays.sort(tri_points, 1, n, (TriPoint p1, TriPoint p2) -> {
            return Double.compare(p1.angle_rel_to(max_y), p2.angle_rel_to(max_y));
        });
        
        for (int i = 0; i < n; i++) tri_points[i].angle_calc = false;
        return true;
    }
    
    private float u, v; // barycentric coordinates of tested point 
    private float temp_det_inv;
    private float v1v2_x, v1v2_y, v1v3_x, v1v3_y;

    private boolean test_point(float px, float py) { 
        float det1 = px * v1v3_y - py * v1v3_x;
        u = det1 * temp_det_inv;
        if (u >= 0f && u <= 1f) {
            float det2 = v1v2_x * py - v1v2_y * px;
            v = det2 * temp_det_inv;
            return v >= 0f && v <= 1f && u + v <= 1f;
        }
        return false;
    }
    
    void line(int x1, int y1, float z1, int x2, int y2, float z2) {
        clip(x1, y1, x2, y2);
        float z1_inv = 1f / z1;
        float z2_inv = 1f / z2;
        if (line_x1 != -1) {
            line_z1_inv = z1_inv;
            line_z2_inv = z2_inv;
            float src_len = -1.0f;
            if (line_x1 != x1 || line_y1 != y1) {
                src_len = dist(x1, y1, x2, y2);
                line_z1_inv = z1_inv + (z2_inv - z1_inv) * dist(x1, y1, line_x1, line_y1) / src_len;
            }
            if (line_x2 != x2 || line_y2 != y2) {
                if (src_len == -1.0f) 
                    src_len = dist(x1, y1, x2, y2);
                line_z2_inv = z2_inv + (z1_inv - z2_inv) * dist(x2, y2, line_x2, line_y2) / src_len;
            }
            bresenham(line_x1, line_y1, line_z1_inv, line_x2, line_y2, line_z2_inv);
        } 
    }
    
    private void bresenham(int x1, int y1, float z1_inv, int x2, int y2, float z2_inv) {
        if (x1 == x2) { // vertical line
            if (y1 == y2) {
                int i = hash(x1, y1, w);
                if (z_buff[i] < z1_inv || z_buff[i] < z2_inv) {
                    g.plotToHash(i);
                    z_buff[i] = Math.max(z1_inv, z2_inv);
                }
            } else {
                if (y1 < y2) ver_line(x1, y1, z1_inv, y2, z2_inv);
                else         ver_line(x1, y2, z2_inv, y1, z1_inv);
            }
        } else if (y1 == y2) { // horizontal line
            if (x1 < x2) scanline(x1, z1_inv, x2, z2_inv, y1);
            else         scanline(x2, z2_inv, x1, z1_inv, y1);
        } else {
            if (x1 > x2) {
                temp_int = x1;
                x1 = x2;
                x2 = temp_int;
                temp_int = y1;
                y1 = y2;
                y2 = temp_int;
                temp_float = z1_inv;
                z1_inv = z2_inv;
                z2_inv = temp_float;
            }
            int dx = x2 - x1, dy;
            if (y1 < y2) {
                dy = y2 - y1;
                if (dx > dy) x_line(x1, y1, z1_inv, x2, z2_inv, dx, dy, 1);
                else if (dx < dy) y_line(x1, y1, z1_inv, y2, z2_inv, dx, dy, 1);
                else line_45(x1, y1, z1_inv, x2, z2_inv, 1);
            } else {
                dy = y1 - y2;
                if (dx > dy) x_line(x1, y1, z1_inv, x2, z2_inv, dx, dy, -1);
                else if (dx < dy) y_line(x1, y1, z1_inv, y2, z2_inv, dx, dy, -1);
                else line_45(x1, y1, z1_inv, x2, z2_inv, -1);
            }
        }
    }
    
//  x1 < x2
    private void x_line(int x1, int y1, float z1_inv, int x2, float z2_inv, int dx, int dy, final int y_inc) {
        int i = hash(x1, y1, w);
        int _dx = 0, _dy = 0;
        float z_inv = z1_inv;
        final float dz_inv = (z2_inv - z1_inv) / sqrt_table[hash(dx, dy, w)];
        for (int x = x1, err = 0;;) {
            if (z_buff[i] < z_inv) {
                g.plotToHash(i);
                z_buff[i] = z_inv;
            }
            if (++x > x2)
                break;
            i++;
            _dx++;
            err += dy;
            if (err << 1 >= dx) {
                _dy++;
                err -= dx;
                i += y_inc * w;
            } 
            z_inv = z1_inv + dz_inv * sqrt_table[hash(_dx, _dy, w)];
        } 
    }
    
    private void y_line(int x1, int y1, float z1_inv, int y2, float z2_inv, int dx, int dy, final int y_inc) {
        int i = hash(x1, y1, w);
        int _dy = 0, _dx = 0;
        float z_inv = z1_inv;
        final float dz_inv = (z2_inv - z1_inv) / sqrt_table[hash(dx, dy, w)];
        for (int y = y1, err = 0;;) {
            if (z_buff[i] < z_inv) {
                g.plotToHash(i);
                z_buff[i] = z_inv;
            }
            y += y_inc;
            if (y == y2) {
                _dy++;
                err += dx;
                i += y_inc * w;
                if (err << 1 >= dy) {
                    i++;
                    _dx++;
                }
                z_inv = z1_inv + dz_inv * sqrt_table[hash(_dx, _dy, w)];
                if (z_buff[i] < z_inv) {
                    g.plotToHash(i);
                    z_buff[i] = z_inv;
                }
                return;
            }
            _dy++;
            err += dx;
            i += y_inc * w;
            if (err << 1 >= dy) {
                i++;
                _dx++;
                err -= dy;
            }
            z_inv += dz_inv;
        }
    }
    
    private void ver_line(int x, int y1, float z1_inv, int y2, float z2_inv) {
        int i = hash(x, y1, w);
        float z_inv = z1_inv;
        final float dz_inv = (z2_inv - z1_inv) / (y2 - y1);
        for (int y = y1;;) {
            if (z_buff[i] < z_inv) {
                g.plotToHash(i);
                z_buff[i] = z_inv;
            }
            if (++y > y2)
                break;
            i += w;
            z_inv += dz_inv;
        }
    }
    
    private void scanline(int x1, float z1_inv, int x2, float z2_inv, int y) {
        int i = hash(x1, y, w);
        float z_inv = z1_inv;
        final float dz_inv = (z2_inv - z1_inv) / (x2 - x1);
        for (int x = x1;;) {
            if (z_buff[i] < z_inv) {
                g.plotToHash(i);
                z_buff[i] = z_inv;
            } 
            if (++x > x2)
                break;
            z_inv += dz_inv;
            i++;
        }
    }
    
    private void scanline_texture(int x1, float z1_inv, float udz1, float vdz1, int x2, float z2_inv, float udz2, float vdz2, int y) {
        int i = hash(x1, y, w);
        float z;
        float z_inv = z1_inv;
        float udz = udz1, vdz = vdz1;
        final float dz_inv = (z2_inv - z1_inv) / (x2 - x1);
        final float dudz = (udz2 - udz1) / (x2 - x1);
        final float dvdz = (vdz2 - vdz1) / (x2 - x1);
        final boolean modulate = this.modulate;
        for (int x = x1;;) {
            if (z_buff[i] < z_inv) {
                z = 1f / z_inv;
                z_buff[i] = z_inv;
                if (modulate) {
                    g.plotToHash(i);
                    g.modulateInHash(i, tex.getRGB(roundPositive(z * udz), roundPositive(z * vdz)));
                } else {
                    g.plotToHash(i, tex.getRGB(roundPositive(z * udz), roundPositive(z * vdz)));
                }
            } 
            if (++x > x2)
                break;
            z_inv += dz_inv;
            udz += dudz;
            vdz += dvdz;
            i++;
        }
    }
    
    private void scanline_interpolate_color(int y, int x1, float rdz1, float gdz1, float bdz1, float z1_inv, int x2, float rdz2, float gdz2, float bdz2, float z2_inv) {
        int i = hash(x1, y, w);
        float z;
        float z_inv = z1_inv;
        float rdz = rdz1, gdz = gdz1, bdz = bdz1;
        
        final float drdz = (rdz2 - rdz1) / (x2 - x1);
        final float dgdz = (gdz2 - gdz1) / (x2 - x1);
        final float dbdz = (bdz2 - bdz1) / (x2 - x1);
        final float dz_inv = (z2_inv - z1_inv) / (x2 - x1);
        for (int x = x1;;) {
            if (z_buff[i] < z_inv) {
                z = 1f / z_inv;
                g.plotToHash(i, rgb(roundPositive(rdz * z), roundPositive(gdz * z), roundPositive(bdz * z)));
                z_buff[i] = z_inv;
            } 
            if (++x > x2)
                break;
            z_inv += dz_inv;
            rdz += drdz;
            gdz += dgdz;
            bdz += dbdz;
            i++;
        }
    }
    
    private void scanline_texture_interpolate_color(int x1, float z1_inv, float udz1, float vdz1, float rdz1, float gdz1, float bdz1, 
                                                    int x2, float z2_inv, float udz2, float vdz2, float rdz2, float gdz2, float bdz2, 
                                                    int y) {
        int i = hash(x1, y, w);
        float z;
        float z_inv = z1_inv;
        float udz = udz1, vdz = vdz1;
        float rdz = rdz1, gdz = gdz1, bdz = bdz1;
        final float dudz = (udz2 - udz1) / (x2 - x1);
        final float dvdz = (vdz2 - vdz1) / (x2 - x1);
        final float drdz = (rdz2 - rdz1) / (x2 - x1);
        final float dgdz = (gdz2 - gdz1) / (x2 - x1);
        final float dbdz = (bdz2 - bdz1) / (x2 - x1);
        final float dz_inv = (z2_inv - z1_inv) / (x2 - x1);
        for (int x = x1;;) {
            if (z_buff[i] < z_inv) {
                z = 1f / z_inv;
                g.plotToHash(i, tex.getRGB(roundPositive(z * udz), roundPositive(z * vdz)));
                g.modulateInHash(i, rgb(roundPositive(rdz * z), roundPositive(gdz * z), roundPositive(bdz * z)));
                z_buff[i] = z_inv;
            } 
            if (++x > x2)
                break;
            z_inv += dz_inv;
            udz += dudz;
            vdz += dvdz;
            rdz += drdz;
            gdz += dgdz;
            bdz += dbdz;
            i++;
        }
    }
    
    private void line_45(int x1, int y1, float z1_inv, int x2, float z2_inv, final int dy) {
        int i = hash(x1, y1, w);
        float z_inv = z1_inv;
        final float dz_inv = (z2_inv - z1_inv) / (x2 - x1);
        for (int x = x1;;) {
            if (z_buff[i] < z_inv) {
                g.plotToHash(i);
                z_buff[i] = z_inv;
            }
            if (++x > x2)
                break;
            z_inv += dz_inv;
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
    
    private static class TriPoint {
        
        int x, y;
        float z, z_inv;
        
        float u, v;
        
        float r, g, b;
        float nx, ny, nz;
        
        double angle;
        boolean angle_calc;
        
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