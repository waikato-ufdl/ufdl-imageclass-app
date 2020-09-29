package com.example.myapplication.ui.images;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.Image;
import android.os.Bundle;

import androidx.annotation.NonNull;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toolbar;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;
import com.example.myapplication.ui.settings.Utility;
import com.github.waikatoufdl.ufdl4j.action.ImageClassificationDatasets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class ImagesFragment extends Fragment {
    private ArrayList<ClassifiedImage> images;
    private int datasetKey;
    private ImageListAdapter adapter;
    private RecyclerView recyclerView;
    private GridLayoutManager gridLayoutManager;
    private ProgressBar progressBar;

    //Lazy loading variables
    ImageClassificationDatasets action;
    Map<String, List<String>> categories;
    private boolean retrievedAll = false;
    Boolean isScrolling = false;
    int currentItems, totalItems, scrolledItems;
    private boolean isLoading = false;

    //lazy load 2.0 vars
    int totalImages;

    //specify the number of images to load upon scroll
    private final int PAGE_LIMIT = 8;

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_images, container, false);

        images = Utility.getImageList(datasetKey);
        recyclerView = (RecyclerView) v.findViewById(R.id.imageRecyclerView);
        progressBar = (ProgressBar) v.findViewById(R.id.progressBar);
        setupImageGrid();

        //start a thread to start the process of displaying the dataset's images to the gridview
       Thread t = new Thread(() -> processImages());
       t.start();

        return v;
    }

    public void processImages()
    {
        try {
            //LoadImages();
            LazyLoadImages();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Method to populate and display the dataset's images to a gridview using the arraylist of ClassifiedImages
     */
    private void setupImageGrid() {
        //get the recycler view, set it's layout to a gridlayout with 2 columns & then set the adapter
        adapter = new ImageListAdapter(this, getContext(), images);
        gridLayoutManager = new GridLayoutManager(getContext(), 2, GridLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setAdapter(adapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if(newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
                {
                    isScrolling = true;
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if(dy > 0 && retrievedAll == false){ // only when scrolling up

                    currentItems = gridLayoutManager.getChildCount();
                    totalItems = gridLayoutManager.getItemCount();
                    scrolledItems = gridLayoutManager.findFirstVisibleItemPosition();

                    if(isScrolling && (currentItems + scrolledItems == totalItems) && isLoading == false){
                        isScrolling = false;
                        isLoading = true;

                        //show progress bar
                        progressBar.setVisibility(View.VISIBLE);

                        // load content in background
                        //start a thread to start the process of displaying the dataset's images to the gridview
                        Thread t = new Thread(() -> { processImages(); });
                        t.start();
                    }
                }
            }
        });
    }

    /**
     * This method retrieves images using an API request and then populates the grid view
     *
     * @throws Exception
     */
    public void LoadImages() throws Exception {
        //try to retrieve the dataset's image list from the Utility class
        images = Utility.getImageList(datasetKey);
        byte[] img;
        String imageFileName;

        //setup the image grid
        setupImageGrid();

        //if the images list is empty than this dataset has never been visited before retrieve images from backend using API
        if (images.isEmpty()) {
            //retrieve categories as this contains the image names + classifications that we need
            ImageClassificationDatasets action = Utility.getClient().action(ImageClassificationDatasets.class);
            Map<String, List<String>> categories = action.getCategories(datasetKey);

            //iterate through the map of categories
            for (Map.Entry<String, List<String>> entry : categories.entrySet()) {

                //get the name of the image file
                imageFileName = entry.getKey();

                try {
                    //retrieve the byte array of images from the API using the dataset's primary key + image name
                    img = Utility.getClient().datasets().getFile(datasetKey, imageFileName);
                }
                catch (Exception e)
                {
                    continue;
                }

                //create a classifiedImage object using image name and classification label and add it to the images arrayList
                images.add(new ClassifiedImage(img, entry.getValue().get(0), imageFileName));

                //update the recycler view
                getActivity().runOnUiThread(() -> {
                    adapter.notifyItemChanged(images.size() - 1);
                });
            }

            //once all the processing has been done, save the image list
            Utility.saveImageList(datasetKey, images);
        }
    }

    public void LazyLoadNaiveApproach() throws Exception {
        //get the stored image list from utility if there is one for this dataset
        isLoading = true;
        //we want to start loading images from where we last stopped
        int startIndex = images.size();
        int loadedItems = 0;
        int iterationIndex = 0;
        byte[] img;
        String imageFileName;

        if (startIndex == 0 || categories == null) {
            //setupImageGrid();
            //retrieve categories as this contains the image names + classifications that we need
            action = Utility.getClient().action(ImageClassificationDatasets.class);
            categories = action.getCategories(datasetKey);
        }

        int totalImages = categories.size();
        //System.out.println(startIndex + " " + totalImages);

        if (!(startIndex >= totalImages)) {
            //iterate through the map of categories
            for (Map.Entry<String, List<String>> entry : categories.entrySet()) {
                if (iterationIndex >= startIndex &&  iterationIndex <= startIndex + 8) {
                    if (((startIndex + loadedItems) <= totalImages)) {
                        //get the name of the image file
                        imageFileName = entry.getKey();

                        //if an image has no classification label (assume image has been deleted)
                        if(entry.getValue().size() > 0) {
                            try {
                                //retrieve the byte array of images from the API using the dataset's primary key + image name
                                img = Utility.getClient().datasets().getFile(datasetKey, imageFileName);
                            } catch (Exception e) {
                                continue;
                            }

                            loadedItems++;

                            //create a classifiedImage object using image name and classification label and add it to the images arrayList
                            images.add(new ClassifiedImage(img, entry.getValue().get(0), imageFileName));


                            //update the recycler view
                            getActivity().runOnUiThread(() -> {
                                adapter.notifyItemChanged(images.size() - 1);
                            });
                        }
                    } else {
                        retrievedAll = true;
                        saveChanges();
                        break;
                    }
                }
                if(iterationIndex > startIndex + 8)
                {
                    saveChanges();
                    break;
                }
                iterationIndex++;
                System.out.println("ITER: " + iterationIndex);
            }
        } else {

            System.out.println("DONE 3");
            retrievedAll = true;
            saveChanges();
        }

        saveChanges();
    }


    public void saveChanges()
    {
        //once all the processing has been done, save the image list
        Utility.saveImageList(datasetKey, images);
        isLoading = false;
        getActivity().runOnUiThread(() -> progressBar.setVisibility(View.GONE));
    }

    public int getDatasetKey() {
        return datasetKey;
    }


    /**
     * Method which retrieves images using the API when they need to be retrieved.
     * Lazy loading: instead of retrieving all the data, retrieve & process in chunks of set size
     * @throws Exception
     */
    public void LazyLoadImages() throws Exception {
        //get the stored image list from utility if there is one for this dataset
        isLoading = true;
        //we want to start loading images from where we last stopped
        int startIndex = images.size();
        int loadedItems = 0;
        byte[] img;
        String imageFileName;

        //if the start index is 0, then we have never loaded this dataset before
        if (startIndex == 0 || categories == null) {

            //retrieve categories as this contains the image names + classifications that we need
            action = Utility.getClient().action(ImageClassificationDatasets.class);
            categories = action.getCategories(datasetKey);
            totalImages = categories.size();
            processCategoryList(startIndex);
        }

        //if we haven't retrieved all images from the dataset yet
        if (!(startIndex >= totalImages)) {

            //iterate through the category entry set
            for (Iterator<Map.Entry<String, List<String>>> entryIterator = categories.entrySet().iterator();
                 entryIterator.hasNext(); ) {

                Map.Entry<String, List<String>> entry = entryIterator.next();

                //if we have haven't yet retrieved all or reached the limit of images to load
                if ((startIndex + loadedItems) <= totalImages && loadedItems < PAGE_LIMIT) {
                    //get the name of the image file
                    imageFileName = entry.getKey();

                    //if an image has no classification label (assume image has been deleted)
                    if (entry.getValue().size() > 0) {
                        try {
                            //retrieve the byte array of images from the API using the dataset's primary key + image name
                            img = Utility.getClient().datasets().getFile(datasetKey, imageFileName);
                        } catch (Exception e) {
                            //remove the current element from the iterator & map as it has been dealt with
                            entryIterator.remove();
                            continue;
                        }

                        loadedItems++;

                        //create a classifiedImage object using image name and classification label and add it to the images arrayList
                        images.add(new ClassifiedImage(img, entry.getValue().get(0), imageFileName));

                        //update the recycler view
                        getActivity().runOnUiThread(() -> {
                            adapter.notifyItemChanged(images.size() - 1);
                        });

                        //remove entry from the map as it has been dealt with
                        entryIterator.remove();
                    }
                }
                //all images have been retrieved from backend
                else if (startIndex + loadedItems > totalImages) {
                    retrievedAll = true;
                    saveChanges();
                    break;
                }
            }
            //save changes to the list stored in memory
            saveChanges();
        }

        if(startIndex >= totalImages)
        {
            retrievedAll = true;
            saveChanges();
        }
    }

    /**
     * This method will discard any images already process in the case that the user has went back to datasets and come back in
     * @param loadedImages The number of images which have already been stored
     */
    public void processCategoryList(int loadedImages)
    {
        int index = 0;

        //if no images have been processed, return
        if(loadedImages == 0) {
            return;
        }

        //iterate through the category entry set
        for (Iterator<Map.Entry<String, List<String>>> entryIterator = categories.entrySet().iterator();
             entryIterator.hasNext(); ) {

            Map.Entry<String, List<String>> entry = entryIterator.next();

            //discard all entries that have been processed
            if(index < loadedImages) {
                entryIterator.remove();
                index++;
            }
            else {
                return;
            }
        }
    }
}