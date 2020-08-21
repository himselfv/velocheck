package asdbsd.velocheck;

import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class ParkingList {
/*
Известные поля в JSON с примерами:
    Id: "0001"
    Position: {"Lat": 55.7449006, "Lon": 37.6020183}
    Name: ""    // везде пустое - возможно, для личной метки
    Address: ""

    TotalPlaces: 12,
    TotalOrdinaryPlaces: 12
    TotalElectricPlaces: 0
    AvailableOrdinaryBikes: 2
    AvailableElectricBikes: 0
    FreePlaces: 10
    FreeOrdinaryPlaces: 10
    FreeElectricPlaces: 0

    Похоже, OrdinaryPlaces и ElectricPlaces взаимозаменяемы, важно только общее число.  А вот велосипеды разные.
    AvailableBikes может быть меньше, чем TotalPlaces-FreePlaces! Часть замков может быть неисправна.

    StationTypes: ["ordinary"]
    HasTerminal: false
    IsLocked: false
    IconsSet: 1
    TemplateId: 6
    IsFavourite: false
*/
    public class Parking {
        int id;
        String name;
        String address;
        Double lat;
        Double lng;
        int totalPlaces;
        int freePlaces;
        int availableOrdinaryBikes;
        int availableElectricBikes;
        boolean isLocked;

        void fromJson(JSONObject parking) throws JSONException {
            this.id = parking.getInt("Id");
            this.address = parking.getString("Address");
            this.name = this.address;
            this.totalPlaces = parking.getInt("TotalPlaces");
            this.freePlaces = parking.getInt("FreePlaces");
            this.availableOrdinaryBikes = parking.getInt("AvailableOrdinaryBikes");
            this.availableElectricBikes = parking.getInt("AvailableElectricBikes");
            this.isLocked = parking.getBoolean("IsLocked");
            JSONObject pos = parking.getJSONObject("Position");
            this.lat = pos.getDouble("Lat");
            this.lng = pos.getDouble("Lon");
        }

        int getStateIconResource() {
            if (isLocked)
                return R.drawable.marker_no_48;
            if (totalPlaces == 0)
                return R.drawable.marker8_48;
            if (freePlaces == totalPlaces)
                return R.drawable.marker0_48;
            if (freePlaces == 0)
                return R.drawable.marker8_48;
            double occup = ((double)(totalPlaces - freePlaces)) / (double)totalPlaces;
            if (occup <= 1.0/7.0)
                return R.drawable.marker1_48;
            else
            if (occup <= 2.0/7.0)
                return R.drawable.marker2_48;
            else
            if (occup <= 3.0/7.0)
                return R.drawable.marker3_48;
            else
            if (occup <= 4.0/7.0)
                return R.drawable.marker4_48;
            else
            if (occup <= 5.0/7.0)
                return R.drawable.marker5_48;
            else
            if (occup <= 6.0/7.0)
                return R.drawable.marker6_48;
            else
                return R.drawable.marker7_48;
        }
    }

    ParkingList() {
    }


    /* External list access */

    protected ArrayList<Parking> list = new ArrayList<>();
    int count() {
        return list.size();
    }

    Parking get(int index) {
        return list.get(index);
    }

    Parking findById(int parkingId) {
        for (Parking p : list) {
            if (p.id == parkingId)
                return p;
        }
        return null;
    }


    /*  Querying and parsing  */

    private String queryUrl = "https://velobike.ru/proxy/parkings/";

    void AsyncUpdate() {
        for (EventHandler handler : handlers)
            handler.onBeginUpdate();
        new RetrieveParkingsTask().execute(queryUrl);
    }

    class RetrieveParkingsTask extends AsyncTask<String, Void, ArrayList<Parking>> {
        private Exception exception;

        @Override
        protected ArrayList<Parking> doInBackground(String... queryUrl) {
            try {
                URL url = new URL(queryUrl[0]);
                URLConnection connection = url.openConnection();

                String line;
                StringBuilder builder = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }

                String jsonString = builder.toString();
                JSONObject json = new JSONObject(jsonString);
                JSONArray parking_list = json.getJSONArray("Items");

                ArrayList<Parking> new_list = new ArrayList<> ();
                for (int i=0; i < parking_list.length(); i++) {
                    JSONObject parking = parking_list.getJSONObject(i);
                    Parking p = new Parking();
                    p.fromJson(parking);
                    new_list.add(p);
                }

                return new_list;
            }
            catch (IOException | JSONException e) {
                this.exception = e;
                return null;
            }
        }

        protected void onPostExecute(ArrayList<Parking> new_list) {
            if (this.exception != null) {
                for (EventHandler handler : handlers)
                    handler.onUpdateFailed(this.exception);
                return;
            }

            ParkingList.this.list = new_list;
            for (EventHandler handler : handlers)
                handler.onUpdateFinished();
        }
    }


    /*  Events  */

    public interface EventHandler {
        void onBeginUpdate();
        void onUpdateFinished();
        void onUpdateFailed(Exception e);
    }


    protected ArrayList<EventHandler> handlers = new ArrayList<>();

    void addParkingEventHandler(EventHandler handler) {
        handlers.add(handler);
    }

    void removeParkingEventHandler(EventHandler handler) {
        handlers.remove(handler);
    }
}
