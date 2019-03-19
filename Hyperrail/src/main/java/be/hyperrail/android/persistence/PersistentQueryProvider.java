/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.persistence;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.crashlytics.android.Crashlytics;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import eu.opentransport.common.contracts.TransportDataRequest;
import eu.opentransport.common.requests.ActualDisturbancesRequest;
import eu.opentransport.common.requests.LiveboardRequest;
import eu.opentransport.common.requests.RoutePlanningRequest;
import eu.opentransport.common.requests.VehicleRequest;

import static be.hyperrail.android.persistence.SuggestionType.FAVORITE;
import static be.hyperrail.android.persistence.SuggestionType.HISTORY;
import static be.hyperrail.android.persistence.SuggestionType.LIST;

/**
 * Store data about recent and favorite searches as json object in preferences.
 * For code-duplication reasons, all data is stored as a RoutePlanningRequest. Stations are stored as a query from the station, with an empty string as destination.
 */
public class PersistentQueryProvider implements Serializable {

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

    /**
     * An instance of sharedPreferences
     */
    private final SharedPreferences sharedPreferences;

    /**
     * Limit the amount of stored items per tag for performance reasons
     */
    private static final int MAX_STORED = 8;

    private static PersistentQueryProvider mInstance;

    private PersistentQueryProvider(Context context) {
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static PersistentQueryProvider getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new PersistentQueryProvider(context);
        }
        return mInstance;
    }

    /**
     * Get favorite and recent routes, and apply user preferences (order, count)
     *
     * @return Sorted array with favorite and recent route requests
     */
    public List<Suggestion<RoutePlanningRequest>> getAllRoutes() {

        int recentLimit = Integer.valueOf(sharedPreferences.getString("routes_history_count", "3"));
        int order = Integer.valueOf(sharedPreferences.getString("routes_order", "0"));
        // 0: recents before favorites
        // 1: favorites before recents

        List<Suggestion<RoutePlanningRequest>> favorites = getRoutes(FAVORITE);

        if (recentLimit == 0) {
            return favorites;
        }

        // Keep some margin to ensure that we will always show the number of recents set by the user
        List<Suggestion<RoutePlanningRequest>> recents = getRoutes(HISTORY,
                recentLimit + favorites.size());

        recents = (List<Suggestion<RoutePlanningRequest>>) removeFromCollection(recents, favorites);

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
     * Get favorite and recent liveboard requests, and apply user preferences (order, count)
     *
     * @return Sorted array with favorite and recent liveboard requests
     */
    public List<Suggestion<LiveboardRequest>> getAllStations() {
        int recentLimit = Integer.valueOf(
                sharedPreferences.getString("stations_history_count", "3"));
        int order = Integer.valueOf(sharedPreferences.getString("stations_order", "0"));
        // 0 || 2: recents before favorites
        // 1 || 3: favorites before recents

        List<Suggestion<LiveboardRequest>> favorites = getStations(FAVORITE);

        if (recentLimit == 0) {
            return favorites;
        }

        // Keep some margin to ensure that we will always show the number of recents set by the user
        List<Suggestion<LiveboardRequest>> recents = getStations(HISTORY,
                recentLimit + favorites.size());

        recents = (List<Suggestion<LiveboardRequest>>) removeFromCollection(recents,
                favorites);

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
     * Get favorite and recent vehicles, and apply user preferences (order, count)
     *
     * @return Sorted array with favorite and recent vehicle requests
     */
    public List<Suggestion<VehicleRequest>> getAllTrains() {
        int recentLimit = Integer.valueOf(sharedPreferences.getString("trains_history_count", "3"));
        int order = Integer.valueOf(sharedPreferences.getString("trains_order", "0"));
        // 0: recents before favorites
        // 1: favorites before recents

        List<Suggestion<VehicleRequest>> favorites = getTrains(FAVORITE);

        if (recentLimit == 0) {
            return favorites;
        }

        // Keep some margin to ensure that we will always show the number of recents set by the user
        List<Suggestion<VehicleRequest>> recents = getTrains(HISTORY,
                recentLimit + favorites.size());

        recents = (List<Suggestion<VehicleRequest>>) removeFromCollection(recents, favorites);

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

    public List<Suggestion<RoutePlanningRequest>> getRoutes(SuggestionType type) {
        return getRoutes(type, MAX_STORED);
    }

    public List<Suggestion<RoutePlanningRequest>> getRoutes(SuggestionType type, int limit) {
        if (type == FAVORITE) {
            return load(TAG_FAV_ROUTES, limit, type, RoutePlanningRequest.class);
        } else if (type == HISTORY) {
            return load(TAG_RECENT_ROUTES, limit, true, type, RoutePlanningRequest.class);
        }
        return null;
    }

    public List<Suggestion<LiveboardRequest>> getStations(SuggestionType type) {
        return getStations(type, MAX_STORED);
    }

    public List<Suggestion<LiveboardRequest>> getStations(SuggestionType type, int limit) {
        if (type == FAVORITE) {
            return load(TAG_FAV_STATIONS, limit, type, LiveboardRequest.class);
        } else if (type == HISTORY) {
            return load(TAG_RECENT_STATIONS, limit, true, type, LiveboardRequest.class);
        }
        return null;
    }

    public List<Suggestion<VehicleRequest>> getTrains(SuggestionType type) {
        return getTrains(type, MAX_STORED);
    }

    public List<Suggestion<VehicleRequest>> getTrains(SuggestionType type, int limit) {
        if (type == FAVORITE) {
            return load(TAG_FAV_TRAINS, limit, type, VehicleRequest.class);
        } else if (type == HISTORY) {
            return load(TAG_RECENT_TRAINS, limit, true, type, VehicleRequest.class);
        }
        return null;
    }

    public <T extends TransportDataRequest> void store(Suggestion<T> query) {
        if (query.getData().getClass() == RoutePlanningRequest.class) {
            if (query.getType() == FAVORITE) {
                store(TAG_FAV_ROUTES, (RoutePlanningRequest) query.getData(),
                        RoutePlanningRequest.class);
            } else if (query.getType() == HISTORY) {
                store(TAG_RECENT_ROUTES, (RoutePlanningRequest) query.getData(),
                        RoutePlanningRequest.class);
            }
        } else if (query.getData().getClass() == LiveboardRequest.class) {
            if (query.getType() == FAVORITE) {
                store(TAG_FAV_STATIONS, (LiveboardRequest) query.getData(),
                        LiveboardRequest.class);
            } else if (query.getType() == HISTORY) {
                store(TAG_RECENT_STATIONS, (LiveboardRequest) query.getData(),
                        LiveboardRequest.class);
            }
        } else if (query.getData().getClass() == VehicleRequest.class) {
            if (query.getType() == FAVORITE) {
                store(TAG_FAV_TRAINS, (VehicleRequest) query.getData(),
                        VehicleRequest.class);
            } else if (query.getType() == HISTORY) {
                store(TAG_RECENT_TRAINS, (VehicleRequest) query.getData(),
                        VehicleRequest.class);
            }
        }
    }

    public <T extends TransportDataRequest> void delete(Suggestion<T> query) {
        if (query.getData().getClass() == RoutePlanningRequest.class) {
            if (query.getType() == FAVORITE) {
                remove(TAG_FAV_ROUTES, query.getData());
            } else if (query.getType() == HISTORY) {
                remove(TAG_RECENT_ROUTES, query.getData());
            }
        } else if (query.getData().getClass() == LiveboardRequest.class) {
            if (query.getType() == FAVORITE) {
                remove(TAG_FAV_STATIONS, query.getData());
            } else if (query.getType() == HISTORY) {
                remove(TAG_RECENT_STATIONS, query.getData());
            }
        } else if (query.getData().getClass() == VehicleRequest.class) {
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
     * @param tag    The tag under which the query will be stored
     * @param object The query to store
     */
    private <T extends TransportDataRequest> void store(String tag, T object, Class<T> classInstance) {
        Set<String> items = new HashSet<>(
                sharedPreferences.getStringSet(tag, new HashSet<String>()));

        // If this query is already in the recents list, remove it (so we can update it)
        items = removeFromPersistentSet(items, object);

        // Keep the amount of items under the set threshold
        while (items.size() >= MAX_STORED) {
            ArrayList<Suggestion<T>> queries = setToList(items, LIST, classInstance);
            queries = sortByTime(queries);

            if (queries.isEmpty()) {
                break;
            }
            // Remove latest query
            items = removeFromPersistentSet(items, queries.get(queries.size() - 1).getData());
        }

        // Store as JSON
        try {
            JSONObject json = object.toJson();
            items.add(json.toString());

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putStringSet(tag, items);
            editor.apply();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public <T extends TransportDataRequest> boolean isFavorite(T toCheck) {
        // TODO: reduce code duplication
        if (toCheck.getClass() == RoutePlanningRequest.class) {
            for (Suggestion<RoutePlanningRequest> favorite : getRoutes(FAVORITE)) {
                if (toCheck.equalsIgnoringTime(favorite.getData())) {
                    return true;
                }
            }
        } else if (toCheck.getClass() == LiveboardRequest.class) {
            for (Suggestion<LiveboardRequest> favorite : getStations(FAVORITE)) {
                if (toCheck.equalsIgnoringTime(favorite.getData())) {
                    return true;
                }
            }
        } else if (toCheck.getClass() == VehicleRequest.class) {
            for (Suggestion<VehicleRequest> favorite : getTrains(FAVORITE)) {
                if (toCheck.equalsIgnoringTime(favorite.getData())) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Load a limited number of routeQueries from a tag
     *
     * @param limit the number of queries to load
     * @param tag   The tag to load
     * @param type  The type which should be applied to the loaded results (for use in other parts of the application, e.g. recent or favorite)
     * @return List or routeQueries
     */
    private <T extends TransportDataRequest> ArrayList<Suggestion<T>> load(String tag, int limit, SuggestionType type, Class<T> classInstance) {
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
    private <T extends TransportDataRequest> ArrayList<Suggestion<T>> load(String tag, int limit, boolean timeSensitive, SuggestionType type, Class<T> classInstance) {

        if (limit <= 0) {
            return new ArrayList<>(0);
        }

        Set<String> items = sharedPreferences.getStringSet(tag, null);

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
    private void remove(String tag, TransportDataRequest query) {
        Set<String> items = new HashSet<>(
                sharedPreferences.getStringSet(tag, new HashSet<String>()));

        // If this query is already in the recents list, remove it (so we can update it)
        items = removeFromPersistentSet(items, query);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet(tag, items);
        editor.apply();
    }

    /**
     * Clear a tag
     *
     * @param tag The tag for which all queries should be cleared
     */
    private void clear(String tag) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet(tag, new HashSet<String>());
        editor.apply();
    }

    /**
     * Sort a list by time, newest first
     *
     * @param items the routeQueries
     * @return The sorted items
     */
    private <T extends TransportDataRequest> ArrayList<Suggestion<T>> sortByTime(ArrayList<Suggestion<T>> items) {

        Collections.sort(items, new Comparator<Suggestion<T>>() {
            @Override
            public int compare(Suggestion<T> o1, Suggestion<T> o2) {
                return o2.getData().getCreatedAt().compareTo(o1.getData().getCreatedAt());
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
    private <T extends TransportDataRequest> ArrayList<Suggestion<T>> setToList(Set<String> set, SuggestionType type, Class<T> objectClass) {
        ArrayList<Suggestion<T>> results = new ArrayList<>(set.size());

        for (String entry : set) {
            try {
                JSONObject object = new JSONObject(entry);

                TransportDataRequest suggestionData;
                if (objectClass == LiveboardRequest.class) {
                    suggestionData = new LiveboardRequest(object);
                } else if (objectClass == RoutePlanningRequest.class) {
                    suggestionData = new RoutePlanningRequest(object);
                } else if (objectClass == VehicleRequest.class) {
                    suggestionData = new VehicleRequest(object);
                } else if (objectClass == RoutePlanningRequest.class) {
                    suggestionData = new RoutePlanningRequest(object);
                } else if (objectClass == ActualDisturbancesRequest.class) {
                    suggestionData = new ActualDisturbancesRequest(object);
                } else {
                    throw new IllegalStateException(
                            "Attempted to deserialize an unsupported TransportDataRequest class!");
                }

                // We verified T extends TransportDataRequest, and are sure suggestionData isan TransportDataRequest, and we've ensured we created the right object
                //noinspection unchecked
                Suggestion<T> s = new Suggestion<>((T) suggestionData, type);
                results.add(s);

            } catch (Exception exception) {
                Crashlytics.log(Level.WARNING.intValue(), "PersistentQuery",
                        "Failed to load stored request for type " + type + ": " + exception.getMessage());
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
    private <T extends TransportDataRequest> Set<String> removeFromPersistentSet(Set<String> collection, T remove) {
        // TODO: this will not work for trains - they will need a better way to compare
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
    private <T extends TransportDataRequest> Collection<Suggestion<T>> removeFromCollection(Collection<Suggestion<T>> collection, Collection<Suggestion<T>> remove) {

        Set<Suggestion<T>> toBeRemoved = new HashSet<>();
        for (Suggestion<T> entry : collection) {
            for (Suggestion<T> removalEntry : remove) {
                if (entry.getData().equalsIgnoringTime(removalEntry.getData())) {
                    toBeRemoved.add(entry);
                }
            }
        }

        collection.removeAll(toBeRemoved);
        return collection;
    }

}
