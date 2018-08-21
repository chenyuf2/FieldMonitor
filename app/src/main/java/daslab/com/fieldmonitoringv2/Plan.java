package daslab.com.fieldmonitoringv2;

import com.o3dr.services.android.lib.coordinate.LatLong;

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class Plan{
    private int numberOfWaypoints;
    String file;
    String planName;
    List<LatLong> latLongs = new LinkedList<>();

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
            for (int i = 0; i < latLongs.size(); i++){
                fileWriter.write(Double.toString(latLongs.get(i).getLatitude()));
                fileWriter.write("\n");

                fileWriter.write(Double.toString(latLongs.get(i).getLongitude()));
                fileWriter.write("\n");
            }
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
