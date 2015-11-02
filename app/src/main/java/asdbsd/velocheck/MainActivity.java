package asdbsd.velocheck;

import java.util.ArrayList;
import java.util.Locale;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

//TODO: Fix bugs with page hiding / showing
//TODO: Configure buttons to show on Google map (remove "open map application", maybe add others?)

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
    PageAdapter.Page pageMap;

    public ParkingListAdapter adapter;
    public ParkingListAdapter favadapter;

    private boolean initFinished = false; //set by onCreate when initialization is finished
    private boolean dataReceived = false; //set by parkings if the data has arrived at least once

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Start updating right away!
        //If the data comes earlier, we'll just process at the end of init
        parkings.addParkingEventHandler(this.parkingListHandler);
        parkings.AsyncUpdate();

        //It's better to just store all shared info in the App (MainActivity can get unloaded
        //indepentenly), but for now this'll suffice
        App app = (App) getApplicationContext();
        app.main = this;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create the list adapters for pages
        this.adapter = new ParkingListAdapter(this);
        this.favadapter = new ParkingListAdapter(this);

        // Create the adapter that will return a fragment for each page
        mSectionsPagerAdapter = new PageAdapter(getSupportFragmentManager());
        mSectionsPagerAdapter.registerDataSetObserver(this.mPageListChangeListener);

        //Add some pages
        Locale l = Locale.getDefault();

        pageFavorites = mSectionsPagerAdapter.addPage(1, new PageAdapter.FragmentConstructor() {
            @Override
            public Fragment createFragment() { return new FavoritesFragment(); }
        });
        pageFavorites.title = getString(R.string.title_section_favorites).toUpperCase(l);
        pageFavorites.icon = getResources().getDrawable(R.drawable.ic_favorited_32);

        pageAll = mSectionsPagerAdapter.addPage(2, new PageAdapter.FragmentConstructor() {
            @Override
            public Fragment createFragment() { return new AllParkingsFragment(); }
        });
        pageAll.title = getString(R.string.title_section_all).toUpperCase(l);
        pageAll.icon = getResources().getDrawable(R.drawable.ic_list_32);

        pageMap = mSectionsPagerAdapter.addPage(3, new PageAdapter.FragmentConstructor() {
            @Override
            public Fragment createFragment() { return new MapFragment(); }
        });
        pageMap.title = getString(R.string.title_section_map).toUpperCase(l);
        pageMap.icon = getResources().getDrawable(R.drawable.ic_map_32);

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOnPageChangeListener(mPageChangeListener);

        actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);
        ReloadTabs(); //Each Tab will bind to a TabListener

        this.favorites = new ArrayList<>();
        LoadFavorites();

        initFinished = true;
        if (dataReceived) // might have missed it while init was not finished. Import manually.
            parkingsUpdated();
    }



    // It's possible to send us some commands from children activities by calling us
    // with FLAG_ACTIVITY_CLEAR_TOP.
    // This preserves the instance and passes the new intent here.

    public final static String EXTRA_LOCATEONMAP = "asdbsd.velocheck.main.LOCATEONMAP";

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        int parkingId = intent.getIntExtra(EXTRA_LOCATEONMAP, -1);
        if (parkingId >= 0) {
            locateOnMap(parkingId);
            return;
        }
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
        if (favStr.isEmpty())
            favs = new String[0];
        else
            favs = favStr.split(",");
        for (String favId : favs)
            favorites.add(Integer.parseInt(favId));
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
        Toast.makeText(this, this.getString(R.string.favorite_added), Toast.LENGTH_SHORT).show();
        ReloadFavadapter();
        ShowHideFavorites();
    }

    void RemoveFavorite(Integer id) {
        favorites.remove(id);
        SaveFavorites();
        Toast.makeText(this, this.getString(R.string.favorite_removed), Toast.LENGTH_SHORT).show();
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


    ParkingList parkings = new ParkingList();
    ParkingList.EventHandler parkingListHandler = new ParkingList.EventHandler() {
        //All of this can be called while onCreate is not yet finished.
        //Do not do anything funny, or check initFinished.

        @Override
        public void onBeginUpdate() {
            Toast.makeText(MainActivity.this, getString(R.string.querying_parkings), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onUpdateFinished() {
            dataReceived = true;
            if (!initFinished) return; //they'll see dataReceived and call parkingsUpdated
            parkingsUpdated();
        }

        @Override
        public void onUpdateFailed(Exception e) {
            Toast.makeText(MainActivity.this, getString(R.string.cannot_update_parkings)
                    + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    };

    //Called when the parking list is updated
    protected void parkingsUpdated() {
        adapter.clear();
        for (int i = 0; i < parkings.count(); i++) {
            ParkingList.Parking p = parkings.get(i);
            adapter.addItem(p);
        }
        adapter.sort();
        adapter.notifyDataSetChanged();

        ReloadFavadapter();

        Toast.makeText(MainActivity.this, getString(R.string.parkings_updated), Toast.LENGTH_SHORT).show();
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
            parkings.AsyncUpdate();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    // Opens MapFragment and centers it on the specified parking
    void locateOnMap(int parkingId) {
        ParkingList.Parking p = parkings.findById(parkingId);
        if (p == null) return;

        this.mViewPager.setCurrentItem(2); //map
        //have to do this before looking for fragment because it might not yet exist

        //Find our fucking map fragment
        MapFragment map = null;
        for (Fragment f : getSupportFragmentManager().getFragments())
            if (f instanceof MapFragment) {
                map = (MapFragment) f;
                break;
            }
        if (map == null)
            return; //wherever it went

        map.moveCamera(p.lat, p.lng);
    }

}
