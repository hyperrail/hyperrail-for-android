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

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.activities;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.google.firebase.analytics.FirebaseAnalytics;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import be.hyperrail.android.R;
import be.hyperrail.android.adapter.OnRecyclerItemClickListener;
import be.hyperrail.android.adapter.OnRecyclerItemLongClickListener;
import be.hyperrail.android.adapter.RouteCardAdapter;
import be.hyperrail.android.infiniteScrolling.InfiniteScrollingAdapter;
import be.hyperrail.android.infiniteScrolling.InfiniteScrollingDataSource;
import be.hyperrail.android.irail.contracts.IRailErrorResponseListener;
import be.hyperrail.android.irail.contracts.IRailSuccessResponseListener;
import be.hyperrail.android.irail.contracts.IrailDataProvider;
import be.hyperrail.android.irail.contracts.RouteTimeDefinition;
import be.hyperrail.android.irail.db.Station;
import be.hyperrail.android.irail.factories.IrailFactory;
import be.hyperrail.android.irail.implementation.Route;
import be.hyperrail.android.irail.implementation.RouteAppendHelper;
import be.hyperrail.android.irail.implementation.RouteResult;
import be.hyperrail.android.irail.implementation.requests.IrailRoutesRequest;
import be.hyperrail.android.persistence.RouteSuggestion;
import be.hyperrail.android.persistence.Suggestion;
import be.hyperrail.android.util.ErrorDialogFactory;
import be.hyperrail.android.util.OnDateTimeSetListener;

import static be.hyperrail.android.persistence.SuggestionType.FAVORITE;

public class RouteActivity extends RecyclerViewActivity<RouteResult> implements InfiniteScrollingDataSource, OnDateTimeSetListener, OnRecyclerItemClickListener<Route>, OnRecyclerItemLongClickListener<Route> {

    private RouteResult mRoutes;

    private Station mSearchFrom;
    private Station mSearchTo;
    private RouteTimeDefinition mSearchTimeType = RouteTimeDefinition.DEPART;
    private DateTime mSearchDate;

    private FirebaseAnalytics mFirebaseAnalytics;

    private boolean initialLoadCompleted = false;

    public static Intent createIntent(Context context, Station from, Station to, DateTime date, RouteTimeDefinition datetype) {
        Intent i = new Intent(context, RouteActivity.class);
        i.putExtra("from", from);
        i.putExtra("to", to);
        if (date != null) {
            i.putExtra("date", date);
        }
        i.putExtra("arrivedepart", datetype.name());
        return i;
    }

    public static Intent createIntent(Context context, Station from, Station to, DateTime date) {
        Intent i = new Intent(context, RouteActivity.class);
        i.putExtra("from", from);
        i.putExtra("to", to);
        if (date != null) {
            i.putExtra("date", date);
        }
        i.putExtra("arrivedepart", RouteTimeDefinition.DEPART);
        return i;
    }

    private Intent createShortcutIntent() {
        Intent i = new Intent(this, RouteActivity.class);
        i.putExtra("shortcut", true);
        i.putExtra("from", mSearchFrom.getId());
        i.putExtra("to", mSearchTo.getId());
        return i;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.containsKey("routes")) {
            this.mRoutes = (RouteResult) savedInstanceState.get("routes");
        }

        Bundle mSearchArgs = getIntent().getExtras();
        if (mSearchArgs == null) {
            throw new IllegalStateException("A RouteActivity requires extra parameters to be created");
        }

