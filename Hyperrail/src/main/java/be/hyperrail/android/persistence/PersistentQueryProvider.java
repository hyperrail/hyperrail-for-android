/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.persistence;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.firebase.crash.FirebaseCrash;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import be.hyperrail.android.irail.contracts.IrailStationProvider;
import be.hyperrail.android.irail.db.Station;
import be.hyperrail.android.irail.factories.IrailFactory;

import static be.hyperrail.android.persistence.SuggestionType.FAVORITE;
import static be.hyperrail.android.persistence.SuggestionType.HISTORY;
import static be.hyperrail.android.persistence.SuggestionType.LIST;

/**
 * Store data about recent and favorite searches as json object in preferences.
 * For code-duplication reasons, all data is stored as a RouteSuggestion. Stations are stored as a query from the station, with an empty string as destination.
 */
public class PersistentQueryProvider {

    /**
     * Name of the preferences file
     */
    private static final String PREFERENCES_NAME = "queries";

    /**
     * Tag under which recent routes are stored
     */
    private static final String TAG_RECENT_ROUTES = "recent_routes";

    /**
     * Tag under which recent stations are stored
     */
    private static final String TAG_RECENT_STATIONS = "recent_stations";

    /**
     * Tag under which recent stations are stored
     */
    private static final String TAG_RECENT_TRAINS = "recent_trains";

    /**
     * Tag under which favorite routes are stored
     */
    private static final String TAG_FAV_ROUTES = "fav_routes";

    /**
     * Tag under which favorite routes are stored
     */
    private static final String TAG_FAV_STATIONS = "fav_stations";

    /**
     * Tag under which favorite trains are stored
     */
    private static final String TAG_FAV_TRAINS = "fav_trains";

    private final Context context;

    /**
     * An instance of sharedPreferences
     */
    private final SharedPreferences sharedPreferences;

    /**
     * Limit the amount of stored items per tag for performance reasons
     */
    private static final int MAX_STORED = 64;

    private final IrailStationProvider stationProvider;

    public PersistentQueryProvider(Context context) {
        this.context = context;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.stationProvider = IrailFactory.getStationsProviderInstance();
    }

    /**
     * Get the favorite routes
     *
     * @return The list of favorite routes
     * @deprecated
     */
    public List<Suggestion<RouteSuggestion>> getFavoriteRoutes() {

        List<Suggestion<RouteSuggestion>> results = load(TAG_FAV_ROUTES, SuggestionType.FAVORITE, RouteSuggestion.class);
        results = sortByName(results);
        return results;
    }

    /**
     * Mark a route as favorite
     *
     * @param from The origin station name
     * @param to   The destination station name
     * @deprecated
     */
    public void addFavoriteRoute(Station from, Station to) {
        store(TAG_FAV_ROUTES, new RouteSuggestion(from, to), RouteSuggestion.class);
    }

