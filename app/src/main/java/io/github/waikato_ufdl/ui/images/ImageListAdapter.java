package io.github.waikato_ufdl.ui.images;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.util.Log;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.stfalcon.imageviewer.StfalconImageViewer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import cn.pedant.SweetAlert.SweetAlertDialog;
import io.github.waikato_ufdl.Classifier;
import io.github.waikato_ufdl.Prediction;
import io.github.waikato_ufdl.R;
import io.github.waikato_ufdl.SessionManager;
import io.github.waikato_ufdl.ui.camera.ClassifierDetails;
import io.github.waikato_ufdl.ui.camera.ClassifierUtils;

public class ImageListAdapter extends ListAdapter<ClassifiedImage, ImageListAdapter.ViewHolder> {
    private final ImagesFragment fragment;
    private final Context context;
    private final LayoutInflater inflater;
    private final ArrayList<ClassifiedImage> selectedImages;
    private boolean isActionMode;
    private ImagesFragmentViewModel imagesViewModel;
    private final String datasetName;
    public StfalconImageViewer<ClassifiedImage> viewer;

    /***
     * Constructor for image list adapter
     * @param fragment the images fragment
     * @param datasetName the name of the dataset
     */
    public ImageListAdapter(ImagesFragment fragment, String datasetName) {
        super(IMAGE_ITEM_DIFF_CALLBACK);
        this.fragment = fragment;
        this.context = fragment.getContext();
        this.inflater = LayoutInflater.from(context);
        this.datasetName = datasetName;

        isActionMode = false;
        selectedImages = new ArrayList<>();
    }

    /***
     * The DiffUtil callback used to calculate the difference between two lists
     */
    public static final DiffUtil.ItemCallback<ClassifiedImage> IMAGE_ITEM_DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ClassifiedImage>() {

                /***
                 * A method to check if two images are the same
                 * @param oldImage the old classified image object
                 * @param newImage the new classified image object
                 * @return true if classified image objects are the same
                 */
                @Override
                public boolean areItemsTheSame(@NonNull ClassifiedImage oldImage, @NonNull ClassifiedImage newImage) {
                    return oldImage.getImageFileName().equals(newImage.getImageFileName());
                }

                /***
                 * A method to check if a classified image object has changed
                 * @param oldImage the old classified image object
                 * @param newImage the new classified image object
                 * @return true if the classified image objects have the same properties
                 */
                @Override
                public boolean areContentsTheSame(@NonNull ClassifiedImage oldImage, @NonNull ClassifiedImage newImage) {
                    return oldImage.getClassificationLabel().equals(newImage.getClassificationLabel()) &&
                            java.util.Objects.equals(oldImage.isSelected(), newImage.isSelected());
                }
            };


