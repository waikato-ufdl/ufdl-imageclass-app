package io.github.waikato_ufdl.ui.gallery;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.Navigation;

import io.github.waikato_ufdl.DBManager;
import io.github.waikato_ufdl.MainActivity;
import io.github.waikato_ufdl.R;

import io.github.waikato_ufdl.ui.settings.Utility;
import com.github.waikatoufdl.ufdl4j.action.Datasets;
import com.github.waikatoufdl.ufdl4j.action.ImageClassificationDatasets;

import java.util.ArrayList;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;

import static io.github.waikato_ufdl.R.layout.spinner_item;

public class GalleryFragment extends Fragment {
    private ImageButton btnNewDataset;
    private DBManager dbManager;
    private EditText datasetName, datasetDescription, datasetTags;
    private Switch datasetSwitch;
    private boolean datasetPublic = false;
    private String datasetNameText, datasetDescriptionText, datasetTagText, dName;
    private ImageClassificationDatasets action;
    private boolean isActionMode;
    private int dKey;
    private ActionMode actionMode = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //pass context to to Utility class & set the theme
        Utility.setContext(getContext());
        getContext().setTheme(Utility.getTheme());
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_gallery, container, false);

        btnNewDataset = root.findViewById(R.id.fab_add_dataset);
        btnNewDataset.setOnClickListener(view -> {
            initiateNewDatasetWindow(view);
        });

        dbManager = new DBManager(getContext());
        isActionMode = false;
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //check that the user settings are not empty
        checkSettings(view);
    }

    /**
     * Method to check whether the required settings have been set, if not, move to settings fragment
     * @param view
     */
    public void checkSettings(View view)
    {
        //if these main user settings are empty, this must be the user's first time using this app
        if(Utility.loadUsername() == null | Utility.loadPassword() == null | Utility.loadServerURL() == null)
        {
            //navigate to settings and make them enter these details
            Navigation.findNavController(view).navigate(R.id.action_nav_gallery_to_settingsFragment);
        }
        else
        {
            //only continue if the client is connected to the API
            if(!Utility.authenticationFailed())
            {
                displayDatasets(view);
            }
        }
    }

    /**
     * Method to populate the listview with the dataset information
     * @param root
     */
    public void displayDatasets(View root)
    {
        try {
            //must start a thread to retrieve the dataset information as networking operations cannot be done on the main thread
            Thread t = new Thread(() -> {
                try {
                    action = Utility.getClient().action(ImageClassificationDatasets.class);
                    final ArrayList<Datasets.Dataset> dataset = (ArrayList) action.list();

                    //must populate the listview on the main thread
                    getActivity().runOnUiThread(() -> {
                        ListView mListView = (ListView) root.findViewById(R.id.list_view_datasets);
                        datasetListAdapter adapter = new datasetListAdapter(getContext(), R.layout.dataset_display, dataset);
                        mListView.setAdapter(adapter);

                        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                                if (!isActionMode) {
                                    Bundle bundle = new Bundle();
                                    bundle.putInt("datasetPK", dataset.get(position).getPK());

                                    //move to the images fragment to display this dataset's images
                                    Navigation.findNavController(view).navigate(R.id.action_nav_gallery_to_imagesFragment, bundle);
                                }
                                else
                                {
                                    dKey = dataset.get(position).getPK();
                                    dName = dataset.get(position).getName();
                                    actionMode.setTitle(String.format("%s Selected", dName));
                                }
                            }
                        });

                        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

                            @Override
                            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                                dKey = dataset.get(position).getPK();
                                dName = dataset.get(position).getName();
                                //initialise Action mode

                                ActionMode.Callback callback = new ActionMode.Callback() {
                                    @Override
                                    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                                        //Initialise menu inflater & inflate menu
                                        MenuInflater menuInflater = mode.getMenuInflater();
                                        menuInflater.inflate(R.menu.context_menu_datasets, menu);
                                        actionMode = mode;
                                        return true;
                                    }

                                    @Override
                                    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                                        //When action mode is preparing
                                        isActionMode = true;
                                        mode.setTitle(String.format("%s Selected", dName));
                                        return true;
                                    }

                                    @Override
                                    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                                        //handles the click of an action mode item

                                        //get menu id
                                        int id = item.getItemId();

                                        //check which menu item was clicked
                                        switch (id)
                                        {
                                            case R.id.action_relabel_dataset:
                                                //when the user presses edit
                                                initiateUpdateDatasetWindow(root, dKey);
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

                                    @Override
                                    public void onDestroyActionMode(ActionMode mode) {
                                        //when action mode is destroyed
                                        isActionMode = false;
                                        actionMode = null;
                                        mode.finish();
                                    }
                                };

                                //Start action mode
                                ((MainActivity) view.getContext()).startActionMode(callback);
                                return true;
                            }
                        });

                    });
                }
                catch (IllegalStateException e) {
                    ((MainActivity) getActivity()).showToast("Please check your username, password and server URL details in settings");
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            });
            t.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public void initiateNewDatasetWindow(View v) {
        try {
            Log.d("initiateNewDatasetWindow: ", "New Dataset Popup");
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            //Inflate the view from a predefined XML layout
            View layout = inflater.inflate(R.layout.new_dataset,
                    null);


            // create a 300px width and 470px height PopupWindow
            final PopupWindow popupWindow = new PopupWindow(layout,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT, true);

            // display the popup in the center
            popupWindow.showAtLocation(v, Gravity.CENTER, 0, 0);

            //darken the background behind the popup window
            darkenBackground(popupWindow);

            dbManager = new DBManager(getContext());

            //get licenses & projects using database manager
            final List<String> spinnerArrayLicenses = dbManager.getLicenses();
            final List<String> spinnerArrayProjects = dbManager.getProjects();

            ArrayAdapter<String> spinnerArrayAdapterLicenses = new ArrayAdapter<String>
                    (getContext(), R.layout.spinner_item,
                            spinnerArrayLicenses); //selected item will look like a spinner set from XML
            spinnerArrayAdapterLicenses.setDropDownViewResource(android.R.layout
                    .simple_spinner_dropdown_item);

            ArrayAdapter<String> spinnerArrayAdapterPojects = new ArrayAdapter<String>
                    (getContext(), R.layout.spinner_item,
                            spinnerArrayProjects); //selected item will look like a spinner set from XML
            spinnerArrayAdapterPojects.setDropDownViewResource(android.R.layout
                    .simple_spinner_dropdown_item);

            //initialise views
            Spinner spinnerLicenses = layout.findViewById(R.id.dataset_license_spinner);
            spinnerLicenses.setAdapter(spinnerArrayAdapterLicenses);
            Spinner spinnerProjects = layout.findViewById(R.id.dataset_project_spinner);
            spinnerProjects.setAdapter(spinnerArrayAdapterPojects);
            Button btnCreateDataset = layout.findViewById(R.id.createDatasetButton);

            datasetName = layout.findViewById(R.id.dataset_name_text);
            datasetDescription = layout.findViewById(R.id.dataset_description_text);
            datasetTags = layout.findViewById(R.id.dataset_tags_text);
            datasetSwitch = layout.findViewById(R.id.makePublic);


            btnCreateDataset.setOnClickListener(view -> {
                if(checkDetailsEntered()){
                    datasetNameText = datasetName.getText().toString().trim();
                    datasetDescriptionText = datasetDescription.getText().toString().trim();
                    datasetTagText = datasetTags.getText().toString().trim();
                    if(datasetSwitch.isChecked()){
                        datasetPublic = true;
                    }
                    int projectKey = dbManager.getProjectKey(spinnerProjects.getSelectedItem().toString());
                    int licenseKey = dbManager.getLicenseKey(spinnerLicenses.getSelectedItem().toString());

                    //create
                    Thread t = new Thread(() -> {
                        try {
                            action.create(datasetNameText, datasetDescriptionText, projectKey, licenseKey, datasetPublic, datasetTagText);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                    t.start();
                    popupWindow.dismiss();
                    reload();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to check if any of the required user setting fields are empty
     * @return boolean
     */
    public boolean checkDetailsEntered()
    {
        //check if any of the inputs are empty and if so, set an error message
        if(isEmpty(datasetName, 0)) {
            datasetName.setError("Required");
            return false;
        }

        return true;
    }

    /**
     * Method to check if an EditText is empty
     * @param editText The EditText to check
     * @return
     */
    public boolean isEmpty(EditText editText, int minLength)
    {
        return editText.getText().toString().trim().length() <= minLength;
    }

    /**
     * A method to reload the fragment
     */
    public void reload(){
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
    public void deleteConfirmation(ActionMode mode, int datasetKey)
    {
        new SweetAlertDialog(getContext(), SweetAlertDialog.WARNING_TYPE)
                .setTitleText("Are you sure?")
                .setContentText("You won't be able to recover the dataset after deletion")
                .setConfirmText("Delete")
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
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
                    }
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
     * Method to delete the selected dataset from list view & backend
     */
    public void deleteDataset(int datasetKey)
    {
            //make an API request to delete an image
            Thread t = new Thread(() -> {
                try {
                    //delete image file
                    action.delete(datasetKey, true);
                    return;
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            });
            t.start();

            reload();
    }

    public void confirmCopyDataset(ActionMode mode, String datasetName, int datasetKey) {
        final EditText editText = new EditText(getContext());
        new SweetAlertDialog(getContext(), SweetAlertDialog.WARNING_TYPE)
                .setTitleText("Copy dataset " + datasetName + " as: ")
                .setConfirmText("Copy")
                .setCustomView(editText)
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                        String newDatasetName = editText.getText().toString().trim();

                        //if a value has been entered
                        if(newDatasetName.length() > 0) {
                            if(!newDatasetName.equals(datasetName)){
                                //copy dataset
                                copyDataset(datasetKey, newDatasetName);

                                //show a success popup
                                sweetAlertDialog
                                        .setTitleText("Success!")
                                        .setContentText("The dataset " + datasetName + " has been copied as: " + newDatasetName)
                                        .setConfirmText("OK")
                                        .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                            @Override
                                            public void onClick(SweetAlertDialog sweetAlertDialog) {
                                                //when the user clicks ok, dismiss the popup
                                                sweetAlertDialog.dismissWithAnimation();
                                                //finish action mode once a user has confirmed the reclassification
                                                mode.finish();
                                            }
                                        })
                                        .changeAlertType(SweetAlertDialog.SUCCESS_TYPE);
                            }else{
                                editText.setError("Dataset name must be different");
                            }
                        }
                        else
                            editText.setError("Please enter a classification label");
                    }
                })
                .setCancelButton("Cancel", new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        //if the user clicks cancel close the popup but leave them on the selection mode
                        sDialog.dismissWithAnimation();
                    }
                })
                .show();
    }

    /**
     * Method to delete the selected dataset from list view & backend
     */
    public void copyDataset(int datasetKey, String newDatasetName)
    {
        //make an API request to copy a dataset
        Thread t = new Thread(() -> {
            try {
                //create a copy of the dataset
                action.copy(datasetKey, newDatasetName);

                return;
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        });
        t.start();

        reload();
    }

    public void initiateUpdateDatasetWindow(View v, int datasetKey) {
        try {
            Log.d("initiateUpdateDatasetWindow: ", "Update Dataset Popup");
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            //Inflate the view from a predefined XML layout
            View layout = inflater.inflate(R.layout.new_dataset,
                    null);

            //            Projects.Project project = Projects.Project.load(dataset.getProject());

            // create a 300px width and 470px height PopupWindow
            final PopupWindow popupWindow = new PopupWindow(layout,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT, true);

            // display the popup in the center
            popupWindow.showAtLocation(v, Gravity.CENTER, 0, 0);

            //darken the background behind the popup window
            darkenBackground(popupWindow);

            dbManager = new DBManager(getContext());

            //get licenses & projects using database manager
            final List<String> spinnerArrayLicenses = dbManager.getLicenses();
            final List<String> spinnerArrayProjects = dbManager.getProjects();

            ArrayAdapter<String> spinnerArrayAdapterLicenses = new ArrayAdapter<String>
                    (getContext(), R.layout.spinner_item,
                            spinnerArrayLicenses); //selected item will look like a spinner set from XML
            spinnerArrayAdapterLicenses.setDropDownViewResource(android.R.layout
                    .simple_spinner_dropdown_item);

            ArrayAdapter<String> spinnerArrayAdapterPojects = new ArrayAdapter<String>
                    (getContext(), R.layout.spinner_item,
                            spinnerArrayProjects); //selected item will look like a spinner set from XML
            spinnerArrayAdapterPojects.setDropDownViewResource(android.R.layout
                    .simple_spinner_dropdown_item);

            //initialise views
            Spinner spinnerLicenses = layout.findViewById(R.id.dataset_license_spinner);
            spinnerLicenses.setAdapter(spinnerArrayAdapterLicenses);
            Spinner spinnerProjects = layout.findViewById(R.id.dataset_project_spinner);
            spinnerProjects.setAdapter(spinnerArrayAdapterPojects);
            Button btnCreateDataset = layout.findViewById(R.id.createDatasetButton);

            datasetName = layout.findViewById(R.id.dataset_name_text);
            datasetDescription = layout.findViewById(R.id.dataset_description_text);
            datasetTags = layout.findViewById(R.id.dataset_tags_text);
            datasetSwitch = layout.findViewById(R.id.makePublic);

            Thread thread = new Thread(() -> {
                try {
                    Datasets.Dataset dataset = action.load(datasetKey);
                    datasetName.setText(dataset.getName());
                    datasetDescription.setText(dataset.getDescription());
                    datasetTags.setText(dataset.getTags());
                    datasetSwitch.setChecked(dataset.isPublic());
                    btnCreateDataset.setText("Update Dataset");
                    int projName = spinnerArrayAdapterPojects.getPosition(dbManager.getProjectName(dataset.getProject()));
                    int licName = spinnerArrayAdapterLicenses.getPosition(dbManager.getLicenseName(dataset.getLicense()));
                    Log.d("initiateUpdateDatasetWindow: ", "Project " + projName + " License " + licName);

                    spinnerProjects.setSelection(projName);
                    spinnerLicenses.setSelection(licName);
                    Log.d("initiateUpdateDatasetWindow: ", "Spinner selections made");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            thread.start();

            btnCreateDataset.setOnClickListener(view -> {
                if(checkDetailsEntered()){
                    datasetNameText = datasetName.getText().toString().trim();
                    datasetDescriptionText = datasetDescription.getText().toString().trim();
                    datasetTagText = datasetTags.getText().toString().trim();
                    if(datasetSwitch.isChecked()){
                        datasetPublic = true;
                    }
                    int projectKey = dbManager.getProjectKey(spinnerProjects.getSelectedItem().toString());
                    int licenseKey = dbManager.getLicenseKey(spinnerLicenses.getSelectedItem().toString());

                    //create
                    Thread t = new Thread(() -> {
                        try {
                            action.update(datasetKey, datasetNameText, datasetDescriptionText, projectKey, licenseKey, datasetPublic, datasetTagText);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                    t.start();
                    popupWindow.dismiss();
                    reload();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}