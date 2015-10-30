package asdbsd.velocheck;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ParkingListAdapter extends BaseAdapter implements Filterable {
    public ArrayList<ParkingList.Parking> list = new ArrayList<ParkingList.Parking>();
    public ArrayList<ParkingList.Parking> filteredList = new ArrayList<ParkingList.Parking>();
    Activity activity;

    private Comparator<ParkingList.Parking> comparator = new Comparator<ParkingList.Parking>() {
        @Override
        public int compare(ParkingList.Parking lhs, ParkingList.Parking rhs) {
            return lhs.name.compareTo(rhs.name);
        }
    };

    public ParkingListAdapter(Activity activity) {
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

    public void addItem(ParkingList.Parking entry) {
        list.add(entry);
        if (mFilter.passesCurrentFilter(entry))
            filteredList.add(entry);
    }

    public void sort() {
        Collections.sort(this.list, comparator);
        Collections.sort(this.filteredList, comparator);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = activity.getLayoutInflater();

        if(convertView == null){
            convertView = inflater.inflate(R.layout.list_row, null);
        }

        TextView txtName = (TextView) convertView.findViewById(R.id.name);
        ImageView picStatus = (ImageView) convertView.findViewById(R.id.status_picture);

        ParkingList.Parking p = filteredList.get(position);
        txtName.setText(p.name);
        picStatus.setImageResource(p.getStateIconResource());

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

        boolean passesFilter(ParkingList.Parking entry, String constraint) {
            return entry.name.toLowerCase().contains(constraint);
        }

        public boolean passesCurrentFilter(ParkingList.Parking entry) {
            return passesFilter(entry, constraint);
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {

            String filterString = constraint.toString().toLowerCase();

            FilterResults results = new FilterResults();

            final ArrayList<ParkingList.Parking> list = ParkingListAdapter.this.list;
            int count = list.size();
            final ArrayList<ParkingList.Parking> nlist = new ArrayList<ParkingList.Parking>(count);

            ParkingList.Parking entry;
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
            ParkingListAdapter.this.filteredList = (ArrayList<ParkingList.Parking>) results.values;
            notifyDataSetChanged();
        }
    };


}
