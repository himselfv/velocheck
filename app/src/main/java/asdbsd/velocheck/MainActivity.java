package asdbsd.velocheck;

import java.util.ArrayList;
import java.util.Locale;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import asdbsd.velocheck.FragmentLifecycle;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.tabs.TabLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class MainActivity extends AppCompatActivity {

    /**
     * Pager control.
     */
    ViewPager mViewPager;

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
        //independently), but for now this'll suffice
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

        //Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.addOnPageChangeListener(mPageChangeListener);

        /*
        Set up TabLayout to reflect ViewPager's pages (with TabLayout it's automatic)
        Sliding tabs using TabLayout:
            https://guides.codepath.com/android/Google-Play-Style-Tabs-using-TabLayout
        Main activity layout example:
          https://stackoverflow.com/a/27729494
        */
        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
        //Have to update icons manually
        updatePageIcons();

        //Set up the main toolbar (used as an action bar)
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        this.favorites = new ArrayList<>();
        LoadFavorites();

        initFinished = true;
        if (dataReceived) // might have missed it while init was not finished. Import manually.
            parkingsUpdated();
    }

    //Called when the activity is brought back to the front, including for the first time.
    @Override
    protected void onResume() {
        super.onResume();
        //When the user reopens the app they expect the data to be fresh, so auto-update after a while.
        if (parkings.isDataOlderThan(60000)) // 1 minute
            parkings.AsyncUpdate();
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
        int currentPosition = 0;

        @Override
        public void onPageSelected(int position) {
            Object page = mSectionsPagerAdapter.visiblePages.get(position);

            Object currentFragment = mSectionsPagerAdapter.getItem(currentPosition);
            if ((currentFragment != null) && (currentFragment instanceof FragmentLifecycle)) {
                FragmentLifecycle fragment = (FragmentLifecycle) currentFragment;
                fragment.onHideFragment();
            }
            Object newFragment = mSectionsPagerAdapter.getItem(position);
            if ((newFragment != null) && (newFragment instanceof FragmentLifecycle)) {
                FragmentLifecycle fragment = (FragmentLifecycle) newFragment;
                fragment.onShowFragment();
            }
            currentPosition = position;
        }
    };

    /*  Page list change listener. Called when the list of available / visible pages changes.
       This is simply an adapter notification because why invent the wheel. */
    DataSetObserver mPageListChangeListener = new DataSetObserver() {
        @Override
        public void onChanged() {
            //Called when the list of tabs is changed
        }

        @Override
        public void onInvalidated() {
            //Called when the list of tabs becomes invalid for whatever reason
        }
    };


    /*
    TabLayout automatically uses ViewPager's pages but not icons
    */
    TabLayout tabLayout;
    void updatePageIcons() {
        for (int i = 0; i < mSectionsPagerAdapter.visiblePages.size(); i ++) {
            PageAdapter.Page page = mSectionsPagerAdapter.visiblePages.get(i);
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            if (tab != null)
                tab.setIcon(page.icon);
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
        //We're only calling this on init, so don't reload favadapter, this'll be done in time.
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
        //pageFavorites.setVisible(favorites.size() != 0);
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
