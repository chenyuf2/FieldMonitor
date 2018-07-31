package daslab.com.fieldmonitoringv2;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Log;

import java.util.ArrayList;

public class AnomalyDetection {
    String imagePath;
    Bitmap image;
    int numberOfGrids;

    int imageHeight;
    int imageWidth;

    double averageHue;
    double averageSaturation;
    double averageValue;

    double varianceHue;
    double varianceSaturation;
    double varianceValue;

    public AnomalyDetection(String imagePath){
        this.imagePath = imagePath;
        this.image = BitmapFactory.decodeFile(imagePath);
        this.imageHeight = this.image.getHeight();
        Log.d("imageHeight", String.valueOf(imageHeight));
        this.imageWidth = this.image.getWidth();
        Log.d("imageWidth", String.valueOf(imageWidth));
        int color = this.image.getPixel(0,0);
        int red = Color.red(color);
        int blue = Color.blue(color);
        int green = Color.green(color);
        int alpha = Color.alpha(color);
        float[] hsv = new float[3];
        Color.colorToHSV(color,hsv);
        Log.d("hsv", String.valueOf(hsv[0]) + "," + String.valueOf(hsv[1]) + "," + String.valueOf(hsv[2]));
        Log.d("color", red + ", " + green + ", " + blue + ", " + alpha);
    }

}
