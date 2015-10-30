package asdbsd.velocheck;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class FavoritesFragment extends ParkingListFragment {

    public FavoritesFragment() { super(); }

    @Override
    protected View createView(LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorites, container, false);
    }

    @Override
    protected ParkingListAdapter retrieveAdapter() {
        return activity.favadapter;
    }

}

