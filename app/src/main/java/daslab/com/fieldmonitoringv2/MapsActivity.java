package daslab.com.fieldmonitoringv2;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
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
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.drone.mission.item.complex.Survey;

import org.droidplanner.services.android.impl.core.helpers.units.Area;
import org.droidplanner.services.android.impl.core.survey.CameraInfo;
import org.droidplanner.services.android.impl.core.survey.SurveyData;
import org.droidplanner.services.android.impl.core.survey.grid.Grid;
import org.droidplanner.services.android.impl.core.survey.grid.GridBuilder;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;

import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnInfoWindowClickListener, AdapterView.OnItemSelectedListener{

    // Initializes the GoogleMap
    private GoogleMap mMap;

    // Number of markers in use currently
    private int numberOfMarkers = 0;

    // At least 4 corners are needed in order to make a shape
    private int numberOfCorners = 4;

    // The corners of the polygon as they are created
    LinkedList<Marker> markerLinkedList = new LinkedList<>();

    // If the corner is removed, it is added to this list, so the proper number can be added back.
    LinkedList<Integer> removedMarkerList = new LinkedList<>();

    // The filename
    String fileName = null;

    FileWriter fileWriter;

    // The overarching directory that contains all the plans
    static File plansDir;

    // The plan directory that is being saved to currently
    File planDir;

    // The points file that contains all the latitude and longitude
    File points;

    // Contains the polygon created to reprsent that area to be flown
    LinkedList<Polygon> polygonList = new LinkedList<>();

    // Current Latitude and Longitude of the tablet, only is set once
    LatLng currentLocation;

    // Init all of the current plan settings
    double overlap = 20.0; // in percentage

    double sidelap = 20.0; // in percentage

    double altitude = 30.0; // in meters

    double speed = 5.0; // in meters per second

    // Inits the survey and camerainfo
    Survey survey = new Survey();

    CameraInfo cameraInfo = new CameraInfo();

    SurveyData surveyData = new SurveyData();

    // Inits a path variable
    Path path = new Path();

    // If opening an already made plan, the lat and lngs are loaded into this list
    LinkedList<LatLng> loadedLatLngs = new LinkedList<>();

    // Contains the lines that connect the corner of the map
    List<Polyline> polylineList = new LinkedList<>();

    // Defaults the current camera specs to the GoPro
    cameraSpecs specs = cameraSpecs.GOPRO_HERO4_BLACK;

    // Plan has not been saved to start out
    boolean planHasBeenSaved = false;

    // Specify whether a gimbal can be used or not
    boolean hasGimbal;

    private LocationCallback mLocationCallback;

    // The actual camera marker positions
    List<Marker> cameraMarkerLocations = new LinkedList<>();

    // The lines that connect the camera positions
    List<Polyline> cameraLocationLineList = new LinkedList<>();

    // Is set if the map has been cleared
    private boolean hasClearedMap = false;

    // Is set if the button has been clicked telling that the plan has finished.
    private boolean planHasFinished = false;

    static Plan plan;

    // Camera Location points
    List<CameraLocation> cameraLocations = new LinkedList<>();

    private HashMap getFileNames(){
        File plansFile = new File(plansDir.getAbsolutePath().concat("/"));
        final HashMap plansHash = new HashMap();
        File[] plansFiles = plansFile.listFiles(new FileFilter() {
            @Override
            public boolean accept(File f) {
                plansHash.put(f.getName(),true);
                return f.isDirectory();
            }
        });
        return plansHash;
    }

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

        // Creates a plans directory in the external sd card
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

        // Sets and updates overlap
        final EditText planID = findViewById(R.id.planID);
        final EditText overlapEditText = findViewById(R.id.overlap);
        overlapEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction( TextView v, int actionId, KeyEvent event ) {
                String overlapText = overlapEditText.getText().toString();
                NumberFormat numberFormat = NumberFormat.getInstance();
                try {
                    Number number = numberFormat.parse(overlapText);
                    overlap = number.doubleValue();
                    if (overlap > 99.0) {
                        overlap = 99.0;
                    }
                    if (overlap <= 0.0) {
                        overlap = 0.0;
                    }
                    if (planHasFinished && !hasClearedMap){
                        updateCamLocation();
                        createPlan();
                        if (planHasBeenSaved){
                            savePlan();
                        }
                    }
                    Log.d("overlapUpdate", String.valueOf(overlap));
                } catch (ParseException e) {
                    Log.d("number", "Number could not be converted");
                    overlap = 20.0;
                }
                return true;
            }
        });

        // Sets and updates sidelap
        final EditText sideLapEditText = findViewById(R.id.sidelap);
        sideLapEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction( TextView v, int actionId, KeyEvent event ) {
                String sidelapText = sideLapEditText.getText().toString();
                NumberFormat numberFormat = NumberFormat.getInstance();
                try {
                    Number number = numberFormat.parse(sidelapText);
                    sidelap = number.doubleValue();
                    if (sidelap > 99.0) {
                        sidelap = 99.0;
                    }
                    if (sidelap <= 0.0) {
                        sidelap = 0.0;
                    }
                    if (planHasFinished && !hasClearedMap){
                        updateCamLocation();
                        createPlan();
                        if (planHasBeenSaved){
                            savePlan();
                        }
                    }
                    Log.d("sidelapUpdate", String.valueOf(sidelap));
                } catch (ParseException e) {
                    Log.d("number", "Number could not be converted");
                    sidelap = 20.0;
                }
                return true;
            }
        });

        // Sets and updates altitude attribute
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
                    if (planHasFinished && !hasClearedMap){
                        updateCamLocation();
                        createPlan();
                        if (planHasBeenSaved){
                            savePlan();
                        }
                    }
                    Log.d("altitudeSet", Double.toString(altitude));
                } catch (ParseException e) {
                    altitude = 20.0;
                    e.printStackTrace();
                }
                return true;
            }
        });

        // Sets and updates speed attribute
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
                    if (planHasFinished && !hasClearedMap){
                        updateCamLocation();
                        createPlan();
                        if (planHasBeenSaved){
                            savePlan();
                        }
                    }
                    Log.d("speedSet", Double.toString(speed));

                } catch (ParseException e) {
                    speed = 5.0;
                    e.printStackTrace();
                }
                return true;
            }
        });

        // The data to be displayed
        List<String> cameraSpinnerArray =  new ArrayList<String>();
        cameraSpinnerArray.add("GoPro Hero 4 Black");
        cameraSpinnerArray.add("GoPro Hero 4 Silver");
        cameraSpinnerArray.add("Micasense 3");
        ArrayAdapter<String> cameraAdapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, cameraSpinnerArray);

        // Updates the camera specs
        cameraAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        final Spinner cameraSpinner = findViewById(R.id.cameraSpinner);
        cameraSpinner.setAdapter(cameraAdapter);
        cameraSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected( AdapterView<?> parent, View view, int position, long id ) {
                switch (parent.getItemAtPosition(position).toString()) {
                    case "GoPro Hero 4 Silver":
                        specs = cameraSpecs.GOPRO_HERO4_SILVER;
                        if (planHasFinished){
                            // Updates camera locations
                            updateCamLocation();
                            // Creates a plan
                            createPlan();
                            // Saves the plan
                            if (planHasBeenSaved){
                                savePlan();
                            }
                        }
                        break;

                    case "GoPro Hero 4 Black":
                        specs = cameraSpecs.GOPRO_HERO4_BLACK;
                        if (planHasFinished){
                            updateCamLocation();
                            createPlan();
                            if (planHasBeenSaved){
                                savePlan();
                            }
                        }
                        break;

                    case "Micasense 3":
                        specs = cameraSpecs.MICASENSE3;
                        if (planHasFinished){
                            updateCamLocation();
                            createPlan();
                            if (planHasBeenSaved){
                                savePlan();
                            }
                        }
                        break;

                    default:
                        specs = cameraSpecs.GOPRO_HERO4_BLACK;
                        if (planHasFinished){
                            updateCamLocation();
                            createPlan();
                            if (planHasBeenSaved){
                                savePlan();
                            }
                        }
                        break;
                }
            }

            @Override
            public void onNothingSelected( AdapterView<?> parent ) {

            }
        });

        // Determines if an already plan was opened
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            // Gets the plan name to open
            String planNameToOpen = extras.getString("planNameToOpen");

            // File pointer to the actual file to open
            File fileToOpen = new File(getExternalFilesDir(null).getAbsolutePath().concat("/Plans"), planNameToOpen.concat("/points.txt"));
            fileName = planNameToOpen;
            Log.d("openfile", "Opening " + fileToOpen.getAbsolutePath());
            int i = 0;
            // Processes the waypoints of the images
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

            // Processes the attributes of the file
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
                    speedEditText.setEnabled(false);
                    speedEditText.setText(Double.toString(speed));
                }
                if ((line = bufferedReader.readLine()) != null){
                    altitude = Double.parseDouble(line);
                    altitudeEditText.setEnabled(false);
                    altitudeEditText.setText(Double.toString(altitude));
                }
                if ((line = bufferedReader.readLine()) != null){
                    hasGimbal = Boolean.parseBoolean(line);
                    CheckBox gimbalCheckBox = findViewById(R.id.gimbalCheckBox);
                    gimbalCheckBox.setChecked(hasGimbal);
                    gimbalCheckBox.setClickable(false);
                }
                if ((line = bufferedReader.readLine()) != null){
                    overlap = Double.parseDouble(line);
                    overlapEditText.setEnabled(false);
                    overlapEditText.setText(Double.toString(overlap));
                }
                if ((line = bufferedReader.readLine()) != null){
                    sidelap = Double.parseDouble(line);
                    sideLapEditText.setEnabled(false);
                    sideLapEditText.setText(Double.toString(sidelap));
                }
                if ((line = bufferedReader.readLine()) != null){
                    String planName = line;
                    planID.setEnabled(false);
                    planID.setText(planName);
                }
                if ((line = bufferedReader.readLine()) != null){
                    String cameraName = line;
                    for (int j = 0; j < cameraSpinner.getAdapter().getCount(); j++){
                        Log.d("cameraSpinner", cameraSpinner.getItemAtPosition(j).toString());
                        Log.d("cameraName", cameraName);
                        if(cameraSpinner.getItemAtPosition(j).toString().equals(cameraName)){
                            Log.d("foundSpinner", cameraName);
                            cameraSpinner.setSelection(j);
                        }
                    }
                    cameraSpinner.setEnabled(false);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        CheckBox gimbalCheckBox = findViewById(R.id.gimbalCheckBox);

        // Sets if the gimbal box is checked or not, defaulted to false
        gimbalCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged( CompoundButton buttonView, boolean isChecked ) {
                if (isChecked){
                    hasGimbal = true;
                }
                else {
                    hasGimbal = false;
                }
            }
        });

        CheckBox footprintCheckBox = findViewById(R.id.footPrintCheckBox);

        // Sets whether the footprints are to be shown or not
        footprintCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged( CompoundButton buttonView, boolean isChecked ) {
                int cameraLocationCount = cameraLocations.size();
                // Makes the footprints visible
                if (isChecked) {
                    for (int i = 0; i < cameraLocationCount; i++){
                        cameraLocations.get(i).visiblePolygon();
                    }
                }
                // Makes the footprints invisible
                else{
                    for (int i = 0; i < cameraLocationCount; i++){
                        cameraLocations.get(i).invisiblePolygon();
                    }
                }
            }
        });

        // Sets the current location if possible
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    currentLocation = new LatLng(location.getLatitude(),location.getLongitude());
                }
            };
        };

        // Checks permissions to allow for allowing External Storage
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.d("storage", "Asking for storage permission");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }

        // Beginning of the outlier detection, commented out to save time
