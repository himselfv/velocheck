package asdbsd.velocheck;

import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.ArrayList;
import java.util.Locale;

public class PageAdapter extends FragmentPagerAdapter {
    public PageAdapter(FragmentManager fm) {
        super(fm);
    }


    /*  Pages  */

    public class Page {
        Fragment fragment;
        String title;
        Drawable icon;
        private boolean visible;
        public void setVisible(boolean Value) {
            if (visible == Value) return;
            visible = Value;
            PageAdapter.this.pageVisibilityChanged(Page.this);
        }

        Page() {
            this.visible = true;
        }
    }

    protected ArrayList<Page> pages = new ArrayList<Page>();
    protected ArrayList<Page> visiblePages = new ArrayList<Page>();

    //These do not call notifyDataSetChanged -- call manually
    Page addFragmentPage(Fragment fragment) {
        Page page = new Page();
        page.fragment = fragment;
        pages.add(page);
        if (page.visible)
            visiblePages.add(page);
        return page;
    }

    Page addFragmentPage(Fragment fragment, String title) {
        Page page = addFragmentPage(fragment);
        page.title = title;
        return page;
    }

    Page removeFragmentPage(Fragment fragment) {
        for (int i = 0; i < pages.size(); i++)
            if (pages.get(i).fragment == fragment) {
                Page page = pages.remove(i);
                if (visiblePages.contains(page))
                    visiblePages.remove(page);
                return page;
            }
        return null;
    }


    /*  Visibility  */

    //Returns an index in visiblePage where this page is, or where it ought to be inserted
    private int getVisiblePageInsertPosition(Page page) {
        int index = pages.indexOf(page);
        if (index < 0) return -1; //not even in pages
        while (index >= 0) {
            Page sibling = pages.get(index);
            if (sibling.visible) return index + 1;
            index --;
        }
        return 0;
    }

    //Called when page visibility changes
    private void pageVisibilityChanged(Page page) {
        if (page.visible) {
            if (!visiblePages.contains(page)) {
                int insertPosition = getVisiblePageInsertPosition(page);
                visiblePages.add(insertPosition, page);
                this.notifyDataSetChanged();
            }
        } else {
            if (visiblePages.contains(page)) {
                visiblePages.remove(page);
                this.notifyDataSetChanged();
            }
        }
    }



    @Override
    public int getCount() {
        return visiblePages.size();
    }

    @Override
    public Fragment getItem(int position) {
        return visiblePages.get(position).fragment;
    }

    //Clients call this on notifyDataSetChanged() to determine which items are now where.
    //Inherited implementation returns POSITION_UNCHANGED for all items, but we have to tell
    //clients that some items have actually been removed.
    @Override
    public int getItemPosition(Object item) {
        int position = visiblePages.indexOf(item);
        if (position >= 0) {
            return position;
        } else {
            return POSITION_NONE;
        }
    }

    @Override
    public CharSequence getPageTitle(int position) {
        Page page = visiblePages.get(position);
        return page.title;
    }


}
