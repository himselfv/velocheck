package asdbsd.velocheck;

import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

// Only works with MainActivity because yeah

public class ParkingListFragment extends Fragment {
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

        return rootView;
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
        //Hide the "Updating..." text once any data is available
        TextView statusText = (TextView) rootView.findViewById(R.id.status_text);
        statusText.setVisibility(View.GONE);
    }

}
