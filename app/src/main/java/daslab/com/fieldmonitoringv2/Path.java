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

    private LinkedList<Marker> photoWaypoints = new LinkedList<>();

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
    }

    public Path(double sidelap, double overlap, double flightSpeed, cameraSpecs specs, String planName, String directoryName){
        this.sidelap = sidelap;
        this.overlap = overlap;
        this.flightSpeed = flightSpeed;
        this.specs = specs;
        this.numberOfTurns = 0;
        this.estimatedFlightTime = getEstimatedFlightTime();
        this.planName = planName;
        this.directoryName = directoryName;
        this.horizSize = 0;
        this.vertSize = 0;
    }

    public LinkedList<Marker> getPhotoWaypoints() {
        return photoWaypoints;
    }

    public int getEstimatedFlightTime(){
        return (int)(this.distanceToTravel /flightSpeed);
    }

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
        setEstimatedFlightTime(markerLatLngs,new LatLng(0.0,0.0));
    }

    public void createPath(LinkedList<LatLng> latLngs, GoogleMap map, LatLng homeLocation){
        distanceToTravel = SphericalUtil.computeLength(latLngs);
        numberOfPhotos = latLngs.size();
    }

    private void setEstimatedFlightTime(List<LatLng> photoPosition, LatLng homeLocation){
        estimatedFlightTime = (SphericalUtil.computeLength(photoPosition) + SphericalUtil.computeDistanceBetween(homeLocation,photoPosition.get(0)) + SphericalUtil.computeDistanceBetween(photoPosition.get(photoPosition.size()-1),homeLocation) + (numberOfTurns * 2))/this.flightSpeed;
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