    /***
     *
     * @param parent Method to create and return a View Holder when required by the recyclerview
     * @param viewType The view type of the new View.
     * @return A new ViewHolder that holds a View of the given view type.
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //initialise view
        View view = inflater.inflate(R.layout.image_display, parent, false);

        //initialise view model
        imagesViewModel = new ViewModelProvider(fragment).get(ImagesFragmentViewModel.class);
        return new ViewHolder(view);
    }

    /***
     * A method called by RecyclerView to display the data at the specified position.
     * @param holder The ViewHolder which should be updated to represent the contents of the item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        ClassifiedImage image = getItem(position);

        if (image != null) {
            //set the classification label
            holder.classification.setText(image.getClassificationLabel());
            String imagePath = (image.getCachedFilePath() != null) ? image.getCachedFilePath() : image.getFullImageFilePath();

            //use glide to load image into the image view for display
            Glide.with(context)
                    .load(new File(imagePath))
                    .placeholder(R.drawable.progress_animation)
                    .into(holder.image);

            if (image.isSelected()) {
                holder.checkBox.setChecked(true);
                holder.checkBox.setVisibility(View.VISIBLE);

                //set textview background colour
                holder.classification.setBackgroundColor(Color.RED);
                holder.classification.setTextColor(Color.WHITE);
            } else {
                holder.checkBox.setChecked(false);
                holder.checkBox.setVisibility(View.GONE);

                TypedValue value = new TypedValue();
                context.getTheme().resolveAttribute(R.attr.imageLabelBackground, value, true);

                //set textview background colour
                holder.classification.setBackgroundColor(value.data);

                if (!new SessionManager(context).loadDarkModeState()) {
                    holder.classification.setTextColor(Color.BLACK);
                }
            }
        }
    }

    /***
     * Changes the selection state of an image.
     * @param position the position of the selected item in the adapter list.
     */
    private void onImageSelect(int position) {
        //set selected item value
        ClassifiedImage image = getItem(position);

        //if an image has been selected
        if (!image.isSelected()) {
            //set its isSelected state to true & add the image to the selectedImages list
            image.setSelected(true);
            selectedImages.add(image);
        } else {
            //else an image has been deselected so set isSelected to false and remove from selectedImages list
            image.setSelected(false);
            selectedImages.remove(image);
        }

        //update the view in the recycler view to show selection/deselection
        notifyItemChanged(position);

        //set text on view model
        imagesViewModel.setText(String.valueOf(selectedImages.size()));
    }

    /***
     * Returns the number of elements in the adapter list
     * @return number of elements in the adapter list.
     */
    @Override
    public int getItemCount() {
        return getCurrentList().size();
    }


    /***
     * The view holder class to describe the item view
     */
    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView classification;
        CheckBox checkBox;

        /***
         * The constructor for the viewholder
         * @param itemView the view
         */
        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            //initialise the views
            image = itemView.findViewById(R.id.gridImageView);
            classification = itemView.findViewById(R.id.gridTextView);
            checkBox = itemView.findViewById(R.id.imageCheckBox);

