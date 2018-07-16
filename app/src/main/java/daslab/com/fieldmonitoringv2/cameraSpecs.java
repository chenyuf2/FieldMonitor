package daslab.com.fieldmonitoringv2;

import com.o3dr.services.android.lib.drone.mission.item.complex.CameraDetail;

import org.droidplanner.services.android.impl.core.survey.CameraInfo;

public class cameraSpecs {
    public static final cameraSpecs GOPRO_HERO4_BLACK = new cameraSpecs(2.5,6.17,4.56);
    public static final cameraSpecs GOPRO_HERO4_SILVER = new cameraSpecs(2.5,5.37,4.04);
    public static final cameraSpecs MICASENSE3 = new cameraSpecs(5.5,3.6,4.8);
    double focalLength, sensorHeight, sensorWidth;

    public cameraSpecs(){
        this.focalLength = 0.0;
        this.sensorHeight = 0.0;
        this.sensorWidth = 0.0;
    }

    public cameraSpecs(double focalLength, double sensorHeight, double sensorWidth){
        this.focalLength = focalLength;
        this.sensorWidth = sensorWidth;
        this.sensorHeight = sensorHeight;
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
}
