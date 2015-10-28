package asdbsd.velocheck;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
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

public class ListViewAdapter extends BaseAdapter {
    public ArrayList<ListViewEntry> list;
    Activity activity;

    public ListViewAdapter(Activity activity) {
        super();
        this.activity = activity;
        this.list = new ArrayList<ListViewEntry>();
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public void clear() {
        list.clear();
    }

    public void addItem(ListViewEntry entry) {
        list.add(entry);
    }

    public void sort() {
        Collections.sort(this.list);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = activity.getLayoutInflater();

        if(convertView == null){
            convertView = inflater.inflate(R.layout.list_row, null);
        }

        TextView txtName = (TextView) convertView.findViewById(R.id.name);
        TextView txtStatus = (TextView) convertView.findViewById(R.id.status);

        ListViewEntry entry = list.get(position);
        txtName.setText(entry.name);
        txtStatus.setText(entry.status);

        return convertView;
    }


}
