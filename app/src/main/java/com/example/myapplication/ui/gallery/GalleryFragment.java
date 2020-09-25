package com.example.myapplication.ui.gallery;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.Navigation;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;
import com.example.myapplication.ui.settings.Utility;
import com.github.waikatoufdl.ufdl4j.action.Datasets;

import java.util.ArrayList;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;
import static androidx.core.content.ContextCompat.getSystemService;
import static com.example.myapplication.R.array.license_array;
import static com.example.myapplication.R.layout.spinner_item;

public class GalleryFragment extends Fragment {
    private ImageButton btnNewDataset;

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

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //check that the user settings are not empty
        checkSettings(view);
        displayDatasets(view);

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

                } catch (Exception e) {
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
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            //Inflate the view from a predefined XML layout
            View layout = inflater.inflate(R.layout.new_dataset,
                    null);
            // create a 300px width and 470px height PopupWindow
            final PopupWindow popupWindow = new PopupWindow(layout,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT, true);

            // display the popup in the center
            popupWindow.showAtLocation(v, Gravity.CENTER, 0, 0);

            Spinner licenseSpinner = (Spinner) v.findViewById(R.id.dataset_license_spinner);
            ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getActivity(),
                    spinner_item);
            dataAdapter.setDropDownViewResource(spinner_item);
            licenseSpinner.setAdapter(dataAdapter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}