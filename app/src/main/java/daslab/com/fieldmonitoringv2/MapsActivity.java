package daslab.com.fieldmonitoringv2;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.maps.android.SphericalUtil;
import com.o3dr.services.android.lib.drone.action.ControlActions;
import com.o3dr.services.android.lib.drone.mission.item.complex.CameraDetail;

import org.droidplanner.services.android.impl.core.drone.variables.Camera;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnInfoWindowClickListener, AdapterView.OnItemSelectedListener {

    // Initializes the GoogleMap
    private GoogleMap mMap;

    // Number of corners in use currently
    private int numberOfMarkers = 0;

    // Defaults to a rectangle
    private int numberOfCorners = 4;

    LinkedList<Marker> markerLinkedList = new LinkedList<>();

    LinkedList<Integer> removedMarkerList = new LinkedList<>();


    // Test commit

    String fileName = null;

    FileWriter fileWriter;

    static File plansDir;
    File planDir, points;

    LinkedList<Polygon> polygonList = new LinkedList<>();

    LatLng currentLocation;

    double overlap = 0.0;

    double sidelap = 0.0;

    double altitude = 30.0;

    double speed = 5.0;

    Path path;

    LinkedList<LatLng> loadedLatLngs = new LinkedList<>();

    List<Polyline> polylineList = new LinkedList<>();

    cameraSpecs specs = cameraSpecs.GOPRO_HERO4_BLACK;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Spinner spinner = findViewById(R.id.spinner);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.plans, android.R.layout.simple_spinner_item);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(this);
        plansDir = new File(getExternalFilesDir(null), "Plans");
        Log.d("plansDir", plansDir.getAbsolutePath());
        if (!plansDir.exists()) {
            Log.d("plansDir", "Plans directory doesn't exist, creating one.");
            if (plansDir.mkdir()) {
                Log.d("Create Dir", "Dir Created");
            }
        } else {
            Log.d("plansDir", "Plans directory already exists");
        }

        final EditText editText = findViewById(R.id.editText);
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction( TextView v, int actionId, KeyEvent event ) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    fileName = editText.getText().toString();
                    if (!fileName.isEmpty()) {
                        planDir = new File(plansDir.getAbsolutePath(), fileName);
                        points = new File(planDir.getAbsolutePath(), "points.txt");
                        if (!planDir.exists()) {
                            Log.d("plansDir", "Plans directory doesn't exist, creating one.");
                            if (planDir.mkdir()) {
                                Log.d("Create Dir", "Directory is created");
                            }
                        } else {
                            Log.d("plansDir", fileName + " directory already exists");
                        }
                        Log.d("file", "File is located at " + planDir.getAbsolutePath());
                        try {
                            Log.d("filewriter", "Successfully set filewriter");
                            fileWriter = new FileWriter(points, true);
                        } catch (IOException e) {
                            Log.d("file", "File could not be created");
                            AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                            builder.setMessage("File name already exists");
                            builder.setTitle("Please change the file name");
                            builder.show();
                        }
                        Log.d("editText", "Action received");
                        handled = true;
                    } else {
                        Log.d("file", "No file name indicated");
                        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                        builder.setMessage("Please enter a file name");
                        builder.setTitle("No file name entered");
                        builder.show();
                    }

                }
                return handled;
            }
        });
        final EditText overlapEditText = findViewById(R.id.overlap);
        overlapEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction( TextView v, int actionId, KeyEvent event ) {
                String overlapText = overlapEditText.getText().toString();
                NumberFormat numberFormat = NumberFormat.getInstance();
                try {
                    Number number = numberFormat.parse(overlapText);
                    overlap = number.doubleValue();
                    if (overlap >= 100.0) {
                        overlap = 100.0;
                    }
                    if (overlap <= 0.0) {
                        overlap = 0.0;
                    }
                } catch (ParseException e) {
                    Log.d("number", "Number could not be converted");
                    overlap = 0.0;
                }
                return true;
            }
        });

        final EditText sideLapEditText = findViewById(R.id.sidelap);
        sideLapEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction( TextView v, int actionId, KeyEvent event ) {
                String sidelapText = sideLapEditText.getText().toString();
                NumberFormat numberFormat = NumberFormat.getInstance();
                try {
                    Number number = numberFormat.parse(sidelapText);
                    overlap = number.doubleValue();
                    if (sidelap >= 100.0) {
                        sidelap = 100.0;
                    }
                    if (sidelap <= 0.0) {
                        sidelap = 0.0;
                    }
                } catch (ParseException e) {
                    Log.d("number", "Number could not be converted");
                    sidelap = 0.0;
                }
                return true;
            }
        });
        final EditText altitudeEditText = findViewById(R.id.altitude);
        altitudeEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction( TextView v, int actionId, KeyEvent event ) {
                String altitudeText = v.getText().toString();
                NumberFormat numberFormat = NumberFormat.getInstance();
                try {
                    Number altitudeNumber = numberFormat.parse(altitudeText);
                    altitude = altitudeNumber.doubleValue();
                    if (altitude > 120.0){
                        altitude = 120.0;
                    }
                    if (altitude < 10.0){
                        altitude = 10.0;
                    }
                    Log.d("altitudeSet", Double.toString(altitude));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                return true;
            }
        });
        final EditText speedEditText = findViewById(R.id.flightSpeed);
        speedEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction( TextView v, int actionId, KeyEvent event ) {
                String speedText = v.getText().toString();
                NumberFormat numberFormat = NumberFormat.getInstance();
                try {
                    Number speedNumber = numberFormat.parse(speedText);
                    speed = speedNumber.doubleValue();
                    if (speed > 15.0){
                        speed = 15.0;
                    }
                    if (speed < 1.0){
                        speed = 1.0;
                    }
                    Log.d("speedSet", Double.toString(speed));

                } catch (ParseException e) {
                    e.printStackTrace();
                }
                return true;
            }
        });

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String planNameToOpen = extras.getString("planNameToOpen");
            File fileToOpen = new File(getExternalFilesDir(null).getAbsolutePath().concat("/Plans"), planNameToOpen.concat("/points.txt"));
            fileName = planNameToOpen;
            Log.d("openfile", "Opening " + fileToOpen.getAbsolutePath());
            int i = 0;
            try {
                BufferedReader br = new BufferedReader(new FileReader(fileToOpen));
                String line, line1;
                while ((line = br.readLine()) != null &&
                        (line1 = br.readLine()) != null) {
                    Log.d("waypoints", line + " & " + line1);
                    loadedLatLngs.add(i, new LatLng(Double.parseDouble(line), Double.parseDouble(line1)));
                    i++;
                }
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            File attributesToOpen = new File(getExternalFilesDir(null).getAbsolutePath().concat("/Plans"),planNameToOpen.concat("/attributes.txt"));
            try {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(attributesToOpen));
                String line;
                if ((line = bufferedReader.readLine()) != null){
                    BigDecimal flightTime = BigDecimal.valueOf(Integer.parseInt(line));
                    int[] intToTime = secondsToMinutesSeconds(flightTime);
                    TextView estimatedFlightTimeTextView = findViewById(R.id.estimated_flight_time);
                    estimatedFlightTimeTextView.setText("Estimated Flight Time: " + intToTime[1] + ":" + intToTime[2]);
                }
                if ((line = bufferedReader.readLine()) != null){
                    TextView totalArea = findViewById(R.id.total_area);
                    totalArea.setText("Total area: " + line + " acres");
                }
                if ((line = bufferedReader.readLine()) != null){
                    speed = Double.parseDouble(line);
                }
                if ((line = bufferedReader.readLine()) != null){
                    altitude = Double.parseDouble(line);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // you need to have a list of data that you want the spinner to display
        List<String> spinnerArray =  new ArrayList<String>();
        spinnerArray.add("GoPro Hero 4 Black");
        spinnerArray.add("GoPro Hero 4 Silver");

        ArrayAdapter<String> cameraAdapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, spinnerArray);

        cameraAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        final Spinner cameraSpinner = findViewById(R.id.cameraSpinner);
        cameraSpinner.setAdapter(cameraAdapter);
        File cameraSpecsFile = new File(getExternalFilesDir(null),"cameraSpecs");
        cameraSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected( AdapterView<?> parent, View view, int position, long id ) {
                switch (parent.getItemAtPosition(position).toString()) {
                    case "GoPro Hero 4 Silver":
                        specs = cameraSpecs.GOPRO_HERO4_SILVER;
                        break;
                        default:
                            specs = cameraSpecs.GOPRO_HERO4_BLACK;
                            break;
                }
            }

            @Override
            public void onNothingSelected( AdapterView<?> parent ) {

            }
        });
        CameraDetail cameraDetail = new CameraDetail();
        Log.d("cameraDetail", cameraDetail.toString());
    }

    private void clearMap() {
        mMap.clear();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

    int estimatedFlightTime = 0;

    @Override
    public void onMapReady( GoogleMap googleMap ) {

        mMap = googleMap;

        // Sets the map type to satellite
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

        // Turns off indoor mapping
        mMap.setIndoorEnabled(false);

        // Checks permissions to allow for allowing GPS location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("location", "Asking for location permission");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        // Turns on the blue dot to show location
        mMap.setMyLocationEnabled(true);

        FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Gets the last known location and updates the camera position to the location
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess( Location location ) {
                if (location == null) {
                    Log.d("location", "Current Location is null");
                } else {
                    currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    //altitude = location.getAltitude();
                    Log.d("location", "Current location is " + location.getLatitude() + " & " + location.getLongitude() + " & " + location.getAltitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 17.5f));
                }
            }
        });

        // Turns the map click listener on
        mMap.setOnMapClickListener(this);
        // Turns the marker click listener on
        mMap.setOnMarkerClickListener(this);
        mMap.setOnInfoWindowClickListener(this);
        clearMap();
        for (LatLng photoPosition :
                loadedLatLngs) {
            mMap.addMarker(new MarkerOptions().position(photoPosition));
        }
        polylineList.add(mMap.addPolyline(new PolylineOptions().addAll(loadedLatLngs)));
        if(fileName != null){
            path = new Path();
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess( Location location ) {
                    if (location == null) {
                        Log.d("location", "Current Location is null");
                    } else {
                        currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    }
                }
            });
            if (currentLocation == null){
                path.createPath(loadedLatLngs,mMap, new LatLng(40.1133650, -87.9654040));
            }
            else {
                path.createPath(loadedLatLngs,mMap, currentLocation);
            }
            TextView estimatedPhotos = findViewById(R.id.estimated_number_of_photos);
            estimatedPhotos.setText("Estimated # of photos: " + path.getNumberOfPhotos());
        }

    }

    // Create corners of shape to be used
    @Override
    public void onMapClick( LatLng latLng ) {
        if (numberOfMarkers < numberOfCorners) {
            if (!removedMarkerList.isEmpty()) {
                Log.d("marker", "Creating marker number " + removedMarkerList.getLast().toString());
                markerLinkedList.add(mMap.addMarker(new MarkerOptions().position(latLng).draggable(false).title(removedMarkerList.removeLast().toString())));

                numberOfMarkers++;
            } else {
                Log.d("marker", "Creating marker number " + numberOfMarkers);
                markerLinkedList.add(mMap.addMarker(new MarkerOptions().position(latLng).draggable(false).title(Integer.toString(numberOfMarkers))));
                numberOfMarkers++;
            }

        }
        if (numberOfMarkers == numberOfCorners) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (Marker marker :
                    markerLinkedList) {
                Log.d("marker", marker.getPosition().toString());
                builder.include(marker.getPosition());
            }
            LatLngBounds build = builder.build();
            List<LatLng> finalBox = new LinkedList<>();
            double aNorth = build.northeast.latitude;
            double aEast = build.northeast.longitude;
            double aSouth = build.southwest.latitude;
            double aWest = build.southwest.longitude;
            finalBox.add(build.northeast);
            finalBox.add(new LatLng(aNorth, aWest));
            finalBox.add(build.southwest);
            finalBox.add(new LatLng(aSouth, aEast));
            Log.d("box", "Box created");
            if (polygonList.isEmpty()) {
                polygonList.add(mMap.addPolygon(new PolygonOptions().addAll(finalBox).fillColor(Color.argb(75, 255, 102, 102))));
            } else {
                polygonList.getFirst().remove();
                polygonList.removeFirst();

                polygonList.add(mMap.addPolygon(new PolygonOptions().addAll(finalBox).fillColor(Color.argb(75, 255, 102, 102))));
            }
            Log.d("area", Double.toString(SphericalUtil.computeArea(polygonList.getFirst().getPoints())));
            Log.d("totalAreaCamera", Double.toString(specs.totalArea(60.0)));
            if (fileName != null) {
                Log.d("saving photos", getExternalFilesDir(null).getAbsolutePath().concat("/Plans/" + fileName));
                path = new Path(60.0, overlap, 10.0, specs, fileName, getExternalFilesDir(null).getAbsolutePath().concat("/Plans/" + fileName));
            } else {
                path = new Path(60.0, overlap, 10.0, specs, "noName", getExternalFilesDir(null).getAbsolutePath().concat("/Plans/"));
            }
            if (currentLocation != null) {
                path.createPath(polygonList.getFirst(), currentLocation, specs.getHorizontalFOV(20.0), specs.getVerticalFOV(20.0), mMap, build);
                int estimatedFlightTime = path.getEstimatedFlightTime();
                BigDecimal flightTime = BigDecimal.valueOf(estimatedFlightTime);
                int[] test = secondsToMinutesSeconds(flightTime);
                TextView estimatedFlightTimeTextView = findViewById(R.id.estimated_flight_time);
                estimatedFlightTimeTextView.setText("Estimated Flight Time: " + test[1] + ":" + test[2]);
                TextView estimatedPhotos = findViewById(R.id.estimated_number_of_photos);
                estimatedPhotos.setText("Estimated # of photos: " + path.getNumberOfPhotos());
                TextView totalArea = findViewById(R.id.total_area);
                DecimalFormat df = new DecimalFormat();
                df.setMaximumFractionDigits(2);
                totalArea.setText("Total area: " + df.format(path.getAcreage()).toString() + " acres");
                if (planDir == null) {
                    AlertDialog.Builder noSavedPlan = new AlertDialog.Builder(MapsActivity.this);
                    noSavedPlan.setMessage("Please enter a file name");
                    noSavedPlan.setTitle("File name needed");
                    noSavedPlan.show();
                }
            }
        }
    }

    private int[] secondsToMinutesSeconds( BigDecimal decimal){
        long longVal = decimal.longValue();
        int hours = (int) longVal / 3600;
        int remainder = (int) longVal - hours * 3600;
        int mins = remainder / 60;
        remainder = remainder - mins * 60;
        int secs = remainder;

        int[] ints = {hours , mins , secs};
        return ints;

    }

    @Override
    public boolean onMarkerClick( Marker marker ) {
        Log.d("marker", "Marker " + marker.getTitle() + " was clicked." + "LatLng = " + marker.getPosition().toString());
        return false;
    }

    @Override
    public void onInfoWindowClick( Marker marker ) {
        Log.d("marker", "Removing marker number " + marker.getTitle());
        marker.remove();
        markerLinkedList.removeLast();
        removedMarkerList.add(Integer.parseInt(marker.getTitle()));
        numberOfMarkers--;

    }

    DataInputStream in = null;

    static Plan plan;

    @Override
    public void onItemSelected( AdapterView<?> parent, View view, int position, long id ) {
        Log.d("spinner", parent.getItemAtPosition(position).toString());
        switch (parent.getItemAtPosition(position).toString()){
            case "Save Plan":
                if (numberOfMarkers != numberOfCorners){
                    AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                    builder.setMessage("You do not have enough points on the map.");
                    builder.setTitle("Please add more markers.");
                    builder.show();
                    break;
                }
                if (planDir == null){
                    Log.d("file", "No file name indicated");
                    AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                    builder.setMessage("Please enter a file name");
                    builder.setTitle("No file name entered");
                    builder.show();
                    break;
                }
                for (Marker marker :
                        markerLinkedList) {
                    Log.d("marker", marker.getPosition().toString());
                }
                plan = new Plan(path.getNumberOfPhotos(),planDir.getAbsolutePath(),fileName);
                plan.markerLinkedList = path.getPhotoWaypoints();
                if (fileWriter != null){
                    plan.writeToPlan(fileWriter);
                }
                Log.d("planDirPath", planDir.getAbsolutePath());
                File attributes = new File(getExternalFilesDir(null).getAbsolutePath(),"Plans/".concat(plan.planName).concat("/attributes.txt"));
                try {
                    FileWriter attributeFileWriter = new FileWriter(attributes);
                    attributeFileWriter.write(path.getEstimatedFlightTime() + "\n");
                    DecimalFormat df = new DecimalFormat();
                    df.setMaximumFractionDigits(2);
                    attributeFileWriter.write(df.format(path.getAcreage()) + "\n");
                    attributeFileWriter.write(Double.toString(speed) + "\n");
                    attributeFileWriter.write(Double.toString(altitude) + "\n");
                    attributeFileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.d("plan", "Saving plan");
                break;
            case "Open Plans":
                Intent activityOpenPlan = new Intent(MapsActivity.this, activity_open_plan.class);
                MapsActivity.this.startActivity(activityOpenPlan);
                Log.d("plan", "Opening plan");
                break;
                default:
                    Log.d("plan","Nothing is chosen");
                    break;
            case "Clear Map":
                clearMap();
                numberOfMarkers = 0;
                for (Polygon polygon :
                        polygonList) {
                    polygon.remove();
                }
                for (Polyline polyline :
                        polylineList) {
                    polyline.remove();
                    fileName = null;
                    TextView estTimeFlight = findViewById(R.id.estimated_flight_time);
                    estTimeFlight.setText("Estimated Time: 0");
                    TextView estPhotos = findViewById(R.id.estimated_number_of_photos);
                    estPhotos.setText("# of photos: 0");
                    TextView totalArea = findViewById(R.id.total_area);
                    totalArea.setText("Total Area: 0 acres");
                    overlap = 0;
                    sidelap = 0;
                }
        }
    }

    @Override
    public void onNothingSelected( AdapterView<?> parent ) {

    }

    public void goFlyActivity( View view ) {
        Intent GCS_3DR_Activity_Intent = new Intent(MapsActivity.this, GCS_3DR_Activity.class);
        if (fileName == null){
            AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
            builder.setMessage("Please enter a file name in order to fly");
            builder.setTitle("File name needed to fly");
            builder.show();
            return;
        }
        else {
            Log.d("filename", fileName);
            GCS_3DR_Activity_Intent.putExtra("planName",fileName);
        }
        if (path != null){
            GCS_3DR_Activity_Intent.putExtra("horizSize", path.getHorizSize());
            GCS_3DR_Activity_Intent.putExtra("vertSize", path.getVertSize());
        }
        GCS_3DR_Activity_Intent.putExtra("altitude", altitude);
        MapsActivity.this.startActivity(GCS_3DR_Activity_Intent);
    }
}