            //set click listeners
            itemView.setOnClickListener(view -> onitemClick(this, getAdapterPosition()));
            itemView.setOnLongClickListener(view -> onItemLongClick(view, getAdapterPosition()));
            checkBox.setOnClickListener(view -> itemView.performClick());
        }
    }

    /***
     * Starts the action mode to activate multi-selection mode
     * @param view the view that was clicked
     * @param position the adapter position of the view holder pressed
     * @return true - long click has been handled
     */
    public boolean onItemLongClick(View view, int position) {
        //if action mode in not enabled, initialise action mode
        if (!isActionMode) {
            view.startActionMode(actionModeCallback);
        }

        onImageSelect(position);
        return true;
    }

    /***
     * A method which triggers when a user clicks on an image. If the action mode is enabled, the image will be highlighted on selection. Else, start the image viewer.
     * @param holder the view holder that was pressed.
     * @param position the adapter position of the pressed view holder.
     */

    public void onitemClick(ViewHolder holder, int position) {
        //if action mode is enabled, change image selection state
        if (isActionMode) {
            onImageSelect(position);
        } else {
            //an image has been selected (but not in selection mode)
            ClassifiedImage image = getItem(position);
            if (image == null) return;
            startImageViewer(image, holder, position);
        }
    }

    /***
     * Starts the image viewer to display the image which was pressed.
     * @param image the classified image object at the selected position.
     * @param holder the selected view holder
     * @param position the adapter position of the selected item
     */
    @SuppressLint("DefaultLocale")
    private void startImageViewer(ClassifiedImage image, ViewHolder holder, int position) {
        View view = View.inflate(context, R.layout.pager_overlay_view, null);
        TextView index = view.findViewById(R.id.imageIndex);
        TextView label = view.findViewById(R.id.imageLabel);

        //set the overlay text views with the appropriate details
        index.setText(String.format("%d/%d", position + 1, getItemCount()));
        label.setText(image.getClassificationLabel());

        if (viewer == null) {
            viewer = new StfalconImageViewer.Builder<>(context, getCurrentList(), (imageView, picture) ->
                    Glide.with(context)
                            .load((picture.getFullImageFilePath() != null) ? picture.getFullImageFilePath() : picture.getCachedFilePath())
                            .placeholder(R.drawable.progress_animation)
                            .into(imageView))
                    .withStartPosition(position)
                    .withTransitionFrom(holder.image)
                    .withOverlayView(view)
                    .withDismissListener(() -> viewer = null)
                    .withImageChangeListener(pos -> {

                        viewer.updateImages(getCurrentList());
                        UpdateTransitionImage(pos);

                        if (pos <= getItemCount()) {
                            //update the image index position and classification label text
                            index.setText(String.format("%d/%d", pos + 1, getItemCount()));
                            label.setText(getItem(pos).getClassificationLabel());
                        }
                    }).show();
        }
    }


    /***
     * A method to update the image viewer's transition image view.
     * @param position the index position of the image being displayed in the image viewer.
     */
    public void UpdateTransitionImage(int position) {
        ViewHolder ImageViewHolder = (ViewHolder) fragment.recyclerView.findViewHolderForAdapterPosition(position);

        if (ImageViewHolder != null) {
            ImageView imageView = ImageViewHolder.image;
            if (imageView != null) {
                viewer.updateTransitionImage(imageView);
            }
        }
    }


    /***
     * A method to confirm the deletion process via a popup before deleting images
     * @param mode the action mode
     */
    public void deleteConfirmation(ActionMode mode) {
        //if the user cancels deletion close the popup but leave them on the selection mode
        new SweetAlertDialog(context, SweetAlertDialog.WARNING_TYPE)
                .setTitleText("Are you sure?")
                .setContentText("You won't be able to recover the image(s) after deletion")
                .setConfirmText("Delete")
                .setConfirmClickListener(sDialog -> {
                    sDialog.dismissWithAnimation();

                    //delete the selected images
                    DeleteTask deleteImages = new DeleteTask(fragment, context, selectedImages, datasetName, mode);
                    deleteImages.execute();
                })
                .setCancelButton("Cancel", SweetAlertDialog::dismissWithAnimation)
                .show();
    }


    /***
     * A method which displays a confirmation dialog prompting the user to confirm the reclassification of images
     * @param mode the action mode
     */
    public void confirmEditCategories(ActionMode mode) {
        final EditText editText = new EditText(context);
        //if the user clicks cancel close the popup but leave them on the selection mode
        new SweetAlertDialog(context, SweetAlertDialog.WARNING_TYPE)
                .setTitleText("Reclassify all selected images as: ")
                .setConfirmText("Reclassify")
                .setCustomView(editText)
                .setConfirmClickListener(sweetAlertDialog -> {
                    String newClassification = editText.getText().toString().trim();

                    //if a value has been entered
                    if (newClassification.length() > 0) {
                        sweetAlertDialog.dismiss();

                        //reclassify selected images
                        ReclassifyTask editCategoriesTask = new ReclassifyTask(fragment, context, selectedImages, newClassification, datasetName, mode);
                        editCategoriesTask.execute();

                    } else
                        editText.setError("Please enter a classification label");
                })
                .setCancelButton("Cancel", SweetAlertDialog::dismissWithAnimation)
                .show();
    }

    /***
     * A method to switch the adapter list to show another list. This will be used to switch between the full sized list and the filtered list.
     * @param list the new filtered list to display
     */
    public void searchCategory(ArrayList<ClassifiedImage> list) {
        submitList(list);
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
            imagesViewModel.getText().observe(fragment, s -> {
                //update the action bar title to show the number of selected images
                mode.setTitle(String.format("%s Selected", s));
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
            if (selectedImages.size() == 0) {
                Toast.makeText(context, "Please select atleast one image", Toast.LENGTH_SHORT).show();
                return false;
            }

            //check which menu item was clicked
            switch (item.getItemId()) {
                case R.id.action_delete:
                    //when user presses delete
                    deleteConfirmation(mode);
                    break;

                case R.id.action_relabel:
                    //when the user presses edit
                    confirmEditCategories(mode);
                    break;

                case R.id.action_select_all:
                    selectAll();
                    break;

                case R.id.action_classify:
                    showAutoClassifierDialog(mode);
                    break;
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

            //go through each selected image & update it's isSelected to false
            for (ClassifiedImage selectedImage : selectedImages) {
                selectedImage.setSelected(false);
                notifyItemChanged(getCurrentList().indexOf(selectedImage));
            }

            selectedImages.clear();
        }
    };

    /***
     * select all images in the adapter list
     */
    public void selectAll() {
        if (isActionMode) {
            selectedImages.clear();
            selectedImages.addAll(getCurrentList());
            selectedImages.forEach(image -> image.setSelected(true));
            notifyDataSetChanged();
            imagesViewModel.setText(String.valueOf(selectedImages.size()));
        }
    }

    /***
     * Display a popup dialog where the user will be able to select a model & the minimum prediction confidence required in order
     * for the classifier to be able to overwrite the existing label.
     * @param mode the action mode
     */
    public void showAutoClassifierDialog(ActionMode mode) {
        //inflate the layout of the dialog & initialise the layout views
        final View layout = View.inflate(context, R.layout.auto_classify_dialog, null);
        final EditText confidenceEditText = layout.findViewById(R.id.requiredConfidence);
        final Spinner spinner = layout.findViewById(R.id.modelSpinner);
        populateModelSpinner(spinner);

        //display the dialog
        new SweetAlertDialog(context, SweetAlertDialog.NORMAL_TYPE)
                .setConfirmText("Run Classifier")
                .setCustomView(layout)
                .setConfirmClickListener(dialog -> {
                    if (spinner.getSelectedItem() == null) {
                        Toast.makeText(context, "There are no models", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }

                    String selectedModel = spinner.getSelectedItem().toString();
                    double confidence = getConfidence(confidenceEditText);

                    if (confidence > 0 && confidence <=1) {
                        dialog.dismiss();
                        classify(selectedModel, confidence, mode);
                    }
                    else confidenceEditText.setError("Required value must be greater than 0 and less than or equal to 1");

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
     * Method to begin the reclassification process
     * @param model the name of the model
     * @param confidence the minimum confidence score required in order for the current label to be overwritten
     * @param mode the action mode
     */
    private void classify(String model, double confidence, ActionMode mode) {
        Classifier classifier;
        ArrayList<ClassifiedImage> updated = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        ClassifierDetails details = ClassifierUtils.deserializeModelJSON(context, model);

        if (details != null) {
            classifier = Classifier.createInstance(context, details);
            if (classifier == null) return;

            for (ClassifiedImage image : selectedImages) {
                if (image != null) {
                    Prediction prediction = classifier.predict(image.getImageBitmap());
                    if (prediction.getConfidence() > confidence) {
                        updated.add(image);
                        labels.add(prediction.getLabel());
                    }
                }
            }

            AutoClassifyTask task = new AutoClassifyTask(fragment, context, updated, labels, datasetName, mode);
            task.execute();
        }
    }

    /***
     * Method to populate the spinner with model names
     * @param spinner the spinner to populate
     */
    private void populateModelSpinner(Spinner spinner) {
        AssetManager assetManager = context.getAssets();
        ArrayList<String> models = new ArrayList<>();
        try {
            for (String modelName : assetManager.list("")) {
                if (modelName.endsWith(".pt") || modelName.endsWith(".tflite")) {
                    models.add(modelName);
                }
            }

            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(context, R.layout.support_simple_spinner_dropdown_item, models);
            spinner.setAdapter(arrayAdapter);
        } catch (IOException e) {
            Log.e("TAG", "Failed to populate model spinner");
        }
    }


}

