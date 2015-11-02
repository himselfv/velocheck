package asdbsd.velocheck;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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

    @Override
    protected void updateStatusText(View rootView) {
        if (activity.favorites.size() <= 0) {
            //Show hint when there are no favorites
            TextView statusText = (TextView) rootView.findViewById(R.id.status_text);
            statusText.setText(getString(R.string.no_favorites_hint));
            statusText.setVisibility(View.VISIBLE);
        } else
            super.updateStatusText(rootView);
    }

}

