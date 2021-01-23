package io.github.waikato_ufdl.ui.manage;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import id.zelory.compressor.Compressor;
import io.github.waikato_ufdl.DBManager;
import io.github.waikato_ufdl.MainActivity;
import io.github.waikato_ufdl.NetworkConnectivityMonitor;
import io.github.waikato_ufdl.R;
import io.github.waikato_ufdl.ui.settings.Utility;

import com.github.waikatoufdl.ufdl4j.action.Datasets;
import com.github.waikatoufdl.ufdl4j.action.ImageClassificationDatasets;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ManageFragment extends Fragment {
    private DBManager dbManager;
    private EditText datasetName, datasetDescription, datasetTags;
    private Button buttonCreateDataset;
    private Spinner spinnerLicenses, spinnerProjects;
    private ArrayAdapter<String> spinnerArrayAdapterLicenses, spinnerArrayAdapterPojects;
    private Switch datasetSwitch;
    private boolean datasetPublic = false;
    private String datasetNameText, datasetDescriptionText, datasetTagText, dName;
    private ImageClassificationDatasets action;
    private boolean isActionMode;
    private int dKey;
    private ActionMode actionMode;
    private datasetRecyclerAdapter.RecyclerViewClickListener listener;
    private RecyclerView mRecyclerView;
    private datasetRecyclerAdapter adapter;
    private String oldDatasetName;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //pass context to to Utility class & set the theme
        Utility.setContext(getContext());
        getContext().setTheme(Utility.getTheme());

        NetworkConnectivityMonitor networkMonitor = new NetworkConnectivityMonitor(getContext());

        networkMonitor.observe(this, isConnected ->
        {
            if (actionMode != null) {
                //disable & hide the delete action menu item depending on connectivity mode
                showActionBarOnlineModeOptions(isConnected);
            }
        });
    }


    public void showActionBarOnlineModeOptions(boolean state) {
        //enable/disable the download dataset function depending on the connectivity state
        actionMode.getMenu().findItem(R.id.action_download_dataset).setEnabled(state);
        actionMode.getMenu().findItem(R.id.action_download_dataset).setVisible(state);

        //enable/disable the copy dataset function depending on the connectivity state
        actionMode.getMenu().findItem(R.id.action_copy_dataset).setEnabled(state);
        actionMode.getMenu().findItem(R.id.action_copy_dataset).setVisible(state);

        //refresh the action bar menu
        actionMode.invalidate();
    }


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_manage, container, false);

        ImageButton btnNewDataset = root.findViewById(R.id.fab_add_dataset);
        btnNewDataset.setOnClickListener(view -> {
            //if actionMode is on, turn it off
            if (actionMode != null) {
                actionMode.finish();
            }
            //then proceed to initiate dataset creation window
            initiateDatasetWindow(view, true);
        });

        dbManager = Utility.dbManager;
        isActionMode = false;
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.datasets_recyclerView);
        adapter = new datasetRecyclerAdapter(getContext(), new ArrayList<>());
        RecyclerView.LayoutManager lm = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(lm);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(adapter);

        if (Utility.isOnlineMode) {
            //check that the user settings are not empty
            checkSettings(view);
        }

        displayDatasets();
    }

    /**
     * Method to check whether the required settings have been set, if not, move to settings fragment
     *
     * @param view
     */
    public void checkSettings(View view) {
        //if these main user settings are empty, this must be the user's first time using this app
        if (Utility.loadUsername() == null | Utility.loadPassword() == null | Utility.loadServerURL() == null) {
            //navigate to settings and make them enter these details
            Navigation.findNavController(view).navigate(R.id.action_nav_gallery_to_settingsFragment);
        } else {
            testConnection();
        }
    }

    /**
     * Method to test connection and then display datasets if connection successful
     */
    public void testConnection() {
        //only continue if the client is connected to the API
        Observable.fromCallable(() -> Utility.isConnected())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Boolean>() {

                    boolean connectionSuccessful = false;

                    @Override
                    public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {
                    }

                    @Override
                    public void onNext(@io.reactivex.rxjava3.annotations.NonNull Boolean connected) {
                        if (connected)
                            connectionSuccessful = true;
                    }

                    /** if an error occurs, show connection failure dialog*/
                    @Override
                    public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                        displayConnectionFailedPopup();
                    }

                    /** on completion, if the connection is successful, display datasets or else show connection failure dialog*/
                    @Override
                    public void onComplete() {
                        if (connectionSuccessful) {
                            displayDatasets();
                        } else {
                            displayConnectionFailedPopup();
                        }
                    }
                });
    }

    /**
     * Method to display a connection failure popup with one button leading to settings and the other to close the popup.
     */
    public void displayConnectionFailedPopup() {
        if (getContext() != null) {
            new SweetAlertDialog(getContext(), SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("Connection Failed")
                    .setContentText("The connection has failed. Please check your login details and try again.")
                    .setCancelText("OK")
                    .setConfirmText("Go to Settings")

                    //clicking this button will navigate the user to the settings screen
                    .setConfirmClickListener(sweetAlertDialog -> {
                        sweetAlertDialog.dismissWithAnimation();
                        //Navigation.findNavController(getView()).navigate(R.id.action_nav_gallery_to_settingsFragment);
                    })
                    //this button will just close the popup
                    .setCancelClickListener(sweetAlertDialog -> sweetAlertDialog.dismissWithAnimation())
                    .show();
        }
    }

    /**
     * Method to populate the listview with the dataset information
     */
    public void displayDatasets() {
        if (action == null && Utility.isOnlineMode) {
            Thread t = new Thread(() -> {
                try {
                    action = Utility.getClient().action(ImageClassificationDatasets.class);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            t.start();
        }

        updateUI();

    }

    /**
     * Initialises the recyclerView listener to deal with onClick and onLongClick of dataset itemviews
     *
     * @param datasetList the list of datasets to display
     */
    private void setRecyclerViewListener(ArrayList<ImageDataset> datasetList) {
        listener = new datasetRecyclerAdapter.RecyclerViewClickListener() {
            @Override
            public void onClick(View v, int position) {
                if (!isActionMode) {
                    Bundle bundle = new Bundle();
                    bundle.putInt("datasetPK", datasetList.get(position).getPK());
                    bundle.putString("datasetName", datasetList.get(position).getName());
                    highlightBackground(position);
                    //move to the images fragment to display this dataset's images
                    Navigation.findNavController(v).navigate(R.id.action_nav_gallery_to_imagesFragment, bundle);
                } else {
                    dKey = datasetList.get(position).getPK();
                    dName = datasetList.get(position).getName();
                    //actionMode.setTitle(String.format("%s Selected", dName));
                    highlightBackground(position);
                }
            }

            @Override
            public void onLongClick(View v, int position) {
                dKey = datasetList.get(position).getPK();
                dName = datasetList.get(position).getName();
                highlightBackground(position);

                //initialise Action mode
                ActionMode.Callback callback = new ActionMode.Callback() {

                    /** displays the action menu */
                    @Override
                    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                        //Initialise menu inflater & inflate menu
                        MenuInflater menuInflater = mode.getMenuInflater();
                        menuInflater.inflate(R.menu.context_menu_datasets, menu);


                        for (int i = 0; i < menu.size(); i++) {
                            menu.getItem(i).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                        }

                        //disable & hide the delete action menu item depending on connectivity mode
                        if (!Utility.isOnlineMode) {
                            menu.findItem(R.id.action_download_dataset).setEnabled(false);
                            menu.findItem(R.id.action_download_dataset).setVisible(false);
                        }

                        actionMode = mode;
                        return true;
                    }

                    /** prepare the action mode and display the number of items selected by the user*/
                    @Override
                    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                        //When action mode is preparing
                        isActionMode = true;
                        //mode.setTitle(String.format("%s Selected", dName));
                        mode.setTitle("");
                        return true;
                    }

                    /** performs a particular operation based on the menu item pressed by the user */
                    @Override
                    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                        //handles the click of an action mode item

                        //get menu id
                        int id = item.getItemId();

                        //check which menu item was clicked
                        switch (id) {
                            case R.id.action_relabel_dataset:
                                //when the user presses edit
                                initiateDatasetWindow(getView(), false);
                                break;

                            case R.id.action_copy_dataset:
                                //when the user presses edit
                                confirmCopyDataset(mode, dName);
                                break;

                            case R.id.action_delete_dataset:
                                //when user presses delete
                                deleteConfirmation(mode, dKey);
                                break;

                            case R.id.action_download_dataset:
                                //when the user presses the download icon
                                if (Utility.isOnlineMode) {
                                    //downloadDatasetContents();
                                    confirmDatasetDownload();
                                }

                        }

                        return false;
                    }

                    /** finish the action mode and remove any image background highlights*/
                    @Override
                    public void onDestroyActionMode(ActionMode mode) {
                        //when action mode is destroyed
                        isActionMode = false;
                        actionMode = null;
                        mode.finish();
                        highlightBackground(-1);
                    }
                };

                //if action mode is off, then turn start it upon long press
                if (actionMode == null) {
                    //Start action mode
                    ((MainActivity) v.getContext()).startActionMode(callback);
                } else {
                    //if we are already in action mode, then just change the name action bar title (as selection focus has changed to another dataset)
                    //actionMode.setTitle(String.format("%s Selected", dName));
                }
            }
        };

        adapter.setListener(listener);
    }


    public LinearLayout getDownloadConfirmationDialogLayout() {
        //create custom linear layout
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setHorizontalGravity(Gravity.CENTER_HORIZONTAL);

        //create views & set the view content
        CheckBox downloadCache = new CheckBox(getContext());
        downloadCache.setText("Download Full Size Images");

        TextView note = new TextView(getContext());
        note.setText("Note: If the checkbox above is not checked, only compressed images will be cached.");

        //add the views to the layout
        layout.addView(downloadCache);
        layout.addView(note);

        //set margins for each of the child views in the layout
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(5, 10, 5, 35);

        layout.getChildAt(0).setLayoutParams(params);
        layout.getChildAt(0).setPadding(20, 0, 0, 0);
        layout.getChildAt(1).setLayoutParams(params);
        layout.getChildAt(1).setPadding(20, 0, 0, 0);

        return layout;
    }

    public void confirmDatasetDownload() {

        LinearLayout layout = getDownloadConfirmationDialogLayout();
        CheckBox fullSizeDownload = (CheckBox) layout.getChildAt(0);

        new SweetAlertDialog(getContext(), SweetAlertDialog.WARNING_TYPE)
                .setCustomView(layout)
                .setTitleText("Confirm Download")
                .setConfirmText("Download")
                .setConfirmClickListener(sDialog -> {

                    //show success dialog
                    sDialog
                            .setTitleText("Successfully Donwloaded!")
                            .setContentText("The selected dataset has been downloaded!")
                            .setConfirmText("OK")
                            .setConfirmClickListener(sweetAlertDialog -> {
                                //when the user clicks ok, dismiss the popup
                                sDialog.dismissWithAnimation();

                                if(fullSizeDownload.isChecked()) {
                                    downloadDatasetContents();
                                }
                                else {
                                    downloadDatasetCache();
                                }

                            })
                            .changeAlertType(SweetAlertDialog.SUCCESS_TYPE);
                })
                .setCancelButton("Cancel", sDialog -> {
                    //if the user cancels deletion close the popup but leave them on the selection mode
                    sDialog.dismissWithAnimation();
                })
                .show();
    }

    public void downloadDatasetCache() {
        String cache_folder = "UFDL_Cache";
        ImageDataset selectedDataset = adapter.getSelectedDataset();
        Context appContext = getActivity().getApplicationContext();

        //create a cache folder if it doesn't already exist
        File cacheFolder = new File(appContext.getCacheDir(), cache_folder);
        if (!cacheFolder.exists()) {
            cacheFolder.mkdirs();
        }

        Thread downloadThread = new Thread(() ->
        {
            try {
                String[] imageFileNames = action.load(selectedDataset.getPK()).getFiles();

                Observable.fromArray(imageFileNames)
                        .map(filename -> {
                            byte[] image;
                            List<String> allLabels;
                            String classificationLabel;
                            int datasetPK = selectedDataset.getPK();

                            //get image file from backend
                            image = action.getFile(datasetPK, filename);
                            
                            allLabels = action.getCategories(datasetPK, filename);
                            classificationLabel = (allLabels.size() > 0) ? allLabels.get(0) : "Unlabelled";

                            File file = new File(cacheFolder, filename);

                            if (!file.exists()) {
                                //write image content to file in external storage
                                FileOutputStream fos = new FileOutputStream(file.getPath());
                                fos.write(image);
                                fos.close();

                                //create a compressed version of the image file
                                Compressor compressor = new Compressor(appContext);
                                File compressedImage = compressor.compressToFile(file);

                                //replace original file with compressed file
                                Files.move(compressedImage.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);

                                //delete the parent folder of where the compressed image was initially stored
                                if (compressedImage.getParentFile().exists()) {
                                    compressedImage.getParentFile().delete();
                                }
                            }

                            dbManager.insertSyncedImage(filename, classificationLabel, null, file.getPath(), selectedDataset.getName());

                            return true;
                        })
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .subscribe();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        downloadThread.start();
    }


    public void downloadDatasetContents() {
        String mainFolderName = "UFDL";
        File folder = new File(getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), mainFolderName);
        ImageDataset selectedDataset = adapter.getSelectedDataset();
        Context appContext = getActivity().getApplicationContext();

        //create main directory if it doesn't exist
        if (!folder.exists()) {
            Log.e("CREATED", folder.mkdirs() + " ");
        }

        //create dataset subdirectory
        File datasetFolder = new File(appContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/" + mainFolderName, selectedDataset.getName());
        if (!datasetFolder.exists()) {
            Log.e("CREATED SUBDIRECTORY", datasetFolder.mkdirs() + " , " + datasetFolder.getPath());
        }

        Thread downloadThread = new Thread(() ->
        {
            try {
                String[] imageFileNames = action.load(selectedDataset.getPK()).getFiles();

                Observable.fromArray(imageFileNames)
                        .map(filename -> {
                            byte[] image;
                            List<String> allLabels;
                            String classificationLabel;
                            int datasetPK = selectedDataset.getPK();

                            //get image file from backend
                            image = action.getFile(datasetPK, filename);

                            allLabels = action.getCategories(datasetPK, filename);
                            classificationLabel = (allLabels.size() > 0) ? allLabels.get(0) : "Unlabelled";

                            File file = new File(datasetFolder, filename);
                            if (!file.exists()) {
                                //write image content to file in external storage
                                FileOutputStream fos = new FileOutputStream(file.getPath());
                                Log.e("TAG", file.getPath());
                                fos.write(image);
                                fos.close();
                            }

                            dbManager.insertSyncedImage(filename, classificationLabel, file.getPath(), null, selectedDataset.getName());

                            return true;
                        })
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .subscribe();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        downloadThread.start();
    }


    /**
     * Method to initialise all the views for the dataset creation/edit window
     *
     * @param layout the layout view
     */
    public void initialiseDatasetCreationViews(View layout) {
        //get licenses & projects using database manager
        final List<String> spinnerArrayLicenses = dbManager.getLicenses();
        final List<String> spinnerArrayProjects = dbManager.getProjects();

        spinnerArrayAdapterLicenses = new ArrayAdapter<String>(getContext(), R.layout.spinner_item, spinnerArrayLicenses);
        spinnerArrayAdapterLicenses.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerArrayAdapterPojects = new ArrayAdapter<String>(getContext(), R.layout.spinner_item, spinnerArrayProjects);
        spinnerArrayAdapterPojects.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        //initialise views
        spinnerLicenses = layout.findViewById(R.id.dataset_license_spinner);
        spinnerLicenses.setAdapter(spinnerArrayAdapterLicenses);
        spinnerProjects = layout.findViewById(R.id.dataset_project_spinner);
        spinnerProjects.setAdapter(spinnerArrayAdapterPojects);
        buttonCreateDataset = layout.findViewById(R.id.createDatasetButton);

        datasetName = layout.findViewById(R.id.dataset_name_text);
        datasetDescription = layout.findViewById(R.id.dataset_description_text);
        datasetTags = layout.findViewById(R.id.dataset_tags_text);
        datasetSwitch = layout.findViewById(R.id.makePublic);
    }


    /**
     * Method to display a popup window which will be used for both dataset creation and dataset editing
     *
     * @param v the view
     */
    public void initiateDatasetWindow(View v, boolean isDatasetCreation) {
        try {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            //Inflate the view from a predefined XML layout
            View layout = inflater.inflate(R.layout.new_dataset, null);

            // create a PopupWindow
            final PopupWindow popupWindow = new PopupWindow(layout,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT, true);

            // display the popup in the center
            popupWindow.showAtLocation(v, Gravity.CENTER, 0, 0);

            //darken the background behind the popup window
            darkenBackground(popupWindow);
            initialiseDatasetCreationViews(layout);

            //we are editing a dataset so first display the current details of the data
            if (!isDatasetCreation) {
                setEditDatasetFields();
                v.invalidate();
            }


            buttonCreateDataset.setOnClickListener(view -> {

                //check that all required fields have been filled
                if (checkDetailsEntered()) {
                    datasetNameText = datasetName.getText().toString().trim();
                    datasetDescriptionText = datasetDescription.getText().toString().trim();
                    datasetTagText = datasetTags.getText().toString().trim();
                    if (datasetSwitch.isChecked()) {
                        datasetPublic = true;
                    }
                    int projectKey = dbManager.getProjectKey(spinnerProjects.getSelectedItem().toString());
                    int licenseKey = dbManager.getLicenseKey(spinnerLicenses.getSelectedItem().toString());

                    if (Utility.isOnlineMode) {
                        Thread t = new Thread(() -> {
                            Datasets.Dataset ds;
                            ImageDataset selectedDataset;
                            try {
                                if (isDatasetCreation) {
                                    //create a new dataset
                                    if ((ds = action.create(datasetNameText, datasetDescriptionText, projectKey, licenseKey, datasetPublic, datasetTagText)) != null) {
                                        dbManager.insertSyncedDataset(ds.getPK(), ds.getName(), ds.getDescription(), ds.getProject(), ds.getLicense(), ds.isPublic(), ds.getTags());
                                    }
                                } else {
                                    selectedDataset = adapter.getSelectedDataset();
                                    //update an existing dataset's details
                                    if ((ds = action.update(selectedDataset.getPK(), datasetNameText, datasetDescriptionText, projectKey, licenseKey, datasetPublic, datasetTagText)) != null) {
                                        dbManager.updateLocalDatasetAfterSync(ds.getPK(), oldDatasetName, ds.getName(), ds.getDescription(), ds.getProject(), ds.getLicense(), ds.isPublic(), ds.getTags());
                                    }
                                }
                            } catch (Exception e) {
                                if (isDatasetCreation) {
                                    dbManager.insertLocalDataset(datasetNameText, datasetDescriptionText, projectKey, licenseKey, datasetPublic, datasetTagText);
                                } else {
                                    dbManager.updateDataset(oldDatasetName, datasetNameText, datasetDescriptionText, projectKey, licenseKey, datasetPublic, datasetTagText);
                                }
                            }
                        });
                        t.start();
                    } else {
                        if (isDatasetCreation) {
                            dbManager.insertLocalDataset(datasetNameText, datasetDescriptionText, projectKey, licenseKey, datasetPublic, datasetTagText);
                        } else {
                            Log.e("CHECK: ", oldDatasetName + " " + datasetName + " " + datasetDescriptionText + " " + projectKey + " " +
                                    licenseKey + " " + datasetPublic + " " + datasetTagText);
                            dbManager.updateDataset(oldDatasetName, datasetNameText, datasetDescriptionText, projectKey, licenseKey, datasetPublic, datasetTagText);
                        }
                    }

                    popupWindow.dismiss();
                    if (actionMode != null) {
                        actionMode.finish();
                    }

                    reload();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to check if all required fields are non-empty
     *
     * @return true if all required details have been provided
     */
    public boolean checkDetailsEntered() {
        //check if any of the inputs are empty and if so, set an error message
        if (isEmpty(datasetName)) {
            datasetName.setError("Required");
            return false;
        }

        return true;
    }

    /**
     * Method to check if an EditText field is empty
     *
     * @param editText The EditText to check
     * @return
     */
    public boolean isEmpty(EditText editText) {
        return editText.getText().toString().trim().length() <= 0;
    }

    /**
     * A method to reload the fragment
     */
    public void reload() {
        // Reload current fragment
        FragmentTransaction ft = getParentFragmentManager().beginTransaction();
        if (Build.VERSION.SDK_INT >= 26) {
            ft.setReorderingAllowed(false);
        }
        ft.detach(this).attach(this).commit();
    }

    /**
     * A method to darken the background when a popup window is displayed
     *
     * @param popupWindow The popup window being displayed
     */
    public static void darkenBackground(PopupWindow popupWindow) {
        //get the popup view & context
        View container = popupWindow.getContentView().getRootView();
        Context context = popupWindow.getContentView().getContext();

        //setup window manager layout parameters to dim the background & update the view
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) container.getLayoutParams();
        layoutParams.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        layoutParams.dimAmount = 0.5f;
        windowManager.updateViewLayout(container, layoutParams);
    }

    /**
     * A method to confirm the deletion process via a popup before deleting a dataset
     *
     * @param mode the action mode
     */
    public void deleteConfirmation(ActionMode mode, int datasetKey) {
        new SweetAlertDialog(getContext(), SweetAlertDialog.WARNING_TYPE)
                .setTitleText("Are you sure?")
                .setContentText("You won't be able to recover the dataset after deletion")
                .setConfirmText("Delete")
                .setConfirmClickListener(sDialog -> {
                    //if a user accepts the deletion, delete all the selected images
                    deleteDataset(datasetKey);

                    //show a successful deletion popup
                    sDialog
                            .setTitleText("Deleted!")
                            .setContentText("The selected dataset has been deleted!")
                            .setConfirmText("OK")
                            .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                @Override
                                public void onClick(SweetAlertDialog sweetAlertDialog) {
                                    //when the user clicks ok, dismiss the popup
                                    sDialog.dismissWithAnimation();
                                    //finish action mode once a user has confirmed the deletion of images, else keep users in selection mode
                                    mode.finish();
                                }
                            })
                            .changeAlertType(SweetAlertDialog.SUCCESS_TYPE);
                })
                .setCancelButton("Cancel", new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        //if the user cancels deletion close the popup but leave them on the selection mode
                        sDialog.dismissWithAnimation();
                    }
                })
                .show();
    }

    /**
     * Method to delete the selected dataset from recycler view & backend
     *
     * @param: the primary of the dataset to delete
     */
    public void deleteDataset(int datasetKey) {
        //make an API request to delete a dataset
        ImageDataset selectedDataset = adapter.getSelectedDataset();

        if (Utility.isOnlineMode) {
            Thread t = new Thread(() -> {
                try {
                    //delete image file
                    if (action.delete(datasetKey, true)) {
                        dbManager.setDatasetSynced(selectedDataset.getName());
                        dbManager.deleteLocalDataset(selectedDataset.getName());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            t.start();
        } else {
            dbManager.deleteSyncedDataset(selectedDataset.getName());
            adapter.deleteSelectedDataset();
        }

        reload();
    }

    /**
     * A method which prompts the user for confirmation prior to creating a copy of a dataset
     *
     * @param mode        the action mode
     * @param datasetName the name to give the copied dataset
     */
    public void confirmCopyDataset(ActionMode mode, String datasetName) {

        //create edit text for new dataset name
        final EditText editText = new EditText(getContext());
        editText.setHint("New Dataset Name");
        editText.setGravity(Gravity.CENTER_HORIZONTAL);
        editText.setPadding(50, 25, 50, 50);
        editText.setHorizontallyScrolling(true);

        new SweetAlertDialog(getContext(), SweetAlertDialog.WARNING_TYPE)
                .setTitleText("Copy dataset " + datasetName + " as: ")
                .setConfirmText("Copy")
                .setCustomView(editText)
                .setConfirmClickListener(sweetAlertDialog -> {
                    String newDatasetName = editText.getText().toString().trim();

                    //if a value has been entered
                    if (newDatasetName.length() > 0) {
                        if (!newDatasetName.equals(datasetName)) {
                            //copy dataset
                            copyDataset(newDatasetName);

                            //show a success popup
                            sweetAlertDialog
                                    .setTitleText("Success!")
                                    .setContentText("The dataset " + datasetName + " has been copied as: " + newDatasetName)
                                    .setConfirmText("OK")
                                    .setConfirmClickListener(sweetAlertDialog1 -> {
                                        //when the user clicks ok, dismiss the popup
                                        sweetAlertDialog1.dismissWithAnimation();
                                        //finish action mode once a user has confirmed the reclassification
                                        mode.finish();
                                    })
                                    .changeAlertType(SweetAlertDialog.SUCCESS_TYPE);
                        } else {
                            editText.setError("Dataset name must be different");
                        }
                    } else
                        editText.setError("Please enter a classification label");
                })
                .setCancelButton("Cancel", sDialog -> {
                    //if the user clicks cancel close the popup but leave them on the selection mode
                    sDialog.dismissWithAnimation();
                })
                .show();
    }

    /**
     * Method to delete the selected dataset from the recyclerview & backend
     *
     * @param newDatasetName the name to give the copied dataset
     */
    public void copyDataset(String newDatasetName) {
        //make an API request to copy a dataset
        ImageDataset selectedDataset = adapter.getSelectedDataset();

        if (Utility.isOnlineMode) {
            Thread t = new Thread(() -> {
                try {
                    //create a copy of the dataset
                    if (action.copy(selectedDataset.getPK(), newDatasetName)) {
                        int pk = action.load(newDatasetName).getPK();
                        dbManager.insertSyncedDataset(pk, newDatasetName, selectedDataset.getDescription(), selectedDataset.getProject(), selectedDataset.getLicense(), selectedDataset.isPublic(),
                                selectedDataset.getTags());
                    }
                } catch (Exception e) {

                }

                getActivity().runOnUiThread(() -> updateUI());
            });
            t.start();
        }
    }

    /**
     * Method to display an existing dataset's details on the edit dataset window
     */
    public void setEditDatasetFields() {

        ImageDataset selectedDataset = adapter.getSelectedDataset();

        oldDatasetName = selectedDataset.getName();
        datasetName.setText(selectedDataset.getName());
        datasetDescription.setText(selectedDataset.getDescription());
        datasetTags.setText(selectedDataset.getTags());
        datasetSwitch.setChecked(selectedDataset.isPublic());
        buttonCreateDataset.setText("Update Dataset");

        //check the current project and license associated to this dataset
        int projName = spinnerArrayAdapterPojects.getPosition(dbManager.getProjectName(selectedDataset.getProject()));
        int licName = spinnerArrayAdapterLicenses.getPosition(dbManager.getLicenseName(selectedDataset.getLicense()));

        //set the spinners to reflect the current project and license details
        spinnerProjects.setSelection(projName);
        spinnerLicenses.setSelection(licName);
    }

    /**
     * Method which animates the loading of datasets
     */
    private void layoutAnimation() {
        LayoutAnimationController layoutAnimationController = AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_animation_fall_down);
        mRecyclerView.setLayoutAnimation(layoutAnimationController);
        mRecyclerView.getAdapter().notifyDataSetChanged();
        mRecyclerView.scheduleLayoutAnimation();
    }

    /**
     * Method to highlight the background of the selected dataset
     *
     * @param position the position of the selected dataset in the recyclerview
     */
    private void highlightBackground(int position) {
        adapter.setSelectedIndex(position);
        adapter.notifyDataSetChanged();
    }


    private void updateUI() {
        ArrayList<ImageDataset> datasetList = dbManager.getDatasetList();
        //set the adapter data, listener and notify change to see datasets
        adapter.setData(datasetList);
        setRecyclerViewListener(datasetList);
        adapter.notifyDataSetChanged();
    }
}