package render;

/**
 *
 * @author Texhnolyze
 */
public class Rasterizer3D {
    
    private static final int X_LESS_0 = 8,
                             Y_LESS_0 = 2,
                             X_GREATER_MAX_X = 4,
                             Y_GREATER_MAX_Y = 1;
    
    private static final float SQRT_2 = (float) Math.sqrt(2.0);
    
    private final Graphics g;
    
    private int w, h, w_plus_one;
    private int x_max, y_max;
    private int temp_x, temp_y;
    private int code_1, code_2;
    private int line_x1, line_y1, line_x2, line_y2;
    
    private float[] z_buff;
    
    public Rasterizer3D(Graphics g) {
        this.g = g;
        w = g.getWidth();
        h = g.getHeight();
        w_plus_one = w + 1;
        x_max = w - 1;
        y_max = h - 1;
        z_buff = new float[w * h];
        clear_z_buffer();
    }
    
    void drawTriangleWithoutShading(int x1, int y1, int z1, int x2, int y2, int z2, int x3, int y3, int z3, int rgb) {
        g.setColor(rgb);
        line(x1, y1, z1, x2, y2, z2);
        line(x2, y2, z2, x3, y3, z3);
        line(x3, y3, z3, x1, y1, z1);
    }
    
    void drawTriangleFlatShading(int x1, int y1, int x2, int y2, int x3, int y3, int rgb) {
        g.setColor(rgb);
        
    }
    
    private void line(int x1, int y1, int z1, int x2, int y2, int z2) {
        code_1 = code(x1, y1);
        code_2 = code(x2, y2);
        clip(x1, y1, x2, y2);
        if (line_x1 != -1) 
            bresenham(line_x1, line_y1, z1, line_x2, line_y2, z2);
    }
    
    private void bresenham(int x1, int y1, int z1, int x2, int y2, int z2) {
        if (x1 == x2) { // vertical line
            if (y1 == y2) {
                int i = index(x1, y1);
                if (z_buff[i] > z1) {
                    g.plot(x1, y1);
                    z_buff[i] = z1;
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
                x1 = x2;
                y1 = y2;
                x2 = temp_x;
                y2 = temp_y;
            }
            int dx = x2 - x1, dy;
            if (y2 > y1) {
                dy = y2 - y1;    
                if (dx > dy)      x_line(x1, y1, z1, x2, z2, dx, dy, 1);
                else if (dx < dy) y_line(x1, y1, z1, y2, z2, dx, dy, 1);
                else              line_45(x1, y1, z1, x2, z2, 1);
                
            } else {
                dy = y1 - y2;
                if (dx > dy)      x_line(x1, y1, z1, x2, z2, dx, dy, -1);
                else if (dx < dy) y_line(x1, y1, z1, y2, z2, dx, dy, -1);
                else              line_45(x1, y1, z1, x2, z2, -1);
            }
        }
    }
    
    private void ver_line(int x, int y1, float z1, int y2, float z2) {
        int i = index(x, y1);
        float z = z1;
        final float dz = (z2 - z1) / (y2 - y1);
        for (int y = y1;;) {
            if (z_buff[i] > z) {
                g.plot(x, y++);
                z_buff[i] = z;
            }
            if (y > y2)
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
                g.plot(x++, y);
                z_buff[i] = z;
            }
            if (x > x2)
                break;
            z += dz;
            i++;
        }
    }
    
    private void line_45(int x1, int y1, float z1, int x2, float z2, final int y_inc) {
        int i = index(x1, y1);
        float z = z1;
        int dx = x2 - x1;
        final float dz = (float) (SQRT_2 * (z2 - z1) / (Math.sqrt(2 * dx * dx)));
        for (int x = x1, y = y1;;) {
            if (z_buff[i] > z) {
                g.plot(x++, y);
                z_buff[i] = z;
            }
            if (x > x2)
                break;
            y += y_inc;
            z += dz;
            i += w_plus_one;
        }
    }
    
    private void x_line(int x1, int y1, float z1, int x2, float z2, int dx, int dy, final int y_inc) {
        int n = w * y_inc;
        int i = index(x1, y1);
        float z = z1;
        final float dz = (float) ((z2 - z1) / (Math.sqrt(dx * dx + dy * dy)));
        for (int x = x1, y = y1, err = 0;;) {
            if (z_buff[i] > z) {
                g.plot(x++, y);
                z_buff[i] = z;
            }
            if (x > x2)
                break;
            i++;
            err += dy;
            if (err << 1 >= dx) {
                y += y_inc;
                i += n;
                err -= dx;
                z += (SQRT_2 * dz);
            } else 
                z += dz;
        }
    }
    
    private void y_line(int x1, int y1, float z1, int y2, float z2, int dx, int dy, final int x_inc) {
        int i = index(x1, y1);
        float z = z1;
        final float dz = (float) ((z2 - z1) / (Math.sqrt(dx * dx + dy * dy)));
        for (int y = y1, x = x1, err = 0;;) {
            if (z_buff[i] > z) {
                g.plot(x, y++);
                z_buff[i] = z;
            }
            if (y > y2)
                break;
            i += w;
            err += dx;
            if (err << 1 >= dy) {
                x += x_inc;
                i += x_inc;
                err -= dy;
                z += (SQRT_2 * dz);
            } else 
                z += dz;
        }
    }
    
    private void clip(int x1, int y1, int x2, int y2) {
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
        temp_y = (int) Math.round(ry + (y - ry) * -(rx / (x - rx)));
    }

    private void y_less_0(float x, float y, float rx, float ry) {
        temp_x = (int) Math.round(rx + (x - rx) * -(ry / (y - ry)));
        temp_y = 0;
    }

    private void x_greater_max_x(float x, float y, float rx, float ry) {
        temp_x = x_max;
        temp_y = (int) Math.round(ry + (y - ry) * ((x_max - rx) / (x - rx)));
    }

    private void y_greater_max_y(float x, float y, float rx, float ry) {
        temp_x = (int) Math.round(rx + (x - rx) * ((y_max - ry) / (y - ry)));
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
    
    private void clear_z_buffer() {
        for (int i = 0; i < z_buff.length; i++)
            z_buff[i] = Float.POSITIVE_INFINITY;
    }
    
    private int index(int x, int y) {
        return x + y * w;
    }
    
}
