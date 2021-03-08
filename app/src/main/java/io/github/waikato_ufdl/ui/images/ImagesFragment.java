package io.github.waikato_ufdl.ui.images;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.github.waikatoufdl.ufdl4j.action.ImageClassificationDatasets;
import com.tbuonomo.viewpagerdotsindicator.SpringDotsIndicator;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.engine.impl.GlideEngine;
import com.zhihu.matisse.filter.Filter;
import com.zhihu.matisse.internal.entity.CaptureStrategy;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import io.github.waikato_ufdl.DBManager;
import io.github.waikato_ufdl.DatasetOperations;
import io.github.waikato_ufdl.NetworkConnectivityMonitor;
import io.github.waikato_ufdl.R;
import io.github.waikato_ufdl.SessionManager;
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper;

import static android.app.Activity.RESULT_OK;

public class ImagesFragment extends Fragment {
    SessionManager sessionManager;
    private ArrayList<ClassifiedImage> images;
    private int datasetKey;
    private String datasetName;
    private ImageListAdapter adapter;
    protected RecyclerView recyclerView;
    private GridLayoutManager gridLayoutManager;
    private ProgressBar progressBar;
    private final int MAX_GALLERY_SELECTION = 20;
    private File cacheFolder;

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


    /***
     * initialises the contents the the options menu
     * @param menu The options menu in which you place your items.
     * @param inflater MenuInflater
     */
    @Override
    public void onCreateOptionsMenu(@NotNull Menu menu, @NotNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        //inflate the options menu
        inflater.inflate(R.menu.context_menu_search, menu);
        MenuItem item = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) item.getActionView();

