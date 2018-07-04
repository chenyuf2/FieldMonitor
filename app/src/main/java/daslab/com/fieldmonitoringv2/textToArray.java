package daslab.com.fieldmonitoringv2;

import android.util.Log;

import com.google.android.gms.internal.maps.zzt;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class textToArray {
    List<Marker> markers = new ArrayList<>();

    public List<Marker> textToArray( File fileName, GoogleMap mMap) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName));
        String line = null, line1 = null;
        int i = 0;
        while ((line = bufferedReader.readLine()) != null && (line1 = bufferedReader.readLine()) != null){
            markers.add(mMap.addMarker(new MarkerOptions().position(new LatLng(Double.parseDouble(line),Double.parseDouble(line1))).visible(false)));
            Log.d("array", "Added lat" + line +"\n" + "long" + line1 + "\n");
        }
        return markers;
    }
}
