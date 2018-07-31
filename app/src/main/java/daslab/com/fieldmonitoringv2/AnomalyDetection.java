package daslab.com.fieldmonitoringv2;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

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

    void anomalyDetection(String imagePath){
        this.imagePath = imagePath;
        this.image = BitmapFactory.decodeFile(imagePath);
        this.imageHeight = this.image.getHeight();
        Log.d("imageHeight", String.valueOf(imageHeight));
        this.imageWidth = this.image.getWidth();
        Log.d("imageWidth", String.valueOf(imageWidth));
    }

}
