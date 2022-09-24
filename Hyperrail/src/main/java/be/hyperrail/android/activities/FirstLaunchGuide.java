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

import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.squareup.picasso.Picasso;

import be.hyperrail.android.BuildConfig;
import be.hyperrail.android.R;
import be.hyperrail.android.logging.HyperRailLog;
import be.hyperrail.opentransportdata.OpenTransportApi;

public class FirstLaunchGuide extends AppCompatActivity {

    private static final HyperRailLog log = HyperRailLog.getLogger(FirstLaunchGuide.class);

    TabLayout mTabLayout;
    private Button mNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_launch_guide);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        ViewPager mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mTabLayout = findViewById(R.id.dotTab);
        mTabLayout.setupWithViewPager(mViewPager, true);

        mNext = findViewById(R.id.button_next);
        findViewById(R.id.button_next).setOnClickListener(v -> {
            if (mTabLayout.getSelectedTabPosition() == mTabLayout.getTabCount() - 1) {
                FirstLaunchGuide.this.finish();
            } else {
                TabLayout.Tab tab = mTabLayout.getTabAt(mTabLayout.getSelectedTabPosition() + 1);
                if (tab != null) {
                    tab.select();
                }
            }
        });

        Button skip = findViewById(R.id.button_skip);
        skip.setOnClickListener(v -> FirstLaunchGuide.this.finish());

        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (mTabLayout.getSelectedTabPosition() == mTabLayout.getTabCount() - 1) {
                    mNext.setText(R.string.finish);
                } else {
                    mNext.setText(R.string.next);
                }

                log.info("Switching to tab " + mTabLayout.getSelectedTabPosition() + " " + Picasso.get().getSnapshot().size + Picasso.get().getSnapshot().maxSize);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Not used
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Not used
            }
        });

        // Load a random station - this ensures the database gets loaded (and filled, if needed).
        // This way users can start using the app right away when searching, instead of having to wait for the database.
        SetupStationsDbTask setupTask = new SetupStationsDbTask();
        setupTask.execute();
    }

    @Override
    public void finish() {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putInt("first_launch_guide", BuildConfig.VERSION_CODE).apply();
        super.finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_first_launch_guide, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class FirstLaunchFragment extends Fragment {
        private static final String ARG_DESCRIPTION = "description";
        private static final String ARG_IMG = "image";
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_TITLE = "title";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static FirstLaunchFragment newInstance(String title, @DrawableRes int illustration, String description) {
            FirstLaunchFragment fragment = new FirstLaunchFragment();
            Bundle args = new Bundle();
            args.putString(ARG_TITLE, title);
            args.putInt(ARG_IMG, illustration);
            args.putString(ARG_DESCRIPTION, description);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_first_launch_guide, container, false);

            if (getArguments() == null) {
                return rootView;
            }

            TextView titleView = rootView.findViewById(R.id.text_title);
            titleView.setText(getArguments().getString(ARG_TITLE));
            titleView.setTextColor(getResources().getColor(R.color.colorTextAlwaysLight)); // TODO: this hard-coded light color should be cleaned up

            TextView descriptionView = rootView.findViewById(R.id.text_description);
            descriptionView.setText(getArguments().getString(ARG_DESCRIPTION));
            descriptionView.setTextColor(getResources().getColor(R.color.colorTextAlwaysLight)); // TODO: this hard-coded light color should be cleaned up

            return rootView;
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            if (getActivity() != null) {
                final ImageView imageView = view.findViewById(R.id.image);
                imageView.post(() -> {
                    int width = imageView.getWidth();
                    int height = imageView.getHeight();
                    int size = Math.min(width, height);
                    Picasso.get().load(getArguments().getInt(ARG_IMG)).config(Bitmap.Config.RGB_565).resize(size, size).into(imageView);
                });
            }
        }
    }

    private static class SetupStationsDbTask extends AsyncTask<Void, Void, Void> {

        private static final HyperRailLog log = HyperRailLog.getLogger(SetupStationsDbTask.class);

        @Override
        protected Void doInBackground(Void... voids) {
            Thread.currentThread().setName("SetupStationsDbTask");
            log.info("Preparing stoplocations database ahead of time");
            OpenTransportApi.getStopLocationProviderInstance().preloadDatabase();
            log.info("Prepared stations stoplocations ahead of time");
            return null;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        TypedArray imgs;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
            imgs = getResources().obtainTypedArray(R.array.firstlaunch_images);
        }

        @Override
        public Fragment getItem(int position) {
            return FirstLaunchFragment.newInstance(
                    getResources().getStringArray(R.array.firstlaunch_titles)[position],
                    imgs.getResourceId(position, 0),
                    getResources().getStringArray(R.array.firstlaunch_descriptions)[position]);
        }

        @Override
        public int getCount() {
            return getResources().getStringArray(R.array.firstlaunch_titles).length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            // Empty page titles, we're using dots!
            return "";
        }
    }
}
