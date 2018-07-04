package daslab.com.fieldmonitoringv2;

import android.Manifest;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
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

import com.MAVLink.MAVLinkPacket;
import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.enums.GOPRO_MODEL;
import com.MAVLink.enums.MAV_CMD;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.MavlinkObserver;
import com.o3dr.android.client.apis.CalibrationApi;
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.android.client.apis.ExperimentalApi;
import com.o3dr.android.client.apis.GimbalApi;
import com.o3dr.android.client.apis.MissionApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.android.client.apis.solo.SoloCameraApi;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.android.client.utils.FileUtils;
import com.o3dr.android.client.utils.data.tlog.TLogParser;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.action.ControlActions;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeEventExtra;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.attribute.error.ErrorType;
import com.o3dr.services.android.lib.drone.companion.solo.SoloAttributes;
import com.o3dr.services.android.lib.drone.companion.solo.SoloState;
import com.o3dr.services.android.lib.drone.companion.solo.tlv.SoloGoproConstants;
import com.o3dr.services.android.lib.drone.companion.solo.tlv.SoloGoproRequestState;
import com.o3dr.services.android.lib.drone.companion.solo.tlv.SoloGoproSetExtendedRequest;
import com.o3dr.services.android.lib.drone.companion.solo.tlv.SoloGoproSetRequest;
import com.o3dr.services.android.lib.drone.companion.solo.tlv.SoloGoproState;
import com.o3dr.services.android.lib.drone.companion.solo.tlv.TLVPacket;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.mission.Mission;
import com.o3dr.services.android.lib.drone.mission.item.MissionItem;
import com.o3dr.services.android.lib.drone.mission.item.command.CameraTrigger;
import com.o3dr.services.android.lib.drone.mission.item.command.ChangeSpeed;
import com.o3dr.services.android.lib.drone.mission.item.command.ReturnToLaunch;
import com.o3dr.services.android.lib.drone.mission.item.command.YawCondition;
import com.o3dr.services.android.lib.drone.mission.item.complex.Survey;
import com.o3dr.services.android.lib.drone.mission.item.spatial.Land;
import com.o3dr.services.android.lib.drone.mission.item.spatial.RegionOfInterest;
import com.o3dr.services.android.lib.drone.mission.item.spatial.Waypoint;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Attitude;
import com.o3dr.services.android.lib.drone.property.Battery;
import com.o3dr.services.android.lib.drone.property.DroneAttribute;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.Home;
import com.o3dr.services.android.lib.drone.property.Speed;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.Type;

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
    private int droneType = Type.TYPE_COPTER;
    private final Handler handler = new Handler();

    private String videoTag = "testvideotag";

    private Button startVideoStream;

    private Button stopVideoStream;

    boolean missionFinished = false;

    private TextureView videoView;

    LinkedList<LatLongAlt> latLongAltArrayList = new LinkedList<>();
    LinkedList<Waypoint> waypoints = new LinkedList<>();

    String currentFile = "";

    VehicleApi vehicleApi;
    //MissionApi missionApi;
    ControlApi controlApi;

    boolean hasReachedAltitude = false;

    private int vertSize = 0;
    private int horizSize = 0;

    Waypoint home = new Waypoint();

    Double altitude = 0.0;

    boolean missionStarted = false;

    Home droneHome;

    double flyHomeBatteryPercentage = 25;

    String planName;

    @Override
    public void onStart() {
        super.onStart();
        this.controlTower.connect(this);
        this.drone = new Drone(this);
        controlApi = ControlApi.getApi(this.drone);

    }

    @Override
    public void onStop() {
        super.onStop();
        if (this.drone.isConnected()) {
            this.drone.disconnect();
            updateConnectedButton(false);
        }
        this.controlTower.unregisterDrone(this.drone);
        this.controlTower.disconnect();
    }

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
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_gcs_3_dr_);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
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
            Log.d("planName", planName);
            horizSize = extras.getInt("horizSize");
            horizSize = extras.getInt("vertSize");
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

    private void setMission(){
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
                        Log.d("orientation", "Error " + error);
                    }
                });
            }

            @Override
            public void onGimbalOrientationCommandError( int error ) {

            }
        };
        gimbalApi.startGimbalControl(orientationListener);
        gimbalApi.updateGimbalOrientation(-90f,0.0f,0.0f,orientationListener);
        Log.d("drone", "Arming drone");
        Mission mission = new Mission();
        ChangeSpeed changeSpeed = new ChangeSpeed();
        changeSpeed.setSpeed(4.0);
        mission.addMissionItem(0, changeSpeed);
        YawCondition yawCondition = new YawCondition();
        yawCondition.setAngle(0.0);
        mission.addMissionItem(yawCondition);
        int i = 2;
        for (Waypoint waypointTest :
                waypoints) {
            Log.d("waypointtest", waypointTest.toString());
        }
        for (Waypoint waypoint1:
                waypoints) {
            mission.addMissionItem(i, waypoint1);
            Log.d("mission", waypoint1.toString());
            i++;

        }
        Waypoint home = new Waypoint();
        droneHome.getCoordinate().setAltitude(0.0);
        home.setCoordinate(droneHome.getCoordinate());
        mission.addMissionItem(i, home);
        i++;
        Land land = new Land();
        mission.addMissionItem(i, land);
        MissionApi missionApi = MissionApi.getApi(drone);
        missionApi.setMission(mission,true);
    }

    LinkedList<Circle> circleLinkedList = new LinkedList<>();

    private void updateDronePosition(LatLong currentPos){
        if (circleLinkedList.size() == 0){
            circleLinkedList.add(mMap.addCircle(new CircleOptions().center(new LatLng(currentPos.getLatitude(),currentPos.getLongitude()))));
        }
        else{
            circleLinkedList.removeFirst().remove();
            circleLinkedList.add(mMap.addCircle(new CircleOptions().center(new LatLng(currentPos.getLatitude(),currentPos.getLongitude()))));
        }
    }

    @Override
    public void onDroneEvent( String event, Bundle extras ) {
        switch (event) {
            case AttributeEvent.STATE_CONNECTED:
                alertUser("Drone Connected");
                Button armButton = findViewById(R.id.btnArmTakeOff);
                armButton.setVisibility(View.VISIBLE);
                updateConnectedButton(this.drone.isConnected());
                break;
                
            case AttributeEvent.STATE_DISCONNECTED:
                alertUser("Drone Disconnected");
                updateConnectedButton(this.drone.isConnected());
                break;
            case AttributeEvent.ALTITUDE_UPDATED:
                Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
                TextView altitudeTextView = findViewById(R.id.currentAltitude);
                altitudeTextView.setText("Altitude: " + String.format("%3.1f", droneAltitude.getAltitude()) + "meters");
                if (droneAltitude.getAltitude() - altitude > -1){
                    startMission();
                }
                break;

            case AttributeEvent.BATTERY_UPDATED:
                Battery droneBattery = this.drone.getAttribute(AttributeType.BATTERY);
                TextView droneBatteryTextView = findViewById(R.id.batteryLeft);
                droneBatteryTextView.setText("Battery: " + Double.toString(droneBattery.getBatteryRemain()) + "%");
                if (droneBattery.getBatteryRemain() < 30.0){
                    alertUser("Your battery is below 30%!");
                }
                if (droneBattery.getBatteryRemain() <= flyHomeBatteryPercentage){
                    alertUser("Your battery is below flyHome, please quit the app and take manual control.");
                    drone.disconnect();
                    updateConnectedButton(false);
                }
                break;
            case AttributeEvent.SPEED_UPDATED:
                Speed droneSpeed = this.drone.getAttribute(AttributeType.SPEED);
                TextView droneSpeedTextView = findViewById(R.id.currentSpeed);
                droneSpeedTextView.setText("Speed: " + String.format("%3.1f", droneSpeed.getGroundSpeed()) + "m/s");
                break;
            case AttributeEvent.HOME_UPDATED:
                droneHome = this.drone.getAttribute(AttributeType.HOME);
                home.setCoordinate(droneHome.getCoordinate());
                break;

            case AttributeEvent.MISSION_ITEM_UPDATED:
                if (numberOfPhotosTaken > 1 && numberOfPhotosTaken < (waypoints.size() + 2)){
                    Bitmap bitmap = videoView.getBitmap();
                    saveToInternalStorage(bitmap, actualPhotosTaken);
                    actualPhotosTaken++;
                    TextView photosTakenTextView = findViewById(R.id.currentPhotosTaken);
                    photosTakenTextView.setText("# Of Photos Taken: " + actualPhotosTaken);
                    // TODO: Try to take photo on GoPro and the tablet
//                    SoloCameraApi soloCameraApi = SoloCameraApi.getApi(this.drone);
//                    soloCameraApi.takePhoto(null);
                    // Log photo position and drone position
                    final double[] droneYaw = { 0.0 };
                    final double[] dronePitch = { 0.0 };
                    final double[] droneRoll = { 0.0 };
                    final boolean[] executedOnce = {false};
                    GimbalApi.GimbalOrientationListener orientationListener = new GimbalApi.GimbalOrientationListener() {
                        @Override
                        public void onGimbalOrientationUpdate( GimbalApi.GimbalOrientation orientation ) {
                            if (!executedOnce[0]){
                                dronePitch[0] = orientation.getPitch();
                                droneYaw[0] = orientation.getYaw();
                                droneRoll[0] = orientation.getRoll();
                                executedOnce[0] = true;
                                try {
                                    SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy-H-m-s");
                                    Calendar c = Calendar.getInstance();
                                    String date = sdf.format(c.getTime());
                                    File currentLog = new File(getExternalFilesDir(null).getAbsolutePath(),"Plans/".concat(planName).concat("/log").concat(date).concat(".txt"));
                                    Speed currentSpeed = new Speed();
                                    FileWriter fileWriter = new FileWriter(currentLog);
                                    final Gps droneGps = drone.getAttribute(AttributeType.GPS);
                                    final Altitude altitude = drone.getAttribute(AttributeType.ALTITUDE);
                                    // Photo #
                                    fileWriter.write("Photo #" + actualPhotosTaken + "\n");

                                    // Drone Lat and Lng
                                    fileWriter.write("LatLng: " + droneGps.getPosition() + "\n");

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
                Log.d("missionItem", "updated");
                numberOfPhotosTaken++;
                break;
            default:
                break;
        }
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
        missionApi.startMission(true, true, new AbstractCommandListener() {
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
        Button connectButton = (Button)findViewById(R.id.btnConnect);
        if (isConnected) {
            connectButton.setText("Disconnect");
        } else {
            connectButton.setText("Connect");
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