package com.example.myapplication.ui.images;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.Image;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toolbar;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;
import com.example.myapplication.ui.settings.Utility;
import com.github.waikatoufdl.ufdl4j.action.ImageClassificationDatasets;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class ImagesFragment extends Fragment {
    private ArrayList<ClassifiedImage> images;
    private int datasetKey;
    public static boolean isActionMode = false;
    public static ArrayList<ClassifiedImage> selectedImages = new ArrayList<>();
    public static ActionMode actionMode = null;
    private ImageListAdapter adapter;


    public ImagesFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //pass context to to Utility class & set the theme
        Utility.setContext(getContext());
        getContext().setTheme(Utility.getTheme());

        //get Bundle from the previous fragment
        datasetKey = getArguments().getInt("datasetPK");
    }

    /**
     * Method to populate and display the dataset's images to a gridview using the arraylist of ClassifiedImages
     */
    private void setupImageGrid()
    {
        //get the recycler view, set it's layout to a gridlayout with 2 columns & then set the adapter
        RecyclerView recyclerView = getView().findViewById(R.id.imageRecyclerView);
        adapter = new ImageListAdapter(this, getContext(), images);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 2, GridLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setAdapter(adapter);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v =  inflater.inflate(R.layout.fragment_images, container, false);

        //start a thread to start the process of displaying the dataset's images to the gridview
        Thread t = new Thread(() -> {
            try {
                LoadImages();

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        t.start();

        return v;
    }


    /**
     * This method retrieves images using an API request and then populates the grid view
     * @throws Exception
     */
    public void LoadImages() throws Exception {
        //try to retrieve the dataset's image list from the Utility class
        images = Utility.getImageList(datasetKey);
        byte[] img;
        int index = 0;

        //setup the image grid
        setupImageGrid();

        //if the images list is empty than this dataset has never been visited before retrieve images from backend using API
        if(images.isEmpty()) {
            //retrieve categories as this contains the image names + classifications that we need
            ImageClassificationDatasets action = Utility.getClient().action(ImageClassificationDatasets.class);
            Map<String, List<String>> categories = action.getCategories(datasetKey);

            //iterate through the map of categories
            for (Map.Entry<String, List<String>> entry : categories.entrySet()) {
                System.out.println(entry.getKey());
                System.out.println(entry.getValue());


                //retrieve the byte array of images from the API using the dataset's primary key + image name
                img = Utility.getClient().datasets().getFile(datasetKey, entry.getKey());

                //create a classifiedImage object using image name and classification label and add it to the images arrayList
                images.add(new ClassifiedImage(img, entry.getValue().get(index)));

                //update the recycler view
                getActivity().runOnUiThread(() -> {adapter.notifyItemInserted(images.size()-1);});
                index++;
            }

            //once all the processing has been done, save the image list
            Utility.saveImageList(datasetKey, images);
        }
    }
}