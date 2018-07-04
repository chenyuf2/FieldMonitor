package daslab.com.fieldmonitoringv2;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class Plan{
    int numberOfWaypoints;
    String file;
    String planName;
    List<Marker> markerLinkedList = new LinkedList<>();

    public Plan(int numberOfWaypoints, String file, String planName){
        this.numberOfWaypoints = numberOfWaypoints;
        this.file = file;
        this.planName = planName;
    }

    public int getNumberOfWaypoints() {
        return numberOfWaypoints;
    }

    public String getPlanName() {
        return planName;
    }

    public String getFile() {
        return file;
    }

    public List<Marker> getMarkerLinkedList() {
        return markerLinkedList;
    }

    public void writeToPlan(FileWriter fileWriter){
        try {
            Marker[] markerArray = markerLinkedList.toArray(new Marker[markerLinkedList.size()]);
            for (int i = 0; i < markerArray.length ; i++){
                fileWriter.write(Double.toString(markerArray[i].getPosition().latitude));
                fileWriter.write("\n");

                fileWriter.write(Double.toString(markerArray[i].getPosition().longitude));
                fileWriter.write("\n");
            }
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<LatLng> readFromPlan(){
        List<LatLng> latLngList = new LinkedList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line, line1;
            while ((line = br.readLine()) != null &&
                    (line1 = br.readLine()) != null) {
                Log.d("line", line + " & " + line1);
                latLngList.add(new LatLng(Double.parseDouble(line), Double.parseDouble(line1)));
            }
            br.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return latLngList;
    }

}
