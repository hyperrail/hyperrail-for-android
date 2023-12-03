package be.hyperrail.android.adapter;

import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListAdapter;
import android.widget.TextView;

import androidx.collection.ArraySet;
import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import be.hyperrail.opentransportdata.common.models.StopLocation;

public class MultilangAutocompleteAdapter implements Filterable, ListAdapter {

    private final FragmentActivity activity;
    private final int layoutResourceId;
    private final StopLocation[] stations;

    private List<StopLocation> visibleStops = new ArrayList<>();
    private Set<DataSetObserver> observers = new ArraySet<>();

    public MultilangAutocompleteAdapter(FragmentActivity activity, int layoutResourceId, StopLocation[] stations) {
        this.activity = activity;
        this.layoutResourceId = layoutResourceId;
        this.stations = stations;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            class MultilangFilterResults extends FilterResults {
                public MultilangFilterResults(List<StopLocation> results) {
                    this.values = results;
                    this.count = results.size();
                }
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                constraint = constraint.toString().toLowerCase();
                List<StopLocation> list = new ArrayList<>();
                for (StopLocation s : stations) {
                    if (stationNameContains(constraint, s)) {
                        list.add(s);
                    }
                }
                Collections.sort(list, new AutocompleteSortComparator(constraint));
                return new MultilangFilterResults(list);
            }

            private boolean stationNameContains(CharSequence constraint, StopLocation s) {
                if (s.getName().toLowerCase().contains(constraint)) {
                    return true; // primary name is not always included in the translations
                }
                for (String name : s.getTranslations().values()) {
                    if (name.toLowerCase().contains(constraint)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                //noinspection unchecked
                updateVisibleStops((List<StopLocation>) results.values);
            }
        };
    }

    private void updateVisibleStops(List<StopLocation> stops) {
        this.visibleStops = stops;
        for (DataSetObserver observer : observers) {
            observer.onChanged();
        }
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true; // No separators or other similar elements in this adapter
    }

    @Override
    public boolean isEnabled(int position) {
        return true; // No separators or other similar elements in this adapter
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        this.observers.add(observer);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        this.observers.remove(observer);
    }

    @Override
    public int getCount() {
        return visibleStops.size();
    }

    @Override
    public StopLocation getItem(int position) {
        return visibleStops.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getSemanticId().hashCode();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return createViewFromResource(activity.getLayoutInflater(), position, convertView, parent, layoutResourceId);
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return visibleStops.isEmpty();
    }

    // From ArrayAdapter
    private View createViewFromResource(LayoutInflater inflater, int position, View convertView, ViewGroup parent, int resource) {
        final View view;
        final TextView text;

        if (convertView == null) {
            view = inflater.inflate(resource, parent, false);
        } else {
            view = convertView;
        }

        //  If no custom field is assigned, assume the whole resource is a TextView
        text = (TextView) view;

        final StopLocation item = getItem(position);
        text.setText(item.getLocalizedName());

        return view;
    }

    private static class AutocompleteSortComparator implements Comparator<StopLocation> {
        private final String lowerCaseConstraint;

        public AutocompleteSortComparator(CharSequence lowerCaseConstraint) {
            this.lowerCaseConstraint = lowerCaseConstraint.toString();
        }

        @Override
        public int compare(StopLocation o1, StopLocation o2) {
            boolean o1nameAdvantage = nameStartsWith(o1, lowerCaseConstraint);
            boolean o2nameAdvantage = nameStartsWith(o2, lowerCaseConstraint);
            if (o1nameAdvantage && !o2nameAdvantage) {
                return -1;
            }
            if (o2nameAdvantage && !o1nameAdvantage) {
                return 1;
            }
            return Float.compare(o2.getAvgStopTimes(), o1.getAvgStopTimes());
        }

        private boolean nameStartsWith(StopLocation location, String prefix) {
            if (location.getName().toLowerCase().startsWith(prefix)) {
                return true; // Name is not always included in the translations
            }
            for (String name : location.getTranslations().values()) {
                if (name.toLowerCase().startsWith(prefix)) {
                    return true;
                }
            }
            return false;
        }
    }
}
