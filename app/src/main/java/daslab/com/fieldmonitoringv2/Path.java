package daslab.com.fieldmonitoringv2;

import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.SphericalUtil;
import com.o3dr.services.android.lib.drone.mission.item.complex.Survey;
import com.o3dr.services.android.lib.drone.mission.item.complex.SurveyDetail;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class Path {
    private double sidelap; // East to West
    private double overlap; // North to South
    private int numberOfPhotos;
    private double estimatedFlightTime; // in seconds
    private double flightSpeed;
    private double distanceToTravel;
    private double acreage;
    private cameraSpecs specs;
    private int numberOfTurns;
    private String planName;
    private String directoryName;
    private int horizSize;
    private int vertSize;
    private double flightAltitude;
    private LatLng homeLocation;

    private LinkedList<Marker> photoWaypoints = new LinkedList<>();

    /**
     * Initilize a path to all zero's and a GoPro Hero 4 Black
     */
    public Path() {
        this.sidelap = 0;
        this.overlap = 0;
        this.numberOfPhotos = 0;
        this.estimatedFlightTime = 0;
        this.flightSpeed = 0.0;
        this.distanceToTravel = 0.0;
        this.acreage = 0.0;
        this.numberOfTurns = 0;
        specs = cameraSpecs.GOPRO_HERO4_BLACK;
        this.horizSize = 0;
        this.vertSize = 0;
        this.flightAltitude = 0.0;
    }

    /**
     * Initilize a path and create
     * @param survey The survey information containing the polygon and camera information
     * @param flightSpeed The flight speed
     * @param homeLocation The location closest to the the polyon
     * @param planName The plan name that the path is being created for
     * @param directoryName The directory name that it is being saved into
     */
    public Path( Survey survey, double flightSpeed, LatLng homeLocation, String planName, String directoryName){

        // Gets the survey detail
        SurveyDetail surveyDetail = survey.getSurveyDetail();

        // Extracts the information from survey detail
        this.sidelap =  surveyDetail.getSidelap();
        this.overlap = surveyDetail.getOverlap();

        // Extracts information from survey
        this.numberOfTurns = survey.getNumberOfLines();
        this.numberOfPhotos = survey.getCameraCount();

        // Sets flight speed
        this.flightSpeed = flightSpeed;

        // Sets the home location
        this.homeLocation = homeLocation;

        // Sets the camera locations into latlng format
        LatLong2LatLngs latLngsConverted = new LatLong2LatLngs(survey.getCameraLocations());
        List<LatLng> cameraLocations = latLngsConverted.getLatLngs();

        // Sets the estimated flight time based off the camera locations
        setEstimatedFlightTime(cameraLocations);

        // Gets the estimated flight time and stores it
        this.estimatedFlightTime = getEstimatedFlightTime();

        this.planName = planName;
        this.directoryName = directoryName;

        // Set the horizontal size based off number of points
        this.horizSize = survey.getNumberOfLines();
        // Set the vertical size based off the points
        this.vertSize = (survey.getCameraCount()/survey.getNumberOfLines());
        this.flightAltitude = surveyDetail.getAltitude();
    }

    /**
     * Gets the estimated flight time in seconds
     * @return the estimated flight time in seconds
     */
    public int getEstimatedFlightTime(){
        return (int)this.estimatedFlightTime;
    }

    // Creates a brand new path
    public void createPath( Polygon polygon, LatLng currentLocation, double horizontalFOV, double verticalFOV, GoogleMap map, LatLngBounds latLngBounds){
        LatLng closestPoint = getClosestPointToHome(currentLocation,polygon);
        Log.d("closestPt", closestPoint.toString());

        int vertSize = 0, horizSize = 0;
        List<LatLng> polygonPts = polygon.getPoints();
        double northSouthHeading = 0.0;
        double eastWestHeading = 0.0;
        LatLng corner;

        if (latLngBounds.contains(getCenter(SphericalUtil.computeOffset(closestPoint,verticalFOV,0.0),SphericalUtil.computeOffset(closestPoint,horizontalFOV,90.0)))){
            northSouthHeading = 0.0;
            eastWestHeading = 90.0;
            corner = getCenter(SphericalUtil.computeOffset(closestPoint,verticalFOV,northSouthHeading),SphericalUtil.computeOffset(closestPoint,horizontalFOV,eastWestHeading));
            Log.d("corner", "Northeast");
            vertSize += numberofMarkers(corner,verticalFOV,latLngBounds,northSouthHeading, overlap);
            horizSize += numberofMarkers(corner,horizontalFOV,latLngBounds,eastWestHeading, sidelap);
        }
        else if (latLngBounds.contains(getCenter(SphericalUtil.computeOffset(closestPoint,verticalFOV,0.0),SphericalUtil.computeOffset(closestPoint,horizontalFOV,270.0)))){
            northSouthHeading = 0.0;
            eastWestHeading = 270.0;
            corner = getCenter(SphericalUtil.computeOffset(closestPoint,verticalFOV,northSouthHeading),SphericalUtil.computeOffset(closestPoint,horizontalFOV,eastWestHeading));
            Log.d("corner", "Northwest");
            vertSize += numberofMarkers(corner,verticalFOV,latLngBounds,northSouthHeading, overlap);
            horizSize += numberofMarkers(corner,horizontalFOV,latLngBounds,eastWestHeading, sidelap);
        }
        else if (latLngBounds.contains(getCenter(SphericalUtil.computeOffset(closestPoint,verticalFOV,180.0),SphericalUtil.computeOffset(closestPoint,horizontalFOV,90.0)))){
            northSouthHeading = 180.0;
            eastWestHeading = 90.0;
            corner = getCenter(SphericalUtil.computeOffset(closestPoint,verticalFOV,northSouthHeading),SphericalUtil.computeOffset(closestPoint,horizontalFOV,eastWestHeading));
            Log.d("corner", "Southeast");
            vertSize += numberofMarkers(corner,verticalFOV,latLngBounds,northSouthHeading, overlap);
            horizSize += numberofMarkers(corner,horizontalFOV,latLngBounds,eastWestHeading, sidelap);
        }
        else if (latLngBounds.contains(getCenter(SphericalUtil.computeOffset(closestPoint,verticalFOV,180.0),SphericalUtil.computeOffset(closestPoint,horizontalFOV,270.0)))){
            northSouthHeading = 180.0;
            eastWestHeading = 270.0;
            corner = getCenter(SphericalUtil.computeOffset(closestPoint,verticalFOV,northSouthHeading),SphericalUtil.computeOffset(closestPoint,horizontalFOV,eastWestHeading));
            Log.d("corner", "Southwest");
            vertSize += numberofMarkers(corner,verticalFOV,latLngBounds,northSouthHeading, overlap);
            horizSize += numberofMarkers(corner,horizontalFOV,latLngBounds,eastWestHeading, sidelap);
        }
        else{
            Log.d("corner", "None found");
            corner = new LatLng(0.0,0.0);
        }
        this.horizSize = horizSize;
        this.vertSize = vertSize;
        for (int i = 0; i <= horizSize; i++){
            for (int j = 0; j < vertSize; j++){
                    photoWaypoints.add(map.addMarker(new MarkerOptions().position(corner).title("Photo".concat(Integer.toString(numberOfPhotos)))));
                    numberOfPhotos++;
                    corner = SphericalUtil.computeOffset(corner,verticalFOV* (1- (overlap/100)),northSouthHeading);

            }
            photoWaypoints.add(map.addMarker(new MarkerOptions().position(corner).title("Photo".concat(Integer.toString(numberOfPhotos)))));
            corner = SphericalUtil.computeOffset(corner, horizontalFOV * (1- (sidelap/100)), eastWestHeading);
            if (northSouthHeading == 0.0){
                northSouthHeading = 180.0;
            }
            else if (northSouthHeading == 180.0){
                northSouthHeading = 0.0;
            }
            numberOfPhotos++;
        }
        List<LatLng> markerLatLngs = new LinkedList<>();
        List<Polyline> polylineList = new LinkedList<>();
        int i = 0;
        File saveWaypoints = new File(directoryName,"points.txt");
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(saveWaypoints);
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (Marker marker:
             photoWaypoints) {
            markerLatLngs.add(i,marker.getPosition());
            try {
                fileWriter.write(Double.toString(markerLatLngs.get(i).latitude));
                fileWriter.write("\n");
                fileWriter.write(Double.toString(markerLatLngs.get(i).longitude));
                fileWriter.write("\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
            i++;
        }
        polylineList.add(map.addPolyline(new PolylineOptions().addAll(markerLatLngs)));
        numberOfTurns = horizSize;
        distanceToTravel = SphericalUtil.computeLength(markerLatLngs);
        setAcreage(polygonPts);
        setEstimatedFlightTime(markerLatLngs);
    }

    /**
     * Generates the distance to travel and the number of photos to be taken for an already created set of latlngs
     * @param latLngs Camera locations to travel
     */
    public void createPath(LinkedList<LatLng> latLngs){
        distanceToTravel = SphericalUtil.computeLength(latLngs);
        numberOfPhotos = latLngs.size();
    }

    /**
     * Sets the estimated flight time based of the camera positions and a startup and land time
     * @param photoPosition Camera Locations
     */
    private void setEstimatedFlightTime(List<LatLng> photoPosition){
        this.estimatedFlightTime = takeOffTime() + flyToStartTime(photoPosition) + estimatedFlightDuration(photoPosition) + flyToHomeTime(photoPosition) + landingTime();
    }

    /**
     * Determines how long it will take to fly a path
     * @param photoPosition Camera Locations
     * @return
     */
    private double estimatedFlightDuration(List<LatLng> photoPosition){
        return ((SphericalUtil.computeLength(photoPosition) +  + (numberOfTurns * 2))/this.flightSpeed);
    }

    private double flyToStartTime(List<LatLng> photoPosition){
        Log.d("homeLoc", homeLocation.toString());
        Log.d("photoPosSize,", String.valueOf(photoPosition.size()));
        Log.d("flightSpeed", String.valueOf(flightSpeed));
        if (photoPosition.size() >= 1){
            return ((SphericalUtil.computeDistanceBetween(homeLocation,photoPosition.get(0)))/ this.flightSpeed);
        }
        return 0.0;
    }

    private double flyToHomeTime(List<LatLng> photoPosition){
        if (photoPosition.size() >= 1){
            return ((SphericalUtil.computeDistanceBetween(photoPosition.get(photoPosition.size()-1),homeLocation))/this.flightSpeed);
        }
        return 0.0;
    }

    private double takeOffTime(){
        return (flightAltitude/2);
    }

    private double landingTime(){
        return (flightAltitude + 10.0);
    }
    public int getHorizSize() {
        return horizSize;
    }

    public int getVertSize() {
        return vertSize;
    }

    private void setAcreage( List<LatLng> latLngs){
        Log.d("m^2", Double.toString(SphericalUtil.computeArea(latLngs)));
        Log.d("acres", Double.toString(SphericalUtil.computeArea(latLngs)* .00024711));
        acreage = SphericalUtil.computeArea(latLngs) * 0.00024711;
    }

    public int getNumberOfPhotos(){
        return numberOfPhotos;
    }

    public double getAcreage(){
        return acreage;
    }

    private LatLng getCenter(LatLng northeast, LatLng southwest){
        return SphericalUtil.interpolate(northeast,southwest,.5);
    }

    private int numberofMarkers(LatLng closestPoint, double FOV, LatLngBounds latLngBounds, double heading, double overlap){
        Log.d("FOV", Double.toString(FOV));
        int size = 0;
        boolean inBounds = true;
        LatLng currentPoint = closestPoint;
        while (inBounds){
            currentPoint = SphericalUtil.computeOffset(currentPoint,FOV*(1 - (overlap/100)),heading);
            if (latLngBounds.contains(currentPoint)){
                size++;
            }
            else{
                inBounds = false;
            }
        }
        Log.d("pts", "Number of pts = " + size);
        return size;
    }

    /*
     * @param polygon in use and current location;
     * @returns closest corner of polygon to current location;
     */
    private LatLng getClosestPointToHome(LatLng homeLocation, Polygon polygon){
        double distanceAway = 0.0;
        List<LatLng> polygonPts = polygon.getPoints();

        LatLng currentClosestCorner = null;

        if (polygonPts.size() > 0){
            currentClosestCorner = polygonPts.get(0);
            distanceAway = SphericalUtil.computeDistanceBetween(homeLocation,polygonPts.get(0));
        }
        int numberOfPolygonPts = polygonPts.size();
        for (int i = 0; i < numberOfPolygonPts; i++){
            if (SphericalUtil.computeDistanceBetween(polygonPts.get(i),homeLocation) < distanceAway){
                Log.d("distance", Double.toString(SphericalUtil.computeDistanceBetween(polygonPts.get(i), homeLocation)));
                distanceAway = SphericalUtil.computeDistanceBetween(polygonPts.get(i), homeLocation);
                currentClosestCorner = polygonPts.get(i);
            }
        }

        return currentClosestCorner;
    }

}