        if (mSearchArgs.containsKey("shortcut")) {
            mSearchFrom = IrailFactory.getStationsProviderInstance().getStationById(mSearchArgs.getString("from"));
            mSearchTo = IrailFactory.getStationsProviderInstance().getStationById(mSearchArgs.getString("to"));
            mSearchTimeType = RouteTimeDefinition.DEPART;
            mSearchDate = null;
        } else {
            mSearchFrom = (Station) mSearchArgs.getSerializable("from");
            mSearchTo = (Station) mSearchArgs.getSerializable("to");
            mSearchTimeType = RouteTimeDefinition.valueOf(mSearchArgs.getString("arrivedepart"));

            if (mSearchArgs.containsKey("date")) {
                mSearchDate = (DateTime) mSearchArgs.getSerializable("date");
            } else {
                mSearchDate = null;
            }
        }
        super.onCreate(savedInstanceState);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, mSearchFrom.getId());
        bundle.putString(FirebaseAnalytics.Param.ORIGIN, mSearchFrom.getName());
        bundle.putString(FirebaseAnalytics.Param.DESTINATION, mSearchFrom.getName());
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "route");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_SEARCH_RESULTS, bundle);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("routes", mRoutes);
    }

    @Override
    protected RouteResult getRestoredInstanceStateItems() {
        if (mRoutes == null) {
            return null;
        } else {
            return mRoutes;
        }
    }

    @Override
    protected void getInitialData() {
        getData();
    }

    protected void getData() {
        Log.d("RouteActivity", "Get original data");

        // Disable infinite scrolling until loading initial data is done
        initialLoadCompleted = false;

        // Clear the view
        showData(null);

        setTitle(R.string.title_route);
        setSubTitle(mSearchFrom.getLocalizedName() + " - " + mSearchTo.getLocalizedName());

        // Restore infinite scrolling
        ((RouteCardAdapter) vRecyclerView.getAdapter()).setInfiniteScrolling(true);

        if (mSearchDate != null) {
            vWarningNotRealtime.setVisibility(View.VISIBLE);
            DateTimeFormatter df = DateTimeFormat.forPattern(getString(R.string.warning_not_realtime_datetime));
            vWarningNotRealtimeText.setText(String.format("%s %s", getString(R.string.warning_not_realtime), df.print(mSearchDate)));
        } else {
            vWarningNotRealtime.setVisibility(View.GONE);
        }

        IrailDataProvider api = IrailFactory.getDataProviderInstance();
        api.abortAllQueries();

        //TODO: pass this request to the activity instead of loose parameters
        IrailRoutesRequest request = new IrailRoutesRequest(mSearchFrom, mSearchTo, mSearchTimeType, mSearchDate);
        request.setCallback(new IRailSuccessResponseListener<RouteResult>() {
                                @Override
                                public void onSuccessResponse(RouteResult data, Object tag) {
                                    vRefreshLayout.setRefreshing(false);
                                    mRoutes = data;
                                    showData(mRoutes);

                                    // Scroll past the load earlier item
                                    ((LinearLayoutManager) vRecyclerView.getLayoutManager()).scrollToPositionWithOffset(1, 0);

                                    initialLoadCompleted = true;
                                }
                            }, new IRailErrorResponseListener() {
                                @Override
                                public void onErrorResponse(Exception e, Object tag) {
                                    // only finish if we're loading new data
                                    ((RouteCardAdapter) vRecyclerView.getAdapter()).setInfiniteScrolling(false);
                                    ErrorDialogFactory.showErrorDialog(e, RouteActivity.this, mRoutes == null);
                                }
                            },
                null);

        api.getRoutes(request);
    }

    public void loadNextRecyclerviewItems() {
        if (!initialLoadCompleted) {
            ((InfiniteScrollingAdapter) vRecyclerView.getAdapter()).setNextLoaded();
            return;
        }

        RouteAppendHelper appendHelper = new RouteAppendHelper();
        appendHelper.appendRouteResult(mRoutes, new IRailSuccessResponseListener<RouteResult>() {
            @Override
            public void onSuccessResponse(RouteResult data, Object tag) {
                // data consists of both old and new routes

                if (data.getRoutes().length == mRoutes.getRoutes().length) {
                    ((
                            InfiniteScrollingAdapter) vRecyclerView.getAdapter()).disableInfiniteNext();
                    // ErrorDialogFactory.showErrorDialog(new FileNotFoundException("No results"), RouteActivity.this,  (mSearchDate == null));
                }

                mRoutes = data;
                showData(mRoutes);

                ((InfiniteScrollingAdapter) vRecyclerView.getAdapter()).setNextLoaded();

                // Scroll past the "load earlier"
                LinearLayoutManager mgr = ((LinearLayoutManager) vRecyclerView.getLayoutManager());
                if (mgr.findFirstVisibleItemPosition() == 0) {
                    mgr.scrollToPositionWithOffset(1, 0);
                }

            }
        }, new IRailErrorResponseListener() {
            @Override
            public void onErrorResponse(Exception e, Object tag) {
                ErrorDialogFactory.showErrorDialog(e, RouteActivity.this, false);
                ((RouteCardAdapter) vRecyclerView.getAdapter()).setNextLoaded();
                ((RouteCardAdapter) vRecyclerView.getAdapter()).setInfiniteScrolling(false);
            }
        });
    }

    public void loadPreviousRecyclerviewItems() {
        if (!initialLoadCompleted) {
            ((InfiniteScrollingAdapter) vRecyclerView.getAdapter()).setPrevLoaded();
            return;
        }

        RouteAppendHelper appendHelper = new RouteAppendHelper();
        appendHelper.prependRouteResult(mRoutes, new IRailSuccessResponseListener<RouteResult>() {
            @Override
            public void onSuccessResponse(RouteResult data, Object tag) {
                // data consists of both old and new routes
                if (data.getRoutes().length == mRoutes.getRoutes().length) {
                    // ErrorDialogFactory.showErrorDialog(new FileNotFoundException("No results"), RouteActivity.this,  (mSearchDate == null));
                    ((InfiniteScrollingAdapter) vRecyclerView.getAdapter()).disableInfinitePrevious();
                }

                mRoutes = data;
                showData(mRoutes);

                // Scroll past the load earlier item
                ((LinearLayoutManager) vRecyclerView.getLayoutManager()).scrollToPositionWithOffset(1, 0);

                ((InfiniteScrollingAdapter) vRecyclerView.getAdapter()).setPrevLoaded();
            }
        }, new IRailErrorResponseListener() {
            @Override
            public void onErrorResponse(Exception e, Object tag) {
                ErrorDialogFactory.showErrorDialog(e, RouteActivity.this, false);
                ((RouteCardAdapter) vRecyclerView.getAdapter()).setInfiniteScrolling(false);
                ((RouteCardAdapter) vRecyclerView.getAdapter()).setPrevLoaded();
            }
        });
    }

    protected void showData(RouteResult routeList) {
        if (mSearchDate == null) {
            vWarningNotRealtime.setVisibility(View.GONE);
        }

        if (routeList != null && routeList.getRoutes() != null && routeList.getRoutes().length > 0) {
            setTitle(R.string.title_route);
            // Ensure we show the correct from-to by showing it from the actual route result
            setSubTitle(routeList.getRoutes()[0].getDepartureStation().getLocalizedName() + " - " + routeList.getRoutes()[0].getArrivalStation().getLocalizedName());
            Log.d("RouteActivity", "Updating routes " + routeList.getRoutes().length);
        }
        Log.d("RouteActivity", "Updating routes");
        ((RouteCardAdapter) vRecyclerView.getAdapter()).updateRoutes(routeList);
    }

    @Override
    protected int getLayout() {
        return R.layout.activity_route;
    }

    @Override
    protected int getMenuLayout() {
        return R.menu.actionbar_searchresult_routes;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_swap:
                Station h = this.mSearchTo;
                this.mSearchTo = this.mSearchFrom;
                this.mSearchFrom = h;
                this.setFavoriteDisplayState(this.isFavorite());
                // Empty the screen
                this.getData();

                return true;
            case R.id.action_shortcut:
                Intent shortcutIntent = createShortcutIntent();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ShortcutInfo.Builder mShortcutInfoBuilder = new ShortcutInfo.Builder(this, mSearchFrom.getLocalizedName() + " - " + mSearchTo.getLocalizedName());
                    mShortcutInfoBuilder.setShortLabel(mSearchFrom.getLocalizedName() + " - " + mSearchTo.getLocalizedName());

                    mShortcutInfoBuilder.setLongLabel("Route from " + mSearchFrom.getLocalizedName() + " to " + mSearchTo.getLocalizedName());
                    mShortcutInfoBuilder.setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher));
                    shortcutIntent.setAction(Intent.ACTION_CREATE_SHORTCUT);
                    mShortcutInfoBuilder.setIntent(shortcutIntent);
                    ShortcutInfo mShortcutInfo = mShortcutInfoBuilder.build();
                    ShortcutManager mShortcutManager = getSystemService(ShortcutManager.class);
                    mShortcutManager.requestPinShortcut(mShortcutInfo, null);
                } else {
                    Intent addIntent = new Intent();
                    addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
                    addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, mSearchFrom.getLocalizedName() + " - " + mSearchTo.getLocalizedName());
                    addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(getApplicationContext(), R.mipmap.ic_launcher));
                    addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                    getApplicationContext().sendBroadcast(addIntent);
                }
                Snackbar.make(vLayoutRoot, R.string.shortcut_created, Snackbar.LENGTH_LONG).show();

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    protected RecyclerView.Adapter getAdapter() {
        RouteCardAdapter adapter = new RouteCardAdapter(this, vRecyclerView, this);
        adapter.setOnItemClickListener(this);
        adapter.setOnItemLongClickListener(this);
        return adapter;
    }

    @Override
    public void onDateTimePicked(DateTime date) {
        mSearchDate = date;

        // empty the view while loading
        this.showData(null);

        // load the route list again for the new date
        getData();
    }

    public void markFavorite(boolean favorite) {
        if (favorite) {
            mPersistentQueryProvider.store(new Suggestion<>(new RouteSuggestion(mSearchFrom, mSearchTo), FAVORITE));
            Snackbar.make(vLayoutRoot, R.string.marked_route_favorite, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.undo, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            RouteActivity.this.markFavorite(false);
                        }
                    })
                    .show();
        } else {
            mPersistentQueryProvider.delete(new Suggestion<>(new RouteSuggestion(mSearchFrom, mSearchTo), FAVORITE));
            Snackbar.make(vLayoutRoot, R.string.unmarked_route_favorite, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.undo, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            RouteActivity.this.markFavorite(true);
                        }
                    })
                    .show();
        }
        setFavoriteDisplayState(favorite);
    }

    public boolean isFavorite() {
        return mPersistentQueryProvider.isFavorite(new RouteSuggestion(mSearchFrom, mSearchTo));
    }

    @Override
    public void onRecyclerItemClick(RecyclerView.Adapter sender, Route object) {

    }

    @Override
    public void onRecyclerItemLongClick(RecyclerView.Adapter sender, Route object) {
        // TODO: cleaner way to detect clicked route, likely to break when inserting date headers!
        Intent i = RouteDetailActivity.createIntent(this, object);
        this.startActivity(i);
    }
}