        //set a query listener on the search bar
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            /***
             * Called when the user submits the query
             * @param query The query text that is to be submitted
             * @return true if the query has been handled by the listener. False to let the SearchView perform the default action.
             */
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            /***
             * Called when the query text is changed by the user.
             * @param newText the new content of the query text field.
             * @return false if the SearchView should perform the default action of showing any suggestions if available, true if the action was handled by the listener.
             */
            @Override
            public boolean onQueryTextChange(String newText) {
                //filter the images to check for classified images with a specific label
                search(newText);
                return false;
            }
        });

        item.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {

            /***
             * Called when a menu item with SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW is expanded
             * @param item Item that was expanded
             * @return true if the item should expand, false if expansion should be suppressed.
             */
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            /***
             * Called when a menu item with SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW is expanded
             * @param item Item that was collapsed
             * @return true if the item should collapse, false if collapsing should be suppressed.
             */
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

        sessionManager = new SessionManager(requireContext());
        requireContext().setTheme(sessionManager.getTheme());

        if (getArguments() != null) {
            //get Bundle from the previous fragment & initialise variables
            datasetKey = getArguments().getInt("datasetPK");
            datasetName = getArguments().getString("datasetName");
            cacheFolder = DatasetOperations.getImageStorageDirectory(requireContext(), datasetName, true);
        }

        retrievedAll = false;
        isLoading = false;
        datasetModified = true;
        dbManager = sessionManager.getDbManager();
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        requireActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_images, container, false);
        images = dbManager.getCachedImageList(datasetName);
        //initialising views & initialising variables
        recyclerView = view.findViewById(R.id.imageRecyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        ImageButton addImages = view.findViewById(R.id.fab_add_images);

        //set on click listener which will start up the gallery image selection user interface
        addImages.setOnClickListener(v -> startGalleryImagePicker());
        setupImageGrid();

        //create network connectivity monitor to observe any connectivity changes whilst on this fragment
        NetworkConnectivityMonitor connectivityMonitor = new NetworkConnectivityMonitor(requireContext());
        connectivityMonitor.observe(getViewLifecycleOwner(), isConnected ->
        {
            SessionManager.isOnlineMode = isConnected;
            if (action == null && isConnected) action = sessionManager.getDatasetAction();
            //if the dataset we are currently viewing has recently been synced, get it's updated dataset key from the local database
            if (datasetKey == -1 && isConnected) {
                datasetKey = dbManager.getDatasetPK(datasetName);
            }

            //if there is an internet connection and there is no visible items in the recyler view
            if (isConnected && (gridLayoutManager.findFirstVisibleItemPosition() == RecyclerView.NO_POSITION ||
                    gridLayoutManager.findLastVisibleItemPosition() <= PAGE_LIMIT) && dbManager.getUnsycnedImages().isEmpty() && !isLoading) {
                //start the process of displaying the dataset's images to the gridview
                new Thread(this::processImages).start();
            }
        });

        return view;
    }

    /***
     * Opens the gallery for image selection
     */
    public void startGalleryImagePicker() {
        isLoading = true;
        int galleryTheme = (sessionManager.loadDarkModeState()) ? R.style.Matisse_Dracula : R.style.Matisse_Zhihu;

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

    /***
     * calls lazy load if an internet connection is available
     */
    public void processImages() {
        try {
            if (SessionManager.isOnlineMode) {
                LazyLoadImages();
            }
        } catch (Exception e) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> progressBar.setVisibility(View.GONE), 1500);

            if (!SessionManager.isOnlineMode) {
                requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "No Internet Connection", Toast.LENGTH_SHORT).show());
            }
        }
    }

    /***
     * Method to populate and display the dataset's images to a gridview using the arraylist of ClassifiedImages
     */
    private void setupImageGrid() {
        //get the recycler view, set it's layout to a gridlayout with 2 columns & then set the adapter
        adapter = new ImageListAdapter(this, datasetName);
        adapter.submitList(images);
        gridLayoutManager = new GridLayoutManager(getContext(), 2, GridLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(gridLayoutManager);
        OverScrollDecoratorHelper.setUpOverScroll(recyclerView, OverScrollDecoratorHelper.ORIENTATION_VERTICAL);
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            /***
             * Callback method to be invoked when the RecyclerView's scroll state changes
             * @param recyclerView The RecyclerView whose scroll state has changed
             * @param newState The updated scroll state (idle, dragging or settling)
             */
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                    isScrolling = true;
                }
            }

            /***
             * Callback method to be invoked when the RecyclerView has been scrolled
             * @param recyclerView The RecyclerView which scrolled
             * @param dx the amount of horizontal scroll
             * @param dy the amount of vertical scroll
             */
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                //only trigger on scrolling up when all data hasn't been retrieved
                if (dy > 0 && !retrievedAll) {
                    currentItems = gridLayoutManager.getChildCount();
                    totalItems = gridLayoutManager.getItemCount();
                    scrolledItems = gridLayoutManager.findFirstVisibleItemPosition();

                    //if the user has scrolled to the bottom of the recyclerview, load more images
                    if (isScrolling && (currentItems + scrolledItems == totalItems) && !isLoading) {

                        if (SessionManager.isOnlineMode) {
                            isScrolling = false;
                            isLoading = true;

                            //show progress bar and attempt to lazy load images
                            progressBar.setVisibility(View.VISIBLE);
                            new Thread(() -> processImages()).start();

                        } else {
                            requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "No Internet Connection", Toast.LENGTH_SHORT).show());
                        }
                    }
                } else {
                    requireActivity().runOnUiThread(() -> progressBar.setVisibility(View.GONE));
                }
            }
        });
    }

    /***
     * Method to hide the progress bar in order to signal the end of any data loading
     */
    public void hideProgressBar() {
        requireActivity().runOnUiThread(() -> progressBar.setVisibility(View.GONE));
        isLoading = false;
    }

    /***
     * Receives the result once the user has returned from the gallery selection interface.
     * @param requestCode The integer request code originally supplied to the gallery image picker
     * @param resultCode The integer result code returned by the child activity
     * @param data An Intent, which can return result data to the caller
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            galleryImages = Matisse.obtainResult(data);
            if (galleryImages.isEmpty()) return;
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
            SpringDotsIndicator indicator = dialog.findViewById(R.id.spring_dots_indicator);
            EditText editText = dialog.findViewById(R.id.editTextCategory);
            CheckBox checkBox = dialog.findViewById(R.id.labelCheckBox);
            Button saveImages = dialog.findViewById(R.id.saveImages);

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

                /***
                 * This method is called to notify you that, somewhere within s, the text has been changed.
                 * Store any changes to the editText
                 * @param s Editable
                 */
                @Override
                public void afterTextChanged(Editable s) {
                    labels[viewPager.getCurrentItem()] = s.toString().trim();
                }
            });

            //set a listener to listen for page changes caused by user swipes
            viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {

                /***
                 * This method will be invoked when the current page is scrolled
                 * @param position Position index of the first page currently being displayed.
                 * @param positionOffset  Value from [0, 1) indicating the offset from the page at position.
                 * @param positionOffsetPixels Value in pixels indicating the offset from position.
                 */
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                    super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                    //user has scrolled from left to right
                    if (position > prevIndex) {
                        //display the label associated with the particular image if one has previously been entered
                        displayImageLabel(editText, viewPager.getCurrentItem());
                    }

                    prevIndex = position;
                }

                /***
                 * This method will be invoked when a new page becomes selected.
                 * @param position Position index of the new selected page.
                 */
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    //user has swiped right to left
                    if (prevIndex == position) {
                        //display the label associated with the particular image if one has previously been entered
                        displayImageLabel(editText, viewPager.getCurrentItem());
                    }

                    indexPosition = position;
                }

                /***
                 * Called when the scroll state changes.
                 * @param state state (idle, dragging, settling)
                 */
                @Override
                public void onPageScrollStateChanged(int state) {
                    super.onPageScrollStateChanged(state);
                }
            });

            //set listener to listen for check box state changes
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                String classification = editText.getText().toString().trim();

                //if the user has checked the checkbox
                if (isChecked) {
                    disableEditText(editText);
                    labels[indexPosition] = classification;
                    //create a backup of the current labels
                    System.arraycopy(labels, 0, backup, 0, labels.length);

                    //fill the labels array with the current classification
                    Arrays.fill(labels, classification);
                } else {
                    enableEditText(editText);
                    //set labels to the backup labels
                    System.arraycopy(backup, 0, labels, 0, backup.length);
                    editText.setText(labels[indexPosition]);
                }
            });

            saveImages.setOnClickListener(v ->
            {
                confirmAddFromGallery(viewPager, dialog);
            });
            dialog.show();
        }
    }

    /***
     * Disables an edit text
     * @param editText the edit text to disable
     */
    public void disableEditText(EditText editText)
    {
        editText.setHint("");
        editText.setFocusable(true);
        editText.setFocusableInTouchMode(true);
        editText.setInputType(InputType.TYPE_NULL);
    }

    /***
     * Enables a EditText
     * @param editText the edit text to enable
     */
    public void enableEditText(EditText editText)
    {
        editText.setHint("Classification label");
        editText.setFocusable(true);
        editText.setFocusableInTouchMode(true);
        editText.setInputType(InputType.TYPE_CLASS_TEXT);
    }

    /***
     * Displays a confirmation dialog for saving images to the dataset and then begins an upload task on user confirmation
     * @param viewPager the viewpager in which the images are displayed
     * @param dialog the popup dialo
     */
    public void confirmAddFromGallery(ViewPager2 viewPager, Dialog dialog) {
        String informativeMessage;

        if (viewPager.getAdapter() == null) return;

        if (allLabelsFilled(viewPager.getAdapter().getItemCount())) {
            informativeMessage = "Save classified images?";
        } else {
            informativeMessage = "All label fields have not been filled out. Any images with empty labels will be classified as '-'. Are you sure you wish to save these images?";
        }

        //if the user cancels deletion close the popup but leave them on the selection mode
        new SweetAlertDialog(requireContext(), SweetAlertDialog.WARNING_TYPE)
                .setTitleText("Are you sure?")
                .setContentText(informativeMessage)
                .setConfirmText("Yes")
                .setConfirmClickListener(sDialog -> {
                    sDialog.dismissWithAnimation();

                    //upload images to backend
                    UploadTask uploadImages = new UploadTask(ImagesFragment.this, getContext(), galleryImages, labels, datasetName);
                    uploadImages.execute();

                    dialog.dismiss();
                })
                .setCancelButton("No", SweetAlertDialog::dismissWithAnimation)
                .show();
    }

    /***
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

    /***
     * A method to check if all images have been labelled
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

    /***
     * Method to display the classification label associated with a particular image
     * @param editText the editText to display a label to (if any)
     * @param position the index position of the displayed image
     */
    public void displayImageLabel(EditText editText, int position) {
        //if a user has entered a label for the image being observed, then show the label in the edit text
        if (labels[position] != null) {
            editText.setText(labels[position]);
        }
        //else show an empty text box
        else
            editText.getText().clear();

        //position the cursor to the end of the text
        editText.setSelection(editText.getText().length());
    }


    /***
     * A method to set the boolean value indicating whether a dataset change has taken place
     * @param bool true if a dataset has been modified
     */
    public void setDatasetModified(boolean bool) {
        datasetModified = bool;
    }

    /***
     * A method to set the boolean value indicating whether all dataset contents have been retrieved
     * @param bool true if all images have been retrieved from backend
     */
    public void setRetrievedAll(boolean bool) {
        retrievedAll = bool;
    }

    /***
     * A method to create the filtered list containing all images which match a given keyword
     * @param keyword the keyword to filter the list by
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

    /***
     * A method to lazy load a certain number of images at a time to the recyclerview
     * @throws Exception if API request fails
     */

    public void LazyLoadImages() throws Exception {
        isLoading = true;
        int startIndex = images.size();
        int loadedItems = 0;
        byte[] img;
        String imageFileName;
        List<String> allLabels;
        String classificationLabel;

        //if there are no images loaded or the dataset has been modified
        if (startIndex == 0 || datasetModified) {
            datasetModified = false;
            //load the dataset files
            imageFileNames = action.load(datasetKey).getFiles();
            totalImages = imageFileNames.length;
        }

        //if all images have not been retrieved from the dataset
        if (startIndex < totalImages) {

            for (int i = startIndex; i < totalImages; i++) {

                //if we have haven't yet retrieved all images or reached the limit of images to lazy load
                if ((startIndex + loadedItems) < totalImages && loadedItems < PAGE_LIMIT) {
                    //get the name of the image file
                    imageFileName = imageFileNames[i];

                    try {
                        //retrieve the image data and classification label of the image
                        img = action.getFile(datasetKey, imageFileName);
                        allLabels = action.getCategories(datasetKey, imageFileName);
                        classificationLabel = (allLabels.size() > 0) ? allLabels.get(0) : "-";
                    } catch (Exception e) {
                        continue;
                    }

                    //create a classifiedImage object, cache the image and then and add it to the images arrayList
                    ClassifiedImage image = new ClassifiedImage(img, classificationLabel, imageFileName);
                    if (cacheImage(image)) {
                        images.add(image);
                        loadedItems++;

                        //update the recycler view
                        requireActivity().runOnUiThread(() -> {
                            adapter.notifyItemInserted(images.size() - 1);

                            if (adapter.viewer != null) {
                                adapter.viewer.updateImages(images);
                            }
                        });
                    }
                }
                //if all images have been retrieved from backend, hide the progress bar
                else if (startIndex + loadedItems > totalImages) {
                    Log.d("TAG", "Retrieved All");
                    retrievedAll = true;
                    hideProgressBar();
                    return;
                }
            }

            //hide the progress bar
            hideProgressBar();
        }

        //if all images have been retrieved, set retrieved all to true and hide the progress bar
        if (startIndex >= totalImages) {
            Log.d("TAG", "Retrieved All Images");
            retrievedAll = true;
            hideProgressBar();
        }
    }

    /***
     * A method to cache images and store the image info to the local SQLite database
     * @param image the classified image object which contains the image data & info
     * @return true if the image has been successfully cached & inserted into the local database
     */
    public boolean cacheImage(ClassifiedImage image) {
        String filename = image.getImageFileName();
        String classificationLabel = image.getClassificationLabel();
        byte[] imageData = image.getImageBytes();

        boolean cacheSuccessful = DatasetOperations.downloadImage(requireContext(), dbManager, cacheFolder, filename, imageData, classificationLabel, true);
        image.setCachePath(new File(cacheFolder, filename).getPath());
        return cacheSuccessful;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recyclerView.setAdapter(null);
    }
}



