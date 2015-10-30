package asdbsd.velocheck;

import android.database.DataSetObserver;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapFragment extends SupportMapFragment {
    final LatLng MOSCOW = new LatLng(55.751244, 37.618423);

    MainActivity activity;
    ParkingList parkings;

    public MapFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        activity = (MainActivity) this.getActivity();
        this.parkings = activity.parkings;
        parkings.addParkingEventHandler(parkingEventHandler);

        this.getMapAsync(mapReadyCallback);

        return view;
    }


    //Called when the map is loaded for the first time
    OnMapReadyCallback mapReadyCallback = new OnMapReadyCallback() {
        @Override
        public void onMapReady(GoogleMap googleMap) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(MOSCOW, 10));
            PopulateMap(googleMap);
            googleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                @Override
                public void onMapLongClick(LatLng latLng) {

                }
            });
            googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    return false;
                }
            });
        }
    };

    //Called when the underlying parking set is reloaded
    ParkingList.EventHandler parkingEventHandler = new ParkingList.EventHandler() {
        @Override
        public void onBeginUpdate() {}

        @Override
        public void onUpdateFinished() {
            GoogleMap googleMap = MapFragment.this.getMap();
            if (googleMap != null)
                PopulateMap(googleMap);
            //otherwise it will be populated in the default getMapAsync
        }

        @Override
        public void onUpdateFailed(Exception e) {}
    };

    //Call when map is available
    protected void PopulateMap(GoogleMap googleMap) {
        googleMap.clear();

        if (parkings.count() == 0) return; //will be called when update finished

        SparseArray<BitmapDescriptor> icons = new SparseArray<>();

        for (int i = 0; i < parkings.count(); i++) {
            MarkerOptions marker = new MarkerOptions();
            ParkingList.Parking p = parkings.get(i);
            marker.position(new LatLng(p.lat, p.lng));
            marker.title(Integer.toString(p.id));
            marker.snippet(Integer.toString(p.freePlaces) + " / " + Integer.toString(p.totalPlaces));

            int icon = p.getStateIconResource();
            BitmapDescriptor icon_desc = icons.get(icon);
            if (icon_desc == null) {
                icon_desc = BitmapDescriptorFactory.fromResource(icon);
                icons.put(icon, icon_desc);
            }
            marker.icon(icon_desc);

            googleMap.addMarker(marker);
        }
    }

}
