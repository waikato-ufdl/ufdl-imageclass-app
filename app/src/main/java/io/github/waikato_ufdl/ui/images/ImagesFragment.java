package io.github.waikato_ufdl.ui.images;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.github.waikatoufdl.ufdl4j.action.ImageClassificationDatasets;
import com.stfalcon.imageviewer.StfalconImageViewer;
import com.tbuonomo.viewpagerdotsindicator.SpringDotsIndicator;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.engine.impl.GlideEngine;
import com.zhihu.matisse.filter.Filter;
import com.zhihu.matisse.internal.entity.CaptureStrategy;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.pedant.SweetAlert.SweetAlertDialog;
import io.github.waikato_ufdl.Classifier;
import io.github.waikato_ufdl.DBManager;
import io.github.waikato_ufdl.NetworkConnectivityMonitor;
import io.github.waikato_ufdl.R;
import io.github.waikato_ufdl.SessionManager;
import io.github.waikato_ufdl.ui.camera.ClassifierDetails;
import io.github.waikato_ufdl.ui.camera.ClassifierUtils;
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper;

import static android.app.Activity.RESULT_OK;

public class ImagesFragment extends Fragment implements ImageListAdapter.InteractionListener {
    SessionManager sessionManager;
    private int datasetKey;
    private String datasetName;
    private ImageListAdapter adapter;
    protected RecyclerView recyclerView;
    private GridLayoutManager gridLayoutManager;
    private ProgressBar progressBar;
    private final int MAX_GALLERY_SELECTION = 200;
    private ImageClassificationDatasets action;
    private boolean selectedAll, filterIsActive, retrievedAll, isActionMode;
    private final int REQUEST_CODE = 1;
    private SearchView searchView;

    //specify the number of images to load upon scroll
    public static final int PAGE_LIMIT = 16;

    //variables related to the popup menu which asks users to label images after gallery selection
    private List<Uri> galleryImages;
    private String[] labels;

    //the index position of the selected image (from gallery)
    private int indexPosition, prevIndex;
    private DBManager dbManager;

    private ArrayList<ClassifiedImage> selectedImages;
    private ImagesFragmentViewModel imagesViewModel;
    private StfalconImageViewer<ClassifiedImage> viewer;
    private int numImagesLoaded;

