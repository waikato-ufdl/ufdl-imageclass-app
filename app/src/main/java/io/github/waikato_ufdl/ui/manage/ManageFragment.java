package io.github.waikato_ufdl.ui.manage;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.Switch;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import io.github.waikato_ufdl.DBManager;
import io.github.waikato_ufdl.MainActivity;
import io.github.waikato_ufdl.R;
import io.github.waikato_ufdl.ui.settings.Utility;
import com.github.waikatoufdl.ufdl4j.action.Datasets;
import com.github.waikatoufdl.ufdl4j.action.ImageClassificationDatasets;
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
    datasetRecyclerAdapter.RecyclerViewClickListener listener;
    RecyclerView mRecyclerView;
    datasetRecyclerAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //pass context to to Utility class & set the theme
        Utility.setContext(getContext());
        getContext().setTheme(Utility.getTheme());
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_manage, container, false);

        ImageButton btnNewDataset = root.findViewById(R.id.fab_add_dataset);
        btnNewDataset.setOnClickListener(view -> {
            //if actionMode is on, turn it off
            if(actionMode != null) {
                actionMode.finish();
            }
            //then proceed to initiate dataset creation window
            initiateDatasetWindow(view, -1); });

        dbManager = new DBManager(getContext());
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

        new Handler(Looper.getMainLooper()).postDelayed((Runnable) () -> {
            //check that the user settings are not empty
            checkSettings(view);
        }, 500);
    }

    /**
     * Method to check whether the required settings have been set, if not, move to settings fragment
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
    public void testConnection()
    {
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
                        if(connected)
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
                        if ((connectionSuccessful)) {
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
    public void displayConnectionFailedPopup()
    {
        new SweetAlertDialog(getContext(), SweetAlertDialog.ERROR_TYPE)
                .setTitleText("Connection Failed")
                .setContentText("The connection has failed. Please check your login details and try again.")
                .setCancelText("OK")
                .setConfirmText("Go to Settings")

                //clicking this button will navigate the user to the settings screen
                .setConfirmClickListener(sweetAlertDialog -> {
                    sweetAlertDialog.dismissWithAnimation();
                    Navigation.findNavController(getView()).navigate(R.id.action_nav_gallery_to_settingsFragment);
                })
                //this button will just close the popup
                .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                        sweetAlertDialog.dismissWithAnimation();
                    }
                })
                .show();
    }

    /**
     * Method to populate the listview with the dataset information
     */
    public void displayDatasets() {
        try {
            //must start a thread to retrieve the dataset information as networking operations cannot be done on the main thread
            Thread t = new Thread(() -> {
                try {
                    action = Utility.getClient().action(ImageClassificationDatasets.class);
                    final ArrayList<Datasets.Dataset> datasetList = (ArrayList<Datasets.Dataset>) action.list();
                    //set the adapter data, listener and notify change to see datasets
                    adapter.setData(datasetList);
                    getActivity().runOnUiThread(() -> {
                        layoutAnimation();
                    });
                    setRecyclerViewListener(datasetList);


                } catch (IllegalStateException e) {
                    ((MainActivity) getActivity()).showToast("Please check your username, password and server URL details in settings");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            t.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Initialises the recyclerView listener to deal with onClick and onLongClick of dataset itemviews
     * @param datasetList the list of datasets to display
     */
    private void setRecyclerViewListener(ArrayList<Datasets.Dataset> datasetList) {
        listener = new datasetRecyclerAdapter.RecyclerViewClickListener() {
            @Override
            public void onClick(View v, int position) {
                if (!isActionMode) {
                    Bundle bundle = new Bundle();
                    bundle.putInt("datasetPK", datasetList.get(position).getPK());
                    highlightBackground(position);
                    //move to the images fragment to display this dataset's images
                    Navigation.findNavController(v).navigate(R.id.action_nav_gallery_to_imagesFragment, bundle);
                } else {
                    dKey = datasetList.get(position).getPK();
                    dName = datasetList.get(position).getName();
                    actionMode.setTitle(String.format("%s Selected", dName));
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
                        actionMode = mode;
                        return true;
                    }

                    /** prepare the action mode and display the number of items selected by the user*/
                    @Override
                    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                        //When action mode is preparing
                        isActionMode = true;
                        mode.setTitle(String.format("%s Selected", dName));
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
                                initiateDatasetWindow(getView(), dKey);
                                break;

                            case R.id.action_copy_dataset:
                                //when the user presses edit
                                confirmCopyDataset(mode, dName, dKey);
                                break;

                            case R.id.action_delete_dataset:
                                //when user presses delete
                                deleteConfirmation(mode, dKey);
                                break;
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
                if(actionMode == null) {
                    //Start action mode
                    ((MainActivity) v.getContext()).startActionMode(callback);
                }
                else
                {
                    //if we are already in action mode, then just change the name action bar title (as selection focus has changed to another dataset)
                    actionMode.setTitle(String.format("%s Selected", dName));
                }
            }
        };

        adapter.setListener(listener);
    }

    /**
     * Method to initialise all the views for the dataset creation/edit window
     * @param layout the layout view
     */
    public void initialiseDatasetCreationViews(View layout)
    {
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
     * @param v the view
     * @param datasetKey the primary key of the dataset. This will also be used to determine which function to perform (create/update)
     *                   If datasetKey = -1 : creating dataset
     *                   else : editing a particular dataset's details
     */
    public void initiateDatasetWindow(View v, int datasetKey) {
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
            if (datasetKey != -1) {
                setEditDatasetFields(datasetKey);
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

                    Thread t = new Thread(() -> {
                        try {
                            if(datasetKey == -1) {
                                //create a new dataset
                                action.create(datasetNameText, datasetDescriptionText, projectKey, licenseKey, datasetPublic, datasetTagText);
                            }
                            else {
                                //update an existing dataset's details
                                action.update(datasetKey, datasetNameText, datasetDescriptionText, projectKey, licenseKey, datasetPublic, datasetTagText);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                    t.start();
                    popupWindow.dismiss();
                    if(actionMode != null) {actionMode.finish();};
                    reload();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to check if all required fields are non-empty
     * @return true if all required details have been provided
     */
    public boolean checkDetailsEntered() {
        //check if any of the inputs are empty and if so, set an error message
        if (isEmpty(datasetName)) {
            datasetName.setError("Required");
            return false; }

        return true;
    }

    /**
     * Method to check if an EditText field is empty
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
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        if (Build.VERSION.SDK_INT >= 26) {
            ft.setReorderingAllowed(false);
        }
        ft.detach(this).attach(this).commit();
    }

    /**
     * A method to darken the background when a popup window is displayed
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
     * @param: the primary of the dataset to delete
     */
    public void deleteDataset(int datasetKey) {
        //make an API request to delete a dataset
        Thread t = new Thread(() -> {
            try {
                //delete image file
                action.delete(datasetKey, true);
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        t.start();

        reload();
    }

    /**
     * A method which prompts the user for confirmation prior to creating a copy of a dataset
     * @param mode the action mode
     * @param datasetName the name to give the copied dataset
     * @param datasetKey the primary key of the dataset
     */
    public void confirmCopyDataset(ActionMode mode, String datasetName, int datasetKey) {
        final EditText editText = new EditText(getContext());
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
                            copyDataset(datasetKey, newDatasetName);

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
     * @param datasetKey the primary key of the dataset
     * @param newDatasetName the name to give the copied dataset
     */
    public void copyDataset(int datasetKey, String newDatasetName) {
        //make an API request to copy a dataset
        Thread t = new Thread(() -> {
            try {
                //create a copy of the dataset
                action.copy(datasetKey, newDatasetName);

                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        t.start();
        reload();
    }

    /**
     * Method to display an existing dataset's details on the edit dataset window
     * @param datasetKey the primary key of the dataset
     */
    public void setEditDatasetFields(int datasetKey) {
        Thread thread = new Thread(() -> {
            try {
                //load the dataset which is currently being edited
                Datasets.Dataset dataset = action.load(datasetKey);

                getActivity().runOnUiThread(() -> {
                    //set the fields to display the current dataset details
                    datasetName.setText(dataset.getName());
                    datasetDescription.setText(dataset.getDescription());
                    datasetTags.setText(dataset.getTags());
                    datasetSwitch.setChecked(dataset.isPublic());
                    buttonCreateDataset.setText("Update Dataset");

                    //check the current project and license associated to this dataset
                    int projName = spinnerArrayAdapterPojects.getPosition(dbManager.getProjectName(dataset.getProject()));
                    int licName = spinnerArrayAdapterLicenses.getPosition(dbManager.getLicenseName(dataset.getLicense()));

                    //set the spinners to reflect the current project and license details
                    spinnerProjects.setSelection(projName);
                    spinnerLicenses.setSelection(licName);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }

    /** Method which animates the loading of datasets */
    private void layoutAnimation()
    {
        LayoutAnimationController layoutAnimationController = AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_animation_fall_down);
        mRecyclerView.setLayoutAnimation(layoutAnimationController);
        mRecyclerView.getAdapter().notifyDataSetChanged();
        mRecyclerView.scheduleLayoutAnimation();
    }

    /**
     * Method to highlight the background of the selected dataset
     * @param position the position of the selected dataset in the recyclerview
     */
    private void highlightBackground(int position)
    {
        adapter.setSelectedIndex(position);
        adapter.notifyDataSetChanged();
    }
}