    /**
     * Check if a route is a favorite
     *
     * @param from The origin station
     * @param to   The destination station
     * @deprecated
     */
    public boolean isFavoriteRoute(Station from, Station to) {
        for (Suggestion<RouteSuggestion> q : getFavoriteRoutes()) {
            if (q.getData().from.getId().equals(from.getId()) && q.getData().to.getId().equals(to.getId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Unmark a route as favorite
     *
     * @param from The origin station name
     * @param to   The destination station name
     */
    public void removeFavoriteRoute(Station from, Station to) {
        remove(TAG_FAV_ROUTES, new RouteSuggestion(from, to));
    }

    /**
     * Unmark a route as favorite
     *
     * @deprecated
     */
    public void removeFavoriteRoute(RouteSuggestion suggestion) {
        remove(TAG_FAV_ROUTES, suggestion);
    }

    /**
     * Clear favorite routes
     *
     * @deprecated
     */
    public void clearFavoriteRoutes() {
        clear(TAG_FAV_ROUTES);
    }

    /**
     * Get recent routes
     *
     * @return List of recent routes
     * @deprecated
     */
    public List<Suggestion<RouteSuggestion>> getRecentRoutes() {
        return getRecentRoutes(sharedPreferences.getInt("routes_history_count", 3));
    }

    /**
     * Get the last n recent routes
     *
     * @param limit the number of recent routes to retrieve
     * @return The list of the last recent routes
     * @deprecated
     */
    public List<Suggestion<RouteSuggestion>> getRecentRoutes(int limit) {
        return load(TAG_RECENT_ROUTES, limit, true, SuggestionType.HISTORY, RouteSuggestion.class);
    }

    /**
     * Add a route to recents
     *
     * @param from The origin station name
     * @param to   The destination station name
     * @deprecated
     */
    public void addRecentRoute(Station from, Station to) {
        store(TAG_RECENT_ROUTES, new RouteSuggestion(from, to), RouteSuggestion.class);
    }

    /**
     * Remove a route from recents
     *
     * @param from The origin station name
     * @param to   The destination station name
     * @deprecated
     */
    public void removeRecentRoute(Station from, Station to) {
        RouteSuggestion q = new RouteSuggestion(from, to);
        remove(TAG_RECENT_ROUTES, q);
    }

    public void removeRecentRoute(RouteSuggestion route) {
        remove(TAG_RECENT_ROUTES, route);
    }

    /**
     * Get favorite stations
     *
     * @return List of favorite stations
     * @deprecated
     */
    public List<Suggestion<StationSuggestion>> getFavoriteStations() {
        List<Suggestion<StationSuggestion>> results = load(TAG_FAV_STATIONS, FAVORITE, StationSuggestion.class);
        results = sortByName(results);
        return results;
    }

    /**
     * Add a station as favorite
     *
     * @param station The station
     * @deprecated
     */
    public void addFavoriteStation(Station station) {
        // Store as a routeQuery without a destination
        store(TAG_FAV_STATIONS, new StationSuggestion(station), StationSuggestion.class);
    }

    /**
     * Check if a station is a favorite
     *
     * @param station The station
     * @deprecated
     */
    public boolean isFavoriteStation(Station station) {
        for (Suggestion<StationSuggestion> q : getFavoriteStations()) {
            if (q.getData().equals(station)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Unmark a station as favorite
     *
     * @param station The station
     * @deprecated
     */
    public void removeFavoriteStation(Station station) {
        remove(TAG_FAV_STATIONS, new StationSuggestion(station));
    }

    /**
     * Get recent stations
     *
     * @return The list of recent stations
     * @deprecated
     */
    public List<Suggestion<StationSuggestion>> getRecentStations() {
        return getRecentStations(sharedPreferences.getInt("stations_history_count", 3));
    }

    /**
     * Get the n most recent station searches
     *
     * @param limit the number of recent searches to return
     * @return the most recent searches
     * @deprecated
     */
    public List<Suggestion<StationSuggestion>> getRecentStations(int limit) {
        return load(TAG_RECENT_STATIONS, limit, true, SuggestionType.HISTORY, StationSuggestion.class);
    }

    /**
     * Add a station to recents
     *
     * @param station The station
     * @deprecated
     */
    public void addRecentStation(Station station) {
        store(TAG_RECENT_STATIONS, new StationSuggestion(station), StationSuggestion.class);
    }

    /**
     * Remove a station from recents
     *
     * @param station The station
     * @deprecated
     */
    public void removeRecentStation(Station station) {
        StationSuggestion q = new StationSuggestion(station);
        remove(TAG_RECENT_STATIONS, q);
    }

    /**
     * Get favorite and recent routes, and apply user preferences (order, count)
     *
     * @return Sorted array with favorite and recent route queries (from, to)
     */
    public List<Suggestion<RouteSuggestion>> getAllRoutes() {

        int recentLimit = Integer.valueOf(sharedPreferences.getString("routes_history_count", "3"));
        int order = Integer.valueOf(sharedPreferences.getString("routes_order", "0"));
        // 0: recents before favorites
        // 1: favorites before recents

        List<Suggestion<RouteSuggestion>> favorites = getRoutes(FAVORITE);

        if (recentLimit == 0) {
            return favorites;
        }

        // Keep some margin to ensure that we will always show the number of recents set by the user
        List<Suggestion<RouteSuggestion>> recents = getRoutes(HISTORY,recentLimit + favorites.size());

        recents = (List<Suggestion<RouteSuggestion>>) removeFromCollection(recents, favorites);

        if (recents.size() > recentLimit) {
            recents = recents.subList(0, recentLimit);
        }

        if (order == 0) {
            recents.addAll(favorites);
            return recents;
        } else {
            favorites.addAll(recents);
            return favorites;
        }
    }

    /**
     * Get favorite and recent stations, and apply user preferences (order, count)
     *
     * @return Sorted array with favorite and recent station names
     */
    public List<Suggestion<StationSuggestion>> getAllStations() {
        int recentLimit = Integer.valueOf(sharedPreferences.getString("stations_history_count", "3"));
        int order = Integer.valueOf(sharedPreferences.getString("stations_order", "0"));
        // 0 || 2: recents before favorites
        // 1 || 3: favorites before recents

        List<Suggestion<StationSuggestion>> favorites = getStations(FAVORITE);

        if (recentLimit == 0) {
            return favorites;
        }

        // Keep some margin to ensure that we will always show the number of recents set by the user
        List<Suggestion<StationSuggestion>> recents = getStations(HISTORY, recentLimit + favorites.size());

        recents = (List<Suggestion<StationSuggestion>>) removeFromCollection(recents, favorites);

        if (recents.size() > recentLimit) {
            recents = recents.subList(0, recentLimit);
        }

        if (order == 0 || order == 2) {
            recents.addAll(favorites);
            return recents;
        } else {
            favorites.addAll(recents);
            return favorites;
        }
    }

    /**
     * Get favorite and recent stations, and apply user preferences (order, count)
     *
     * @return Sorted array with favorite and recent station names
     */
    public List<Suggestion<TrainSuggestion>> getAllTrains() {
        int recentLimit = Integer.valueOf(sharedPreferences.getString("stations_history_count", "3"));
        int order = Integer.valueOf(sharedPreferences.getString("routes_order", "0"));
        // 0 || 2: recents before favorites
        // 1 || 3: favorites before recents

        List<Suggestion<TrainSuggestion>> favorites = getTrains(FAVORITE);

        if (recentLimit == 0) {
            return favorites;
        }

        // Keep some margin to ensure that we will always show the number of recents set by the user
        List<Suggestion<TrainSuggestion>> recents = getTrains(HISTORY, recentLimit + favorites.size());

        recents = (List<Suggestion<TrainSuggestion>>) removeFromCollection(recents, favorites);

        if (recents.size() > recentLimit) {
            recents = recents.subList(0, recentLimit);
        }

        if (order == 0) {
            recents.addAll(favorites);
            return recents;
        } else {
            favorites.addAll(recents);
            return favorites;
        }
    }

    public List<Suggestion<RouteSuggestion>> getRoutes(SuggestionType type) {
        return getRoutes(type, MAX_STORED);
    }

    public List<Suggestion<RouteSuggestion>> getRoutes(SuggestionType type, int limit) {
        if (type == FAVORITE) {
            return load(TAG_FAV_ROUTES, limit, type, RouteSuggestion.class);
        } else if (type == HISTORY) {
            return load(TAG_RECENT_ROUTES, limit, type, RouteSuggestion.class);
        }
        return null;
    }

    public List<Suggestion<StationSuggestion>> getStations(SuggestionType type) {
        return getStations(type, MAX_STORED);
    }

    public List<Suggestion<StationSuggestion>> getStations(SuggestionType type, int limit) {
        if (type == FAVORITE) {
            return load(TAG_FAV_STATIONS, limit, type, StationSuggestion.class);
        } else if (type == HISTORY) {
            return load(TAG_RECENT_STATIONS, limit, type, StationSuggestion.class);
        }
        return null;
    }

    public List<Suggestion<TrainSuggestion>> getTrains(SuggestionType type) {
        return getTrains(type, MAX_STORED);
    }

    public List<Suggestion<TrainSuggestion>> getTrains(SuggestionType type, int limit) {
        if (type == FAVORITE) {
            return load(TAG_FAV_TRAINS, limit, type, TrainSuggestion.class);
        } else if (type == HISTORY) {
            return load(TAG_RECENT_TRAINS, limit, type, TrainSuggestion.class);
        }
        return null;
    }

    public <T extends Suggestable> void store(Suggestion<T> query) {
        if (query.getData().getClass() == RouteSuggestion.class) {
            if (query.getType() == FAVORITE) {
                store(TAG_FAV_ROUTES, (RouteSuggestion) query.getData(), RouteSuggestion.class);
            } else if (query.getType() == HISTORY) {
                store(TAG_RECENT_ROUTES, (RouteSuggestion) query.getData(), RouteSuggestion.class);
            }
        } else if (query.getData().getClass() == StationSuggestion.class) {
            if (query.getType() == FAVORITE) {
                store(TAG_FAV_STATIONS, (StationSuggestion) query.getData(), StationSuggestion.class);
            } else if (query.getType() == HISTORY) {
                store(TAG_RECENT_STATIONS, (StationSuggestion) query.getData(), StationSuggestion.class);
            }
        } else if (query.getData().getClass() == TrainSuggestion.class) {
            if (query.getType() == FAVORITE) {
                store(TAG_FAV_TRAINS, (TrainSuggestion) query.getData(), TrainSuggestion.class);
            } else if (query.getType() == HISTORY) {
                store(TAG_RECENT_TRAINS, (TrainSuggestion) query.getData(), TrainSuggestion.class);
            }
        }
    }

    public <T extends Suggestable> void delete(Suggestion<T> query) {
        if (query.getData().getClass() == RouteSuggestion.class) {
            if (query.getType() == FAVORITE) {
                remove(TAG_FAV_ROUTES, query.getData());
            } else if (query.getType() == HISTORY) {
                remove(TAG_RECENT_ROUTES, query.getData());
            }
        } else if (query.getData().getClass() == StationSuggestion.class) {
            if (query.getType() == FAVORITE) {
                remove(TAG_FAV_STATIONS, query.getData());
            } else if (query.getType() == HISTORY) {
                remove(TAG_RECENT_STATIONS, query.getData());
            }
        } else if (query.getData().getClass() == TrainSuggestion.class) {
            if (query.getType() == FAVORITE) {
                remove(TAG_FAV_TRAINS, query.getData());
            } else if (query.getType() == HISTORY) {
                remove(TAG_RECENT_TRAINS, query.getData());
            }
        }
    }

    /**
     * Store a query under a tag
     *
     * @param tag   The tag under which the query will be stored
     * @param query The query to store
     */
    private <T extends Suggestable> void store(String tag, T query, Class<T> classInstance) {
        store(tag, query, MAX_STORED, classInstance);
    }

    /**
     * Store a query under a tag
     *
     * @param tag    The tag under which the query will be stored
     * @param object The query to store
     * @param limit  The maximum number of items to store. If exceeded, oldest will be removed
     */
    private <T extends Suggestable> void store(String tag, T object, int limit, Class<T> classInstance) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
        Set<String> items = new HashSet<>(settings.getStringSet(tag, new HashSet<String>()));

        // If this query is already in the recents list, remove it (so we can update it)
        items = removeFromPersistentSet(items, object);

        // Keep the amount of items under the set threshold
        while (items.size() >= limit) {
            ArrayList<Suggestion<T>> queries = setToList(items, LIST, classInstance);
            queries = sortByTime(queries);
            // Remove latest query
            items = removeFromPersistentSet(items, queries.get(queries.size() - 1).getData());
        }

        // Store as JSON
        try {
            JSONObject json = object.serialize();
            items.add(json.toString());

            SharedPreferences.Editor editor = settings.edit();
            editor.putStringSet(tag, items);
            editor.apply();

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    /**
     * Load routeQueries from a tag
     *
     * @param tag  The tag to load
     * @param type The type which should be applied to the loaded results (for use in other parts of the application, e.g. recent or favorite)
     * @return List or routeQueries
     */
    private <T extends Suggestable> ArrayList<Suggestion<T>> load(String tag, SuggestionType type, Class<T> classInstance) {
        return load(tag, Integer.MAX_VALUE, type, classInstance);
    }

    /**
     * Load a limited number of routeQueries from a tag
     *
     * @param limit the number of queries to load
     * @param tag   The tag to load
     * @param type  The type which should be applied to the loaded results (for use in other parts of the application, e.g. recent or favorite)
     * @return List or routeQueries
     */
    private <T extends Suggestable> ArrayList<Suggestion<T>> load(String tag, int limit, SuggestionType type, Class<T> classInstance) {
        return load(tag, limit, false, type, classInstance);
    }

    /**
     * Load a limited number of routeQueries from a tag
     *
     * @param limit         the number of queries to load
     * @param timeSensitive Whether or not the results should be ordered by date (most recent first
     * @param tag           The tag to load
     * @param type          The type which should be applied to the loaded results (for use in other parts of the application, e.g. recent or favorite)
     * @return List or routeQueries
     */
    private <T extends Suggestable> ArrayList<Suggestion<T>> load(String tag, int limit, boolean timeSensitive, SuggestionType type, Class<T> classInstance) {

        if (limit <= 0) {
            return new ArrayList<>(0);
        }

        SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
        Set<String> items = settings.getStringSet(tag, null);

        if (items == null) {
            return new ArrayList<>(0);
        }

        ArrayList<Suggestion<T>> results = setToList(items, type, classInstance);

        // apply time sort
        if (timeSensitive) {
            results = sortByTime(results);
        }

        // apply limit
        if (results.size() > limit) {
            results.subList(0, limit - 1);
        }

        return results;
    }

    /**
     * Remove a query from a tag
     *
     * @param tag   The tag to remove from
     * @param query The query to remove
     */
    private void remove(String tag, Suggestable query) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
        Set<String> items = new HashSet<>(settings.getStringSet(tag, new HashSet<String>()));

        // If this query is already in the recents list, remove it (so we can update it)
        items = removeFromPersistentSet(items, query);

        SharedPreferences.Editor editor = settings.edit();
        editor.putStringSet(tag, items);
        editor.apply();
    }

    /**
     * Clear a tag
     *
     * @param tag The tag for which all queries should be cleared
     */
    private void clear(String tag) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putStringSet(tag, new HashSet<String>());
        editor.apply();
    }

    /**
     * Sort a list by name
     *
     * @param items the routeQueries
     * @return The sorted items
     */
    private <T extends Suggestable> List<Suggestion<T>> sortByName(List<Suggestion<T>> items) {
        Collections.sort(items, new Comparator<Suggestion<T>>() {
            @Override
            public int compare(Suggestion<T> o1, Suggestion<T> o2) {
                return o1.getData().getSortingName().compareTo(o2.getData().getSortingName());
            }
        });
        return items;
    }

    /**
     * Sort a list by time, newest first
     *
     * @param items the routeQueries
     * @return The sorted items
     */
    private <T extends Suggestable> ArrayList<Suggestion<T>> sortByTime(ArrayList<Suggestion<T>> items) {

        Collections.sort(items, new Comparator<Suggestion<T>>() {
            @Override
            public int compare(Suggestion<T> o1, Suggestion<T> o2) {
                return o2.getData().getSortingDate().compareTo(o1.getData().getSortingDate());
            }
        });

        return items;
    }

    /**
     * Load a stringset to a list of routeQueries
     *
     * @param set  The stringset
     * @param type The type which should be assigned to these queries
     * @return List of queries
     */
    private <T extends Suggestable> ArrayList<Suggestion<T>> setToList(Set<String> set, SuggestionType type, Class<T> objectClass) {
        ArrayList<Suggestion<T>> results = new ArrayList<>(set.size());

        for (String entry : set) {
            try {
                JSONObject object = new JSONObject(entry);

                T suggestionData = objectClass.newInstance();
                suggestionData.deserialize(object);

                Suggestion<T> s = new Suggestion<>(suggestionData, type);
                results.add(s);

            } catch (JSONException exception) {
                FirebaseCrash.logcat(Level.WARNING.intValue(), "PersistentQuery", "Failed to load routequery for type " + type.toString() + ": " + exception.getMessage());
                // ignored
            } catch (IllegalAccessException | InstantiationException e) {
                e.printStackTrace();
            }
        }

        return results;
    }

    /**
     * Remove a query from a stringset
     *
     * @param collection The items from which should be removed
     * @param remove     The item to remove
     * @return The filtered collection
     */
    private <T extends Suggestable> Set<String> removeFromPersistentSet(Set<String> collection, T remove) {
        Set<String> toBeRemoved = new HashSet<>();
        for (String entry : collection) {
            try {
                JSONObject object = new JSONObject(entry);
                if (remove.equals(object)) {
                    toBeRemoved.add(entry);
                }
            } catch (JSONException exception) {
                // ignored
            }
        }

        collection.removeAll(toBeRemoved);
        return collection;
    }

    /**
     * Remove queries from another collection
     *
     * @param collection The items from which should be removed
     * @param remove     The items to remove
     * @return The filtered collection
     */
    private <T extends Suggestable> Collection<Suggestion<T>> removeFromCollection(Collection<Suggestion<T>> collection, Collection<Suggestion<T>> remove) {

        Set<Suggestion<T>> toBeRemoved = new HashSet<>();
        for (Suggestion<T> entry : collection) {
            for (Suggestion<T> removalEntry : remove) {
                if (entry.getData().equals(removalEntry.getData())) {
                    toBeRemoved.add(entry);
                }
            }
        }

        collection.removeAll(toBeRemoved);
        return collection;
    }

}
