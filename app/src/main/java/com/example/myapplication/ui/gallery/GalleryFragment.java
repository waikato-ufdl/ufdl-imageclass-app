package com.example.myapplication.ui.gallery;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;
import com.example.myapplication.ui.settings.Utility;
import com.github.waikatoufdl.ufdl4j.action.Datasets;

import java.util.ArrayList;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;
import static androidx.core.content.ContextCompat.getSystemService;

public class GalleryFragment extends Fragment {
    private ImageButton btnNewDataset;
    private ArrayList<Datasets.Dataset> dataset;

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

        displayDatasets(root);
       
                
        btnNewDataset = root.findViewById(R.id.fab_add_dataset);
        btnNewDataset.setOnClickListener(view -> {
            initiateNewDatasetWindow(view);
        });

        return root;
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
                    dataset = (ArrayList) ((MainActivity) getActivity()).getClient().datasets().list();

                    //must populate the listview on the main thread
                    getActivity().runOnUiThread(() -> {
                        ListView mListView = (ListView) root.findViewById(R.id.listViewDatasets);
                        datasetListAdapter adapter = new datasetListAdapter(getContext(), R.layout.dataset_display, dataset);
                        mListView.setAdapter(adapter);
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
                    LinearLayout.LayoutParams.FILL_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT, true);
            // display the popup in the center
            popupWindow.showAtLocation(v, Gravity.CENTER, 0, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}