package render;

import static render.Graphics.*;
import static render.MathUtils.*;

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
    
    private int code_1, code_2;
    private int line_x1, line_y1, line_x2, line_y2;
    private float line_z1, line_z2;
    private float line_r1, line_g1, line_b1, line_r2, line_g2, line_b2;
    
    private float[] z_buff;
    private float[] sqrt_table;
    
    public Rasterizer3D(Graphics g) {
        this.g = g;
        updateBounds();
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
    
    public void drawTriangleWireframe(int x1, int y1, float z1, int x2, int y2, float z2, int x3, int y3, float z3, int rgb) {
        g.setColor(rgb);
        line(x1, y1, z1, x2, y2, z2);
        line(x2, y2, z2, x3, y3, z3);
        line(x3, y3, z3, x1, y1, z1);
    }
    
    // most of logic stolen from: http://grafika.me/node/67
    public void drawTriangleFlatShading(int x1, int y1, float z1, int x2, int y2, float z2, int x3, int y3, float z3, int rgb) {
        g.setColor(rgb);
        if (y3 < y2) {
            temp_x = x3;
            temp_y = y3;
            temp_float = z3;
            x3 = x2;
            y3 = y2;
            z3 = z2;
            x2 = temp_x;
            y2 = temp_y;
            z2 = temp_float;
        }
        if (y3 < y1) {
            temp_x = x3;
            temp_y = y3;
            temp_float = z3;
            x3 = x1;
            y3 = y1;
            z3 = z1;
            x1 = temp_x;
            y1 = temp_y;
            z1 = temp_float;
        }
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
        float dx13 = 0f, dx12 = 0f, dx23 = 0f;
        float dz13 = 0f, dz12 = 0f, dz23 = 0f;
        if (y3 != y1) {
            dx13 = (float) (x3 - x1) / (y3 - y1);
            dz13 = (z3 - z1) / (y3 - y1);
        }
        if (y2 != y1) {
            dx12 = (float) (x2 - x1) / (y2 - y1);
            dz12 = (z2 - z1) / (y2 - y1);
        }
        if (y3 != y2) {
            dx23 = (float) (x3 - x2) / (y3 - y2);
            dz23 = (z3 - z2) / (y3 - y2);
        }
        float _dx13 = dx13;
        float _dz13 = dz13;
        if (dx13 > dx12) {
            temp_float = dx13;
            dx13 = dx12;
            dx12 = temp_float;
            temp_float = dz13;
            dz13 = dz12;
            dz12 = temp_float;
        }
        float lz = z1, rz = z1;
        float wx1 = x1, wx2 = x1;
        for (int y = y1; y < y2; y++) {
            int lx = round(wx1);
            int rx = round(wx2);
            clip(lx, y, rx, y);
            if (line_x1 != -1) { // scanline is outside the screen
                line_z1 = lz;
                line_z2 = rz;
                if (line_x1 != lx) 
                    line_z1 = new_val(lz, rz, line_x1 - lx, rx - lx);
                if (line_x2 != rx) 
                    line_z2 = new_val(rz, lz, rx - line_x2, rx - lx);
                hor_line(line_x1, line_z1, line_x2, line_z2, y);
            }
            wx1 += dx13;
            wx2 += dx12;
            lz += dz13;
            rz += dz12;
        }
        if (y1 == y2) {
            wx1 = x1;
            wx2 = x2;
            lz = z1;
            rz = z2;
        }
        if (_dx13 < dx23) {
            temp_float = _dx13;
            _dx13 = dx23;
            dx23 = temp_float;
            temp_float = _dz13;
            _dz13 = dz23;
            dz23 = temp_float;
        }
        for (int y = y2; y <= y3; y++) {
            int lx = round(wx1);
            int rx = round(wx2);
            clip(lx, y, rx, y);
            if (line_x1 != -1) {
                line_z1 = lz;
                line_z2 = rz;
                if (line_x1 != lx)
                    line_z1 = new_val(lz, rz, line_x1 - lx, rx - lx);
                if (line_x2 != rx)
                    line_z2 = new_val(rz, lz, rx - line_x2, rx - lx);
                hor_line(line_x1, line_z1, line_x2, line_z2, y);
            }
            wx1 += _dx13;
            wx2 += dx23;
            lz += _dz13;
            rz += dz23;
        }
    }
    
    
    public void drawTriangleGouraudShading(int x1, int y1, float z1, int rgb1, int x2, int y2, float z2, int rgb2, int x3, int y3, float z3, int rgb3) {
        if (y3 < y2) {
            temp_x = x3;
            temp_y = y3;
            temp_float = z3;
            temp_int = rgb3;
            x3 = x2;
            y3 = y2;
            z3 = z2;
            rgb3 = rgb2;
            x2 = temp_x;
            y2 = temp_y;
            z2 = temp_float;
            rgb2 = temp_int;
        }
        if (y3 < y1) {
            temp_x = x3;
            temp_y = y3;
            temp_float = z3;
            temp_int = rgb3;
            x3 = x1;
            y3 = y1;
            z3 = z1;
            rgb3 = rgb1;
            x1 = temp_x;
            y1 = temp_y;
            z1 = temp_float;
            rgb1 = temp_int;
        }
        if (y2 < y1) {
            temp_x = x2;
            temp_y = y2;
            temp_float = z2;
            temp_int = rgb2;
            x2 = x1;
            y2 = y1;
            z2 = z1;
            rgb2 = rgb1;
            x1 = temp_x;
            y1 = temp_y;
            z1 = temp_float;
            rgb1 = temp_int;
        }
        float dx13 = 0f, dx12 = 0f, dx23 = 0f;
        float dz13 = 0f, dz12 = 0f, dz23 = 0f;
        float dr13 = 0f, dr12 = 0f, dr23 = 0f;
        float dg13 = 0f, dg12 = 0f, dg23 = 0f;
        float db13 = 0f, db12 = 0f, db23 = 0f;
        if (y3 != y1) {
            float dy = y3 - y1;
            dx13 = (float) (x3 - x1) / dy;
            dz13 = (z3 - z1) / dy;
            dr13 = ((float) red(rgb3) - red(rgb1)) / dy;
            dg13 = ((float) green(rgb3) - green(rgb1)) / dy;
            db13 = ((float) blue(rgb3) - blue(rgb1)) / dy;
        }
        if (y2 != y1) {
            float dy = y2 - y1;
            dx12 = (float) (x2 - x1) / dy;
            dz12 = (z2 - z1) / dy;
            dr12 = ((float) red(rgb2) - red(rgb1)) / dy;
            dg12 = ((float) green(rgb2) - green(rgb1)) / dy;
            db12 = ((float) blue(rgb2) - blue(rgb1)) / dy;
        }
        if (y3 != y2) {
            float dy = y3 - y2;
            dx23 = (float) (x3 - x2) / dy;
            dz23 = (z3 - z2) / dy;
            dr23 = ((float) red(rgb3) - red(rgb2)) / dy;
            dg23 = ((float) green(rgb3) - green(rgb2)) / dy;
            db23 = ((float) blue(rgb3) - blue(rgb2)) / dy;
        }
        float _dx13 = dx13;
        float _dz13 = dz13;
        float _dr13 = dr13;
        float _dg13 = dg13;
        float _db13 = db13;
        if (dx13 > dx12) {
            temp_float = dx13;
            dx13 = dx12;
            dx12 = temp_float;
            temp_float = dz13;
            dz13 = dz12;
            dz12 = temp_float;
            temp_float = dr13;
            dr13 = dr12;
            dr12 = temp_float;
            temp_float = dg13;
            dg13 = dg12;
            dg12 = temp_float;
            temp_float = db13;
            db13 = db12;
            db12 = temp_float;
        }
        float lz = z1, rz = z1;
        float wx1 = x1, wx2 = x1;
        float lr = red(rgb1), lg = green(rgb1), lb = blue(rgb1);
        float rr = lr, rg = lg, rb = lb;
        for (int y = y1; y < y2; y++) {
            int lx = round(wx1);
            int rx = round(wx2);
            clip(lx, y, rx, y);
            if (line_x1 != -1) {
                line_z1 = lz;
                line_z2 = rz;
                line_r1 = lr;
                line_g1 = lg;
                line_b1 = lb;
                line_r2 = rr;
                line_g2 = rg;
                line_b2 = rb;
                if (line_x1 != lx) {
                    line_z1 = new_val(lz, rz, line_x1 - lx, rx - lx);
                    line_r1 = new_val(lr, rr, line_x1 - lx, rx - lx);
                    line_g1 = new_val(lg, rg, line_x1 - lx, rx - lx);
                    line_b1 = new_val(lb, rb, line_x1 - lx, rx - lx);
                }
                if (line_x2 != rx) {
                    line_z2 = new_val(rz, lz, rx - line_x2, rx - lx);
                    line_r2 = new_val(rr, lr, rx - line_x2, rx - lx);
                    line_g2 = new_val(rg, lg, rx - line_x2, rx - lx);
                    line_b2 = new_val(rb, lb, rx - line_x2, rx - lx);
                }
                hor_line_interpolate_color(y, line_x1, line_z1, line_r1, 
                                           line_g1, line_b1, line_x2, 
                                           line_z2, line_r2, line_g2, line_b2);
            }
            wx1 += dx13;
            wx2 += dx12;
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
            wx2 = x2;
            rz = z2;
            rr = red(rgb2);
            rg = green(rgb2);
            rb = blue(rgb2);
        }
        if (_dx13 < dx23) {
            temp_float = _dx13;
            _dx13 = dx23;
            dx23 = temp_float;
            temp_float = _dz13;
            _dz13 = dz23;
            dz23 = temp_float;
            temp_float = _dr13;
            _dr13 = dr23;
            dr23 = temp_float;
            temp_float = _dg13;
            _dg13 = dg23;
            dg23 = temp_float;
            temp_float = _db13;
            _db13 = db23;
            db23 = temp_float;
        }
        for (int y = y2; y <= y3; y++) {
            int lx = round(wx1);
            int rx = round(wx2);
            clip(lx, y, rx, y);
            if (line_x1 != -1) {
                line_z1 = lz;
                line_z2 = rz;
                line_r1 = lr;
                line_g1 = lg;
                line_b1 = lb;
                line_r2 = rr;
                line_g2 = rg;
                line_b2 = rb;
                if (line_x1 != lx) {
                    line_z1 = new_val(lz, rz, line_x1 - lx, rx - lx);
                    line_r1 = new_val(lr, rr, line_x1 - lx, rx - lx);
                    line_g1 = new_val(lg, rg, line_x1 - lx, rx - lx);
                    line_b1 = new_val(lb, rb, line_x1 - lx, rx - lx);
                }
                if (line_x2 != rx) {
                    line_z2 = new_val(rz, lz, rx - line_x2, rx - lx);
                    line_r2 = new_val(rr, lr, rx - line_x2, rx - lx);
                    line_g2 = new_val(rg, lg, rx - line_x2, rx - lx);
                    line_b2 = new_val(rb, lb, rx - line_x2, rx - lx);
                }
                hor_line_interpolate_color(y, line_x1, line_z1, line_r1, 
                                           line_g1, line_b1, line_x2, 
                                           line_z2, line_r2, line_g2, line_b2);
            }
            wx1 += _dx13;
            wx2 += dx23;
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
    
    private void hor_line_interpolate_color(int y, int x1, float z1, float r1, float g1, float b1, int x2, float z2, float r2, float g2, float b2) {
        int i = index(x1, y);
        float z = z1;
        float dx = max(1, x2 - x1);
        final float dz = (z2 - z1) / dx;
        final float dr = (r2 - r1) / dx;
        final float dg = (g2 - g1) / dx;
        final float db = (b2 - b1) / dx;
        float r = r1, g = g1, b = b1;
        for (int x = x1;;) {
            if (z_buff[i] > z) {
                this.g.plot(x, y, rgb(roundPositive(r), roundPositive(g), roundPositive(b)));
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
    
    private float new_val(float v1, float v2, float new_len, float old_len) {
        return v1 + (v2 - v1) * new_len / old_len;
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
        code_1 = code(x1, y1);
        code_2 = code(x2, y2);
        for (;;) {
            if ((code_1 | code_2) == 0) {
                line_x1 = x1;
                line_x2 = x2;
                line_y1 = y1;
                line_y2 = y2;
                break;
            } else {
                if ((code_1 & code_2) == 0) {
                    if (code_1 != 0) {
                        transfer(x1, y1, x2, y2, code_1);
                        x1 = temp_x;
                        y1 = temp_y;
                        code_1 = code(x1, y1);
                    } else { // code_2 != 0
                        transfer(x2, y2, x1, y1, code_2);
                        x2 = temp_x;
                        y2 = temp_y;
                        code_2 = code(x2, y2);
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
    
}
