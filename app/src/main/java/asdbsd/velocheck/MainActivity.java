package asdbsd.velocheck;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Locale;

//import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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
     * Pager control.
     */
    ViewPager mViewPager;

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    public ListViewAdapter adapter;
    public ListViewAdapter favadapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mSectionsPagerAdapter.registerDataSetObserver(this.mPageListChangeListener);

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOnPageChangeListener(mPageChangeListener);

        actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);
        //Each Tab will bind to a TabListener later

        this.adapter = new ListViewAdapter(this);
        this.favadapter = new ListViewAdapter(this);

        this.favorites = new ArrayList<Integer>();
        LoadFavorites();

        UpdateParkings();
        ReloadTabs(); //TODO: Not here but when the page list changes
    }


    /*  Page change listener. Called when the selected page changes in the ViewPager  */
    ViewPager.OnPageChangeListener mPageChangeListener = new ViewPager.SimpleOnPageChangeListener() {
        @Override
        public void onPageSelected(int position) {
            Object page = mSectionsPagerAdapter.visiblePages.get(position);
            ActionBar.Tab tab = actionBar.getSelectedTab();
            //Take care to avoid infinite loop
            if ((tab == null) || (tab.getTag() != page)) {
                for (int i = 0; i < actionBar.getTabCount(); i++)
                    if (actionBar.getTabAt(i).getTag() == page) {
                        actionBar.setSelectedNavigationItem(i);
                        break;
                    }
            }
        }
    };

    /*  Page list change listener. Called when the list of available / visible pages changes.
       This is simply an adapter notification because why invent the wheel. */
    DataSetObserver mPageListChangeListener = new DataSetObserver() {
        @Override
        public void onChanged() {
            //Called when the list of tabs is changed
            ReloadTabs();
        }

        @Override
        public void onInvalidated() {
            //Called when the list of tabs becomes invalid for whatever reason
            ClearTabs();
        }
    };


    /*  Tab control listener. Called when the user changes tabs.  */
    ActionBar actionBar;
    ActionBar.TabListener tabListener = new ActionBar.TabListener() {
        public void onTabSelected(ActionBar.Tab tab, android.support.v4.app.FragmentTransaction ft) {
            // switched to a different tab -- open associated fragment
            Object tag = tab.getTag();
            for (int i = 0; i < mSectionsPagerAdapter.visiblePages.size(); i++)
                if (mSectionsPagerAdapter.visiblePages.get(i) == tag)
                    if (mViewPager.getCurrentItem() != i)
                        mViewPager.setCurrentItem(i);
        }

        public void onTabUnselected(ActionBar.Tab tab, android.support.v4.app.FragmentTransaction ft) {
            // switched to other tab -- will react in onTabSelected() for that tab
        }

        public void onTabReselected(ActionBar.Tab tab, android.support.v4.app.FragmentTransaction ft) {
            // user clicked the already selected tab again. Whatever.
        }
    };

    void ClearTabs() {
        actionBar.removeAllTabs();
    }

    //Called when the page list has changed and tab list needs to be reloaded
    void ReloadTabs() {
        actionBar.removeAllTabs();
        for (int i = 0; i < mSectionsPagerAdapter.visiblePages.size(); i ++) {
            PageAdapter.Page page = mSectionsPagerAdapter.visiblePages.get(i);
            ActionBar.Tab tab = actionBar.newTab();
            tab.setText(page.title);
            tab.setIcon(page.icon);
            tab.setTag(page);
            tab.setTabListener(this.tabListener);
            actionBar.addTab(tab);
        }
    }



    /*  Favorites  */

    ArrayList<Integer> favorites; // a list of parking ids

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
        ShowHideFavorites();
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
        ShowHideFavorites();
    }

    void RemoveFavorite(Integer id) {
        favorites.remove(id);
        SaveFavorites();
        ReloadFavadapter();
        ShowHideFavorites();
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

    //Favorites are the first pane, but when there are none we want to go straight to full list
    void ShowHideFavorites() {
        mSectionsPagerAdapter.SetFavoritesVisible(favorites.size() != 0);
    }


    /*  Parkings list  */

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


    /*  Options menu   */

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
    public class SectionsPagerAdapter extends PageAdapter {
        Page pageFavorites;
        Page pageAll;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
            Locale l = Locale.getDefault();

            pageFavorites = this.addFragmentPage(new ParkingListFragment(ParkingListFragment.FRAGMENT_FAVORITES));
            pageFavorites.title = getString(R.string.title_section_favorites).toUpperCase(l);
            pageFavorites.icon = getResources().getDrawable(R.drawable.ic_favorites_48);

            pageAll = this.addFragmentPage(new ParkingListFragment(ParkingListFragment.FRAGMENT_ALL));
            pageAll.title = getString(R.string.title_section_all).toUpperCase(l);
            pageAll.icon = getResources().getDrawable(R.drawable.ic_list_48);
        }

        public void SetFavoritesVisible(boolean value) {
            pageFavorites.setVisible(value);
        }

    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public class ParkingListFragment extends Fragment {
        public static final int FRAGMENT_ALL        = 0;
        public static final int FRAGMENT_FAVORITES  = 1;

        int fragmentType;
        ListViewAdapter adapter;

        public ParkingListFragment(int fragmentType) {
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
                    ParkingListFragment.this.adapter = activity.adapter;
                    break;
                case FRAGMENT_FAVORITES:
                    ParkingListFragment.this.adapter = activity.favadapter;
                    break;
            }

            ListView listView = (ListView)rootView.findViewById(R.id.listView1);
            listView.setAdapter(adapter);

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
                    entry = ParkingListFragment.this.adapter.list.get(info.position);
                    if (entry != null) {
                        AddFavorite(entry.id);
                        Toast.makeText(MainActivity.this, "Added", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                case R.id.action_remove_from_favorites:
                    entry = ParkingListFragment.this.adapter.list.get(info.position);
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
