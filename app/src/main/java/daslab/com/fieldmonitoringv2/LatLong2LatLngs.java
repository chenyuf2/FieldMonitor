package daslab.com.fieldmonitoringv2;

import com.google.android.gms.maps.model.LatLng;
import com.o3dr.services.android.lib.coordinate.LatLong;

import java.util.LinkedList;
import java.util.List;

public class LatLong2LatLngs {

    private List<LatLng> latLngs;

    /**
     * Takes LatLong list and converts it to LatLng list
     * @param latLongs As a list
     */
    public LatLong2LatLngs(List<LatLong> latLongs) {
        List<LatLng> latLngs = new LinkedList<>();
        for (LatLong latLong :
                latLongs) {
            latLngs.add(new LatLng(latLong.getLatitude(), latLong.getLongitude()));
        }
        this.latLngs = latLngs;
    }

    /**
     * Returns the latlng list
     * @return LatLng list
     */
    public List<LatLng> getLatLngs() {
        return latLngs;
    }

}
