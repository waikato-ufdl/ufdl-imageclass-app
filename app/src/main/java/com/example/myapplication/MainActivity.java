package com.example.myapplication;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import com.example.myapplication.ui.settings.Utility;
import com.github.waikatoufdl.ufdl4j.Client;
import com.github.waikatoufdl.ufdl4j.action.Datasets;
import com.github.waikatoufdl.ufdl4j.action.ImageClassificationDatasets;
import com.github.waikatoufdl.ufdl4j.action.Licenses;
import com.github.waikatoufdl.ufdl4j.auth.MemoryOnlyStorage;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.core.view.GravityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{

    private AppBarConfiguration mAppBarConfiguration;
    private Button buttonSettings;
    private static final int VERIFY_PERMISSIONS_REQUEST = 1;
    private DatabaseHelper databaseHelper;
    private NavigationView navigationView;
    private DrawerLayout drawerLayout;
    private Client client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utility.setContext(this);
        setTheme(Utility.getTheme());
        databaseHelper = new DatabaseHelper(this);

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
        //establish a connection to the UFDL backend using server URL, username, password. Need to also provide a tokenStorageHandler to
        //handle the storage and retrieval of the access and refresh tokens which will be used in API calls.
        client = new Client("http://127.0.0.1:8000", "admin", "admin", new MemoryOnlyStorage());
        //ArrayList<byte[]> images = new ArrayList<>();


        /*
        //an example to see whether we are able to retrieve the list of licenses from the backend
        try {
            ImageClassificationDatasets action = client.action(ImageClassificationDatasets.class);
            //System.out.println("\nDatasets:");
            for (Datasets.Dataset dataset : client.datasets().list()) {
                //System.out.println(dataset.getName());
            }

            Map<String, List<String>>  categories = action.getCategories(3);

            for(Map.Entry<String, List<String>> entry: categories.entrySet()) {
                System.out.println(entry.getKey());
                images.add(client.datasets().getFile(3, entry.getKey()));
            }

            System.out.println(images.size());


            for (Licenses.License license : client.licenses().list()) {
                //System.out.println(license.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
         */
    }


    public Client getClient()
    {
        return client;
    }

    private void runActivity()
    {
        //start a thread to connect to the server
        Thread t = new Thread(() -> connectToServer());
        t.start();


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