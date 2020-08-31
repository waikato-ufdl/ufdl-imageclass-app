package com.example.myapplication;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import com.example.myapplication.ui.settings.Utility;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;


public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private Button buttonSettings;
    private static final int VERIFY_PERMISSIONS_REQUEST = 1;

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


    private void runActivity()
    {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
//        FloatingActionButton fab = findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });


        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.settingsFragment)
                .setDrawerLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
    }

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