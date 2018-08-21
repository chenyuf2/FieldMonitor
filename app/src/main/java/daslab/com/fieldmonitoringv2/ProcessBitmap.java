package daslab.com.fieldmonitoringv2;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.support.v8.renderscript.Type;

public class ProcessBitmap extends AsyncTask<ContextAndBitmap, Void, Bitmap>{

    /**
     *
     * @param ContextAndBitmaps
     * @return edited bitmap with all the cells that are marked as interesting.
     */
    @Override
    protected Bitmap doInBackground( ContextAndBitmap... ContextAndBitmaps ) {

        // Gets the application context and bitmap to process
        ContextAndBitmap contextAndBitmap = ContextAndBitmaps[0];

        // Extracts the bitmap from the resource
        Bitmap bitmap = contextAndBitmap.bitmap;

        // Starts up the renderscript using the context
        android.support.v8.renderscript.RenderScript rs = android.support.v8.renderscript.RenderScript.create(contextAndBitmap.context);

        // Creates a Renderscript Allocation from the bitmap image
        android.support.v8.renderscript.Allocation bitmapAllocation = android.support.v8.renderscript.Allocation.createFromBitmap(rs, bitmap);

        // Inititlize the outlier detection script
        ScriptC_outlierDetection outlierDetection = new ScriptC_outlierDetection(rs);

        // Create the type of region of interest that is 40 x 30
        Type regionsOfInterest = new Type.Builder(rs, android.support.v8.renderscript.Element.I32(rs)).setX(40).setY(30).create();

        // Creates the allocation for the region of interest
        android.support.v8.renderscript.Allocation roi = android.support.v8.renderscript.Allocation.createTyped(rs,regionsOfInterest);

        // Sets the region of interest within the script
        outlierDetection.set_regionsOfInterest(roi);

        // Initilizes the region of interests to 0, to default to not interesting
        outlierDetection.invoke_initROI();

        // Sets the input to bitmap allocation, to access pixels outside of the actual current pixel
        outlierDetection.set_bitmapImage(bitmapAllocation);

        // Adds up the overall hues for the entire image
        outlierDetection.forEach_addHueChannel(bitmapAllocation);

        // Calculates the variance for the entire image
        outlierDetection.forEach_calculateVariance(bitmapAllocation);

        // Finds the areas of interests for a 4000x3000 image
        outlierDetection.forEach_findAreasOfInterest(bitmapAllocation);

        // The region of interests can only be returned to a 1D array
        int[] regionsOfInterest1D = new int[1200];

        // This copies the array into the 1d region of interest
        roi.copy2DRangeTo(0,0,40,30,regionsOfInterest1D);

        // Changes the 1D array to a 2D array for easy access to create rectangles for the image
        int regionsOfInterest2D[][] = oneDToTwoD(30,40,regionsOfInterest1D);

        // Creates a mutable bitmap to add the rectangles to the image
        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        // Creates a canvas for the bitmap to be drawn on
        Canvas canvas = new Canvas(mutableBitmap);

        // Creates the paint and style type
        Paint paint = new Paint();
        paint.setStrokeWidth(7.0f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.RED);

        // Goes through and colors the cells that are marked as interesting.
        for (int i = 0; i < 30; i++) {
            for (int j = 0; j < 40; j++) {
                if (regionsOfInterest2D[i][j] == 1){
                    Rect rect = new Rect(i*100,j*100,(i*100)+99,(j*100)+99);
                    canvas.drawRect(rect,paint);
                }
            }
        }

        // Destroys the memory that was used for the image, to free up space
        bitmapAllocation.destroy();
        roi.destroy();
        outlierDetection.destroy();
        rs.destroy();

        // Returns the image to be displayed
        return mutableBitmap;
    }

    /**
     * Takes in the height and width of a 1D array and converts it to a 2D array
     * @param height Height of the array
     * @param width Width of the array
     * @param array 1D array of ints to be converted
     * @return 2D Array with specified height and width
     */
    private int[][] oneDToTwoD(int height, int width, int[] array){
        if(array.length != height * width){
            throw new IllegalArgumentException("Invalid array length");
        }
        int returnArray[][] = new int[height][width];
        for(int i = 0; i < height; i++){
            System.arraycopy(array, (i * width), returnArray[i], 0, width);
        }
        return returnArray;
    }
}
