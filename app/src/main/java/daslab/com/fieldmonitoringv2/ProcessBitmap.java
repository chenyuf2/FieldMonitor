package daslab.com.fieldmonitoringv2;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.support.v8.renderscript.Script;
import android.support.v8.renderscript.Type;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

public class ProcessBitmap extends AsyncTask<ContextAndString, Void, Bitmap>{

    @Override
    protected Bitmap doInBackground( ContextAndString... contextAndStrings ) {

        ContextAndString contextAndString = contextAndStrings[0];

        Bitmap bitmap = contextAndString.bitmap;

        android.support.v8.renderscript.RenderScript rs = android.support.v8.renderscript.RenderScript.create(contextAndString.context);

        android.support.v8.renderscript.Allocation inputAllocation = android.support.v8.renderscript.Allocation.createFromBitmap(rs, bitmap);
        ScriptC_outlierDetection outlierDetection = new ScriptC_outlierDetection(rs);

        android.support.v8.renderscript.Allocation hueAverage = android.support.v8.renderscript.Allocation.createSized(rs, android.support.v8.renderscript.Element.F32(rs),1);
        outlierDetection.forEach_addHueChannel(inputAllocation);
        //float hueAverageValue[] = new float[1];

        outlierDetection.forEach_getAverageImageHue(hueAverage);
        //hueAverage.copyTo(hueAverageValue);
        //Log.d("hue", String.valueOf(hueAverageValue[0]/360));
        outlierDetection.invoke_resetHueSum();
        outlierDetection.forEach_calculateVariance(inputAllocation);

        android.support.v8.renderscript.Allocation hueVariance = android.support.v8.renderscript.Allocation.createSized(rs, android.support.v8.renderscript.Element.F32(rs),1);
        //float hueVarianceValue[] = new float[1];
        outlierDetection.forEach_getVarianceImageHue(hueVariance);
        //hueVariance.copyTo(hueVarianceValue);
        //Log.d("hue1", String.valueOf(hueVarianceValue[0]/129600));
        return bitmap;
    }

    //    @TargetApi(Build.VERSION_CODES.O)
//    @Override
//    protected PixelsAndHSV doInBackground( String... strings ) {
//
//        BitmapFactory.Options options = new BitmapFactory.Options();
//
//        options.inJustDecodeBounds = true;
//        // Downscales by a factor of 6, this needs to be a multiple of 2
//        //options.inSampleSize = 5;
//
//        // Decodes the image from the image path with the options
//        Bitmap bitmap = BitmapFactory.decodeFile(strings[0],options);
//
//        // Gets the height and width of the decoded image
//        int width = options.outWidth;
//        int height = options.outHeight;
//
//        Log.d("width", String.valueOf(width));
//        Log.d("height", String.valueOf(height));
//
//        float hueValues = 0;
//        int saturationValues = 0;
//        int valueValues = 0;
//        int pixelCount = 0;
//        Pixel[][] pixels = new Pixel[width][height];
//
//        BitmapRegionDecoder bitmapRegionDecoder = null;
//        try {
//            bitmapRegionDecoder = BitmapRegionDecoder.newInstance(strings[0],false);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        int cellSize = 10;
//
//        ConcurrentHashMap<PixelCoordinate, Pixel> coordinatePixelConcurrentHashMap = new ConcurrentHashMap<>();
//
//        int numberOfCellsX = (width)/cellSize;
//        int numberOfCellsY = (height)/cellSize;
//
//        int xStart;
//        int xEnd;
//        int yStart;
//        int yEnd;
//
//        //Go from pixel 0 - 99 x 0 - 99 in the subimage
//        // Go through all the x rows across
//        // Move down one y
//
////        for (int x = 0; x < (width/cellSize); x++){
////            for (int y = 0; y < (height/cellSize); y++) {
////                Rect rect = new Rect(0,0,9,9);
////                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
////                options.inJustDecodeBounds = false;
////                Log.d("rect", rect.toShortString());
////                bitmap = bitmapRegionDecoder.decodeRegion(rect,options);
////                for(int i = 0; i < bitmap.getWidth(); i++){
////                    for (int j = 0; j < bitmap.getHeight(); j++){
////                        int c = bitmap.getPixel(i, j);
////                        pixelCount++;
////                        float[] hsvValues = new float[3];
////                        Color.colorToHSV(c,hsvValues);
////                        hueValues += (hsvValues[0]/360);
////                        saturationValues += (hsvValues[1]*100);
////                        valueValues += (hsvValues[2]*100);
////                        Pixel pixel = new Pixel(c,(x*cellSize) + i,(y*cellSize) + j,(hsvValues[0]/360),hsvValues[1],hsvValues[2]);
////                        PixelCoordinate pixelCoordinate = new PixelCoordinate((x*cellSize) + i,(y*cellSize) + j);
////                        coordinatePixelConcurrentHashMap.put(pixelCoordinate,pixel);
////                    }
////                }
////                bitmap.recycle();
////            }
////        }
//
////        // Gets the hsv values for each pixel and puts them in the pixel array
////        for(int x = 0; x < width; x++){
////            for (int y = 0; y < height; y++){
////                int c = bitmap.getPixel(x, y);
////                pixelCount++;
////                float[] hsvValues = new float[3];
////                Color.colorToHSV(c,hsvValues);
////                hueValues += (hsvValues[0]/360);
////                if (x % width == 0){
////                    Log.d("hsvVal", String.valueOf(hsvValues[0]/360));
////                }
////                saturationValues += (hsvValues[1]*100);
////                valueValues += (hsvValues[2]*100);
////                pixels[x][y] = new Pixel(c,x,y,(hsvValues[0]/360),hsvValues[1],hsvValues[2]);
////            }
////        }
//
//        // calculate average of bitmap hsv values
////        float hueOverall = (hueValues/pixelCount);
////        float saturationOverall = (saturationValues/pixelCount);
////        float valueOverall = (valueValues/pixelCount);
//
//        float[] hsvOverall = {0,0,0};//{hueOverall,saturationOverall,valueOverall};
//
//        float hueVariance = 0;
//        float hueVariancePixel;
//        float saturationVariance = 0;
//        float saturationVariancePixel;
//        float valueVariance = 0;
//        float valueVariancePixel;
//
////        for(int x = 0; x < width - 1; x++){
////            for (int y = 0; y < height - 1; y++){
////                float[] hsvValues = pixels[x][y].hsv;
////                hueVariancePixel = (hsvValues[0] - hueOverall) * (hsvValues[0] - hueOverall);
////                if (x % width == 0){
////                    Log.d("hsvVal1", String.valueOf(hsvValues[0]));
////                }
////                hueVariance += hueVariancePixel;
////                saturationVariancePixel = (hsvValues[1] - saturationOverall) * (hsvValues[1] - saturationOverall);
////                saturationVariance += saturationVariancePixel;
////                valueVariancePixel = (hsvValues[2] - valueOverall) * (hsvValues[2] - valueOverall);
////                valueVariance += valueVariancePixel;
////                float[] varianceValues = {hueVariancePixel,saturationVariancePixel,valueVariancePixel};
////                pixels[x][y].setHsvVariance(varianceValues,pixelCount);
////            }
////        }
//
////        hueVariance /= pixelCount;
////        saturationVariance /= pixelCount;
////        valueVariance /= pixelCount;
//
//        float[] hsvVarianceOverall = {hueVariance,saturationVariance,valueVariance};
//        int[] heightWidth = {height,width};
//
//        return new PixelsAndHSV(pixels,hsvOverall, hsvVarianceOverall, pixelCount, heightWidth);
//    }

}
