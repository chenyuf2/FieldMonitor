package daslab.com.fieldmonitoringv2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.util.ArrayList;

public class OutlierDetection {
    String imagePath;
    Bitmap image;
    int numberOfCells;

    int cellSize = 100;

    int imageHeight;
    int imageWidth;

    Pixel[][] pixels;

    int pixelCount;

    float[] hsvVarianceOverall;

    float[] hsvAverageOverall;

    Grid grid;

    Bitmap bitmap1;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public OutlierDetection( String imagePath, Context context) {
        this.imagePath = imagePath;


        long startTime = System.nanoTime();

        Bitmap img = BitmapFactory.decodeFile(imagePath);
        if (img != null){
            ContextAndString contextAndString = new ContextAndString(img,context);
            Bitmap bitmap = new ProcessBitmap().doInBackground(contextAndString);
        }
        long endTime = System.nanoTime();

        long duration = (endTime - startTime);


        Log.d("timer", String.valueOf(duration/1000000));
        //PixelsAndHSV pixelsAndHSV = new ProcessBitmap().doInBackground(imagePath);
//        this.imageHeight = pixelsAndHSV.heightWidth[0];
//        this.imageWidth = pixelsAndHSV.heightWidth[1];
//        pixels = pixelsAndHSV.pixels;
//        hsvAverageOverall = pixelsAndHSV.meanHsv;
//        pixelCount = pixelsAndHSV.pixelCount;
//        hsvVarianceOverall = pixelsAndHSV.varianceOverall;
//        this.pixelCount = this.imageHeight * this.imageWidth;
//        this.numberOfCells = this.pixelCount/(this.cellSize*this.cellSize);
        //grid = new Grid(this.numberOfCells, this.cellSize, this.imageHeight, this.imageWidth);
        //grid.goThrough(pixels);

    }

    public Bitmap showBitmap(){
//        BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inSampleSize = 5;
//        Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);
//        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
//        Canvas canvas = new Canvas(mutableBitmap);
//        Paint paint = new Paint();
//        paint.setStrokeWidth(2.0f);
//        paint.setStyle(Paint.Style.STROKE);
//        paint.setColor(Color.RED);
//        ArrayList<Cell> cellArrayList = grid.getCells();
//        for (int i = 0 ; i < cellArrayList.size(); i++){
//            if (cellArrayList.get(i).isInteresting()){
//                Cell cell = cellArrayList.get(i);
//                Rect rect = new Rect(cell.xStart,cell.yStart,cell.xEnd,cell.yEnd);
//                canvas.drawRect(rect,paint);
//            }
//        }
        return bitmap1;
    }

    public class Grid{
        int numberOfCells;
        int cellSize;
        int imageHeight;
        int imageWidth;
        ArrayList<Cell> cells;
        int numberOfCellsX;
        int numberOfCellsY;

        public ArrayList<Cell> getCells() {
            return cells;
        }

        public Grid( int numberOfCells, int cellSize, int imageHeight, int imageWidth){
            this.numberOfCells = numberOfCells;
            this.cellSize = cellSize;
            this.imageHeight = imageHeight;
            this.imageWidth = imageWidth;
            int xStart;
            int xEnd;
            int yStart;
            int yEnd;
            numberOfCellsX = (this.imageWidth)/this.cellSize;
            numberOfCellsY = (this.imageHeight)/this.cellSize;
            Log.d("xCells", String.valueOf(numberOfCellsX));
            Log.d("yCells", String.valueOf(numberOfCellsY));
            ArrayList<Cell> cellArrayList = new ArrayList<>();
            for (int y = 0; y < numberOfCellsY; y++){
                for (int x = 0; x < numberOfCellsX; x++){
                    if (x == 0 && y == 0){
                        xStart = y * (cellSize - 1);
                        yStart = x * (cellSize - 1);
                    }
                    else{
                        xStart = y * cellSize;
                        yStart = x * cellSize;
                    }
                    xEnd = xStart + (cellSize - 1);
                    yEnd = yStart + (cellSize - 1);

                    Cell cell = new Cell(xStart,xEnd,yStart,yEnd);
                    cellArrayList.add(cell);
                }
            }
            cells = cellArrayList;
        }

