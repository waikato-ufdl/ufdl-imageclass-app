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

import com.github.waikatoufdl.ufdl4j.action.Datasets;
import com.github.waikatoufdl.ufdl4j.action.ImageClassificationDatasets;
import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.github.waikato_ufdl.ui.images.ClassifiedImage;
import io.github.waikato_ufdl.ui.manage.ImageDataset;
import io.github.waikato_ufdl.ui.settings.Utility;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private AppBarConfiguration mAppBarConfiguration;
    private static final int VERIFY_PERMISSIONS_REQUEST = 1;
    private NavigationView navigationView;
    private DrawerLayout drawerLayout;


    private final int CREATE = 1;
    private final int UPDATE = 2;
    private final int DELETE = 3;

    ImageClassificationDatasets action;
    DBManager dbManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utility.setContext(this);
        setTheme(Utility.getTheme());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //check permissions array in Utility, if there are any permissions which have not been granted ask user
        //to verify permissions
        if (checkPermissionArray(Utility.PERMISSIONS)) {
            //the whole application is dependent on these permissions, so only continue if permissions
            //have been granted
            runActivity();
        } else {
            //ask user to grant permissions
            verifyPermissions(Utility.PERMISSIONS);
        }

        Utility.dbManager = new DBManager(getApplicationContext());
        dbManager = Utility.dbManager;

        //network monitor to monitor any changes in the network connection
        NetworkConnectivityMonitor networkMonitor = new NetworkConnectivityMonitor(getApplicationContext());

        //observe network connectivity changes and change the boolean value of the isOnline mode accordingly
        networkMonitor.observe(this, isConnected -> {
            Utility.isOnlineMode = isConnected;

            if (Utility.getClient() != null && isConnected) {
                ArrayList<ImageDataset> unsyncedDatasets = dbManager.getUnsyncedDatasets();

                try {
                    action = Utility.getClient().action(ImageClassificationDatasets.class);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                //go through each dataset which requires syncing
                Observable.fromCallable(() -> (unsyncedDatasets))
                        .flatMap(datasets -> Observable.fromIterable(unsyncedDatasets))
                        .map(dataset -> {

                            Datasets.Dataset ds;

                            //check which sync operation needs to be performed on the particular dataset
                            switch (dataset.getSyncStatus()) {

                                //create the a new dataset and update the local database
                                case CREATE:
                                    if ((ds = action.create(dataset.getName(), dataset.getDescription(), dataset.getProject(), dataset.getLicense(), dataset.isPublic(), dataset.getTags())) != null) {
                                        dbManager.setDatasetSynced(dataset.getName());
                                        dbManager.updateLocalDatasetAfterSync(ds.getPK(), dataset.getName(), dataset.getName(), dataset.getDescription(), dataset.getProject(), dataset.getLicense(), dataset.isPublic(), dataset.getTags());
                                    }
                                    break;

                                //update the dataset details on the server side
                                case UPDATE:
                                    if ((ds = action.update(dataset.getPK(), dataset.getName(), dataset.getDescription(), dataset.getProject(), dataset.getLicense(), dataset.isPublic(), dataset.getTags())) != null) {
                                        dbManager.setDatasetSynced(ds.getName());
                                    }
                                    break;

                                //delete the dataset from the backend and also remove it from the local database
                                case DELETE:
                                    if (action.delete(dataset.getPK(), true)) {
                                        dbManager.setDatasetSynced(dataset.getName());
                                        dbManager.deleteLocalDataset(dataset.getName());
                                    }
                                    break;
                            }

                            return true;
                        })
                        .observeOn(AndroidSchedulers.mainThread())
                        .onErrorResumeNext(error -> Observable.empty())
                        .subscribeOn(Schedulers.io())
                        .subscribe(new Observer<Boolean>() {
                            @Override
                            public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {
                            }

                            @Override
                            public void onNext(@io.reactivex.rxjava3.annotations.NonNull Boolean aBoolean) {

                            }

                            @Override
                            public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                                Log.e("SYNC ERROR: ", e.getMessage());
                            }


                            /** Once we are done with the dataset synchronisation, begin to synchronise images */
                            @Override
                            public void onComplete() {
                                if (dbManager.getUnsyncedDatasets().isEmpty()) {
                                    syncImages();
                                }
                            }
                        });
            }

            String connected = (isConnected) ? "Online Mode" : "Offline Mode";
            Toast.makeText(this, connected, Toast.LENGTH_SHORT).show();
        });
    }


    public void syncImages() {
        ArrayList<ClassifiedImage> unsyncedImages = dbManager.getUnsycnedImages();

        //if there is an internet connection available and there are images to sync
        if (Utility.isOnlineMode && !unsyncedImages.isEmpty()) {

            //go through each image which requires syncing
            Observable.fromCallable(() -> (unsyncedImages))
                    .flatMap(images -> Observable.fromIterable(unsyncedImages))
                    .map(image -> {
                        //perform a task on the background thread

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

                                //add the file to the server
                                if (action.addFile(datasetPK, imageFile, filename)) {
                                    //if the file has successfully been uploaded then update the image categories
                                    if (action.addCategories(datasetPK, Arrays.asList(filename), Arrays.asList(label))) {

                                        //set the image sync status to synced on the local database
                                        dbManager.setImageSynced(image.getDatasetName(), filename);

                                        Log.e("TAG : ", "CREATED");
                                    }
                                }
                                break;

                            //reclassification of an image
                            case UPDATE:
                                List<String> oldLabel;

                                //retrieve the old categories from the server side
                                if ((oldLabel = action.getCategories(datasetPK, filename)) != null) {

                                    //remove the old categories of the image and replace then with the new categories
                                    if (action.removeCategories(datasetPK, Arrays.asList(filename), oldLabel))
                                        if (action.addCategories(datasetPK, Arrays.asList(filename), Arrays.asList(label))) {
                                            //set the image status to synced
                                            dbManager.setImageSynced(image.getDatasetName(), filename);
                                            Log.e("TAG : ", "UPDATED");
                                        }
                                }
                                break;

                            //deleting an image from the server
                            case DELETE:
                                Log.e("CHECK: ", datasetPK + " " + filename);
                                if (action.deleteFile(datasetPK, filename)) {
                                    dbManager.setImageSynced(image.getDatasetName(), filename);
                                    dbManager.deleteSyncedImage(image.getDatasetName(), filename);

                                    Log.e("TAG : ", "DELETED");
                                }
                                break;
                        }

                        return true;
                    })
                    .observeOn(AndroidSchedulers.mainThread())
                    .onErrorResumeNext(error -> Observable.empty())
                    .subscribeOn(Schedulers.io())
                    .subscribe(new Observer<Boolean>() {


                        @Override
                        public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@io.reactivex.rxjava3.annotations.NonNull Boolean aBoolean) {

                        }

                        @Override
                        public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                            Log.e("SYNC ERROR: ", e.getMessage());
                        }


                        @Override
                        public void onComplete() {
                        }
                    });
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
     *
     * @param message
     */
    public void showToast(String message) {
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
    }

    private void runActivity() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.


        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.settingsFragment, R.id.imagesFragment, R.id.website)
                .setOpenableLayout(drawerLayout)
                .build();


        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        //if the user has used this application before, use their details to connect to the API
        if (Utility.loadUsername() != null & Utility.loadPassword() != null & Utility.loadServerURL() != null) {
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
                Log.d("WEB_INTENT", "Open website");
                startActivity(new Intent(
                        Intent.ACTION_VIEW, Uri.parse(String.valueOf(R.string.application_url))
                ));

                break;
            }
        }

        item.setChecked(true);
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }


    /**
     * Handles the top left button click of the navigation menu
     *
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
     *
     * @param permissions
     * @return
     */
    public boolean checkPermissionArray(String[] permissions) {
        for (int i = 0; i < permissions.length; i++) {
            String check = permissions[i];

            //check each individual permission
            if (!checkPermissions(check)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check whether a single permission has been granted
     *
     * @param permission
     * @return
     */
    public boolean checkPermissions(String permission) {
        int permissionRequest = ActivityCompat.checkSelfPermission(MainActivity.this, permission);

        //permission has not been granted for the particular permission required
        if (permissionRequest != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        //else permission has been granted
        return true;
    }

    /**
     * Method to ask a user to verify the necessary permissions
     *
     * @param permissions
     */
    public void verifyPermissions(String[] permissions) {
        ActivityCompat.requestPermissions(MainActivity.this, permissions, VERIFY_PERMISSIONS_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == VERIFY_PERMISSIONS_REQUEST) {
            Boolean allPermissionsGranted = true;
            //check if all permissions have been granted
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
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
}