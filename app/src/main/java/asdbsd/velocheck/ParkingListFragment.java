package asdbsd.velocheck;

import android.database.DataSetObserver;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.util.Log;
import asdbsd.velocheck.FragmentLifecycle;

// Only works with MainActivity because yeah

public class ParkingListFragment extends Fragment implements FragmentLifecycle {
    MainActivity activity;
    ParkingListAdapter adapter;

    public ParkingListFragment() {
        super();
    }

    protected ParkingListAdapter retrieveAdapter() {
        return null; //override and ask for proper one from activity
    }

    //Override this in children to only override the inflation
    protected View createView(LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_parkinglist, container, false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = createView(inflater, container, savedInstanceState);

        activity = (MainActivity) this.getActivity();
        this.adapter = retrieveAdapter();

        adapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                View rootView = ParkingListFragment.this.getView();
                if (rootView != null)
                    ParkingListFragment.this.updateStatusText(rootView);
            }
        });
        updateStatusText(rootView);

        ListView listView = (ListView)rootView.findViewById(R.id.listView1);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ParkingList.Parking parking = adapter.filteredList.get(position);
                ParkingDetailsActivity.show(ParkingListFragment.this.activity, parking.id);
            }
        });

        registerForContextMenu(listView); //propagate events to this parent object

        setupSwipeRefresh(rootView);

        return rootView;
    }

    @Override
    public void onDestroyView() {
        releaseSwipeRefresh();
        super.onDestroyView();
    }


    /*
    Swipe-refresh layout. See:
      http://developer.alexanderklimov.ru/android/layout/swiperefreshlayout.php
      https://developer.android.com/training/swipe/respond-refresh-request
    Each page has its own swipe-refresh => all have to watch for parking updates.

    The adapter has no refresh() method, no back link to notify its owners to reload it,
    no begin/end/fail update notifications (only "changed"), so have to access .parkings directly.
    */
    SwipeRefreshLayout swipeRefresh_list;
    SwipeRefreshLayout swipeRefresh_empty;
    ParkingList.EventHandler parkingListHandler;
    public void setupSwipeRefresh(View rootView) {
        swipeRefresh_list = (SwipeRefreshLayout) rootView.findViewById(R.id.swiperefresh_list);
        swipeRefresh_empty = (SwipeRefreshLayout) rootView.findViewById(R.id.swiperefresh_empty);

        SwipeRefreshLayout.OnRefreshListener swipeRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
            @Override public void onRefresh() {
                Log.i("Parkings", this.toString()+": onRefresh ("+this.toString()+")");
                activity.parkings.AsyncUpdate();
            }
        };
        swipeRefresh_list.setOnRefreshListener(swipeRefreshListener);
        swipeRefresh_empty.setOnRefreshListener(swipeRefreshListener);

        this.parkingListHandler = new ParkingList.EventHandler() {
            //All can happen while onCreate is not yet finished.
            @Override public void onBeginUpdate() { swipeRefresh_list.setRefreshing(true); swipeRefresh_empty.setRefreshing(true); }
            @Override public void onUpdateFinished() { swipeRefresh_list.setRefreshing(false); swipeRefresh_empty.setRefreshing(false); }
            @Override public void onUpdateFailed(Exception e) { swipeRefresh_list.setRefreshing(false); swipeRefresh_empty.setRefreshing(false); }
        };
        activity.parkings.addParkingEventHandler(parkingListHandler);
    }
    public void releaseSwipeRefresh() {
        activity.parkings.removeParkingEventHandler(this.parkingListHandler);
    }

    @Override
    public void onShowFragment() {
        if (this.swipeRefresh_empty != null)
            this.swipeRefresh_empty.setEnabled(true);
        if (this.swipeRefresh_list != null)
            this.swipeRefresh_list.setEnabled(true);
    }
    @Override
    public void onHideFragment() {
        if (this.swipeRefresh_empty != null)
            this.swipeRefresh_empty.setEnabled(false);
        if (this.swipeRefresh_list != null)
            this.swipeRefresh_list.setEnabled(false);
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu,
                                    final View v, final ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId()==R.id.listView1) {

            MenuInflater inflater = activity.getMenuInflater();
            inflater.inflate(R.menu.menu_list, menu);

            //Retrieve selected item
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            ParkingList.Parking entry = adapter.filteredList.get(info.position);
            if (entry == null) return;

            //Depending on the selected item, show or hide menu items
            MenuItem addToFavorites = menu.findItem(R.id.action_add_to_favorites);
            MenuItem removeFromFavorites = menu.findItem(R.id.action_remove_from_favorites);

            if (activity.favorites.contains(entry.id)) {
                addToFavorites.setVisible(false);
                removeFromFavorites.setVisible(true);
            } else {
                addToFavorites.setVisible(true);
                removeFromFavorites.setVisible(false);
            }

            //Somehow onContextItemSelected fires in an incorrect Fragment, so use this
            //http://stackoverflow.com/questions/29195764/oncontextitemselected-is-not-called-in-fragment
            MenuItem.OnMenuItemClickListener listener = new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    onMenuItemSelected(item);
                    return true;
                }
            };

            for (int i = 0, n = menu.size(); i < n; i++)
                menu.getItem(i).setOnMenuItemClickListener(listener);
        }
    }

    public boolean onMenuItemSelected(MenuItem item) {
        //Retrieve selected item
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        ParkingList.Parking entry;

        switch(item.getItemId()) {
            case R.id.action_add_to_favorites:
                entry = ParkingListFragment.this.adapter.filteredList.get(info.position);
                if (entry != null)
                    activity.AddFavorite(entry.id);
                return true;
            case R.id.action_remove_from_favorites:
                entry = ParkingListFragment.this.adapter.filteredList.get(info.position);
                if (entry != null)
                    activity.RemoveFavorite(entry.id);
                return true;
            case R.id.action_locate_on_map:
                entry = ParkingListFragment.this.adapter.filteredList.get(info.position);
                if (entry != null)
                    activity.locateOnMap(entry.id);
                return true;
        }
        return super.onContextItemSelected(item);
    }

    //Called when creating the view, and on every adapter dataset change
    protected void updateStatusText(View rootView) {
        //Hide the "Updating..." text once any data is available for the first time
        if (activity.parkings.count() > 0) {
            View statusText = rootView.findViewById(R.id.swiperefresh_empty);
            statusText.setVisibility(View.GONE);
        }
    }

}
