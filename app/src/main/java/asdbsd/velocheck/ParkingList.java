package asdbsd.velocheck;

import android.os.AsyncTask;
import android.widget.Toast;

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

    public class Parking {
        int id;
        String name;
        String address;
        Double lat;
        Double lng;
        int freePlaces;
        int totalPlaces;
    }

    ParkingList() {
    }


    /* External list access */

    protected ArrayList<Parking> list = new ArrayList<Parking>();
    int count() {
        return list.size();
    }

    Parking get(int index) {
        return list.get(index);
    }


    /*  Querying and parsing  */

    private String queryUrl = "http://velobike.ru/proxy/parkings/";

    void AsyncUpdate() {
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

                ArrayList<Parking> new_list = new ArrayList<Parking> ();
                for (int i=0; i < parking_list.length(); i++) {
                    JSONObject parking = parking_list.getJSONObject(i);

                    Parking p = new Parking();
                    p.id = parking.getInt("Id");
                    p.address = parking.getString("Address");
                    p.name = p.address;
                    p.freePlaces = parking.getInt("FreePlaces");
                    p.totalPlaces = parking.getInt("TotalPlaces");
                    JSONObject pos = parking.getJSONObject("Position");
                    p.lat = pos.getDouble("Lat");
                    p.lng = pos.getDouble("Lon");
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


    protected ArrayList<EventHandler> handlers = new ArrayList<EventHandler>();

    void addParkingEventHandler(EventHandler handler) {
        handlers.add(handler);
    }

    void removeParkingEventHandler(EventHandler handler) {
        handlers.remove(handler);
    }
}
