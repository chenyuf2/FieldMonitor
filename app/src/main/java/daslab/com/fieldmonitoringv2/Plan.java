package daslab.com.fieldmonitoringv2;

import com.google.android.gms.maps.model.Marker;

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class Plan{
    private int numberOfWaypoints;
    String file;
    String planName;
    List<Marker> markerLinkedList = new LinkedList<>();

    public Plan(int numberOfWaypoints, String file, String planName){
        this.numberOfWaypoints = numberOfWaypoints;
        this.file = file;
        this.planName = planName;
    }

    public String getFile() {
        return file;
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

}
