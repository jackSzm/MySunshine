package com.example.android.sunshine.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.example.android.sunshine.app.sync.SunshineSyncAdapter;

public class MainActivity extends ActionBarActivity implements ForecastFragment.Callback {

    private static final String DETAIL_FRAGMENT_TAG = "DetailFragment";

    private boolean mTwoPane;
    private String currentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        currentLocation = Utility.getPreferredLocation(this);

        if (findViewById(R.id.weather_detail_container) != null) {
            mTwoPane = true;
            if (savedInstanceState == null) {
                replaceDetailFragment(new DetailFragment());
            }
        } else {
            mTwoPane = false;
            setupActionBar();
        }

        ForecastFragment forecastFragment = ((ForecastFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_forecast));
        forecastFragment.setUseTodayLayout(!mTwoPane);

        SunshineSyncAdapter.initializeSyncAdapter(this);
    }

    private void setupActionBar() {
        getSupportActionBar().setElevation(0f);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String location = Utility.getPreferredLocation(this);

        if (locationChanged(location)) {
            currentLocation = location;
            updateVisibleFragments();
        }
    }

    private boolean locationChanged(String location) {
        return location != null && !location.equals(currentLocation);
    }

    private void updateVisibleFragments() {
        FragmentManager fragmentManager = getSupportFragmentManager();

        ForecastFragment forecastFragment = (ForecastFragment) fragmentManager.findFragmentById(R.id.fragment_forecast);
        if (forecastFragment != null) {
            forecastFragment.onLocationChanged();
        }

        DetailFragment detailFragment = (DetailFragment) fragmentManager.findFragmentByTag(DETAIL_FRAGMENT_TAG);
        if (detailFragment != null) {
            detailFragment.onLocationChanged(currentLocation);
        }
    }

    @Override
    public void onItemSelected(Uri contentUri) {
        if (mTwoPane) {
            DetailFragment fragment = buildDetailFragment(contentUri);
            replaceDetailFragment(fragment);
        } else {
            Intent intent = new Intent(this, DetailActivity.class)
                    .setData(contentUri);
            startActivity(intent);
        }
    }

    private DetailFragment buildDetailFragment(Uri contentUri) {
        Bundle args = new Bundle();
        args.putParcelable(DetailFragment.DETAIL_URI, contentUri);
        DetailFragment fragment = new DetailFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private void replaceDetailFragment(DetailFragment fragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.weather_detail_container, fragment, DETAIL_FRAGMENT_TAG).commit();
    }
}
