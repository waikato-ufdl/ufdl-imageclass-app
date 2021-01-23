package io.github.waikato_ufdl.ui.images;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import id.zelory.compressor.Compressor;
import io.github.waikato_ufdl.DBManager;
import io.github.waikato_ufdl.NetworkConnectivityMonitor;
import io.github.waikato_ufdl.R;
import io.github.waikato_ufdl.ui.settings.Utility;

import com.github.waikatoufdl.ufdl4j.action.ImageClassificationDatasets;
import com.tbuonomo.viewpagerdotsindicator.SpringDotsIndicator;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.engine.impl.GlideEngine;
import com.zhihu.matisse.filter.Filter;
import com.zhihu.matisse.internal.entity.CaptureStrategy;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper;

import static android.app.Activity.RESULT_OK;

public class ImagesFragment extends Fragment {
    private ArrayList<ClassifiedImage> images;
    private int datasetKey;
    private String datasetName;
    private ImageListAdapter adapter;
    public RecyclerView recyclerView;
    private GridLayoutManager gridLayoutManager;
    private ProgressBar progressBar;
    private final int MAX_GALLERY_SELECTION = 20;

    //Lazy loading variables
    private ImageClassificationDatasets action;
    private String[] imageFileNames;
    private boolean retrievedAll, isScrolling, isLoading, datasetModified;
    private int currentItems, totalItems, scrolledItems;
    private int totalImages;
    private final int REQUEST_CODE = 1;
    //specify the number of images to load upon scroll
    public final int PAGE_LIMIT = 8;

    //variables related to the popup menu which asks users to label images
    private List<Uri> galleryImages;
    private String[] labels;

    //the index position of the selected image (from gallery)
    private int indexPosition, prevIndex;
    private DBManager dbManager;

