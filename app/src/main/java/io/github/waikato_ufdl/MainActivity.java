package io.github.waikato_ufdl;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import io.github.waikato_ufdl.R;

import io.github.waikato_ufdl.ui.settings.Utility;

import com.google.android.material.navigation.NavigationView;
import androidx.core.view.GravityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{

    private AppBarConfiguration mAppBarConfiguration;
    private static final int VERIFY_PERMISSIONS_REQUEST = 1;
    private NavigationView navigationView;
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utility.setContext(this);
        setTheme(Utility.getTheme());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //check permissions array in Utility, if there are any permissions which have not been granted ask user
        //to verify permissions
        if(checkPermissionArray(Utility.PERMISSIONS))
        {
            //the whole application is dependent on these permissions, so only continue if permissions
            //have been granted
            runActivity();
        }
        else
        {
            //ask user to grant permissions
            verifyPermissions(Utility.PERMISSIONS);
        }
    }

    /**
     * A method to establish a connection to the UFDL backend
     */
    public void connectToServer() {
        Log.d("connectToServer: ", "RUNNING connectToServer()");
        Utility.connectToServer();
    }

    /**
     * A method to display a toast message
     * @param message
     */
    public void showToast(String message)
    {
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
    }

    private void runActivity()
    {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView= findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.


        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.settingsFragment, R.id.imagesFragment)
                .setDrawerLayout(drawerLayout)
                .build();


        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration );
        NavigationUI.setupWithNavController(navigationView, navController);

        //if the user has used this application before, use their details to connect to the API
        if(Utility.loadUsername() != null & Utility.loadPassword() != null & Utility.loadServerURL() != null) {
            //start a thread to connect to the server
            Thread t = new Thread(() -> connectToServer());
            t.start();
        }
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case R.id.nav_home: {
                Navigation.findNavController(this, R.id.nav_host_fragment).navigate(R.id.nav_home);
                break;
            }

            case R.id.nav_gallery: {
                Navigation.findNavController(this, R.id.nav_host_fragment).navigate(R.id.nav_gallery);
                break;
            }

            case R.id.settingsFragment: {
                Navigation.findNavController(this, R.id.nav_host_fragment).navigate(R.id.settingsFragment);
                break;
            }

            case R.id.website: {
//                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.google.com"));
//                startActivity(browserIntent);

                Log.d("WEB_INTENT", "Open website");
                startActivity( new Intent( Intent.ACTION_VIEW, Uri.parse("https://waikato-ufdl.github.io/")));
//                Log.d("WEB_INTENT", "Go home");
//                Navigation.findNavController(this, R.id.nav_host_fragment).navigate(R.id.nav_home);

                break;
            }
        }

        item.setChecked(true);
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }


    /**
     * Handles the top left button click of the navigation menu
     * @return
     */
    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }


    /**
     * check an array of permissions
     * @param permissions
     * @return
     */
    public boolean checkPermissionArray(String[] permissions)
    {
        for(int i = 0; i < permissions.length; i++)
        {
            String check = permissions[i];

            //check each individual permission
            if(!checkPermissions(check))
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Check whether a single permission has been granted
     * @param permission
     * @return
     */
    public boolean checkPermissions(String permission)
    {
        int permissionRequest = ActivityCompat.checkSelfPermission(MainActivity.this, permission);

        //permission has not been granted for the particular permission required
        if (permissionRequest != PackageManager.PERMISSION_GRANTED)
        {
            return false;
        }

        //else permission has been granted
        return true;
    }

    /**
     * Method to ask a user to verify the necessary permissions
     * @param permissions
     */
    public void verifyPermissions(String[] permissions)
    {
        ActivityCompat.requestPermissions(MainActivity.this, permissions, VERIFY_PERMISSIONS_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == VERIFY_PERMISSIONS_REQUEST) {
        Boolean allPermissionsGranted = true;
            //check if all permissions have been granted
            for (int i=0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if(allPermissionsGranted)
            {
                runActivity();
            }
            else
            {
                Intent intent = new Intent(MainActivity.this,MainActivity.class);
                startActivity(intent);
                finish();
            }
        }
    }
}