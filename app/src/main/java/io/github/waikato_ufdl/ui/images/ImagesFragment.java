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

import io.github.waikato_ufdl.R;

import io.github.waikato_ufdl.R;
import io.github.waikato_ufdl.ui.UriUtils;
import io.github.waikato_ufdl.ui.settings.Utility;

import com.github.waikatoufdl.ufdl4j.action.ImageClassificationDatasets;
import com.tbuonomo.viewpagerdotsindicator.SpringDotsIndicator;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.engine.impl.GlideEngine;
import com.zhihu.matisse.filter.Filter;
import com.zhihu.matisse.internal.entity.CaptureStrategy;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.pedant.SweetAlert.SweetAlertDialog;
import id.zelory.compressor.Compressor;
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
    private ImageClassificationDatasets action;
    private String[] imageFileNames;
    private boolean retrievedAll = false, isScrolling = false,  isLoading = false, datasetModified = false;
    private int currentItems, totalItems, scrolledItems;
    private int totalImages;
    private int REQUEST_CODE = 1;

    //specify the number of images to load upon scroll
    public final int PAGE_LIMIT = 8;

    //variables related to the popup menu which asks users to label images
    private List<Uri> galleryImages;
    private String[] labels;

    //the index position of the selected image (from gallery)
    private int indexPosition, prevIndex;


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
                        .captureStrategy(new CaptureStrategy(true, "io.github.waikato_ufdl.fileprovider")) //where to store images
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


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            galleryImages = Matisse.obtainResult(data);
            Log.d("Matisse", "mSelected: " + galleryImages);

            final Dialog dialog = new Dialog(getContext(), ViewGroup.LayoutParams.MATCH_PARENT);

            //initialise index position;
            indexPosition = 0;
            prevIndex = 0;
            labels = new String[9];

            //create a backup of the current labels
            String[] backup = new String[9];

            dialog.setContentView(R.layout.gallery_selection_label);

            //initialise views
            ViewPager2 viewPager = dialog.findViewById(R.id.labelViewPager);
            GallerySelectionAdapter galleryAdapter = new GallerySelectionAdapter(getContext(), galleryImages);
            SpringDotsIndicator indicator = (SpringDotsIndicator) dialog.findViewById(R.id.spring_dots_indicator);
            EditText editText = (EditText)  dialog.findViewById(R.id.editTextCategory);
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
                    System.out.println("POS: " + viewPager.getCurrentItem());
                    labels[viewPager.getCurrentItem()] =  editText.getText().toString().trim();
                }
            });

            //set a listener to listen for page changes caused by user swipes
            viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                    super.onPageScrolled(position, positionOffset, positionOffsetPixels);

                    //the user is scrolling from the right to the left
                    if(prevIndex > position)
                    {
                        //labels[prevIndex] = editText.getText().toString().trim();
                    }
                    //user has scrolled from left to right
                    else if (position > prevIndex)
                    {
                        //display the label associated with the particular image if one has previously been entered
                        displayImageLabel(editText, viewPager.getCurrentItem());
                    }

                    prevIndex = position;
                }

                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);

                    //scrolling from left to right
                    if(prevIndex != position)
                    {
                        //set the label to the image if a user has entered one in
                        //labels[prevIndex] = editText.getText().toString().trim();
                    }
                    //user has swiped right to left
                    else
                    {
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
                    if(isChecked)
                    {
                        labels[indexPosition] = classification;
                        //create a backup of the current labels
                        System.arraycopy(labels, 0, backup, 0, labels.length);

                        //fill the labels array with the current classification
                        Arrays.fill(labels, classification);
                    }
                    else
                    {
                        //set labels to the backup labels
                        System.arraycopy(backup, 0, labels, 0, backup.length);
                        editText.setText(labels[indexPosition]);
                    }
                }
            });

            saveImages.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String informativeMessage;
                    System.out.println("PORQUESH" + viewPager.getAdapter().getItemCount());

                    if(allLabelsFilled(viewPager.getAdapter().getItemCount()))
                    {
                        informativeMessage = "Save classified images?";
                    }
                    else
                    {
                        informativeMessage = "All labels have not been filled out. Any images with empty labels will be labeled as 'unlabelled'. Are you sure you wish to save these images?";
                    }

                    new SweetAlertDialog(getContext(), SweetAlertDialog.WARNING_TYPE)
                            .setTitleText("Are you sure?")
                            .setContentText(informativeMessage)
                            .setConfirmText("Yes")
                            .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                @Override
                                public void onClick(SweetAlertDialog sDialog) {
                                    //if a user accepts, save all images
                                    try {
                                        saveImageFiles();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                    //show a successful popup
                                    sDialog
                                            .setTitleText("Successful!")
                                            .setContentText("Successfully saved images!")
                                            .setConfirmText("OK")
                                            .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                                @Override
                                                public void onClick(SweetAlertDialog sweetAlertDialog) {
                                                    //if the recycler view has less images than the page_limit, load in the same amount of images that have been deleted
                                                    if(images.size() < PAGE_LIMIT)
                                                    {
                                                        reload();
                                                    }

                                                    retrievedAll = false;
                                                    datasetModified = true;

                                                    //when the user clicks ok, dismiss the popup
                                                    sDialog.dismissWithAnimation();

                                                    dialog.dismiss();
                                                }
                                            })
                                            .changeAlertType(SweetAlertDialog.SUCCESS_TYPE);
                                }
                            })
                            .setCancelButton("No", new SweetAlertDialog.OnSweetClickListener() {
                                @Override
                                public void onClick(SweetAlertDialog sDialog) {
                                    //if the user cancels deletion close the popup but leave them on the selection mode
                                    sDialog.dismissWithAnimation();
                                }
                            })
                            .show();

                }
            });

            dialog.show();
        }
    }


    /**
     * A method to reload the current fragment
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
     * A method to store classified images into the backend via API requests.
     * @throws Exception
     */
    public void saveImageFiles() throws Exception {
        //iterate through the selected images
        for (int i = 0; i < galleryImages.size(); i++) {
            //use the image URI path to create image file
            Uri selectedImageUri = galleryImages.get(i);
            File imageFile = new File(UriUtils.getPathFromUri(getContext(), selectedImageUri));
            imageFile = new Compressor(getContext()).compressToFile(imageFile);

            String label = (labels[i] != null && labels[i].length() > 0) ? labels[i] : "unlabelled";


            ExecutorService executor = Executors.newSingleThreadExecutor();

            File finalImageFile = imageFile;

            executor.execute(() -> {;

                try{
                    //add image file + label to the backend
                    action.addFile(datasetKey, finalImageFile, finalImageFile.getName());
                    action.addCategories(datasetKey, Arrays.asList(finalImageFile.getName()), Arrays.asList(label));
                    datasetModified = true;
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            });

            executor.shutdown();
        }
    }

    /**
     * A method to check if all images have been labelled
     * @param numImages The number of images to check
     * @return True if all images has been assigned a label
     */
    public boolean allLabelsFilled(int numImages)
    {
        for(int i=0; i < numImages; i++)
        {
            String label = labels[i];
            if(label  == null || label.length() < 1)
            {
                return false;
            }
        }

        return true;
    }

    /**
     * //display the label associated with a particular image if one has previously been entered
     * @param editText the editText to display a label if any
     * @param position the position indicating the image the user is current looking at
     */
    public void displayImageLabel(EditText editText, int position)
    {
        //if a user has entered a label for the image being observed, then show the label in the edit text
        if(labels[position] != null) {
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
        String classificationLabel;

        //if the start index is 0, then we have never loaded this dataset before
        if (startIndex == 0 || datasetModified) {
            datasetModified = false;
            //retrieve categories as this contains the image names + classifications that we need
            action = Utility.getClient().action(ImageClassificationDatasets.class);
            imageFileNames = Utility.getClient().datasets().load(datasetKey).getFiles();
            totalImages = imageFileNames.length;
        }


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
                        classificationLabel = (action.getCategories(datasetKey, imageFileName).size() > 0)? action.getCategories(datasetKey, imageFileName).get(0): "Unlabelled";
                    } catch (Exception e) {
                        continue;
                    }

                    //create a classifiedImage object using image name and classification label and add it to the images arrayList
                    images.add(new ClassifiedImage(img, classificationLabel, imageFileName));
                    loadedItems++;

                    //update the recycler view
                    getActivity().runOnUiThread(() -> {
                        adapter.notifyItemChanged(images.size() - 1);
                    });
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
}