package asdbsd.velocheck;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends ActionBarActivity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    public ListViewAdapter adapter;
    public ListViewAdapter favadapter;

    ArrayList<Integer> favorites;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        this.adapter = new ListViewAdapter(this);
        this.favadapter = new ListViewAdapter(this);

        this.favorites = new ArrayList<Integer>();
        LoadFavorites();

        UpdateParkings();
    }

    void LoadFavorites() {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        String favStr = preferences.getString("favorites", "");
        String[] favs;
        if (favStr == "")
            favs = new String[0];
        else
            favs = favStr.split(",");
        for (int i = 0; i < favs.length; i++)
            favorites.add(Integer.parseInt(favs[i]));
        //We're only calling this on init, so
        //don't reload favadapter, this'll be done in time.
    }

    void SaveFavorites() {
        String[] favs = new String[favorites.size()];
        for (int i = 0; i < favorites.size(); i++)
            favs[i] = Integer.toString(favorites.get(i));

        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("favorites", TextUtils.join(",", favs));
        editor.commit();
    }

    void AddFavorite(Integer id) {
        favorites.add(id);
        SaveFavorites();
        ReloadFavadapter();
    }

    void RemoveFavorite(Integer id) {
        favorites.remove(id);
        SaveFavorites();
        ReloadFavadapter();
    }

    //Filters parkings from adapter into favadapter according to favorites
    void ReloadFavadapter() {
        favadapter.clear();
        for (Integer i = 0; i < adapter.list.size(); i++) {
            if (favorites.contains(adapter.list.get(i).id))
                favadapter.addItem(adapter.list.get(i));
        }
        favadapter.sort();
        favadapter.notifyDataSetChanged();
    }

    JSONObject[] parkings;

    class RetrieveParkingsTask extends  AsyncTask<String, Void, JSONObject> {
        private Exception exception;

        @Override
        protected JSONObject doInBackground(String... queryUrl) {
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
                return new JSONObject(jsonString);
            }
            catch (IOException | JSONException e) {
                this.exception = e;
                return null;
            }
        }

        protected void onPostExecute(JSONObject json) {
            if (this.exception != null) {
                Toast.makeText(MainActivity.this, "Cannot update parkings: "
                        + this.exception.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            adapter.clear();
            try {
                JSONArray parking_list = json.getJSONArray("Items");
                JSONObject[] new_parkings = new JSONObject[parking_list.length()];
                for (int i=0; i < parking_list.length(); i++) {
                    JSONObject parking = parking_list.getJSONObject(i);
                    new_parkings[i] = parking;
                    adapter.addItem(
                        new ListViewEntry(
                            parking.getInt("Id"),
                            parking.getString("Address"),
                            Integer.toString(parking.getInt("FreePlaces")) + " / "
                                    + Integer.toString(parking.getInt("TotalPlaces"))
                        ));
                }

                parkings = new_parkings;
            }
            catch (JSONException e) {
                Toast.makeText(MainActivity.this, "Cannot update parkings: "
                        + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
            adapter.sort();
            adapter.notifyDataSetChanged();

            ReloadFavadapter();

            Toast.makeText(MainActivity.this, "Updated.", Toast.LENGTH_SHORT).show();
        }
    }


    protected void UpdateParkings() {
        Toast.makeText(MainActivity.this, "Querying parkings...", Toast.LENGTH_SHORT).show();
        String queryUrl = "http://velobike.ru/proxy/parkings/";
        new RetrieveParkingsTask().execute(queryUrl);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_update) {
            UpdateParkings();
            return true;
        }

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            switch (position) {
                case 0:
                    return new PlaceholderFragment(PlaceholderFragment.FRAGMENT_FAVORITES);
                case 1:
                    return new PlaceholderFragment(PlaceholderFragment.FRAGMENT_ALL);
            }
            return null;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_section_favorites).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section_all).toUpperCase(l);
            }
            return null;
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public class PlaceholderFragment extends Fragment {
        public static final int FRAGMENT_ALL        = 0;
        public static final int FRAGMENT_FAVORITES  = 1;

        int fragmentType;
        ListViewAdapter adapter;

        public PlaceholderFragment(int fragmentType) {
            super();
            this.fragmentType = fragmentType;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);

            MainActivity activity = (MainActivity)this.getActivity();
            switch (this.fragmentType) {
                case FRAGMENT_ALL:
                    PlaceholderFragment.this.adapter = activity.adapter;
                    break;
                case FRAGMENT_FAVORITES:
                    PlaceholderFragment.this.adapter = activity.favadapter;
                    break;
            }

            ListView listView = (ListView)rootView.findViewById(R.id.listView1);
            listView.setAdapter(adapter);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Toast.makeText(PlaceholderFragment.this.getActivity(),
                            adapter.list.get(position).name, Toast.LENGTH_SHORT).show();
                }
            });

            registerForContextMenu(listView); //propagate events to this parent object
            return rootView;
        }

        @Override
        public void onCreateContextMenu(final ContextMenu menu,
                                        final View v, final ContextMenuInfo menuInfo) {
            super.onCreateContextMenu(menu, v, menuInfo);
            if (v.getId()==R.id.listView1) {

                MenuInflater inflater = getMenuInflater();
                inflater.inflate(R.menu.menu_list, menu);

                //Retrieve selected item
                AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
                ListViewEntry entry = adapter.list.get(info.position);
                if (entry == null) return;

                //Depending on the selected item, show or hide menu items
                MenuItem addToFavorites = menu.findItem(R.id.action_add_to_favorites);
                MenuItem removeFromFavorites = menu.findItem(R.id.action_remove_from_favorites);

                if (favorites.contains(entry.id)) {
                    addToFavorites.setVisible(false);
                    removeFromFavorites.setVisible(true);
                } else {
                    addToFavorites.setVisible(true);
                    removeFromFavorites.setVisible(false);
                }

                //Somehow onContextItemSelected fires in an incorrect Fragment, so use this
                //http://stackoverflow.com/questions/29195764/oncontextitemselected-is-not-called-in-fragment
                OnMenuItemClickListener listener = new OnMenuItemClickListener() {
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
            AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

            ListViewEntry entry;

            switch(item.getItemId()) {
                case R.id.action_add_to_favorites:
                    entry = PlaceholderFragment.this.adapter.list.get(info.position);
                    if (entry != null) {
                        AddFavorite(entry.id);
                        Toast.makeText(MainActivity.this, "Added", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                case R.id.action_remove_from_favorites:
                    entry = PlaceholderFragment.this.adapter.list.get(info.position);
                    if (entry != null) {
                        RemoveFavorite(entry.id);
                        Toast.makeText(MainActivity.this, "Removed", Toast.LENGTH_SHORT).show();
                    }
                    return true;
            }
            return super.onContextItemSelected(item);
        }

    }

}