    /***
     * Default constructor for the ImagesFragment
     */
    public ImagesFragment() {
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
        searchView = (SearchView) item.getActionView();
        searchView.setImeOptions(EditorInfo.IME_ACTION_DONE);

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
                filterIsActive = true;
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
                if (imagesViewModel != null && imagesViewModel.getImageList() != null)
                    adapter.submitList(imagesViewModel.getImageList().getValue());

                filterIsActive = false;
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
            datasetKey = getArguments().getInt("datasetPK");
            datasetName = getArguments().getString("datasetName");
        }
        dbManager = sessionManager.getDbManager();
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        requireActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        View view = inflater.inflate(R.layout.fragment_images, container, false);
        recyclerView = view.findViewById(R.id.imageRecyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        ImageButton addImages = view.findViewById(R.id.fab_add_images);

        //set on click listener which will start up the gallery image selection user interface
        addImages.setOnClickListener(v -> startGalleryImagePicker());
        setupImageGrid();
        selectedImages = new ArrayList<>();
        viewer = null;
        numImagesLoaded = 0;
        initialiseViewModel();

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

            //if there are no visible items in the recycler view
            if (recyclerIsEmpty()) {
                if (isConnected & !retrievedAll) downloadAll();
                else process();
            }
        });

        return view;
    }

    /***
     * Checks to see if the recyclerview is empty (no visible items)
     * @return true if there are no visible items or false if there are items that are visible.
     */
    private boolean recyclerIsEmpty() {
        return (gridLayoutManager.findFirstVisibleItemPosition() == RecyclerView.NO_POSITION ||
                gridLayoutManager.findLastVisibleItemPosition() <= PAGE_LIMIT);
    }

    /***
     * Initialises the images view model
     */
    private void initialiseViewModel() {
        imagesViewModel = new ViewModelProvider(this).get(ImagesFragmentViewModel.class);
        imagesViewModel.getImageList().observe(getViewLifecycleOwner(), images -> adapter.submitList(images));
        imagesViewModel.setImageList(new ArrayList<>());
    }


    public void process() {
        if (imagesViewModel == null) return;
        if (imagesViewModel.getImageList().getValue() == null) return;
        ArrayList<ClassifiedImage> list = new ArrayList<>(imagesViewModel.getImageList().getValue());
        if (list.size() == dbManager.getImageCount(datasetName)) return;
        showProgressBar();

        ArrayList<ClassifiedImage> moreImages = dbManager.loadImages(datasetName, PAGE_LIMIT, list.size());
        list.addAll(moreImages);
        imagesViewModel.setImageList(list);
        numImagesLoaded = list.size();
        new Handler(Looper.getMainLooper()).postDelayed(this::hideProgressBar, 3000);
    }

    /***
     * Opens the gallery for image selection
     */
    public void startGalleryImagePicker() {
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
     * Method to populate and display the dataset's images to a gridview using the arraylist of ClassifiedImages
     */
    private void setupImageGrid() {
        //get the recycler view, set it's layout to a gridlayout with 2 columns & then set the adapter
        adapter = new ImageListAdapter(requireContext(), this);
        adapter.submitList(new ArrayList<>());
        gridLayoutManager = new GridLayoutManager(getContext(), 2, GridLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(gridLayoutManager);
        OverScrollDecoratorHelper.setUpOverScroll(recyclerView, OverScrollDecoratorHelper.ORIENTATION_VERTICAL);
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            boolean isScrolling;

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

                if (dy > 0) {
                    int currentItems = gridLayoutManager.getChildCount();
                    int totalItems = gridLayoutManager.getItemCount();
                    int scrolledItems = gridLayoutManager.findFirstVisibleItemPosition();

                    if (isScrolling && (currentItems + scrolledItems == totalItems)) {
                        isScrolling = false;
                        if (!selectedAll && !filterIsActive) process();
                    }
                }
            }
        });
    }

    /***
     * Method to show the progress bar in order to signal data is loading.
     */
    public void showProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
    }

    /***
     * Method to hide the progress bar in order to signal the end of any data loading
     */
    public void hideProgressBar() {
        progressBar.setVisibility(View.GONE);
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

            saveImages.setOnClickListener(v -> confirmAddFromGallery(viewPager, dialog));
            dialog.show();
        }
    }

    /***
     * Disables an edit text
     * @param editText the edit text to disable
     */
    public void disableEditText(EditText editText) {
        editText.setHint("");
        editText.setFocusable(true);
        editText.setFocusableInTouchMode(true);
        editText.setInputType(InputType.TYPE_NULL);
    }

    /***
     * Enables a EditText
     * @param editText the edit text to enable
     */
    public void enableEditText(EditText editText) {
        editText.setHint("Classification label");
        editText.setFocusable(true);
        editText.setFocusableInTouchMode(true);
        editText.setInputType(InputType.TYPE_CLASS_TEXT);
    }

    /***
     * Displays a confirmation dialog for saving images to the dataset and then begins an upload task on user confirmation
     * @param viewPager the viewpager in which the images are displayed
     * @param dialog the popup dialog
     */
    public void confirmAddFromGallery(ViewPager2 viewPager, Dialog dialog) {
        String informativeMessage;
        if (viewPager.getAdapter() == null) return;

        if (allLabelsFilled(viewPager.getAdapter().getItemCount())) {
            informativeMessage = "Save classified images?";
        } else {
            informativeMessage = "All label fields have not been filled out. Any images with empty labels will be classified as '-'. Are you sure you wish to save these images?";
        }

        //if the user cancels deletion close the popup but leave them in the selection mode
        new SweetAlertDialog(requireContext(), SweetAlertDialog.WARNING_TYPE)
                .setTitleText("Are you sure?")
                .setContentText(informativeMessage)
                .setConfirmText("Yes")
                .setConfirmClickListener(sDialog -> {
                    sDialog.dismissWithAnimation();

                    //upload images to backend
                    UploadTask uploadImages = new UploadTask(requireContext(), galleryImages, labels, datasetName) {
                        @Override
                        public void runOnCompletion() {
                            //load images to the recycler view if the current amount of images is less than the page limit
                            if (gridLayoutManager.getItemCount() < PAGE_LIMIT || (gridLayoutManager.getChildCount() & 1) == 1) process();
                        }
                    };
                    uploadImages.execute();
                    dialog.dismiss();
                })
                .setCancelButton("No", SweetAlertDialog::dismissWithAnimation)
                .show();
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
        else editText.getText().clear();

        //position the cursor to the end of the text
        editText.setSelection(editText.getText().length());
    }

    /***
     * A method to create the filtered list containing all images which match a given keyword
     * @param keyword the keyword to filter the list by
     */
    private void search(String keyword) {
        if (imagesViewModel == null) return;
        if (imagesViewModel.getImageList().getValue() == null) return;
        ArrayList<ClassifiedImage> filteredSearchList = new ArrayList<>();

        dbManager.getCachedImageList(datasetName).forEach(image -> {
            if (image.getClassificationLabel().toLowerCase().contains(keyword.toLowerCase())) {
                filteredSearchList.add(image);
            }
        });

        //display the filtered list
        adapter.submitList(filteredSearchList);
    }

    /***
     * Downloads image files from the server and updates the UI. If the image files have already been downloaded, update the UI using images stored in Local Database.
     */
    public void downloadAll() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                new DownloadTask(requireContext(), datasetName, true) {
                    @Override
                    public void onPageLimitLoaded() {
                        process();
                    }
                }.execute();
            } catch (DownloadTask.ImagesAlreadyExistException e) {
                retrievedAll = true;
                requireActivity().runOnUiThread(this::process);
            } catch (Exception e) {
                Log.e("TAG", "Error occurred while trying to download image files: \n" + e.getMessage());
                requireActivity().runOnUiThread(this::process);
            }
        });
        executor.shutdown();
    }

    /***
     * A method which triggers when a user clicks on an image. If the action mode is enabled, the image will be highlighted on selection. Else, start the image viewer.
     * @param position the position of the image that was clicked
     * @param image the classified image that was clicked
     */
    @Override
    public void onImageClick(int position, ClassifiedImage image) {
        //if action mode is enabled, change image selection state
        if (isActionMode) {
            onImageSelect(position, image);
        } else {
            if (image == null) return;
            startImageViewer(image, position);
        }
    }

    /***
     * Changes the selection state of an image.
     * @param position the position of the selected item in the adapter list.
     */
    private void onImageSelect(int position, ClassifiedImage image) {
        String selectionText = imagesViewModel.getText().getValue();
        int numSelected = (selectionText != null) ? Integer.parseInt(imagesViewModel.getText().getValue()) : 0;

        //if an image has been selected
        if (!image.isSelected()) {
            //set its isSelected state to true
            image.setSelected(true);
            numSelected++;
        } else {
            //else an image has been deselected so set its selection state to false
            image.setSelected(false);
            numSelected--;
        }

        //update the view in the recycler view to show selection state & update selection text
        adapter.notifyItemChanged(position);
        imagesViewModel.setText(String.valueOf(numSelected));

        /*
        ClassifiedImage selectedImage;
        String selectionText = imagesViewModel.getText().getValue();
        int numSelected = (selectionText != null) ? Integer.parseInt(imagesViewModel.getText().getValue()) : 0;

        if (imagesViewModel == null) return;
        if (imagesViewModel.getImageList().getValue() == null) return;

        selectedImage = (ClassifiedImage) image.clone();
        if (!selectedImage.isSelected()) {
            selectedImage.setSelected(true);
            numSelected++;
        } else {
            selectedImage.setSelected(false);
            numSelected--;
        }

        ArrayList<ClassifiedImage> imageList = new ArrayList<>(imagesViewModel.getImageList().getValue());
        imageList.remove(position);
        imageList.add(position, selectedImage);
        imagesViewModel.setImageList(imageList);
        imagesViewModel.setText(String.valueOf(numSelected));

         */
    }

    /***
     * Starts the image viewer to display the image which was pressed.
     * @param image the classified image object at the selected position.
     * @param position the adapter position of the selected item
     */
    @SuppressLint("DefaultLocale")
    private void startImageViewer(ClassifiedImage image, int position) {
        View view = View.inflate(requireContext(), R.layout.pager_overlay_view, null);
        TextView index = view.findViewById(R.id.imageIndex);
        TextView label = view.findViewById(R.id.imageLabel);
        ImageListAdapter.ViewHolder holder = ((ImageListAdapter.ViewHolder) recyclerView.findViewHolderForAdapterPosition(position));
        if (holder == null) return;

        //set the overlay text views with the appropriate details
        index.setText(String.format("%d/%d", position + 1, adapter.getItemCount()));
        label.setText(image.getClassificationLabel());

        if (viewer == null) {
            viewer = new StfalconImageViewer.Builder<>(requireContext(), adapter.getCurrentList(), (imageView, picture) ->
                    Glide.with(requireContext())
                            .load((picture.getFullImageFilePath() != null) ? picture.getFullImageFilePath() : picture.getCachedFilePath())
                            .placeholder(R.drawable.progress_animation)
                            .into(imageView))
                    .withStartPosition(position)
                    .withTransitionFrom(holder.image)
                    .withOverlayView(view)
                    .withDismissListener(() -> viewer = null)
                    .withImageChangeListener(pos -> {

                        viewer.updateImages(adapter.getCurrentList());
                        UpdateTransitionImage(pos);

                        if (pos <= adapter.getItemCount()) {
                            //update the image index position and classification label text
                            index.setText(String.format("%d/%d", pos + 1, adapter.getItemCount()));
                            label.setText(adapter.getItemAtPos(pos).getClassificationLabel());
                        }
                    }).show();
        }
    }


    /***
     * A method to update the image viewer's transition image view.
     * @param position the index position of the image being displayed in the image viewer.
     */
    public void UpdateTransitionImage(int position) {
        ImageListAdapter.ViewHolder ImageViewHolder = (ImageListAdapter.ViewHolder) recyclerView.findViewHolderForAdapterPosition(position);

        if (ImageViewHolder != null) {
            ImageView imageView = ImageViewHolder.image;
            if (imageView != null) {
                viewer.updateTransitionImage(imageView);
            }
        }
    }

    /***
     * Handles long clicks events on images. If the action mode is not enabled, enable it to enter multi-selection mode. If the action mode is enabled, long click should
     * just toggle image selection.
     * @param position the adapter position of the selected image
     * @param image the selected classified image object
     */
    @Override
    public void onImageLongClick(int position, ClassifiedImage image) {
        //if action mode in not enabled, initialise action mode
        if (!isActionMode) {
            requireActivity().startActionMode(actionModeCallback);
        }
        onImageSelect(position, image);
    }


    ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        /***
         * Called when action mode is first created. The menu supplied will be used to generate action buttons for the action mode.
         * @param mode ActionMode being created
         * @param menu Menu used to populate action buttons
         * @return true if the action mode should be created, false if entering this mode should be aborted.
         */
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            //Initialise menu inflater & inflate menu
            MenuInflater menuInflater = mode.getMenuInflater();
            menuInflater.inflate(R.menu.context_menu_contents, menu);

            //display all icons on the action bar rather than in a dropdown
            for (int i = 0; i < menu.size(); i++) {
                menu.getItem(i).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            }
            return true;
        }

        /***
         * Called to refresh an action mode's action menu whenever it is invalidated.
         * @param mode ActionMode being prepared
         * @param menu Menu used to populate action buttons
         * @return true if the menu or action mode was updated, false otherwise.
         */
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            isActionMode = true;

            //Whenever, a user selects/deselects an item
            imagesViewModel.getText().observe(getViewLifecycleOwner(), s -> {
                //update the action bar title to show the number of selected images
                mode.setTitle(String.format("%s", s));
            });

            return true;
        }

        /***
         * Called to report a user click on an action button.
         * @param mode The current ActionMode
         * @param item The item that was clicked
         * @return true if this callback handled the event, false if the standard MenuItem invocation should continue.
         */
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int itemId = item.getItemId();

            if (imagesViewModel != null && imagesViewModel.getText() != null && imagesViewModel.getText().getValue() != null) {
                int numSelected = Integer.parseInt(imagesViewModel.getText().getValue());

                if (numSelected == 0 && itemId != R.id.action_select_all) {
                    Toast.makeText(requireContext(), "Please select atleast one image", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }

            if (itemId == R.id.action_delete) {//when user presses delete
                deleteConfirmation(mode);
            } else if (itemId == R.id.action_relabel) {//when the user presses edit
                confirmEditCategories(mode);
            } else if (itemId == R.id.action_select_all) {
                if (!filterIsActive) selectAll();
                else filteredSelectAll();
            } else if (itemId == R.id.action_classify) {
                showAutoClassifierDialog(mode);
            }

            return false;
        }

        /***
         * Called when the action mode is about to be destroyed.
         * @param mode The current ActionMode being destroyed
         */
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            isActionMode = false;
            imagesViewModel.setText(String.valueOf(0));
            deselectAll();
            selectedAll = false;
            selectedImages.clear();
        }
    };

    /***
     * A method to confirm the deletion process via a popup before deleting images
     * @param mode the action mode
     */
    public void deleteConfirmation(ActionMode mode) {
        //if the user cancels deletion close the popup but leave them on the selection mode
        new SweetAlertDialog(requireContext(), SweetAlertDialog.WARNING_TYPE)
                .setTitleText("Are you sure?")
                .setContentText("You won't be able to recover the image(s) after deletion")
                .setConfirmText("Delete")
                .setConfirmClickListener(sDialog -> {
                    sDialog.dismissWithAnimation();

                    adapter.getCurrentList().forEach(image -> {
                        if (image.isSelected()) selectedImages.add(image);
                    });

                    //delete the selected images
                    DeleteTask deleteImages = new DeleteTask(requireContext(), selectedImages, datasetName) {
                        @Override
                        public void runOnCompletion() {
                            updateUI();
                            mode.finish();
                            if (gridLayoutManager.getItemCount() < PAGE_LIMIT) process();
                        }
                    };
                    deleteImages.execute();
                })
                .setCancelButton("Cancel", SweetAlertDialog::dismissWithAnimation)
                .show();
    }


    /***
     * Loads the same number of images which were in the recycler prior to select all
     */
    private void updateUI() {
        //imagesViewModel.setImageList(null);
        imagesViewModel.setImageList(dbManager.loadImages(datasetName, numImagesLoaded, 0));
        if(filterIsActive) search(searchView.getQuery().toString());
    }

    /***
     * A method which displays a confirmation dialog prompting the user to confirm the reclassification of images
     * @param mode the action mode
     */
    public void confirmEditCategories(ActionMode mode) {
        final EditText editText = new EditText(requireContext());
        //if the user clicks cancel close the popup but leave them on the selection mode
        new SweetAlertDialog(requireContext(), SweetAlertDialog.WARNING_TYPE)
                .setTitleText("Reclassify all selected images as: ")
                .setConfirmText("Reclassify")
                .setCustomView(editText)
                .setConfirmClickListener(sweetAlertDialog -> {
                    String newClassification = editText.getText().toString().trim();

                    //if a value has been entered
                    if (newClassification.length() > 0) {
                        sweetAlertDialog.dismiss();

                        adapter.getCurrentList().forEach(image -> {
                            if (image.isSelected()) selectedImages.add(image);
                        });

                        //reclassify selected images
                        ReclassifyTask editCategoriesTask = new ReclassifyTask(requireContext(), selectedImages, newClassification, datasetName) {
                            @Override
                            public void runOnCompletion() {
                                updateUI();
                                mode.finish();
                            }
                        };
                        editCategoriesTask.execute();

                    } else
                        editText.setError("Please enter a classification label");
                })
                .setCancelButton("Cancel", SweetAlertDialog::dismissWithAnimation)
                .show();
    }


    /***
     * Display a popup dialog where the user will be able to select a model & the minimum prediction confidence required in order
     * for the classifier to be able to overwrite the existing label.
     * @param mode the action mode
     */
    public void showAutoClassifierDialog(ActionMode mode) {
        //inflate the layout of the dialog & initialise the layout views
        final View layout = View.inflate(requireContext(), R.layout.auto_classify_dialog, null);
        final EditText confidenceEditText = layout.findViewById(R.id.requiredConfidence);
        final Spinner spinner = layout.findViewById(R.id.modelSpinner);
        populateModelSpinner(spinner);

        //display the dialog
        new SweetAlertDialog(requireContext(), SweetAlertDialog.NORMAL_TYPE)
                .setConfirmText("Run Classifier")
                .setCustomView(layout)
                .setConfirmClickListener(dialog -> {
                    if (spinner.getSelectedItem() == null) {
                        Toast.makeText(requireContext(), "There are no models to use", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        return;
                    }

                    String selectedModel = spinner.getSelectedItem().toString();
                    double confidence = getConfidence(confidenceEditText);

                    if (confidence > 0 && confidence <= 1) {
                        dialog.dismiss();
                        classify(selectedModel, confidence, mode);
                    } else
                        confidenceEditText.setError("Required value must be greater than 0 and less than or equal to 1");

                })
                .show();
    }

    /***
     * Gets the confidence score entered by the user in double format if the input is valid or displays an error on the edittext
     * @param confidenceEditText the edit text which contains the user input
     * @return double value between 0 to 1 on success or -1 if error occured.
     */
    private double getConfidence(EditText confidenceEditText) {
        try {
            return Double.parseDouble(confidenceEditText.getText().toString().trim());
        } catch (Exception e) {
            confidenceEditText.setError("Required value must be greater than 0 and less than or equal to 1");
        }

        return -1;
    }

    /***
     * Method to begin the auto-classification process
     * @param model the name of the model
     * @param confidence the minimum confidence score required in order for the current label to be overwritten
     * @param mode the action mode
     */
    private void classify(String model, double confidence, ActionMode mode) {
        Classifier classifier;
        ClassifierDetails details = ClassifierUtils.deserializeModelJSON(requireContext(), model);

        if (details != null) {
            classifier = Classifier.createInstance(requireContext(), details);
            if (classifier == null) return;

            adapter.getCurrentList().forEach(image -> {
                if (image.isSelected()) selectedImages.add(image);
            });

            AutoClassifyTask task = new AutoClassifyTask(requireContext(), selectedImages, classifier, confidence, datasetName) {
                @Override
                public void runOnCompletion() {
                    updateUI();
                    mode.finish();
                }
            };
            task.execute();
        }
    }

    /***
     * Method to populate the spinner with model names
     * @param spinner the spinner to populate
     */
    private void populateModelSpinner(Spinner spinner) {
        AssetManager assetManager = requireContext().getAssets();
        ArrayList<String> models = new ArrayList<>();
        try {
            for (String modelName : assetManager.list("")) {
                if (modelName.endsWith(".pt") || modelName.endsWith(".tflite")) {
                    models.add(modelName);
                }
            }
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(requireContext(), R.layout.support_simple_spinner_dropdown_item, models);
            spinner.setAdapter(arrayAdapter);
        } catch (IOException e) {
            Log.e("TAG", "Failed to populate model spinner\n" + e.getMessage());
        }
    }

    /***
     * select all images in the adapter list
     */
    public void selectAll() {
        selectedAll = true;
        ArrayList<ClassifiedImage> images = dbManager.getCachedImageList(datasetName);
        images.forEach(image -> image.setSelected(true));
        //imagesViewModel.setImageList(null);
        imagesViewModel.setImageList(images);
        imagesViewModel.setText(String.valueOf(images.size()));

        //adapter.notifyItemRangeChanged(0, images.size()-1);

        /*
        int totalImages = (int) dbManager.getImageCount(datasetName);
        selectedAll = true;
        selectedImages.clear();
        selectedImages.addAll(adapter.getCurrentList());
        selectedImages.forEach(image -> image.setSelected(true));
        selectedImages.removeIf(image -> !image.isSelected());
        adapter.submitList(selectedImages);
        visibleSelection.addAll(selectedImages);
        selectedImages.addAll(dbManager.loadImages(datasetName, totalImages - adapter.getItemCount(), selectedImages.size()));
        imagesViewModel.setText(String.valueOf(selectedImages.size()));

         */



        /*
        selectedImages.clear();
        selectedImages.addAll(adapter.getCurrentList());
        selectedImages.forEach(image -> image.setSelected(true));
        adapter.notifyDataSetChanged();
        imagesViewModel.setText(String.valueOf(selectedImages.size()));
         */
    }


    public void deselectAll() {
        updateUI();
        imagesViewModel.setText(String.valueOf(0));
    }

    public void filteredSelectAll() {
        selectedAll = true;
        adapter.getCurrentList().forEach(image -> image.setSelected(true));
        adapter.notifyItemRangeChanged(0, adapter.getItemCount());
        imagesViewModel.setText(String.valueOf(adapter.getItemCount()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recyclerView.setAdapter(null);
    }
}