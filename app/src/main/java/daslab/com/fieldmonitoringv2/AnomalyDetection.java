package daslab.com.fieldmonitoringv2;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

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

    HashMap<Pixel,PixelCoordinate> pixelHashMap = new HashMap<>();


    public AnomalyDetection(String imagePath){
        this.imagePath = imagePath;
        this.image = BitmapFactory.decodeFile(imagePath);
        this.imageHeight = this.image.getHeight();
        Log.d("imageHeight", String.valueOf(imageHeight));
        this.imageWidth = this.image.getWidth();
        Log.d("imageWidth", String.valueOf(imageWidth));
        getPixels();
        if (pixelHashMap.containsValue(new PixelCoordinate(5,5))){
            Log.d("true", "true");
        }
    }

    private void getPixels() {
        for (int i = 0; i < this.imageWidth-1; i++) {
            for (int j = 0; j < this.imageHeight-1; j++) {
                Pixel pixel = new Pixel(this.image.getPixel(i,j),i,j);
                PixelCoordinate pixelCoordinate = new PixelCoordinate(i,j);
                pixelHashMap.put(pixel,pixelCoordinate);
            }
        }
    }

}
