package daslab.com.fieldmonitoringv2;

public class PixelsAndHSV {
    Pixel[][] pixels;
    float[] meanHsv;
    float[] varianceOverall;
    int pixelCount;
    int[] heightWidth;

    public PixelsAndHSV( Pixel[][] pixels, float[] meanHsv, float[] varianceOverall, int pixelCount, int[] heightWidth){
        this.pixels = pixels;
        this.meanHsv = meanHsv;
        this.pixelCount = pixelCount;
        this.varianceOverall = varianceOverall;
        this.heightWidth = heightWidth;
    }
}
