package io.github.waikato_ufdl;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.github.waikatoufdl.ufdl4j.action.ImageClassificationDatasets;
import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.github.waikato_ufdl.ui.images.ClassifiedImage;
import io.github.waikato_ufdl.ui.manage.ImageDataset;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    NetworkConnectivityMonitor networkMonitor;
    private AppBarConfiguration mAppBarConfiguration;
    private static final int VERIFY_PERMISSIONS_REQUEST = 1;
    private DrawerLayout drawerLayout;
    private SessionManager sessionManager;
    private final int CREATE = 1;
    private final int UPDATE = 2;
    private final int DELETE = 3;
    private ImageClassificationDatasets action;
    private DBManager dbManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sessionManager = new SessionManager(this);
        setTheme(sessionManager.getTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //check if permissions have been granted
        if (checkPermissionArray(SessionManager.PERMISSIONS)) {
            runActivity();
        } else {
            //ask user to grant permissions
            requestPermissions(SessionManager.PERMISSIONS);
        }

        dbManager = sessionManager.getDbManager();
        //monitor any changes in the network connection
        networkMonitor = new NetworkConnectivityMonitor(getApplicationContext());
        networkMonitor.observe(this, isConnected -> {
            SessionManager.isOnlineMode = isConnected;

            if (SessionManager.getClient() != null && isConnected && sessionManager.isLoggedIn()) {
                if (action == null) action = sessionManager.getDatasetAction();
                ArrayList<ImageDataset> unsyncedDatasets = dbManager.getUnsyncedDatasets();
                syncDatasets(unsyncedDatasets);
            }
            String connected = (isConnected) ? "Online Mode" : "Offline Mode";
            Toast.makeText(this, connected, Toast.LENGTH_SHORT).show();
        });
    }

    /***
     * Method to sync all unsynced datasets to the server.
     * @param unsyncedDatasets the list of datasets to sync
     */
    private void syncDatasets(ArrayList<ImageDataset> unsyncedDatasets) {
        //go through each dataset which requires syncing
        Observable.fromCallable(() -> (unsyncedDatasets))
                .flatMap(datasets -> Observable.fromIterable(unsyncedDatasets))
                .map(dataset -> {
                    try {
                        //check which sync operation needs to be performed on the particular dataset
                        switch (dataset.getSyncStatus()) {
                            //create the a new dataset and update the local database
                            case CREATE:
                                DatasetOperations.create(dbManager, dataset.getName(), dataset.getDescription(), dataset.getProject(), dataset.getLicense(), dataset.isPublic(), dataset.getTags());
                                break;

                            //update the dataset details on the server side
                            case UPDATE:
                                DatasetOperations.update(dbManager, dataset.getPK(), dataset.getName(), dataset.getDescription(), dataset.getProject(), dataset.getLicense(), dataset.isPublic(), dataset.getTags());
                                break;

                            //delete the dataset from the backend and also remove it from the local database
                            case DELETE:
                                DatasetOperations.delete(this, dbManager, dataset.getName());
                                break;
                        }
                    } catch (Exception e){ Log.e("TAG", "Sync Error:" + e.getMessage());}
                    return true;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorResumeNext(error -> Observable.empty())
                .subscribeOn(Schedulers.io())
                .doOnError(e -> Log.e("TAG", "Dataset Sync Error: " + e.getMessage()))
                .doOnComplete(() -> {
                    //Sync images once all datasets have been synced
                    if (dbManager.getUnsyncedDatasets().isEmpty()) syncImages();
                })
                .subscribe();
    }


    /***
     * Method to sync all unsynced images to the server.
     */
    public void syncImages() {
        ArrayList<ClassifiedImage> unsyncedImages = dbManager.getUnsycnedImages();

        //if there is an internet connection available and there are images to sync
        if (SessionManager.isOnlineMode && !unsyncedImages.isEmpty()) {
            //go through each image which requires syncing
            Observable.fromCallable(() -> (unsyncedImages))
                    .flatMap(images -> Observable.fromIterable(unsyncedImages))
                    .map(image -> {
                        int datasetPK = dbManager.getDatasetPK(image.getDatasetName());
                        String filename = image.getImageFileName();
                        String label = image.getClassificationLabel();
                        File imageFile = null;

                        //check if the file path is not null before trying to create an image file
                        if (image.getFullImageFilePath() != null) {
                            imageFile = new File(image.getFullImageFilePath());
                        }

                        //check which sync operation needs to be done on the image
                        switch (image.getSyncStatus()) {
                            //create an image (uploading from mobile device to gallery)
                            case CREATE:
                                try {
                                    ImageOperations.upload(dbManager, datasetPK, image.getDatasetName(), imageFile, label);
                                } catch (Exception e) {
                                    Log.e("TAG", "API Request Failed -- Image Upload Failed");
                                }
                                break;

                            //reclassification of an image
                            case UPDATE:
                                List<String> oldLabel = action.getCategories(datasetPK, filename);
                                if (oldLabel != null) {
                                    ImageOperations.update(dbManager, datasetPK, image.getDatasetName(), filename, oldLabel, label);
                                }
                                break;

                            //deleting an image from the server
                            case DELETE:
                                ImageOperations.delete(dbManager, datasetPK, image.getDatasetName(), filename, image.getCachedFilePath());
                                break;
                        }
                        return true;
                    })
                    .observeOn(AndroidSchedulers.mainThread())
                    .onErrorResumeNext(error -> Observable.empty())
                    .subscribeOn(Schedulers.io())
                    .doOnError(e -> Log.e("TAG", "Image Sync Error: " + e.getMessage()))
                    .subscribe();
        }
    }

    /***
     * Method to setup the navigation component and then connect to the server if user login details are available
     */
    private void runActivity() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.settingsFragment, R.id.imagesFragment, R.id.website)
                .setOpenableLayout(drawerLayout)
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        //if the user has used this application before, use their details to connect to the API
        if (sessionManager.isLoggedIn()) {
            //start a thread to connect to the server
            new Thread(() -> sessionManager.connectToServer()).start();
        }
    }

    /***
     * Method to handle the navigation menu transitions when the user clicks on a navigation menu item
     * @param item the menu item that the user pressed
     * @return true if the navigation transition has been handled
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        int itemId = item.getItemId();
        if (itemId == R.id.nav_home) {
            Navigation.findNavController(this, R.id.nav_host_fragment).navigate(R.id.nav_home);
        } else if (itemId == R.id.nav_gallery) {
            Navigation.findNavController(this, R.id.nav_host_fragment).navigate(R.id.nav_gallery);
        } else if (itemId == R.id.settingsFragment) {
            Navigation.findNavController(this, R.id.nav_host_fragment).navigate(R.id.settingsFragment);
        } else if (itemId == R.id.website) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(String.valueOf(R.string.application_url))));
        }

        item.setChecked(true);
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    /***
     * This method is called whenever the user chooses to navigate up within the activity hierarchy from the action bar.
     */
    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }


    /***
     * Checks if all required permissions have been granted
     * @param permissions the set of permission to check
     * @return true if all permissions have been granted
     */
    public boolean checkPermissionArray(String[] permissions) {
        for (String check : permissions) {
            //check each individual permission
            if (!checkPermissions(check)) {
                return false;
            }
        }

        return true;
    }

    /***
     * Check whether a single permission has been granted
     * @param permission the permission to check
     * @return true if the permission has been granted
     */
    public boolean checkPermissions(String permission) {
        int permissionRequest = ActivityCompat.checkSelfPermission(this, permission);

        //return whether or not the permission has been granted
        return permissionRequest == PackageManager.PERMISSION_GRANTED;
    }

    /***
     * Method to request a set of permissions from the user
     * @param permissions the set of permissions to request
     */
    public void requestPermissions(String[] permissions) {
        ActivityCompat.requestPermissions(this, permissions, VERIFY_PERMISSIONS_REQUEST);
    }


    /***
     * Callback for the result from requesting permission from the user
     * @param requestCode the request code passed
     * @param permissions the requested permissions
     * @param grantResults the grant results for the permissions
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == VERIFY_PERMISSIONS_REQUEST) {
            boolean allPermissionsGranted = true;
            //check if all permissions have been granted
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                runActivity();
            } else {
                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }
    }

    /***
     * Toggles the navigation drawer state. If the drawer is open, it will closed. Else, if it is closed, it will be opened.
     */
    public void toggleDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }
}