package daslab.com.fieldmonitoringv2;

import android.graphics.Color;

public class Pixel {
    int color;
    int red;
    int green;
    int blue;
    PixelCoordinate pixelCoord;
    float[] hsv = new float[3];

    public Pixel(int color, int x, int y){
        this.red = Color.red(color);
        this.blue = Color.blue(color);
        this.green = Color.green(color);
        Color.colorToHSV(color,hsv);
        hsv[1] = hsv[1]*100;
        hsv[2] = hsv[2]*100;
        pixelCoord = new PixelCoordinate(x,y);
    }
}
