package daslab.com.fieldmonitoringv2;

public class cameraSpecs {
    public static final cameraSpecs GOPRO_HERO4_BLACK = new cameraSpecs(2.5,6.17,4.56, 12, "GoPro Hero 4 Black");
    public static final cameraSpecs GOPRO_HERO4_SILVER = new cameraSpecs(2.5,5.37,4.04, 12, "GoPro Hero 4 Silver");
    public static final cameraSpecs MICASENSE3 = new cameraSpecs(5.5,3.6,4.8, 20.3,"Micasense 3");
    double focalLength, sensorHeight, sensorWidth, sensorResolution;
    String cameraName;

    public cameraSpecs(double focalLength, double sensorHeight, double sensorWidth, double sensorResolution,String cameraName){
        this.focalLength = focalLength;
        this.sensorWidth = sensorWidth;
        this.sensorHeight = sensorHeight;
        this.sensorResolution = sensorResolution;
        this.cameraName = cameraName;
    }

    public double getHorizontalFOV(double altitude){
        return (this.sensorWidth/this.focalLength)*altitude;
    }

    public double getVerticalFOV(double altitude){
        return (this.sensorHeight/this.focalLength)*altitude;
    }

    public double totalArea(double altitude){
        return getHorizontalFOV(altitude)*getVerticalFOV(altitude);
    }
    public double getFocalLength(){
        return this.focalLength;
    }
}
