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
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import be.hyperrail.android.util.ArrayUtils;

/**
 * Store data about recent and favorite searches as json object in preferences.
 * For code-duplication reasons, all data is stored as a RouteQuery. Stations are stored as a query from the station, with an empty string as destination.
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
     * Tag under which favorite routes are stored
     */
    private static final String TAG_FAV_ROUTES = "fav_routes";

    /**
     * Tag under which favorite routes are stored
     */
    private static final String TAG_FAV_STATIONS = "fav_stations";

    private final Context context;

    /**
     * An instance of sharedPreferences
     */
    private final SharedPreferences sharedPreferences;

    /**
     * Limit the amount of stored items per tag for performance reasons
     */
    private static final int MAX_STORED = 64;

    public PersistentQueryProvider(Context context) {
        this.context = context;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    /**
     * Get the favorite routes
     *
     * @return The list of favorite routes
     */
    public List<RouteQuery> getFavoriteRoutes() {
        List<RouteQuery> results = load(TAG_FAV_ROUTES, RouteQuery.RouteQueryType.FAVORITE_ROUTE);
        results = sortByName(results);
        return results;
    }

    /**
     * Mark a route as favorite
     *
     * @param from The origin station name
     * @param to   The destination station name
     */
    public void addFavoriteRoute(String from, String to) {
        store(TAG_FAV_ROUTES, new RouteQuery(from, to));
    }

    /**
     * Check if a route is a favorite
     *
     * @param from The origin station name
     * @param to   The destination station name
     */
    public boolean isFavoriteRoute(String from, String to) {
        for (RouteQuery q : getFavoriteRoutes()) {
            if (q.from.equals(from) && q.to.equals(to)) {
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
    public void removeFavoriteRoute(String from, String to) {
        remove(TAG_FAV_ROUTES, new RouteQuery(from, to));
    }

    /**
     * Clear favorite routes
     */
    public void clearFavoriteRoutes() {
        clear(TAG_FAV_ROUTES);
    }

    /**
     * Get recent routes
     *
     * @return List of recent routes
     */
    public List<RouteQuery> getRecentRoutes() {
        return getRecentRoutes(sharedPreferences.getInt("routes_history_count", 3));
    }

    /**
     * Get the last n recent routes
     *
     * @param limit the number of recent routes to retrieve
     * @return The list of the last recent routes
     */
    public List<RouteQuery> getRecentRoutes(int limit) {
        return load(TAG_RECENT_ROUTES, limit, true, RouteQuery.RouteQueryType.RECENT_ROUTE);
    }

    /**
     * Add a route to recents
     *
     * @param from The origin station name
     * @param to   The destination station name
     */
    public void addRecentRoute(String from, String to) {
        store(TAG_RECENT_ROUTES, new RouteQuery(from, to));
    }

    /**
     * Remove a route from recents
     *
     * @param from The origin station name
     * @param to   The destination station name
     */
    public void removeRecentRoute(String from, String to) {
        RouteQuery q = new RouteQuery(from, to);
        remove(TAG_RECENT_ROUTES, q);
    }

    /**
     * Clear the recent routes
     */
    public void clearRecentRoutes() {
        clear(TAG_RECENT_ROUTES);
    }

    /**
     * Get favorite stations
     *
     * @return List of favorite stations
     */
    public List<RouteQuery> getFavoriteStations() {
        List<RouteQuery> results = load(TAG_FAV_STATIONS, RouteQuery.RouteQueryType.FAVORITE_STATION);
        results = sortByName(results);
        return results;
    }

    /**
     * Add a station as favorite
     *
     * @param name The name of the station
     */
    public void addFavoriteStation(String name) {
        // Store as a routeQuery without a destination
        store(TAG_FAV_STATIONS, new RouteQuery(name, ""));
    }

    /**
     * Check if a station is a favorite
     *
     * @param name The name of the station
     */
    public boolean isFavoriteStation(String name) {
        for (RouteQuery q : getFavoriteStations()) {
            if (q.from.equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Unmark a station as favorite
     *
     * @param name The name of the station
     */
    public void removeFavoriteStation(String name) {
        remove(TAG_FAV_STATIONS, new RouteQuery(name, ""));
    }

    /**
     * Clear favorite stations
     */
    public void clearFavoriteStations() {
        clear(TAG_FAV_STATIONS);
    }

    /**
     * Get recent stations
     *
     * @return The list of recent stations
     */
    public List<RouteQuery> getRecentStations() {
        return getRecentStations(sharedPreferences.getInt("stations_history_count", 3));
    }

    /**
     * Get the n most recent station searches
     *
     * @param limit the number of recent searches to return
     * @return the most recent searches
     */
    public List<RouteQuery> getRecentStations(int limit) {
        return load(TAG_RECENT_STATIONS, limit, true, RouteQuery.RouteQueryType.RECENT_STATION);
    }

    /**
     * Add a station to recents
     *
     * @param name The station name
     */
    public void addRecentStation(String name) {
        store(TAG_RECENT_STATIONS, new RouteQuery(name, ""));
    }

    /**
     * Remove a station from recents
     *
     * @param name The station name
     */
    public void removeRecentStation(String name) {
        RouteQuery q = new RouteQuery(name, "");
        remove(TAG_RECENT_STATIONS, q);
    }

    /**
     * Clear recent stations
     */
    public void clearRecentStations() {
        clear(TAG_RECENT_STATIONS);
    }

    /**
     * Get favorite and recent routes, and apply user preferences (order, count)
     *
     * @return Sorted array with favorite and recent route queries (from, to)
     */
    public RouteQuery[] getAllRoutes() {

        int recentLimit = Integer.valueOf(sharedPreferences.getString("routes_history_count", "3"));
        int order = Integer.valueOf(sharedPreferences.getString("routes_order", "0"));
        // 0: recents before favorites
        // 1: favorites before recents

        List<RouteQuery> favorites = getFavoriteRoutes();

        if (recentLimit == 0) {
            return Arrays.copyOf(favorites.toArray(), favorites.size(), RouteQuery[].class);
        }

        // Keep some margin to ensure that we will always show the number of recents set by the user
        List<RouteQuery> recents = getRecentRoutes(recentLimit + favorites.size());

        recents = (List<RouteQuery>) removeFromCollection(recents, favorites);

        if (recents.size() > recentLimit) {
            recents = recents.subList(0, recentLimit);
        }

        Object[] concat;
        if (order == 0) {
            concat = ArrayUtils.concatenate(recents.toArray(), favorites.toArray());
        } else {
            concat = ArrayUtils.concatenate(favorites.toArray(), recents.toArray());
        }

        return Arrays.copyOf(concat, concat.length, RouteQuery[].class);
    }

    /**
     * Get favorite and recent stations, and apply user preferences (order, count)
     *
     * @return Sorted array with favorite and recent station names
     */
    public RouteQuery[] getAllStations() {
        int recentLimit = Integer.valueOf(sharedPreferences.getString("stations_history_count", "3"));
        int order = Integer.valueOf(sharedPreferences.getString("stations_order", "0"));
        // 0 || 2: recents before favorites
        // 1 || 3: favorites before recents

        List<RouteQuery> favorites = getFavoriteStations();

        if (recentLimit == 0) {
            return Arrays.copyOf(favorites.toArray(), favorites.size(), RouteQuery[].class);
        }

        // Keep some margin to ensure that we will always show the number of recents set by the user
        List<RouteQuery> recents = getRecentStations(recentLimit + favorites.size());

        recents = (List<RouteQuery>) removeFromCollection(recents, favorites);

        if (recents.size() > recentLimit) {
            recents = recents.subList(0, recentLimit);
        }

        Object[] concat;

        if (order == 0 || order == 2) {
            concat = ArrayUtils.concatenate(recents.toArray(), favorites.toArray());
        } else {
            concat = ArrayUtils.concatenate(favorites.toArray(), recents.toArray());
        }

        return Arrays.copyOf(concat, concat.length, RouteQuery[].class);
    }

    /**
     * Store a query under a tag
     *
     * @param tag   The tag under which the query will be stored
     * @param query The query to store
     */
    private void store(String tag, RouteQuery query) {
        store(tag, query, MAX_STORED);
    }

    /**
     * Store a query under a tag
     *
     * @param tag   The tag under which the query will be stored
     * @param query The query to store
     * @param limit The maximum number of items to store. If exceeded, oldest will be removed
     */
    private void store(String tag, RouteQuery query, int limit) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
        Set<String> items = new HashSet<>(settings.getStringSet(tag, new HashSet<String>()));

        // If this query is already in the recents list, remove it (so we can update it)
        items = removeFromPersistentSet(items, query);

        // Keep the amount of items under the set threshold
        while (items.size() >= limit) {
            ArrayList<RouteQuery> queries = setToList(items, RouteQuery.RouteQueryType.UNSET);
            queries = sortByTime(queries);
            // Remove latest query
            items = removeFromPersistentSet(items, queries.get(queries.size() - 1));
        }

        // Store as JSON
        try {
            JSONObject object = new JSONObject();
            object.put("from", query.from);
            object.put("to", query.to);
            object.put("created_at", new Date().getTime());
            Log.d("Persistence", "created at " + (long) object.get("created_at"));
            items.add(object.toString());

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
    private ArrayList<RouteQuery> load(String tag, RouteQuery.RouteQueryType type) {
        return load(tag, Integer.MAX_VALUE, type);
    }

    /**
     * Load a limited number of routeQueries from a tag
     *
     * @param limit the number of queries to load
     * @param tag   The tag to load
     * @param type  The type which should be applied to the loaded results (for use in other parts of the application, e.g. recent or favorite)
     * @return List or routeQueries
     */
    private ArrayList<RouteQuery> load(String tag, int limit, RouteQuery.RouteQueryType type) {
        return load(tag, limit, false, type);
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
    private ArrayList<RouteQuery> load(String tag, int limit, boolean timeSensitive, RouteQuery.RouteQueryType type) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCES_NAME, 0);
        Set<String> items = settings.getStringSet(tag, null);

        if (items == null) {
            return new ArrayList<>(0);
        }

        ArrayList<RouteQuery> results = setToList(items, type);

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
    private void remove(String tag, RouteQuery query) {
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
    private List<RouteQuery> sortByName(List<RouteQuery> items) {
        Collections.sort(items, new Comparator<RouteQuery>() {
            @Override
            public int compare(RouteQuery o1, RouteQuery o2) {
                if (!o1.from.equals(o2.from)) {
                    return o1.from.compareTo(o2.from);
                }
                return o1.to.compareTo(o2.to);
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
    private ArrayList<RouteQuery> sortByTime(ArrayList<RouteQuery> items) {

        Collections.sort(items, new Comparator<RouteQuery>() {
            @Override
            public int compare(RouteQuery o1, RouteQuery o2) {
                return o2.created_at.compareTo(o1.created_at);
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
    private ArrayList<RouteQuery> setToList(Set<String> set, RouteQuery.RouteQueryType type) {
        ArrayList<RouteQuery> results = new ArrayList<>(set.size());

        for (String entry : set) {
            try {
                JSONObject object = new JSONObject(entry);
                RouteQuery query = new RouteQuery(object.getString("from"), object.getString("to"));
                query.created_at = new Date(object.getLong("created_at"));
                query.type = type;
                results.add(query);
            } catch (JSONException exception) {
                // ignored
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
    private Set<String> removeFromPersistentSet(Set<String> collection, RouteQuery remove) {
        Set<String> toBeRemoved = new HashSet<>();
        for (String entry : collection) {
            try {
                JSONObject object = new JSONObject(entry);
                if (object.getString("from").equals(remove.from) && object.getString("to").equals(remove.to)) {
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
    private Collection<RouteQuery> removeFromCollection(Collection<RouteQuery> collection, Collection<RouteQuery> remove) {

        Set<RouteQuery> toBeRemoved = new HashSet<>();
        for (RouteQuery entry : collection) {
            for (RouteQuery removalEntry : remove) {
                if (entry.from.equals(removalEntry.from) && entry.to.equals(removalEntry.to)) {
                    toBeRemoved.add(entry);
                }
            }
        }

        collection.removeAll(toBeRemoved);
        return collection;
    }

}
