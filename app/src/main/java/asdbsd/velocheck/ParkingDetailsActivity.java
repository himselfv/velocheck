package asdbsd.velocheck;
// Details and extended manipulations for a specific parking
// Usage:
//   ParkingDetailsActivity.show(context, parkingId)

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class ParkingDetailsActivity extends Activity {
    public final static String EXTRA_PARKINGID = "asdbsd.velocheck.parkingdetails.PARKINGID";

    App app;
    int parkingId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parkingdetails);

        ImageView favstar = (ImageView) findViewById(R.id.favstar);
        favstar.setOnClickListener(favstarOnClickListener);

        if (savedInstanceState == null)
            savedInstanceState = getIntent().getExtras();
        parkingId = savedInstanceState.getInt(EXTRA_PARKINGID);

        app = (App) getApplicationContext();
        app.main.parkings.addParkingEventHandler(parkingEventHandler);

        reload();
     }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(EXTRA_PARKINGID, parkingId);
    }

    public static void show(Context context, int parkingId) {
        Intent intent = new Intent(context, ParkingDetailsActivity.class);
        intent.putExtra(ParkingDetailsActivity.EXTRA_PARKINGID, parkingId);
        context.startActivity(intent);
    }

    void reload() {
        ParkingList.Parking p = app.main.parkings.findById(parkingId);

        ImageView favstar = (ImageView) findViewById(R.id.favstar);
        if (app.main.favorites.contains(p.id))
            favstar.setImageResource(R.drawable.ic_favorited_48);
        else
            favstar.setImageResource(R.drawable.ic_not_favorited_48);

        TextView title = (TextView) findViewById(R.id.title);
        title.setText(p.address);

        TextView occupiedPlaces = (TextView) findViewById(R.id.value_occupied_places);
        occupiedPlaces.setText(Integer.toString(p.totalPlaces - p.freePlaces));

        TextView freePlaces = (TextView) findViewById(R.id.value_free_places);
        freePlaces.setText(Integer.toString(p.freePlaces));
    }


    ParkingList.EventHandler parkingEventHandler = new ParkingList.EventHandler() {
        @Override
        public void onBeginUpdate() {}

        @Override
        public void onUpdateFinished() { reload(); }

        @Override
        public void onUpdateFailed(Exception e) { }
    };


    View.OnClickListener favstarOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (app.main.favorites.contains(parkingId))
                app.main.RemoveFavorite(parkingId);
            else
                app.main.AddFavorite(parkingId);
            reload();
        }
    };


    public void onLocateOnMapClick(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(MainActivity.EXTRA_LOCATEONMAP, this.parkingId);
        startActivity(intent);
    }


}