        public void goThrough(Pixel[][] pixels){
            for (int i = 0; i < cells.size(); i++){
                float hueAverageCell = 0;
                int saturationAverageCell = 0;
                int valueAverageCell = 0;

                float hsv[];

                float hsvAverageCell[] = new float[3];

                float hsvVarianceCell[] = new float[3];

                int yEnd = cells.get(i).yEnd;
                int xEnd = cells.get(i).xEnd;
                int xStart = cells.get(i).xStart;
                int yStart = cells.get(i).yStart;
                for (int y = yStart; y < yEnd; y++){
                    for (int x = xStart; x < xEnd; x++){
                        hsv = pixels[y][x].hsv;
                        hueAverageCell += hsv[0];
                        saturationAverageCell += hsv[1];
                        valueAverageCell += hsv[2];
                    }
                }

                int cellSize = (yEnd - yStart) * (xEnd - xStart);
                hsvAverageCell[0] = hueAverageCell/cellSize;
                hsvAverageCell[1] = saturationAverageCell/cellSize;
                hsvAverageCell[2] = valueAverageCell/cellSize;
                cells.get(i).setHsvAverage(hsvAverageCell);
                int hueVarianceCell = 0, saturationVarianceCell = 0, valueVarianceCell = 0;

                for (int y = yStart; y < yEnd; y++){
                    for (int x = xStart; x < xEnd; x++){
                        float[] hsvValues = pixels[y][x].hsv;
                        hueVarianceCell += (hsvValues[0] - hueAverageCell) * (hsvValues[0] - hueAverageCell);
                        saturationVarianceCell += (hsvValues[1] - saturationAverageCell) * (hsvValues[1] - saturationAverageCell);
                        valueVarianceCell += (hsvValues[2] - valueAverageCell) * (hsvValues[2] - valueAverageCell);
                    }
                }

                hsvVarianceCell[0] = hueVarianceCell/cellSize;
                hsvVarianceCell[1] = saturationVarianceCell/cellSize;
                hsvVarianceCell[2] = valueVarianceCell/cellSize;
                cells.get(i).setHsvVariance(hsvVarianceCell);
                cells.get(i).setInteresting(hsvAverageOverall,hsvVarianceOverall, hsvAverageCell);
            }
        }
    }

    public class Cell{
        int xStart, xEnd, yStart, yEnd;
        boolean interesting = false;
        float[] hsvAverage = new float[3];
        float[] hsvVariance = new float[3];

        public Cell(int xStart, int xEnd, int yStart, int yEnd){
            this.xStart = xStart;
            this.xEnd = xEnd;
            this.yStart = yStart;
            this.yEnd = yEnd;
        }

        @Override
        public String toString() {
            String finalString = String.valueOf(xStart) + "," + String.valueOf(yStart) + "," + String.valueOf(xEnd) + "," + String.valueOf(yEnd);
            return finalString;
        }

        public void setHsvAverage( float[] hsvAverage ) {
            this.hsvAverage = hsvAverage;
        }

        public void setHsvVariance( float[] hsvVariance ) {
            this.hsvVariance = hsvVariance;
        }

        public void setInteresting(float overallAverage[], float overallVariance[], float cellHSVAverage[]) {
            //Look at the average of the cell and if it less than
            // the average of the whole hue channel plus the hue variance. Mark it as interesting
            float imageAverage = overallAverage[0];
            float imageVariance = overallVariance[0];

            if (cellHSVAverage[0] < (imageAverage + imageVariance)){
                this.interesting = true;
            }
        }

        public boolean isInteresting() {
            return interesting;
        }
    }
}