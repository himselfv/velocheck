package asdbsd.velocheck;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class MapFragment extends Fragment {
    MainActivity activity;
    ListViewAdapter adapter;

    //Pass the source for the list
    public MapFragment(ListViewAdapter adapter) {
        super();
        //I know we're supposed to pass data through bundle because Android may destroy
        //and recreate us and yadda yadda
        //We're too stupid for that, okay?
        this.adapter = adapter;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_map, container, false);

        activity = (MainActivity) this.getActivity();

        return rootView;
    }

}
