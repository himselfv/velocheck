package asdbsd.velocheck;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;

class ListViewEntry implements Comparable<ListViewEntry> {
    Integer id;
    String name;
    String status;

    public ListViewEntry(Integer id, String name, String status) {
        super();
        this.id = id;
        this.name = name;
        this.status = status;
    }

    @Override
    public int compareTo(ListViewEntry other) {
        return this.name.compareTo(other.name);
    }
}

public class ListViewAdapter extends BaseAdapter implements Filterable {
    public ArrayList<ListViewEntry> list = new ArrayList<ListViewEntry>();
    public ArrayList<ListViewEntry> filteredList = new ArrayList<ListViewEntry>();
    Activity activity;

    public ListViewAdapter(Activity activity) {
        super();
        this.activity = activity;
    }

    @Override
    public int getCount() {
        return filteredList.size();
    }

    @Override
    public Object getItem(int position) {
        return filteredList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public void clear() {
        list.clear();
        filteredList.clear();
    }

    public void addItem(ListViewEntry entry) {
        list.add(entry);
        if (mFilter.passesCurrentFilter(entry))
            filteredList.add(entry);
    }

    public void sort() {
        Collections.sort(this.list);
        Collections.sort(this.filteredList);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = activity.getLayoutInflater();

        if(convertView == null){
            convertView = inflater.inflate(R.layout.list_row, null);
        }

        TextView txtName = (TextView) convertView.findViewById(R.id.name);
        TextView txtStatus = (TextView) convertView.findViewById(R.id.status);

        ListViewEntry entry = filteredList.get(position);
        txtName.setText(entry.name);
        txtStatus.setText(entry.status);

        return convertView;
    }


    /*  Filtering  */

    @Override
    public android.widget.Filter getFilter() {
        return mFilter;
    }

    ItemFilter mFilter = new ItemFilter();
    private class ItemFilter extends Filter {
        protected String constraint = ""; //must be lowercase

        boolean passesFilter(ListViewEntry entry, String constraint) {
            return entry.name.toLowerCase().contains(constraint);
        }

        public boolean passesCurrentFilter(ListViewEntry entry) {
            return passesFilter(entry, constraint);
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {

            String filterString = constraint.toString().toLowerCase();

            FilterResults results = new FilterResults();

            final ArrayList<ListViewEntry> list = ListViewAdapter.this.list;
            int count = list.size();
            final ArrayList<ListViewEntry> nlist = new ArrayList<ListViewEntry>(count);

            ListViewEntry entry;
            for (int i = 0; i < count; i++) {
                entry = list.get(i);
                if (passesFilter(entry, filterString))
                    nlist.add(entry);
            }

            results.values = nlist;
            results.count = nlist.size();

            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            this.constraint = constraint.toString().toLowerCase();
            ListViewAdapter.this.filteredList = (ArrayList<ListViewEntry>) results.values;
            notifyDataSetChanged();
        }
    };


}