//        String testPic = "/sdcard/GOPR1920.JPG";
//        OutlierDetection outlierDetection = null;
//        ImageView imageView = findViewById(R.id.image);
//        imageView.setVisibility(View.VISIBLE);
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
//            outlierDetection = new OutlierDetection(testPic, getApplicationContext());
//            imageView.setImageBitmap(outlierDetection.showBitmap());
//        }
    }

    /**
     * Tries to save the plan name
     * @return True if plan name was saved, false otherwise
     */
    private boolean savePlanName(){
        // Assume the plan name cannot be saved
        boolean handled = false;

        // Creates a hashmap of the file names
        HashMap fileNames = getFileNames();

        // Gets the current plan name
        EditText planIdText = findViewById(R.id.planID);
        fileName = planIdText.getText().toString();

        Log.d("fileName", fileName);

        // Starts creation of the filename
        if (!fileName.isEmpty() && !fileNames.containsKey(fileName)) {
            planDir = new File(plansDir.getAbsolutePath(), fileName);
            Log.d("planDir", planDir.toString());
            points = new File(planDir.getAbsolutePath(), "points.txt");
            if (!planDir.exists()) {
                // Creates a plan directory
                Log.d("plansDir", "Plans directory doesn't exist, creating one.");
                if (planDir.mkdir()) {
                    Log.d("Create Dir", "Directory is created");
                }
            }
            // Directory already exists
            else {
                Log.d("plansDir", fileName + " directory already exists");
            }

            Log.d("file", "File is located at " + planDir.getAbsolutePath());

            // Sets the filewriter
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
            Log.d("planID", "Action received");
            handled = true;
        }
        return handled;
    }

    /**
     * Updates the camera locations if something has been edited
     */
    public void updateCamLocation(){
        for (Marker cameraLocation :
                cameraMarkerLocations) {
            cameraLocation.remove();
        }
        polygonList.removeFirst().remove();
        for (CameraLocation camLocation :
                cameraLocations) {
            camLocation.removeFootprint();
        }
        for (Polyline camLine: cameraLocationLineList) {
            camLine.remove();
        }
        Collection<Polyline> cameraLocationLines = cameraLocationLineList;
        polylineList.removeAll(cameraLocationLines);
    }

    class CameraLocation{

        private List<LatLng> footprintLatLng = new LinkedList<>();
        private Polygon footprintPolygon;
        private double horizView = (specs.getHorizontalFOV(altitude)) / 2;
        private double vertView = (specs.getVerticalFOV(altitude)) / 2;

        /**
         * Inits the footprint of the camera position
         * @param cameraLocation A camera location given as a LatLong
         */
        CameraLocation(LatLong cameraLocation){
            // Builds a square given the original camera location, since it will be the middle of the square
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(SphericalUtil.computeOffsetOrigin(new LatLng(cameraLocation.getLatitude(), cameraLocation.getLongitude()), horizView, 0));
            builder.include(SphericalUtil.computeOffsetOrigin(new LatLng(cameraLocation.getLatitude(), cameraLocation.getLongitude()), vertView, 90));
            builder.include(SphericalUtil.computeOffsetOrigin(new LatLng(cameraLocation.getLatitude(), cameraLocation.getLongitude()), horizView, 180));
            builder.include(SphericalUtil.computeOffsetOrigin(new LatLng(cameraLocation.getLatitude(), cameraLocation.getLongitude()), vertView, 270));
            LatLngBounds bounds = builder.build();
            double North = bounds.northeast.latitude;
            double East = bounds.northeast.longitude;
            double South = bounds.southwest.latitude;
            double West = bounds.southwest.longitude;
            LatLng northWest = new LatLng(North, West);
            LatLng southEast = new LatLng(South, East);
            this.footprintLatLng.add(bounds.northeast);
            this.footprintLatLng.add(southEast);
            this.footprintLatLng.add(bounds.southwest);
            this.footprintLatLng.add(northWest);

            // Adds the polygon to the map
            this.footprintPolygon = mMap.addPolygon(new PolygonOptions().addAll(footprintLatLng).fillColor(Color.argb(45,66, 235, 244)));

            // Sets the polygon invisible
            invisiblePolygon();
        }

        /**
         * Removes the footprint from the map
         */
        public void removeFootprint(){
            footprintPolygon.remove();
        }

        /**
         * Sets the polygon to invisible
         */
        public void invisiblePolygon(){
            this.footprintPolygon.setStrokeColor(Color.TRANSPARENT);
            this.footprintPolygon.setFillColor(Color.TRANSPARENT);
        }

        /**
         * Sets the polygon to visible
         */
        public void visiblePolygon(){
            this.footprintPolygon.setFillColor(Color.argb(45,66, 235, 244));
            this.footprintPolygon.setStrokeColor(Color.BLACK);
        }
    }

    /**
     * Clears the map of all objects
     */
    private void clearMap() {
        mMap.clear();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

    @Override
    public void onMapReady( GoogleMap googleMap ) {

        // Sets the map
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
        // Turns the window click listener on
        mMap.setOnInfoWindowClickListener(this);

        // Makes sure the map contains no points currently
        clearMap();

        // If a plan is opened, this adds the markers to the map
        for (LatLng photoPosition :
                loadedLatLngs) {
            mMap.addMarker(new MarkerOptions().position(photoPosition));
        }

        // Creates the line between all the points on the map
        polylineList.add(mMap.addPolyline(new PolylineOptions().addAll(loadedLatLngs)));

        // Generates a path between the points and sees what is the closest point to fly to.
        // Also sets the # of photos
        if(fileName != null){
            path = new Path();
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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

            path.createPath(loadedLatLngs);

            TextView estimatedPhotos = findViewById(R.id.estimated_number_of_photos);
            estimatedPhotos.setText("# of photos: " + path.getNumberOfPhotos());
        }
        final Button finishPlanButton = findViewById(R.id.finishPlan);
        finishPlanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick( View v ) {
                createPlan();
                finishPlanButton.setVisibility(View.GONE);
                planHasFinished = true;
            }
        });
    }

    /**
     * Creates a plan and does error checking
     */
    private void createPlan(){

        // If there is less than 4 markers a plan cannot be created
        if (numberOfMarkers < 4){
            AlertDialog.Builder noSavedPlan = new AlertDialog.Builder(MapsActivity.this);
            noSavedPlan.setMessage("Not enough corners.");
            noSavedPlan.setTitle("You need at least 4 corners to continue.");
            noSavedPlan.show();
            return;
        }

        // Inits the markers latitude and longitude list
        List<LatLng> markersLatLng = new LinkedList<>();

        // Fills the markers lat/lng
        for (Marker marker :
                markerLinkedList) {
            markersLatLng.add(marker.getPosition());
        }

        // A polygon is not on the map at this point
        if (polygonList.isEmpty()) {
            polygonList.add(mMap.addPolygon(new PolygonOptions().addAll(markersLatLng).fillColor(Color.argb(75, 255, 102, 102))));
        }
        // A polygon is on the map, and is removed
        else {
            polygonList.removeFirst();

            polygonList.add(mMap.addPolygon(new PolygonOptions().addAll(markersLatLng).fillColor(Color.argb(75, 255, 102, 102))));
        }
        if (currentLocation != null) {
            // Updates info
            updateCameraInfo();
            updateSurveyData();

            // Creates a polygon through dronekit
            org.droidplanner.services.android.impl.core.polygon.Polygon polygon = new org.droidplanner.services.android.impl.core.polygon.Polygon();

            // Inits the polygon corners
            LinkedList<LatLong> polygonCorners = new LinkedList<>();

            // Adds the polygon corners
            for (LatLng latLngBox :
                    markersLatLng) {
                polygonCorners.add(new LatLong(latLngBox.latitude,latLngBox.longitude));
            }
            polygon.addPoints(polygonCorners);

            // Sets the survey
            survey.setPolygonPoints(polygon.getPoints());

            // Gets the area in square meters
            Area areaSqMeters = polygon.getArea();

            // Sets the area in square meters
            survey.setPolygonArea(areaSqMeters.valueInSqMeters());

            // Gets the current LatLong
            LatLong currentLocationLatLong = new LatLong(currentLocation.latitude,currentLocation.longitude);

            // Inits the gridbuilder with the relevant data
            GridBuilder gridBuilder = new GridBuilder(polygon,surveyData,currentLocationLatLong);

            // TODO: Introduce a better path planning algorithm
            // Generates the grid and sets the camera locations
            try {
                Grid grid = gridBuilder.generate(false);
                survey.setGridPoints(grid.gridPoints);
                survey.setCameraLocations(grid.getCameraLocations());
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Sets the camera location list
            for (LatLong cameraLocation :
                    survey.getCameraLocations()) {
                cameraLocations.add(new CameraLocation(cameraLocation));
                cameraMarkerLocations.add(mMap.addMarker(new MarkerOptions().position(new LatLng(cameraLocation.getLatitude(), cameraLocation.getLongitude())).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))));
            }

            // Sets the path
            path = new Path(survey, speed, currentLocation, fileName, getExternalFilesDir(null).getAbsolutePath().concat("/Plans/" + fileName));

            // Convert LatLongs to LatLngs
            LatLong2LatLngs latLngsConverted = new LatLong2LatLngs(survey.getCameraLocations());

            // Adds a polyline through the camera points
            cameraLocationLineList.add(mMap.addPolyline(new PolylineOptions().addAll(latLngsConverted.getLatLngs())));

            // Gets and sets estiamted flight time
            int estimatedFlightTime = path.getEstimatedFlightTime();
            BigDecimal flightTime = BigDecimal.valueOf(estimatedFlightTime);
            int[] test = secondsToMinutesSeconds(flightTime);
            TextView estimatedFlightTimeTextView = findViewById(R.id.estimated_flight_time);
            estimatedFlightTimeTextView.setText("Estimated Flight Time: " + test[1] + ":" + test[2]);

            // Sets number of photos that will be taken
            TextView estimatedPhotos = findViewById(R.id.estimated_number_of_photos);
            estimatedPhotos.setText("Estimated # of photos: " + survey.getCameraCount());

            // Sets the area that will be covered
            TextView totalArea = findViewById(R.id.total_area);
            DecimalFormat df = new DecimalFormat();
            df.setMaximumFractionDigits(2);
            totalArea.setText("Total area: " + df.format(sqMeters2Acres(survey.getPolygonArea())) + " acres");
        }

        // The plan is created at this point and showing footprints will be allowed
        CheckBox showFootprints = findViewById(R.id.footPrintCheckBox);
        showFootprints.setVisibility(View.VISIBLE);
        // Doesn't allow for the map to be clicked again after the plan was created
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getView().setClickable(false);

    }

    /**
     * Updates the camera info
     */
    private void updateCameraInfo(){
        cameraInfo.focalLength = specs.focalLength;
        cameraInfo.isInLandscapeOrientation = true;
        cameraInfo.sidelap = sidelap;
        cameraInfo.overlap = overlap;
        cameraInfo.name = specs.cameraName;
        cameraInfo.sensorHeight = specs.sensorHeight;
        cameraInfo.sensorResolution =  specs.sensorResolution;
        cameraInfo.sensorWidth = specs.sensorWidth;
    }

    /**
     * Updates survey data
     */
    private void updateSurveyData(){
        surveyData.setAltitude(altitude);
        surveyData.setCameraInfo(cameraInfo);
    }

    /**
     * Create markers for the shape
     * @param latLng The position on the map that was clicked
     */
    @Override
    public void onMapClick( LatLng latLng ) {
        Bundle extras = getIntent().getExtras();

        // Creates markers on the map, determing whether or not to place ones that have been removed.
        if (extras == null || hasClearedMap){
            if (!removedMarkerList.isEmpty()) {
                Log.d("marker", "Creating marker number " + removedMarkerList.getLast().toString());
                markerLinkedList.add(mMap.addMarker(new MarkerOptions().position(latLng).draggable(false).title(removedMarkerList.removeLast().toString())));

                numberOfMarkers++;
            } else {
                Log.d("marker", "Creating marker number " + numberOfMarkers);
                markerLinkedList.add(mMap.addMarker(new MarkerOptions().position(latLng).draggable(false).title(Integer.toString(numberOfMarkers))));
                numberOfMarkers++;
            }
            // Allows a person to finish their plan if there is enough corners
            if (numberOfMarkers >= 4){
                Button finishPlanButton = findViewById(R.id.finishPlan);
                finishPlanButton.setVisibility(View.VISIBLE);
            }
        }

    }

    /**
     * Converts sqMeters to acres
     * @param sqMeters Sqaure meters
     * @return Acres
     */
    private double sqMeters2Acres(double sqMeters){
        return (0.00024710538146717 * sqMeters);
    }

    /**
     * Converts seconds to hours, minutes, and seconds
     * @param decimal In seconds
     * @return A int[3] containing hours, minutes, and seconds respectively
     */
    private int[] secondsToMinutesSeconds( BigDecimal decimal){
        // Converts the decimal value into pure seconds
        long longVal = decimal.longValue();

        // Get the number of hours by dividing by 60 minutes * 60 seconds
        int hours = (int) longVal / 3600;

        // Sets the remainder of minutes
        int remainder = (int) longVal - hours * 3600;

        // Get the minutes by dividing by how many seconds are in a minute
        int mins = remainder / 60;

        // Sets the remainder of seconds
        remainder = remainder - mins * 60;

        // Get the seconds
        int secs = remainder;

        int[] ints = {hours , mins , secs};

        return ints;

    }

    @Override
    public boolean onMarkerClick( Marker marker ) {
        Log.d("marker", "Marker " + marker.getTitle() + " was clicked." + "LatLng = " + marker.getPosition().toString());
        // Centers the camera on the marker clicked
        return false;
    }

    /**
     * Removes a marker if the info window is clicked
     * @param marker The marker that was clicked
     */
    @Override
    public void onInfoWindowClick( Marker marker ) {
        Log.d("marker", "Removing marker number " + marker.getTitle());

        // Removes the marker
        marker.remove();

        markerLinkedList.removeLast();

        // Adds it to the removed marker list, to be added back
        removedMarkerList.add(Integer.parseInt(marker.getTitle()));

        // Reduces the number of markers that are on the map
        numberOfMarkers--;

    }

    @Override
    public void onItemSelected( AdapterView<?> parent, View view, int position, long id ) {
        Log.d("spinner", parent.getItemAtPosition(position).toString());
        switch (parent.getItemAtPosition(position).toString()){
            case "Save Plan":
                // Not enough markers have been placed on the map yet to create a region
                if (numberOfMarkers < numberOfCorners){
                    AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                    builder.setMessage("You do not have enough points on the map.");
                    builder.setTitle("Please add more markers.");
                    builder.show();
                    break;
                }

                // Plan is not complete
                if (!planHasFinished){
                    AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                    builder.setMessage("A plan must be complete, in order to save.");
                    builder.setTitle("Please complete a plan");
                    builder.show();
                    break;
                }

                EditText planID = findViewById(R.id.planID);

                // No plan name was entered
                if (planID.getText().toString().equals("")){
                    AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                    builder.setMessage("A plan name must be entered, in order to save.");
                    builder.setTitle("Please enter a name");
                    builder.show();
                    break;
                }

                // Plan name already exists
                if(!savePlanName()){
                    AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                    builder.setMessage("Plan name already exists");
                    builder.setTitle("Please change the plan name.");
                    builder.show();
                    break;
                }
                // Proceeds to save the plan
                savePlan();
                break;

            case "Open Plans":

                // Prompts the user if they have not saved a plan and they are about to abandon it
                if (!planHasBeenSaved && (numberOfMarkers > 0)){
                    AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                    builder.setMessage("Do you want to save your plan before opening a plan?");    //set message

                    builder.setPositiveButton("Save", new DialogInterface.OnClickListener() { //when click on Save
                        public void onClick(DialogInterface dialog, int which) {
                            if (planHasFinished){
                                savePlan();
                            }
                            Intent activityOpenPlan = new Intent(MapsActivity.this, activity_open_plan.class);
                            MapsActivity.this.startActivity(activityOpenPlan);
                        }
                    }).setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {  //not removing items if cancel is done
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent activityOpenPlan = new Intent(MapsActivity.this, activity_open_plan.class);
                            MapsActivity.this.startActivity(activityOpenPlan);
                        }
                    }).show();  //show alert dialog
                    break;
                }

                // Opens all the plans that have been created
                Intent activityOpenPlan = new Intent(MapsActivity.this, activity_open_plan.class);
                MapsActivity.this.startActivity(activityOpenPlan);
                Log.d("plan", "Opening plan");
                break;

            case "Clear Map":

                // Clears the map of any paths or markers
                clearMap();

                // Resets the path
                path = new Path();

                // Resets the number of markers
                numberOfMarkers = 0;

                // Removes all of the poylgons and polylines
                polygonList.clear();
                polylineList.clear();

                // Clears the linked lists
                markerLinkedList.clear();
                removedMarkerList.clear();

                // Resets the filename
                fileName = null;


                // Resets flight time, number of photos, and total area
                TextView estTimeFlight = findViewById(R.id.estimated_flight_time);
                estTimeFlight.setText("Estimated Time: 0");
                TextView estPhotos = findViewById(R.id.estimated_number_of_photos);
                estPhotos.setText("# of photos: 0");
                TextView totalArea = findViewById(R.id.total_area);
                totalArea.setText("Total Area: 0 acres");

                // Allows the planID to be used
                planID = findViewById(R.id.planID);
                planID.setEnabled(true);
                planID.setText("");

                // Resets and reenables the parameters at the top of the app such as sidelap, overlap, altitude, speed, and the gimbal
                EditText overlapEditText = findViewById(R.id.overlap);
                overlapEditText.setEnabled(true);
                overlapEditText.setText("");
                EditText sidelapEditText = findViewById(R.id.sidelap);
                sidelapEditText.setEnabled(true);
                sidelapEditText.setText("");
                EditText altitudeEditText = findViewById(R.id.altitude);
                altitudeEditText.setEnabled(true);
                altitudeEditText.setText("");
                EditText speedEditText = findViewById(R.id.flightSpeed);
                speedEditText.setEnabled(true);
                speedEditText.setText("");
                CheckBox gimbalCheckBox = findViewById(R.id.gimbalCheckBox);
                gimbalCheckBox.setClickable(true);
                Spinner cameraSpinner = findViewById(R.id.cameraSpinner);
                cameraSpinner.setEnabled(true);
                overlap = 0;
                sidelap = 0;
                CheckBox footprintCheckBox = findViewById(R.id.footPrintCheckBox);
                footprintCheckBox.setVisibility(View.GONE);
                hasClearedMap = true;
                break;
            default:
                Log.d("plan","Nothing is chosen");
                break;
        }
    }

    @Override
    public void onNothingSelected( AdapterView<?> parent ) {
    }


    /**
     * Saves the plan to the specified plan name directory and the relevant information
     */
    public void savePlan(){

        // Sets the plan
        plan = new Plan(path.getNumberOfPhotos(),planDir.getAbsolutePath(),fileName);
        plan.latLongs = survey.getCameraLocations();

        // Sets the filewriter to the plan
        if (fileWriter != null){
            plan.writeToPlan(fileWriter);
        }

        Log.d("planDirPath", planDir.getAbsolutePath());

        // Creates the attributes file to store speed, altitude, hasGimbal, overlap, sidelap, planname, and camera specs
        File attributes = new File(getExternalFilesDir(null).getAbsolutePath(),"Plans/".concat(plan.planName).concat("/attributes.txt"));
        try {
            FileWriter attributeFileWriter = new FileWriter(attributes);
            attributeFileWriter.write(path.getEstimatedFlightTime() + "\n");
            DecimalFormat df = new DecimalFormat();
            df.setMaximumFractionDigits(2);
            attributeFileWriter.write(df.format(sqMeters2Acres(survey.getPolygonArea())) + "\n");
            attributeFileWriter.write(Double.toString(speed) + "\n");
            attributeFileWriter.write(Double.toString(altitude) + "\n");
            attributeFileWriter.write(Boolean.toString(hasGimbal)+"\n");
            attributeFileWriter.write(Double.toString(overlap) + "\n");
            attributeFileWriter.write(Double.toString(sidelap) + "\n");
            attributeFileWriter.write(plan.planName.concat("\n"));
            attributeFileWriter.write(specs.cameraName);
            attributeFileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // At this point the plan has been saved
        planHasBeenSaved = true;
    }

    /**
     * Connected to GoFly Button, sends an intent to the goFlyActivity
     * @param view OnClick view
     */
    public void goFlyActivity( View view ) {
        // Starts up the intent
        Intent GCS_3DR_Activity_Intent = new Intent(MapsActivity.this,GCS_3DR_Activity.class);
        // Checks if a filename exists
        if (fileName == null){
            AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
            builder.setMessage("Please enter a file name in order to fly");
            builder.setTitle("File name needed to fly");
            builder.show();
            return;
        }
        // Puts the plan name, since it should exist
        GCS_3DR_Activity_Intent.putExtra("planName",fileName);

        // Checks if the path is null and passes the horizontal and vertical size of points
        if (path != null){
            GCS_3DR_Activity_Intent.putExtra("horizSize", path.getHorizSize());
            GCS_3DR_Activity_Intent.putExtra("vertSize", path.getVertSize());

        }

        // Puts relevant information
        GCS_3DR_Activity_Intent.putExtra("hasGimbal", hasGimbal);
        GCS_3DR_Activity_Intent.putExtra("altitude", altitude);
        GCS_3DR_Activity_Intent.putExtra("speed", speed);
        GCS_3DR_Activity_Intent.putExtra("survey", survey);

        // Starts up the next activity
        MapsActivity.this.startActivity(GCS_3DR_Activity_Intent);
    }
}
