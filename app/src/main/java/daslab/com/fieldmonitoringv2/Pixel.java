package daslab.com.fieldmonitoringv2;

import android.graphics.Color;

public class Pixel {
    int color;
    int red;
    int green;
    int blue;

    PixelCoordinate pixelCoord;
    float[] hsv = new float[3];
    float[] hsvVariance = new float[3];

    public Pixel(int color, int x, int y, float hue, float variance, float value){
        this.red = Color.red(color);
        this.blue = Color.blue(color);
        this.green = Color.green(color);
        hsv[0] = hue;
        hsv[1] = variance*100;
        hsv[2] = value*100;
        pixelCoord = new PixelCoordinate(x,y);
    }

    public void setHsvVariance(float[] variance, int pixelCount){
        this.hsvVariance[0] = (hsv[0] - variance[0]) * (hsv[0] - variance[0])/pixelCount;
        this.hsvVariance[1] = (hsv[1] - variance[1]) * (hsv[1] - variance[1])/pixelCount;
        this.hsvVariance[2] = (hsv[2] - variance[2]) * (hsv[2] - variance[2])/pixelCount;
    }

    @Override
    public String toString() {
        String finalString = "X: " + this.pixelCoord.x + " Y: " + this.pixelCoord.y + " Red: " + this.red + " " + "Blue: " + this.blue + " " + "Green: " + this.green;
        return finalString;
    }
}
