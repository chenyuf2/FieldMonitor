package daslab.com.fieldmonitoringv2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.android.client.apis.GimbalApi;
import com.o3dr.android.client.apis.MissionApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.android.client.apis.solo.SoloCameraApi;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.mission.Mission;
import com.o3dr.services.android.lib.drone.mission.item.command.ChangeSpeed;
import com.o3dr.services.android.lib.drone.mission.item.command.ReturnToLaunch;
import com.o3dr.services.android.lib.drone.mission.item.command.YawCondition;
import com.o3dr.services.android.lib.drone.mission.item.spatial.Waypoint;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Attitude;
import com.o3dr.services.android.lib.drone.property.Battery;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.Home;
import com.o3dr.services.android.lib.drone.property.Speed;
import com.o3dr.services.android.lib.drone.property.State;

import com.o3dr.services.android.lib.model.AbstractCommandListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;

public class GCS_3DR_Activity extends AppCompatActivity implements DroneListener, TowerListener, OnMapReadyCallback {

    private ControlTower controlTower;

    private Drone drone;
    private final Handler handler = new Handler();

    private String videoTag = "videoTag";

    private Button startVideoStream;

    private Button stopVideoStream;

    private TextureView videoView;

    LinkedList<LatLongAlt> latLongAltArrayList = new LinkedList<>();
    LinkedList<Waypoint> waypoints = new LinkedList<>();

    VehicleApi vehicleApi;
    ControlApi controlApi;

    Waypoint home = new Waypoint();

    Double altitude = 30.0;
    double speed = 5.0;

    boolean alertedOnce = false;

    double flyHomeBatteryPercentage = 25;

    String planName;

    // Is set to false once the drone mission has been set, indicating it has happened.
    boolean hasntHappened = true;

    boolean hasGimbal;

    // On activity start, create a new drone, control tower, and set the ControlApi
    @Override
    public void onStart() {
        super.onStart();
        this.controlTower.connect(this);
        this.drone = new Drone(this);
        controlApi = ControlApi.getApi(this.drone);

    }

    // On activity stop, disconnect the drone, unregister the drone, and disconnect the tower
    @Override
    public void onStop() {
        super.onStop();
        if (this.drone.isConnected()) {
            this.drone.disconnect();
            // Updates the drone is connected button
            updateConnectedButton(false);
        }
        this.controlTower.unregisterDrone(this.drone);
        this.controlTower.disconnect();
    }

    // On tower connected, register the drone to the control tower
    @Override
    public void onTowerConnected() {
        drone.unregisterDroneListener(this);
        controlTower.registerDrone(drone, handler);
        this.drone.registerDroneListener(this);
    }

    @Override
    public void onTowerDisconnected() {

    }


    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        // Forces the tablet into landscape mode
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        setContentView(R.layout.activity_gcs_3_dr_);

