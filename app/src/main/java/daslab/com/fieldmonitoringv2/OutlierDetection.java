package daslab.com.fieldmonitoringv2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;


/**
* Represents an instance of the outlier detection software
 * @author seanbarber
 */
public class OutlierDetection {

    String imagePath;
    Bitmap image;
    Bitmap bitmap;

    /**
     * Sets up and runs the outlier detection, requiring the imagepath to where the photo was taken, and the application context
     * @param imagePath
     * @param context
     */
    public OutlierDetection( String imagePath, Context context) {


        // Sets the imagepath
        this.imagePath = imagePath;

        // Starts a timer to test the function speed
        long startTime = System.nanoTime();

        // Decodes the image
        Bitmap img = BitmapFactory.decodeFile(imagePath);
        if (img != null){

            // Sets up the run call with the application context and image
            ContextAndBitmap contextAndString = new ContextAndBitmap(img,context);
            // Does the run call to process the bitmap in the background
            bitmap = new ProcessBitmap().doInBackground(contextAndString);
        }

        // Ends the timer
        long endTime = System.nanoTime();

        // Logs the length of processing an image
        Log.d("timer", String.valueOf((endTime - startTime)/1000000));

    }

    /**
     * Gets the bitmap to be displayed.
     * @return edited bitmap that displays which grid cells are marked as regions of interest
     */
    public Bitmap showBitmap(){
        return bitmap;
    }
}