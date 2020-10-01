package com.example.myapplication.ui.images;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.Image;
import android.net.Uri;
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
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toolbar;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;
import com.example.myapplication.ui.settings.Utility;
import com.github.waikatoufdl.ufdl4j.action.Generic;
import com.github.waikatoufdl.ufdl4j.action.ImageClassificationDatasets;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.engine.impl.GlideEngine;
import com.zhihu.matisse.filter.Filter;
import com.zhihu.matisse.internal.entity.CaptureStrategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static android.app.Activity.RESULT_OK;


public class ImagesFragment extends Fragment {
    private ArrayList<ClassifiedImage> images;
    private int datasetKey;
    private ImageListAdapter adapter;
    private RecyclerView recyclerView;
    private GridLayoutManager gridLayoutManager;
    private ProgressBar progressBar;
    private ImageButton addImages;

    //Lazy loading variables
    ImageClassificationDatasets action;
    Map<String, List<String>> categories;
    private boolean retrievedAll = false;
    Boolean isScrolling = false;
    int currentItems, totalItems, scrolledItems;
    private boolean isLoading = false;

    //lazy load 2.0 vars
    int totalImages;
    private int REQUEST_CODE = 1;
    private boolean deleted = false;


    //specify the number of images to load upon scroll
    public final int PAGE_LIMIT = 8;

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
        addImages = (ImageButton) v.findViewById(R.id.fab_add_images);
        addImages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int galleryTheme = (Utility.loadDarkModeState()) ? R.style.Matisse_Dracula: R.style.Matisse_Zhihu;

                Matisse.from(ImagesFragment.this)
                        .choose(MimeType.ofImage()) //show only images
                        .countable(true)    //show count on selected images
                        .capture(true)  //show preview of images
                        .captureStrategy(new CaptureStrategy(true, "com.example.android.fileprovider")) //where to store images
                        .maxSelectable(9)   //maximum amount of images which can be selected
                        .addFilter(new GifSizeFilter(320, 320, 5 * Filter.K * Filter.K)) //define the preview size
                        .gridExpectedSize(getResources().getDimensionPixelSize(R.dimen.grid_expected_size)) //show images in grid format
                        .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                        .thumbnailScale(0.85f)     //set the thumbnail size
                        .imageEngine(new GlideEngine())     //use glide library to display images
                        .theme(galleryTheme)
                        .originalEnable(true)
                        .showPreview(false) // Default is `true`
                        .forResult(REQUEST_CODE);
            }
        });

        setupImageGrid();

        //start a thread to start the process of displaying the dataset's images to the gridview
       Thread t = new Thread(() -> processImages());
       t.start();

        return v;
    }

    /**
     * Method to call lazy load
     */
    public void processImages()
    {
        try {
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

                if(dy > 0 && !retrievedAll){ // only when scrolling up

                    currentItems = gridLayoutManager.getChildCount();
                    totalItems = gridLayoutManager.getItemCount();
                    scrolledItems = gridLayoutManager.findFirstVisibleItemPosition();

                    if(isScrolling && (currentItems + scrolledItems == totalItems) && !isLoading){
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
     * Method to save any processing changes to the image list in memory & then signals the end of any data loading
     */
    public void saveChanges()
    {
        //once all the processing has been done, save the image list
        Utility.saveImageList(datasetKey, images);
        getActivity().runOnUiThread(() -> progressBar.setVisibility(View.GONE));
        isLoading = false;
    }

    public int getDatasetKey() {
        return datasetKey;
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        List<Uri> mSelected;

        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            mSelected = Matisse.obtainResult(data);
            Log.d("Matisse", "mSelected: " + mSelected);
        }
    }


    /**
     * A method to load in and process a certain number of images at a time so that not all images are processed and displayed at once.
     * @throws Exception
     */
    public void LazyLoadImages() throws Exception {
        //get the stored image list from utility if there is one for this dataset
        isLoading = true;
        int startIndex = images.size();
        int loadedItems = 0;
        byte[] img;
        String imageFileName;

        //if the start index is 0, then we have never loaded this dataset before
        if (startIndex == 0 || categories == null || deleted) {
            deleted = false;
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

                    System.out.println(imageFileName + " " + totalImages);

                    try {
                        //retrieve the byte array of images from the API using the dataset's primary key + image name
                        img =action.getFile(datasetKey, imageFileName);
                    }
                    catch (Exception e) {
                        continue;
                    }


                    //create a classifiedImage object using image name and classification label and add it to the images arrayList
                    images.add(new ClassifiedImage(img, entry.getValue().get(0), imageFileName));
                    loadedItems++;

                    //update the recycler view
                    getActivity().runOnUiThread(() -> {
                        adapter.notifyItemChanged(images.size() - 1);
                    });

                    //remove entry from the map as it has been dealt with
                    entryIterator.remove();
                }
                //all images have been retrieved from backend
                else if (startIndex + loadedItems > totalImages) {
                    retrievedAll = true;
                    saveChanges();
                    return;
                }
            }
            //save changes to the list stored in memory
            saveChanges();
        }

        if(startIndex >= totalImages)
        {
            retrievedAll = true;
            saveChanges();
            return;
        }
    }

    //the number of images deleted
    public void setDeleted(boolean bool)
    {
        deleted = bool;
    }
}