package asdbsd.velocheck;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Locale;

import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.os.AsyncTask;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


//TODO: Fix bugs with page hiding / showing
//TODO: Fav icon to the left of each item
//TODO: Text filter control
//TODO: Google MapView

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
    PageAdapter mSectionsPagerAdapter;
    PageAdapter.Page pageFavorites;
    PageAdapter.Page pageAll;

    public ListViewAdapter adapter;
    public ListViewAdapter favadapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create the list adapters for pages
        this.adapter = new ListViewAdapter(this);
        this.favadapter = new ListViewAdapter(this);

        // Create the adapter that will return a fragment for each page
        mSectionsPagerAdapter = new PageAdapter(getSupportFragmentManager());
        mSectionsPagerAdapter.registerDataSetObserver(this.mPageListChangeListener);

        //Add some pages
        Locale l = Locale.getDefault();

        pageFavorites = mSectionsPagerAdapter.addFragmentPage(new ParkingListFragment(MainActivity.this.favadapter));
        pageFavorites.title = getString(R.string.title_section_favorites).toUpperCase(l);
        pageFavorites.icon = getResources().getDrawable(R.drawable.ic_favorites_32);

        pageAll = mSectionsPagerAdapter.addFragmentPage(new ParkingListFragment(MainActivity.this.adapter));
        pageAll.title = getString(R.string.title_section_all).toUpperCase(l);
        pageAll.icon = getResources().getDrawable(R.drawable.ic_list_32);

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOnPageChangeListener(mPageChangeListener);

        actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);
        ReloadTabs(); //Each Tab will bind to a TabListener

        this.favorites = new ArrayList<Integer>();
        LoadFavorites();

        UpdateParkings();
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
        editor.apply();
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
        pageFavorites.setVisible(favorites.size() != 0);
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


}
