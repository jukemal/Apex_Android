package com.example.apex;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.apex.api.maps_api.MapApiServiceGenerator;
import com.example.apex.api.weather_api.WeatherApiServiceGenerator;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        getWindow().setNavigationBarColor(getResources().getColor(R.color.colorPrimary));

        WeatherApiServiceGenerator.setupRetrofit(getString(R.string.WEATHER_API_KEY));

        MapApiServiceGenerator.setupRetrofit(getString(R.string.MAPS_API_KEY));

        BottomNavigationView navView = findViewById(R.id.nav_view);

        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_navigation, R.id.navigation_parking, R.id.navigation_dashboard, R.id.navigation_profile)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.app_bar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_bluetooth) {
            Navigation.findNavController(this, R.id.nav_host_fragment).navigate(R.id.navigation_bluetooth);
        }
        return super.onOptionsItemSelected(item);
    }
}