    public ImagesFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.main, menu);
        MenuItem item = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) item.getActionView();
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

        //get Bundle from the previous fragment & initialise variables
        datasetKey = getArguments().getInt("datasetPK");
        datasetName = getArguments().getString("datasetName");

        retrievedAll = false;
        isLoading = false;
        datasetModified = true;
        dbManager = Utility.dbManager;

        try {
            action = Utility.getClient().action(ImageClassificationDatasets.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_images, container, false);
        images = dbManager.getCachedImageList(datasetName);
        //initialising views & initialising variables
        recyclerView = (RecyclerView) v.findViewById(R.id.imageRecyclerView);
        progressBar = (ProgressBar) v.findViewById(R.id.progressBar);
        ImageButton addImages = (ImageButton) v.findViewById(R.id.fab_add_images);

        //set on click listener which will start up the gallery
        addImages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLoading = true;
                int galleryTheme = (Utility.loadDarkModeState()) ? R.style.Matisse_Dracula : R.style.Matisse_Zhihu;

                Matisse.from(ImagesFragment.this)
                        .choose(MimeType.ofImage()) //show only images
                        .countable(true)    //show count on selected images
                        .capture(true)  //show preview of images
                        .captureStrategy(new CaptureStrategy(true, "io.github.waikato_ufdl.fileprovider")) //where to store images
                        .maxSelectable(MAX_GALLERY_SELECTION)   //maximum amount of images which can be selected
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

        //create network connectivity monitor to observe any connectivity changes whilst on this fragment
        NetworkConnectivityMonitor connectivityMonitor = new NetworkConnectivityMonitor(getContext());

        connectivityMonitor.observe(getViewLifecycleOwner(), isConnected ->
        {
            Utility.isOnlineMode = isConnected;

            //if the dataset we are currently viewing has recently been synced, get it's updated dataset key from the local database
            if (datasetKey == -1 && isConnected) {
                datasetKey = dbManager.getDatasetPK(datasetName);
            }

            //if there is an internet connection and there is no visible items in the recyler view
            if (isConnected && (gridLayoutManager.findFirstVisibleItemPosition() == RecyclerView.NO_POSITION ||
                    gridLayoutManager.findLastVisibleItemPosition() <= PAGE_LIMIT) && dbManager.getUnsycnedImages().isEmpty() && !isLoading) {
                //start the process of displaying the dataset's images to the gridview
                Thread t = new Thread(() -> processImages());
                t.start();
            }
        });

        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //scrollToPosition();
    }

    /**
     * Method to call lazy load
     */
    public void processImages() {
        try {
            if (Utility.isOnlineMode) {
                LazyLoadImages();
            }
        } catch (Exception e) {
            if (!Utility.isOnlineMode) {
                getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "No Internet Connection", Toast.LENGTH_SHORT).show());
            }
        }
    }


    /**
     * Method to populate and display the dataset's images to a gridview using the arraylist of ClassifiedImages
     */
    private void setupImageGrid() {
        //get the recycler view, set it's layout to a gridlayout with 2 columns & then set the adapter
        adapter = new ImageListAdapter(this, getContext(), images, action, datasetKey, datasetName);
        gridLayoutManager = new GridLayoutManager(getContext(), 2, GridLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(gridLayoutManager);
        OverScrollDecoratorHelper.setUpOverScroll(recyclerView, OverScrollDecoratorHelper.ORIENTATION_VERTICAL);
        recyclerView.setAdapter(adapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) ;
                {
                    isScrolling = true;
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (dy > 0 && !retrievedAll) { // only when scrolling up
                    currentItems = gridLayoutManager.getChildCount();
                    totalItems = gridLayoutManager.getItemCount();
                    scrolledItems = gridLayoutManager.findFirstVisibleItemPosition();


                    if (isScrolling && (currentItems + scrolledItems == totalItems) && !isLoading) {

                        if (Utility.isOnlineMode) {
                            isScrolling = false;
                            isLoading = true;

                            //show progress bar
                            progressBar.setVisibility(View.VISIBLE);


                            // load content in background
                            //start a thread to start the process of displaying the dataset's images to the gridview
                            Thread t = new Thread(() -> processImages());
                            t.start();
                        } else {
                            getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "No Internet Connection", Toast.LENGTH_SHORT).show());
                        }
                    }
                }
            }
        });
    }

    /**
     * Method to save any processing changes to the image list in memory & then signals the end of any data loading
     */
    public void saveChanges() {
        //once all the processing has been done, save the image list
        Utility.saveImageList(datasetKey, images);
        getActivity().runOnUiThread(() -> progressBar.setVisibility(View.GONE));
        isLoading = false;
    }

    public int getDatasetKey() {
        return datasetKey;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            galleryImages = Matisse.obtainResult(data);

            if (!galleryImages.isEmpty()) {
                final Dialog dialog = new Dialog(getContext(), ViewGroup.LayoutParams.MATCH_PARENT);

                //initialise index position;
                indexPosition = 0;
                prevIndex = 0;
                labels = new String[MAX_GALLERY_SELECTION];

                //create a backup of the current labels
                String[] backup = new String[MAX_GALLERY_SELECTION];

                dialog.setContentView(R.layout.gallery_selection_label);

                //initialise views
                ViewPager2 viewPager = dialog.findViewById(R.id.labelViewPager);
                GallerySelectionAdapter galleryAdapter = new GallerySelectionAdapter(getContext(), galleryImages);
                SpringDotsIndicator indicator = (SpringDotsIndicator) dialog.findViewById(R.id.spring_dots_indicator);
                EditText editText = (EditText) dialog.findViewById(R.id.editTextCategory);
                CheckBox checkBox = (CheckBox) dialog.findViewById(R.id.labelCheckBox);
                Button saveImages = (Button) dialog.findViewById(R.id.saveImages);

                //set adapter & default start position
                viewPager.getChildAt(indexPosition).setOverScrollMode(View.OVER_SCROLL_NEVER);
                viewPager.setAdapter(galleryAdapter);
                indicator.setViewPager2(viewPager);

                editText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        labels[viewPager.getCurrentItem()] = editText.getText().toString().trim();
                    }
                });

                //set a listener to listen for page changes caused by user swipes
                viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                        super.onPageScrolled(position, positionOffset, positionOffsetPixels);

                        //the user is scrolling from the right to the left
                        if (prevIndex > position) {
                            //labels[prevIndex] = editText.getText().toString().trim();
                        }
                        //user has scrolled from left to right
                        else if (position > prevIndex) {
                            //display the label associated with the particular image if one has previously been entered
                            displayImageLabel(editText, viewPager.getCurrentItem());
                        }

                        prevIndex = position;
                    }

                    @Override
                    public void onPageSelected(int position) {
                        super.onPageSelected(position);

                        //scrolling from left to right
                        if (prevIndex != position) {
                            //set the label to the image if a user has entered one in
                            //labels[prevIndex] = editText.getText().toString().trim();
                        }
                        //user has swiped right to left
                        else {
                            //display the label associated with the particular image if one has previously been entered
                            displayImageLabel(editText, viewPager.getCurrentItem());
                        }

                        indexPosition = position;
                    }

                    @Override
                    public void onPageScrollStateChanged(int state) {
                        super.onPageScrollStateChanged(state);
                    }
                });

                //set listener to listen for check box state changes
                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        String classification = editText.getText().toString().trim();

                        //if the user has checked the checkbox
                        if (isChecked) {
                            labels[indexPosition] = classification;
                            //create a backup of the current labels
                            System.arraycopy(labels, 0, backup, 0, labels.length);

                            //fill the labels array with the current classification
                            Arrays.fill(labels, classification);
                        } else {
                            //set labels to the backup labels
                            System.arraycopy(backup, 0, labels, 0, backup.length);
                            editText.setText(labels[indexPosition]);
                        }
                    }
                });

                saveImages.setOnClickListener(v -> {
                    String informativeMessage;

                    if (allLabelsFilled(viewPager.getAdapter().getItemCount())) {
                        informativeMessage = "Save classified images?";
                    } else {
                        informativeMessage = "All label fields have not been filled out. Any images with empty labels will be classified as 'unlabelled'. Are you sure you wish to save these images?";
                    }

                    new SweetAlertDialog(getContext(), SweetAlertDialog.WARNING_TYPE)
                            .setTitleText("Are you sure?")
                            .setContentText(informativeMessage)
                            .setConfirmText("Yes")
                            .setConfirmClickListener(sDialog -> {
                                sDialog.dismissWithAnimation();


                                //upload images to backend
                                UploadTask uploadImages = new UploadTask(ImagesFragment.this, getContext(), galleryImages, labels, datasetName, action);
                                uploadImages.execute();

                                dialog.dismiss();

                            })
                            .setCancelButton("No", sDialog -> {
                                //if the user cancels deletion close the popup but leave them on the selection mode
                                sDialog.dismissWithAnimation();
                            })
                            .show();
                });
                dialog.show();
            }
        }
    }

    /**
     * A method to reload the current fragment
     */
    public void reload() {
        // Reload current fragment
        isLoading = true;
        FragmentTransaction ft = getParentFragmentManager().beginTransaction();
        if (Build.VERSION.SDK_INT >= 26) {
            ft.setReorderingAllowed(false);
        }
        ft.detach(this).attach(this).commit();
        isLoading = false;
    }

    /**
     * A method to check if all images have been labelled
     *
     * @param numImages The number of images to check
     * @return True if all images has been assigned a label
     */
    public boolean allLabelsFilled(int numImages) {
        for (int i = 0; i < numImages; i++) {
            String label = labels[i];
            if (label == null || label.length() < 1) {
                return false;
            }
        }

        return true;
    }

    /**
     * //display the label associated with a particular image if one has previously been entered
     *
     * @param editText the editText to display a label if any
     * @param position the position indicating the image the user is current looking at
     */
    public void displayImageLabel(EditText editText, int position) {
        //if a user has entered a label for the image being observed, then show the label in the edit text
        if (labels[position] != null) {
            editText.setText(labels[position]);
        }
        //else show an empty text box
        else
            editText.getText().clear();

        //position to the cursor to the end of the text in the text box
        editText.setSelection(editText.getText().length());
    }


    /**
     * A method to set the boolean value indicating whether a dataset change to the API has taken place
     *
     * @param bool true if a dataset has been modified
     */
    public void setDatasetModified(boolean bool) {
        datasetModified = bool;
    }

    public void setRetrievedAll(boolean bool) {
        retrievedAll = bool;
    }

    public int getNumImages() {
        return images.size();
    }

    /**
     * This method will be used to create the filtered list containing all images which match the keyword
     *
     * @param keyword
     */
    private void search(String keyword) {
        ArrayList<ClassifiedImage> filteredSearchList = new ArrayList<>();

        //iterate through the classified images
        for (ClassifiedImage image : images) {
            //if the classification label contains the keyword entered by the user, add the image to the filtered list
            if (image.getClassificationLabel().toLowerCase().contains(keyword.toLowerCase())) {
                filteredSearchList.add(image);
            }
        }

        //display the filtered list
        adapter.searchCategory(filteredSearchList);
    }

    /**
     * A method to load in and process a certain number of images at a time so that not all images are processed and displayed at once.
     *
     * @throws Exception
     */

    public void LazyLoadImages() throws Exception {
        //get the stored image list from utility if there is one for this dataset
        isLoading = true;
        int startIndex = images.size();
        int loadedItems = 0;
        byte[] img;
        String imageFileName;
        List<String> allLabels;
        String classificationLabel;

        Log.e("CHECK: ", "OKAY");

        if (startIndex == 0 || datasetModified) {
            datasetModified = false;
            //retrieve categories as this contains the image names + classifications that we need
            imageFileNames = action.load(datasetKey).getFiles();
            totalImages = imageFileNames.length;
        }

        Log.e("CHECK", startIndex + " , total: " + totalImages);

        //if we haven't retrieved all images from the dataset yet
        if (!(startIndex >= totalImages)) {

            //iterate through the image list
            for (int i = startIndex; i < totalImages; i++) {

                //if we have haven't yet retrieved all or reached the limit of images to load
                if ((startIndex + loadedItems) <= totalImages && loadedItems < PAGE_LIMIT) {
                    //get the name of the image file
                    imageFileName = imageFileNames[i];

                    try {
                        //retrieve the byte array of images from the API using the dataset's primary key + image name
                        img = action.getFile(datasetKey, imageFileName);
                        allLabels = action.getCategories(datasetKey, imageFileName);
                        classificationLabel = (allLabels.size() > 0) ? allLabels.get(0) : "Unlabelled";
                    } catch (Exception e) {
                        continue;
                    }

                    //create a classifiedImage object using image name and classification label and add it to the images arrayList
                    ClassifiedImage image = new ClassifiedImage(img, classificationLabel, imageFileName);
                    if (cacheImage(image)) {
                        images.add(image);
                        loadedItems++;

                        //update the recycler view
                        getActivity().runOnUiThread(() -> {
                            adapter.notifyItemChanged(images.size() - 1);

                            /*
                            if(ImagePagerFragment.viewPager != null)
                            {
                                ImagePagerFragment.viewPager.getAdapter().notifyDataSetChanged();
                            }
                             */

                            if (adapter.viewer != null) {
                                adapter.viewer.updateImages(images);
                            }
                            //adapter.updateViewerList();
                        });
                    }
                }
                //all images have been retrieved from backend
                else if (startIndex + loadedItems > totalImages) {
                    Log.e("CHECK", "I FINISH HERE");
                    retrievedAll = true;
                    saveChanges();
                    return;
                }
            }
            //save changes to the list stored in memory
            saveChanges();
        }

        if (startIndex >= totalImages) {
            Log.e("CHECK", "I FINISH HERE * 2");
            retrievedAll = true;
            saveChanges();
            return;
        }
    }

    public boolean cacheImage(ClassifiedImage image) {
        String filename = image.getImageFileName();
        String classificationLabel = image.getClassificationLabel();
        byte[] imageData = image.getImageBytes();
        String cache_folder = "UFDL_Cache";

        //create a cache folder if it doesn't already exist
        File cacheFolder = new File(getContext().getCacheDir(), cache_folder);
        if (!cacheFolder.exists()) {
            cacheFolder.mkdirs();
        }

        //create image file
        File imageFile = new File(cacheFolder, filename);

        if (!imageFile.exists()) {

            try {
                //write image content to file in cache directory
                FileOutputStream fos = new FileOutputStream(imageFile.getPath());
                fos.write(imageData);
                fos.close();

                //create a compressed version of the image file
                Compressor compressor = new Compressor(getContext());
                File compressedImage = compressor.compressToFile(imageFile);

                //replace original file with compressed file
                Files.move(compressedImage.toPath(), imageFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                //delete the parent folder of where the compressed image was initially stored
                if (compressedImage.getParentFile().exists()) {
                    compressedImage.getParentFile().delete();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //add the image information to the database
        boolean cacheSuccessful = dbManager.insertSyncedImage(filename, classificationLabel, null, imageFile.getPath(), datasetName);
        image.setCachePath(imageFile.getPath());

        return cacheSuccessful;
    }
}



