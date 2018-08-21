package daslab.com.fieldmonitoringv2;

public class cameraSpecs {
    /**
     * GoPro Hero 4 Black sensor info
     */
    public static final cameraSpecs GOPRO_HERO4_BLACK = new cameraSpecs(2.5,6.17,4.56, 12, "GoPro Hero 4 Black");
    /**
     * GoPro Hero 4 Silver sensor info
     */
    public static final cameraSpecs GOPRO_HERO4_SILVER = new cameraSpecs(2.5,5.37,4.04, 12, "GoPro Hero 4 Silver");
    /**
     * Micasense sensor info
     */
    public static final cameraSpecs MICASENSE3 = new cameraSpecs(5.5,3.6,4.8, 20.3,"Micasense 3");

    double focalLength, sensorHeight, sensorWidth, sensorResolution;

    String cameraName;

    /**
     * Setter Function to init a camera
     * @param focalLength Image focal length of current camera
     * @param sensorHeight Image sensor height of current camera
     * @param sensorWidth Image sensor width of current camera
     * @param sensorResolution Image sensor resolution of current camera
     * @param cameraName Camera name of current camera
     */
    public cameraSpecs(double focalLength, double sensorHeight, double sensorWidth, double sensorResolution,String cameraName){
        this.focalLength = focalLength;
        this.sensorWidth = sensorWidth;
        this.sensorHeight = sensorHeight;
        this.sensorResolution = sensorResolution;
        this.cameraName = cameraName;
    }

    /**
     *
     * @param altitude Determines the size of the horizontal FOV based off the given altitude
     * @return The size of the horizontal FOV in meters
     */
    public double getHorizontalFOV(double altitude){
        return (this.sensorWidth/this.focalLength)*altitude;
    }

    /**
     *
     * @param altitude Determines the size of the vertical FOV based off the given altitude
     * @return The size of the vertical FOV in meters
     */
    public double getVerticalFOV(double altitude){
        return (this.sensorHeight/this.focalLength)*altitude;
    }
}
