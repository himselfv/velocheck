package asdbsd.velocheck;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

// Only works with MainActivity because yeah

public class ParkingListFragment extends Fragment {
    MainActivity activity;
    ListViewAdapter adapter;

    //Pass the source for the list
    public ParkingListFragment(ListViewAdapter adapter) {
        super();
        //I know we're supposed to pass data through bundle because Android may destroy
        //and recreate us and yadda yadda
        //We're too stupid for that, okay?
        this.adapter = adapter;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        activity = (MainActivity) this.getActivity();

        ListView listView = (ListView)rootView.findViewById(R.id.listView1);
        listView.setAdapter(adapter);

        EditText editFilter = (EditText)rootView.findViewById(R.id.edit_filter);
        editFilter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        editFilter.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                return false;
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(ParkingListFragment.this.getActivity(),
                        adapter.list.get(position).name, Toast.LENGTH_SHORT).show();
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
            ListViewEntry entry = adapter.list.get(info.position);
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

        ListViewEntry entry;

        switch(item.getItemId()) {
            case R.id.action_add_to_favorites:
                entry = ParkingListFragment.this.adapter.list.get(info.position);
                if (entry != null) {
                    activity.AddFavorite(entry.id);
                    Toast.makeText(activity, "Added", Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.action_remove_from_favorites:
                entry = ParkingListFragment.this.adapter.list.get(info.position);
                if (entry != null) {
                    activity.RemoveFavorite(entry.id);
                    Toast.makeText(activity, "Removed", Toast.LENGTH_SHORT).show();
                }
                return true;
        }
        return super.onContextItemSelected(item);
    }

}