        // Sets the map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        // Sets the control tower to this application
        this.controlTower = new ControlTower(getApplicationContext());
        final Button arm = findViewById(R.id.btnArmTakeOff);
        arm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick( View v ) {
                updateArmedButton();
                if (drone.isConnected()){
                    vehicleApi = VehicleApi.getApi(drone);
                    vehicleApi.arm(true,false, new AbstractCommandListener() {
                        @Override
                        public void onSuccess() {
                        }

                        @Override
                        public void onError( int executionError ) {
                            Log.d("arming","Error arming " + executionError);
                        }

                        @Override
                        public void onTimeout() {
                            Log.d("arming","Timeout while arming");
                        }
                    });
                }

            }
        });

        Button connectButton = findViewById(R.id.btnConnect);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick( View v ) {
                startConnect();
            }
        });
        Bundle extras = getIntent().getExtras();
        if(extras !=null) {
            planName = extras.getString("planName");
            altitude = extras.getDouble("altitude");
            speed = extras.getDouble("speed");
            hasGimbal = extras.getBoolean("hasGimbal");
            Log.d("planName", planName);
            File currentFileWaypoints = new File(getExternalFilesDir(null).getAbsolutePath(),"Plans/".concat(planName).concat("/points.txt"));
            int i = 0;
            try {
                Log.d("waypoints", "Getting waypoints from " + currentFileWaypoints.getAbsolutePath());
                BufferedReader br = new BufferedReader(new FileReader(currentFileWaypoints));
                String line, line1;
                while ((line = br.readLine()) != null &&
                        (line1 = br.readLine()) != null) {
                    latLongAltArrayList.add(i,new LatLongAlt(new LatLong(Double.parseDouble(line),Double.parseDouble(line1)),altitude));
                    Waypoint waypoint = new Waypoint();
                    waypoint.setCoordinate(latLongAltArrayList.get(i));
                    waypoints.add(i, waypoint);
                    i++;
                }
                br.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }

        }
        Button takeoff = findViewById(R.id.takeoff);
        takeoff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick( View v ) {
                controlApi.takeoff(altitude, new AbstractCommandListener() {
                    @Override
                    public void onSuccess() {
                    }

                    @Override
                    public void onError( int executionError ) {
                        Log.d("takeoff", "Takeoff error " + executionError);

                    }

                    @Override
                    public void onTimeout() {
                    }
                });
            }
        });

        videoView = (TextureView) findViewById(R.id.video_content);
        videoView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable( SurfaceTexture surface, int width, int height) {
                startVideoStream.setEnabled(true);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                startVideoStream.setEnabled(false);
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });
        startVideoStream = (Button) findViewById(R.id.start_video_stream);
        startVideoStream.setEnabled(false);
        startVideoStream.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startVideoStream(new Surface(videoView.getSurfaceTexture()));
            }
        });

        stopVideoStream = (Button) findViewById(R.id.stop_video_stream);
        stopVideoStream.setEnabled(false);
        stopVideoStream.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopVideoStream();
            }
        });
    }

    // Initiates the video stream
    private void startVideoStream(Surface videoSurface) {
        SoloCameraApi.getApi(drone).startVideoStream(videoSurface, videoTag, false, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                if (stopVideoStream != null)
                    stopVideoStream.setEnabled(true);

                if (startVideoStream != null)
                    startVideoStream.setEnabled(false);
            }

            @Override
            public void onError(int executionError) {
                alertUser("Error while starting the video stream: " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("Timed out while attempting to start the video stream.");
            }
        });
    }

    // Saves a bitmap image to the internal storage.
    // For saving an image from the video stream
    private String saveToInternalStorage(Bitmap bitmapImage, int photoNumber){
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        // path to /data/data/yourapp/app_data/imageDir
        File directory = cw.getDir("photos", Context.MODE_PRIVATE);
        // Create imageDir
        File mypath=new File(directory,"photo" + photoNumber + ".png");

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            Log.d("photo", "Photo saved");
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return directory.getAbsolutePath();
    }

    // Stops the video stream
    private void stopVideoStream() {
        SoloCameraApi.getApi(drone).stopVideoStream(videoTag, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                if (stopVideoStream != null)
                    stopVideoStream.setEnabled(false);

                if (startVideoStream != null)
                    startVideoStream.setEnabled(true);
            }

            @Override
            public void onError(int executionError) {
            }

            @Override
            public void onTimeout() {
            }
        });
    }

    LinkedList<Circle> circleLinkedList = new LinkedList<>();

    private void updateDronePosition(LatLong currentPos){
        if (circleLinkedList.size() == 0){
            circleLinkedList.add(mMap.addCircle(new CircleOptions().center(new LatLng(currentPos.getLatitude(),currentPos.getLongitude())).radius(5).strokeColor(Color.BLACK)));
        }
        else{
            circleLinkedList.removeFirst().remove();
            circleLinkedList.add(mMap.addCircle(new CircleOptions().center(new LatLng(currentPos.getLatitude(),currentPos.getLongitude())).radius(5).strokeColor(Color.BLACK)));
        }
    }

    // Sets the Mission
    public void setMission(){
        MissionApi missionApiTest = MissionApi.getApi(drone);
        Mission mission = new Mission();
        ChangeSpeed changeSpeed = new ChangeSpeed();
        changeSpeed.setSpeed(speed);
        mission.addMissionItem(changeSpeed);
        YawCondition yawCondition = new YawCondition();
        yawCondition.setAngle(0);
        mission.addMissionItem(yawCondition);
        int i = 2;
        for (Waypoint waypoint1:
                waypoints) {
            mission.addMissionItem(i, waypoint1);
            Log.d("mission", waypoint1.toString());
            i++;

        }
        ReturnToLaunch rtl = new ReturnToLaunch();
        rtl.setReturnAltitude(0.0);
        mission.addMissionItem(rtl);
        i++;
        missionApiTest.setMission(mission,true);
    }

    public void setGimbal(){
        Log.d("gimbal", "attempting to start gimbal control");
        final GimbalApi gimbalApi = GimbalApi.getApi(drone);
        final GimbalApi.GimbalOrientationListener orientationListener = new GimbalApi.GimbalOrientationListener() {
            @Override
            public void onGimbalOrientationUpdate( GimbalApi.GimbalOrientation orientation ) {
                gimbalApi.updateGimbalOrientation(orientation, new GimbalApi.GimbalOrientationListener() {
                    @Override
                    public void onGimbalOrientationUpdate( GimbalApi.GimbalOrientation orientation ) {
                        Log.d("orientation", "Updated");
                    }

                    @Override
                    public void onGimbalOrientationCommandError( int error ) {
                    }
                });
            }

            @Override
            public void onGimbalOrientationCommandError( int error ) {

            }
        };
        gimbalApi.startGimbalControl(orientationListener);
        // Sets the gimbal orientation to face directly down
        gimbalApi.updateGimbalOrientation(-90f,0.0f,0.0f,orientationListener);
    }

    @Override
    public void onDroneEvent( String event, Bundle extras ) {
        switch (event) {
            case AttributeEvent.STATE_CONNECTED:
                // Once the drone is connected, allows for the arm button to be pressed
                Button armButton = findViewById(R.id.btnArmTakeOff);
                armButton.setVisibility(View.VISIBLE);
                // Updates the connected button
                updateConnectedButton(this.drone.isConnected());
                if (hasGimbal){
                    setGimbal();
                }
                setMission();
                break;
                
            case AttributeEvent.STATE_DISCONNECTED:
                // If drone is disconnected, alert the user and update the button
                alertUser("Drone Disconnected");
                updateConnectedButton(this.drone.isConnected());
                break;

            case AttributeEvent.ALTITUDE_UPDATED:
                // Updates the altitude
                Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
                TextView altitudeTextView = findViewById(R.id.currentAltitude);
                altitudeTextView.setText("Altitude: " + String.format("%3.1f", droneAltitude.getAltitude()) + "meters");

                // If the drone is within a .5 meter height of the altitude wanted start the mission
                if (droneAltitude.getAltitude() - altitude > -1 && hasntHappened){
                    Log.d("starting mission", "attempting");
                    hasntHappened = false;
                    startMission();
                }
                break;

            case AttributeEvent.BATTERY_UPDATED:
                // Updates the battery left
                Battery droneBattery = this.drone.getAttribute(AttributeType.BATTERY);
                TextView droneBatteryTextView = findViewById(R.id.batteryLeft);
                droneBatteryTextView.setText("Battery: " + Double.toString(droneBattery.getBatteryRemain()) + "%");

                // Warns the user if the drone goes below 30% battery
                if (droneBattery.getBatteryRemain() < 30.0){
                    alertUser("Your battery is below 30%!");
                }
                // TODO: Have the user take control of the drone by disconnecting
                if (droneBattery.getBatteryRemain() <= flyHomeBatteryPercentage && !alertedOnce){
                    alertedOnce = true;
                    // TODO: Test this feature on a larger field
//                    VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_RTL, new AbstractCommandListener() {
//                        @Override
//                        public void onSuccess() {
//                            Log.d("ChangeVehicleMode", "Vehicle mode change successful.");
//                        }
//                        @Override
//                        public void onError(int executionError) {
//                            Log.d("ChangeVehicleMode", "Vehicle mode change fail.");
//                        }
//
//                        @Override
//                        public void onTimeout() {
//                            Log.d("ChangeVehicleMode", "Vehicle mode change timed out.");
//                        }
//                    });
                    alertUser("Your battery is below flyHome, please quit the app and take manual control.");
                    drone.disconnect();
                    updateConnectedButton(false);
                }
                break;

            case AttributeEvent.SPEED_UPDATED:
                // Updates the speed attribute
                final Speed droneSpeed = this.drone.getAttribute(AttributeType.SPEED);
                TextView droneSpeedTextView = findViewById(R.id.currentSpeed);
                droneSpeedTextView.setText("Speed: " + String.format("%3.1f", droneSpeed.getGroundSpeed()) + "m/s");
                break;

            case AttributeEvent.GPS_POSITION:
                Gps gpsPos = this.drone.getAttribute(AttributeType.GPS);
                if (gpsPos != null && gpsPos.isValid()){
                    // TODO: Test this feature
                    updateDronePosition(gpsPos.getPosition());
                }
                break;

            case AttributeEvent.HOME_UPDATED:
                // Updates the home position
                Home droneHomeTest = this.drone.getAttribute(AttributeType.HOME);
                home.setCoordinate(droneHomeTest.getCoordinate());
                Log.d("droneHomeTest", droneHomeTest.toString());
                break;

            case AttributeEvent.MISSION_ITEM_UPDATED:
                // Takes a photo at the mission item and updates the log to show what position the drone is currently in
                if (numberOfPhotosTaken > 1 && numberOfPhotosTaken < (waypoints.size() + 2)){
                    Log.d("photoSpot", "attemping to take photo");
                    // Gets the view from the video and captures it for processing
                    //Bitmap bitmap = videoView.getBitmap();
                    //saveToInternalStorage(bitmap, actualPhotosTaken);
                    actualPhotosTaken++;
                    TextView photosTakenTextView = findViewById(R.id.currentPhotosTaken);
                    photosTakenTextView.setText("# Of Photos Taken: " + actualPhotosTaken);
                    // TODO: Try to take photo on GoPro and the tablet
                    SoloCameraApi soloCameraApi = SoloCameraApi.getApi(this.drone);
                    soloCameraApi.takePhoto(null);

                    // Log photo position and drone position
                    final double[] droneYaw = { 0.0 };
                    final double[] dronePitch = { 0.0 };
                    final double[] droneRoll = { 0.0 };
                    final boolean[] executedOnce = {false};
                    if (hasGimbal){
                        Log.d("hasGimbalOr", "true");
                        GimbalApi.GimbalOrientationListener orientationListener = new GimbalApi.GimbalOrientationListener() {
                            @Override
                            public void onGimbalOrientationUpdate( GimbalApi.GimbalOrientation orientation ) {
                                if (!executedOnce[0]){
                                    dronePitch[0] = orientation.getPitch();
                                    droneYaw[0] = orientation.getYaw();
                                    droneRoll[0] = orientation.getRoll();
                                    executedOnce[0] = true;
                                    try {
                                        FileWriter fileWriter = logDroneStats();

                                        Log.d("pitchWrite", Double.toString(dronePitch[0]));
                                        Log.d("yawWrite", Double.toString(droneYaw[0]));
                                        Log.d("rollWrite", Double.toString(droneRoll[0]));

                                        // Gimbal Yaw
                                        fileWriter.write("Gimbal Yaw " + droneYaw[0] + "\n");

                                        // Gimbal Roll
                                        fileWriter.write("Gimbal Roll " + droneRoll[0] + "\n");

                                        // Gimbal Pitch
                                        fileWriter.write("Gimbal Pitch " + dronePitch[0] + "\n");
                                        fileWriter.close();

                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }

                            }

                            @Override
                            public void onGimbalOrientationCommandError( int error ) {
                                Log.d("gimbal", "orientation error " + error);
                            }
                        };
                        GimbalApi.getApi(drone).startGimbalControl(orientationListener);
                        GimbalApi.getApi(drone).stopGimbalControl(orientationListener);
                    }
                    else {
                        try {
                            logDroneStats();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }
                Log.d("missionItem", "updated");
                numberOfPhotosTaken++;
                break;
            default:
                break;
        }
    }

    private FileWriter logDroneStats() throws IOException {
            @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy-H-m-s");
            Calendar c = Calendar.getInstance();
            String date = sdf.format(c.getTime());
            File currentLog = new File(getExternalFilesDir(null).getAbsolutePath(),"Plans/".concat(planName).concat("/log").concat(date).concat(".txt"));
            Speed currentSpeed;
            FileWriter fileWriter = new FileWriter(currentLog);
            final Gps droneGps = drone.getAttribute(AttributeType.GPS);
            final Altitude altitude = drone.getAttribute(AttributeType.ALTITUDE);
            // Photo #
            fileWriter.write("Photo #" + actualPhotosTaken + "\n");

            // Drone Lat and Lng
            fileWriter.write(droneGps.getPosition() + "\n");

            // Drone Altitude
            fileWriter.write("Alt: " + altitude + "\n");

            // Drone Speed
            currentSpeed = new Speed();
            fileWriter.write("Speed: " + Double.toString(currentSpeed.getAirSpeed()).concat("\n"));

            // Drone Yaw
            YawCondition currentYaw = new YawCondition();
            fileWriter.write("Drone Yaw: " + currentYaw.getAngle() + "\n");

            // Drone Roll
            Attitude currentAttitude = new Attitude();
            fileWriter.write("Drone Roll: " + currentAttitude.getRoll() + "\n");

            // Drone Pitch
            fileWriter.write("Drone Pitch: " + currentAttitude.getPitch()  + "\n");

            return fileWriter;
    }

    private void updateArmedButton() {
        Button takeoffButton = findViewById(R.id.takeoff);
        State vehicleState = drone.getAttribute(AttributeType.STATE);
        Log.d("vehiclestatearm", Boolean.toString(vehicleState.isArmed()));
        takeoffButton.setVisibility(View.VISIBLE);
    }

    int numberOfPhotosTaken = 0;

    int actualPhotosTaken = 0;

    private void startMission(){
        final MissionApi missionApi = MissionApi.getApi(this.drone);
        missionApi.setMissionSpeed(4.0f,null);
        missionApi.startMission(true, false, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                Log.d("mission", "Started");
            }

            @Override
            public void onError( int executionError ) {
                Log.d("mission", "Error " + executionError);

            }

            @Override
            public void onTimeout() {
                Log.d("mission", "Timeout");

            }
        });
    }

    @Override
    public void onDroneServiceInterrupted( String errorMsg ) {

    }

    protected void alertUser(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    protected void updateConnectedButton(Boolean isConnected) {
        Button connectButton = findViewById(R.id.btnConnect);
        if (isConnected) {
            connectButton.setText(R.string.disconnect);
        } else {
            connectButton.setText(R.string.connect);
        }
    }

    public void startConnect() {
        Log.d("drone", "trying to connect");
        if (this.drone.isConnected()) {
            this.drone.disconnect();
        }
        else{
            ConnectionParameter connectionParams = ConnectionParameter.newUdpConnection(14550, null);
            this.drone.connect(connectionParams);
        }

    }

    GoogleMap mMap;

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
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<android.location.Location>() {
            @Override
            public void onSuccess( android.location.Location location ) {
                if (location == null) {
                    Log.d("location", "Current Location is null");
                } else {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 17.5f));
                }
            }
        });
        LinkedList<LatLng> polyLineList = new LinkedList<>();
        for (Waypoint waypoint :
                waypoints) {
            LatLng point = new LatLng(waypoint.getCoordinate().getLatitude(),waypoint.getCoordinate().getLongitude());
            polyLineList.add(point);
            mMap.addMarker(new MarkerOptions().position(point));
        }
        mMap.addPolyline(new PolylineOptions().addAll(polyLineList));
    }
}
