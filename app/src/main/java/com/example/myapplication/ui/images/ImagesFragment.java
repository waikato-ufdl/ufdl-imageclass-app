package com.example.myapplication.ui.images;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toolbar;
import android.widget.ViewSwitcher;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;
import com.example.myapplication.ui.settings.Utility;
import com.github.waikatoufdl.ufdl4j.action.Generic;
import com.github.waikatoufdl.ufdl4j.action.ImageClassificationDatasets;
import com.tbuonomo.viewpagerdotsindicator.SpringDotsIndicator;
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

import me.everything.android.ui.overscroll.OverScrollDecoratorHelper;

import static android.app.Activity.RESULT_OK;


public class ImagesFragment extends Fragment {
    private ArrayList<ClassifiedImage> images;
    private ArrayList<ClassifiedImage> filteredSearchList;
    private int datasetKey;
    private ImageListAdapter adapter;
    private RecyclerView recyclerView;
    private GridLayoutManager gridLayoutManager;
    private ProgressBar progressBar;
    private ImageButton addImages;
    private SearchView searchView;

    //Lazy loading variables
    ImageClassificationDatasets action;
    Map<String, List<String>> categories;
    private boolean retrievedAll = false;
    Boolean isScrolling = false;
    int currentItems, totalItems, scrolledItems;
    private boolean isLoading = false;
    int totalImages;
    private int REQUEST_CODE = 1;
    private boolean datasetModified = false;

    //specify the number of images to load upon scroll
    public final int PAGE_LIMIT = 8;

    //variables related to the popup menu which asks users to label images
    private ImageSwitcher imageSwitcher;
    private Button previousButton, nextButton;
    private List<Uri> galleryImages;
    private String[] labels;

    //the index position of the selected image (from gallery)
    private int indexPosition;
    private int prevIndex;

    public ImagesFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.main, menu);
        MenuItem item = menu.findItem(R.id.action_search);
        searchView = (SearchView) item.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                //filter the images to check for classified images with a specific label
                search(newText);
                return false;
            }
        });

        item.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                adapter.searchCategory(images);
                return true;
            }
        });
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //pass context to to Utility class & set the theme
        Utility.setContext(getContext());
        getContext().setTheme(Utility.getTheme());

        //get Bundle from the previous fragment
        datasetKey = getArguments().getInt("datasetPK");

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_images, container, false);

        //initialising views & initialising variables
        images = Utility.getImageList(datasetKey);
        recyclerView = (RecyclerView) v.findViewById(R.id.imageRecyclerView);
        progressBar = (ProgressBar) v.findViewById(R.id.progressBar);
        addImages = (ImageButton) v.findViewById(R.id.fab_add_images);

        //set on click listener which will start up the gallery
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
        OverScrollDecoratorHelper.setUpOverScroll(recyclerView, OverScrollDecoratorHelper.ORIENTATION_VERTICAL);
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

        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            galleryImages = Matisse.obtainResult(data);
            Log.d("Matisse", "mSelected: " + galleryImages);

            final Dialog dialog = new Dialog(getContext(), ViewGroup.LayoutParams.MATCH_PARENT);
            //dialog.setContentView(R.layout.label_images);

            /*
            imageSwitcher = (ImageSwitcher) dialog.findViewById(R.id.imageSwitcher);
            imageSwitcher.setFactory(new ViewSwitcher.ViewFactory() {
                @Override
                public View makeView() {
                    return new ImageView(getContext());
                }
            });


            previousButton = (Button) dialog.findViewById(R.id.buttonPrev);
            nextButton = (Button) dialog.findViewById(R.id.buttonNext);
            indexPosition = 0;

            //display the previous image & the category associated with the image if entered
            previousButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(indexPosition > 0) {
                        indexPosition--;
                        imageSwitcher.setImageURI(galleryImages.get(indexPosition));
                    }
                }
            });

            //display the next image & the category associated with the image if entered
            nextButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(indexPosition < galleryImages.size()-1) {
                        indexPosition++;
                        imageSwitcher.setImageURI(galleryImages.get(indexPosition));
                    }
                }
            });

            //set the first image to the image switcher
            //imageSwitcher.setImageURI(galleryImages.get(0));

            dialog.show();
            imageSwitcher.setImageURI(galleryImages.get(0));

             */

            //initialise index position;
            indexPosition = 0;
            prevIndex = 0;
            labels = new String[9];

            dialog.setContentView(R.layout.gallery_selection_label);

            //initialise view pager & adapter
            ViewPager2 viewPager = dialog.findViewById(R.id.labelViewPager);
            GallerySelectionAdapter galleryAdapter = new GallerySelectionAdapter(getContext(), galleryImages);
            SpringDotsIndicator indicator = (SpringDotsIndicator) dialog.findViewById(R.id.spring_dots_indicator);
            EditText editText = (EditText)  dialog.findViewById(R.id.editTextCategory);

            //set adapter & default start position
            viewPager.getChildAt(indexPosition).setOverScrollMode(View.OVER_SCROLL_NEVER);
            viewPager.setAdapter(galleryAdapter);
            indicator.setViewPager2(viewPager);


            viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                    super.onPageScrolled(position, positionOffset, positionOffsetPixels);

                    //the user is scrolling from the right to the left
                    if(prevIndex > position)
                    {
                        labels[prevIndex] = editText.getText().toString().trim();
                        Log.e("HOOOOO", prevIndex + " " + position);
                    }
                    else if (position > prevIndex)
                    {
                        Log.e("SOOOOOOOOOOOOOOOOOO000000000000", position + " ");

                        if(labels[position] != null) {
                            editText.setText(labels[position]);
                        }
                        else
                            editText.getText().clear();

                        editText.setSelection(editText.getText().length());
                    }

                    prevIndex = position;

                }

                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);

                    //scrolling from left to right
                    if(prevIndex != position)
                    {
                        labels[prevIndex] = editText.getText().toString().trim();
                        //labels.set(prevIndex, editText.getText().toString().trim());
                        Log.e("SOOOOO", prevIndex + " " + position);
                    }
                    else
                    {
                        Log.e("SOOOOOOOOOOOOOOOOOO", position + " ");

                        if(labels[position] != null) {
                            editText.setText(labels[position]);
                        }
                        else
                            editText.getText().clear();

                        editText.setSelection(editText.getText().length());
                    }


                }

                @Override
                public void onPageScrollStateChanged(int state) {
                    super.onPageScrollStateChanged(state);
                }
            });

            dialog.show();
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
        if (startIndex == 0 || categories == null || datasetModified) {
            datasetModified = false;
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

    /**
     * A method to set the boolean value indicating whether a dataset change to the API has taken place
     * @param bool true if a dataset has been modified
     */
    public void setDatasetModified(boolean bool)
    {
        datasetModified = bool;
    }

    /**
     * This method will be used to create the filtered list containing all images which match the keyword
     * @param keyword
     */
    private void search(String keyword)
    {
        filteredSearchList = new ArrayList<>();

        //iterate through the classified images
        for (ClassifiedImage image: images)
        {
            //if the classification label contains the keyword entered by the user, add the image to the filtered list
            if(image.getClassification().toLowerCase().contains(keyword.toLowerCase()))
            {
                filteredSearchList.add(image);
            }
        }

        //display the filtered list
        adapter.searchCategory(filteredSearchList);
    }
}