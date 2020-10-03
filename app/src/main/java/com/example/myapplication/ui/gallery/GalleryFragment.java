package com.example.myapplication.ui.gallery;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.Navigation;

import com.example.myapplication.DBManager;
import com.example.myapplication.MainActivity;
import com.example.myapplication.R;
import com.example.myapplication.ui.settings.Utility;
import com.github.waikatoufdl.ufdl4j.action.Datasets;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;
import static androidx.core.content.ContextCompat.getSystemService;
import static com.example.myapplication.R.array.license_array;
import static com.example.myapplication.R.layout.spinner_item;

public class GalleryFragment extends Fragment {
    private ImageButton btnNewDataset;
    private DBManager dbManager;
    private EditText datasetName, datasetDescription, datasetTags;
    private Switch datasetSwitch;
    private boolean datasetPublic = false;
    private String datasetNameText, datasetDescriptionText, datasetTagText;

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
            if(!Utility.authenticationFailed());
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
                    final ArrayList<Datasets.Dataset> dataset = (ArrayList) (Utility.getClient().datasets().list());

                    //must populate the listview on the main thread
                    getActivity().runOnUiThread(() -> {
                        ListView mListView = (ListView) root.findViewById(R.id.list_view_datasets);
                        datasetListAdapter adapter = new datasetListAdapter(getContext(), R.layout.dataset_display, dataset);
                        mListView.setAdapter(adapter);

                        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                Bundle bundle = new Bundle();
                                bundle.putInt("datasetPK", dataset.get(position).getPK());

                                //move to the images fragment to display this dataset's images
                                Navigation.findNavController(view).navigate(R.id.action_nav_gallery_to_imagesFragment, bundle);
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
            dbManager = new DBManager(getContext());
            Log.d("initiateNewDatasetWindow: ", "Get Licenses from SQLite");
            final List<String> spinnerArrayLicenses = dbManager.getLicenses();
            final List<String> spinnerArrayProjects = dbManager.getProjects();

            Log.d("initiateNewDatasetWindow: ", "Create adapter");
//            ArrayAdapter dataAdapter = new ArrayAdapter<String>(getActivity(),
//                    spinner_item, licenses);
//            Log.d("initiateNewDatasetWindow: ", "Set drop down resource");
//            dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            Log.d("initiateNewDatasetWindow: ", "Set adapter for spinner");
//            licenseSpinner.setAdapter(dataAdapter);
            ArrayAdapter<String> spinnerArrayAdapterLicenses = new ArrayAdapter<String>
                    (getContext(), spinner_item,
                            spinnerArrayLicenses); //selected item will look like a spinner set from XML
            spinnerArrayAdapterLicenses.setDropDownViewResource(android.R.layout
                    .simple_spinner_dropdown_item);

            ArrayAdapter<String> spinnerArrayAdapterPojects = new ArrayAdapter<String>
                    (getContext(), spinner_item,
                            spinnerArrayProjects); //selected item will look like a spinner set from XML
            spinnerArrayAdapterPojects.setDropDownViewResource(android.R.layout
                    .simple_spinner_dropdown_item);
            Log.d("initiateNewDatasetWindow: ", "Get spinner");
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
                Log.d("initiateNewDatasetWindow: ", "Check details entered");
                if(checkDetailsEntered()){
                    Log.d("initiateNewDatasetWindow: ", "Get edit text values");
                    datasetNameText = datasetName.getText().toString().trim();
                    datasetDescriptionText = datasetDescription.getText().toString().trim();
                    datasetTagText = datasetTags.getText().toString().trim();
                    Log.d("initiateNewDatasetWindow: ", "Check public");
                    if(datasetSwitch.isChecked()){
                        Log.d("initiateNewDatasetWindow: ", "set public");
                        datasetPublic = true;
                    }
                    Log.d("initiateNewDatasetWindow: ", "Get project key");
                    int projectKey = dbManager.getProjectKey(spinnerProjects.getSelectedItem().toString());
                    Log.d("initiateNewDatasetWindow: ", "Get license key");
                    int licenseKey = dbManager.getLicenseKey(spinnerLicenses.getSelectedItem().toString());
                    Log.d("initiateNewDatasetWindow: ", "Dataset values");
                    Log.d("initiateNewDatasetWindow: ", "NAME: " + datasetNameText);
                    Log.d("initiateNewDatasetWindow: ", "DESCRIPTION: " + datasetDescriptionText);
                    Log.d("initiateNewDatasetWindow: ", "PROJECT KEY: " + projectKey);
                    Log.d("initiateNewDatasetWindow: ", "LICENSE KEY: " + licenseKey);
                    Log.d("initiateNewDatasetWindow: ", "PUBLIC: " + datasetPublic);
                    Log.d("initiateNewDatasetWindow: ", "TAGS: " + datasetTagText);
                    Thread t = new Thread(() -> {
                        try {
                            Log.d("initiateNewDatasetWindow: ", "Create dataset");
                            Utility.getClient().datasets().create(datasetNameText, datasetDescriptionText, projectKey, licenseKey, datasetPublic, datasetTagText);
                            Log.d("initiateNewDatasetWindow: ", "Dataset created");
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
            Log.d("initiateNewDatasetWindow: ", "ERROR THROWN");
            e.printStackTrace();
        }
    }

    /**
     * Method to check if any of the required user setting fields are empty
     * @return boolean
     */
    public boolean checkDetailsEntered()
    {
        boolean valid = true;

        //check if any of the inputs are empty and if so, set an error message
        if(isEmpty(datasetName, 0)) {
            datasetName.setError("Required");
            valid = false;
        }
//        if(isEmpty(password, 0))
//        {
//            password.setError("Required");
//            valid = false;
//        }
//        if(isEmpty(serverURL, 7))
//        {
//            serverURL.setError("Required");
//            valid = false;
//        }

        return valid;
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

    public void reload(){
        // Reload current fragment
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        if (Build.VERSION.SDK_INT >= 26) {
            ft.setReorderingAllowed(false);
        }
        ft.detach(this).attach(this).commit();
    }
}