package asdbsd.velocheck;

import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.ArrayList;

public class PageAdapter extends FragmentPagerAdapter {
    public PageAdapter(FragmentManager fm) {
        super(fm);
    }

    interface FragmentConstructor {
        Fragment createFragment();
    }

    /*  Pages  */

    public class Page {
        FragmentConstructor constructor;
        long id;
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

    protected ArrayList<Page> pages = new ArrayList<>();
    protected ArrayList<Page> visiblePages = new ArrayList<>();

    //These do not call notifyDataSetChanged -- call manually
    Page addPage(long id, FragmentConstructor constructor) {
        Page page = new Page();
        page.constructor = constructor;
        page.id = id;
        pages.add(page);
        if (page.visible)
            visiblePages.add(page);
        return page;
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
                // shift the ID returned by getItemId outside the range of all previous fragments
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
        Page page = visiblePages.get(position);
        if (page == null)
            return null;
        else {
            return page.constructor.createFragment();
        }
    }

    @Override
    public long getItemId (int position) {
        Page page = visiblePages.get(position);
        return page.id;
    }

    //Clients call this on notifyDataSetChanged() to determine which items are now where.
    //Inherited implementation returns POSITION_UNCHANGED for all items, but we have to tell
    //clients that some items have actually been removed.
    @Override
    public int getItemPosition(Object item) {
        return POSITION_NONE;
        /*int position = visiblePages.indexOf(item);
        if (position >= 0) {
            return position;
        } else {
            return POSITION_NONE;
        }*/
    }

    @Override
    public CharSequence getPageTitle(int position) {
        Page page = visiblePages.get(position);
        return page.title;
    }


}
