package io.github.waikato_ufdl.ui.images;

import android.content.Context;
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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import io.github.waikato_ufdl.MainActivity;
import io.github.waikato_ufdl.R;
import io.github.waikato_ufdl.ui.settings.Utility;
import com.github.waikatoufdl.ufdl4j.action.ImageClassificationDatasets;
import com.stfalcon.imageviewer.StfalconImageViewer;
import java.io.File;
import java.util.ArrayList;
import cn.pedant.SweetAlert.SweetAlertDialog;

public class ImageListAdapter extends RecyclerView.Adapter<ImageListAdapter.ViewHolder> {
    private ImagesFragment fragment;
    private Context context;
    private LayoutInflater inflater;
    private ArrayList<ClassifiedImage> imageList;
    private ArrayList<ClassifiedImage> selectedImages;
    private boolean isActionMode;
    ImagesFragmentViewModel imagesViewModel;
    ImageClassificationDatasets action;
    private int datasetPK;
    private String datasetName;
    public StfalconImageViewer<ClassifiedImage> viewer;

    /**
     * Constructor for image list adapter
     *
     * @param fragment  the images fragment
     * @param context   the context
     * @param imageList the list of images within a particular data set
     * @param action    the action which will be used to perform API requests specifically on Image Classification datasets
     * @param datasetPK the particular dataset's primary key
     */
    public ImageListAdapter(ImagesFragment fragment, Context context, ArrayList<ClassifiedImage> imageList, ImageClassificationDatasets action, int datasetPK, String datasetName) {
        this.fragment = fragment;
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.imageList = imageList;
        this.action = action;
        this.datasetPK = datasetPK;
        this.datasetName = datasetName;

        isActionMode = false;
        selectedImages = new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //initialise view
        View view = inflater.inflate(R.layout.image_display, parent, false);

        //initialise view model
        imagesViewModel = new ViewModelProvider(fragment).get(ImagesFragmentViewModel.class);
        return new ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        ClassifiedImage image = imageList.get(position);

        if (image != null) {
            //set the classification label
            holder.classification.setText(image.getClassificationLabel());

            String imagePath = (image.getCachedFilePath() != null) ? image.getCachedFilePath() : image.getFullImageFilePath();

            //use glide to load image into the image view for display
            Glide.with(context)
                    .load(new File(imagePath))
                    .placeholder(R.drawable.progress_animation)
                    .into(holder.image);

            holder.image.setTransitionName(imagePath);
        }

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


            if (!Utility.loadDarkModeState()) {
                holder.classification.setTextColor(Color.BLACK);
            }
        }

        //on long press of an image we want to invoke the action mode
        holder.itemView.setOnLongClickListener(v -> {
            //if action mode in not enabled, initialise action mode
            if (!isActionMode) {
                //initialise Action mode
                ActionMode.Callback callback = new ActionMode.Callback() {

                    /** show the action mode menu */
                    @Override
                    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                        //Initialise menu inflater & inflate menu
                        MenuInflater menuInflater = mode.getMenuInflater();
                        menuInflater.inflate(R.menu.context_menu, menu);
                        return true;
                    }

                    /** setup the action mode */
                    @Override
                    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                        //When action mode is preparing
                        isActionMode = true;
                        onImageSelect(holder);

                        //Whenever, a user selects/deselects an item
                        imagesViewModel.getText().observe(fragment, s -> {
                            //update the action bar title to show the number of selected images
                            mode.setTitle(String.format("%s Selected", s));
                        });

                        return true;
                    }

                    /** A method which runs a particular operation based on the menu item pressed by the user */
                    @Override
                    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                        //handles the click of an action mode item

                        //get menu id
                        int id = item.getItemId();

                        //check which menu item was clicked
                        switch (id) {
                            case R.id.action_delete:
                                //when user presses delete
                                deleteConfirmation(mode);
                                break;

                            case R.id.action_relabel:
                                //when the user presses edit
                                confirmEditCategories(mode);
                                break;
                        }

                        return false;
                    }

                    /** This method runs when the action mode is ended*/
                    @Override
                    public void onDestroyActionMode(ActionMode mode) {
                        //when action mode is destroyed
                        isActionMode = false;

                        //go through each selected image & update it's isSelected to false
                        for (ClassifiedImage selectedImage : selectedImages) {
                            selectedImage.setSelected(false);
                        }

                        //clear selected images list & notify adapter
                        selectedImages.clear();
                        notifyDataSetChanged();
                    }
                };
                //Start action mode
                ((MainActivity) v.getContext()).startActionMode(callback);
            } else {
                //when action mode is already enabled call the onImageSelect method
                onImageSelect(holder);
            }
            return true;
        });

        holder.itemView.setOnClickListener(v -> {
            //if action mode is enabled
            if (isActionMode) {
                //if a user selects an image,
                onImageSelect(holder);
            } else {
                //an image has been selected (but not in selection mode)
                Log.v("TAG", holder.getAdapterPosition() + " ");

                View view = LayoutInflater.from(context).inflate(R.layout.pager_overlay_view, null, false);
                TextView index = view.findViewById(R.id.imageIndex);
                TextView label = view.findViewById(R.id.imageLabel);


                index.setText((position + 1) + "/" + imageList.size());
                label.setText(image.getClassificationLabel());

                viewer = new StfalconImageViewer.Builder<>(context, imageList, (imageView, picture) ->
                        Glide.with(context)
                                .load((picture.getFullImageFilePath() != null) ? picture.getFullImageFilePath() : picture.getCachedFilePath())
                                .placeholder(R.drawable.progress_animation)
                                .into(imageView))
                        .withStartPosition(position)
                        .withTransitionFrom(holder.image)
                        .withOverlayView(view)
                        .withImageChangeListener(pos -> {

                            viewer.updateImages(imageList);
                            UpdateTransitionImage(pos);

                            //update the image index position and classification label text
                            if (position <= imageList.size()) {
                                index.setText((pos + 1) + "/" + imageList.size());
                                label.setText(imageList.get(pos).getClassificationLabel());
                            }
                        }).show();
            }
        });
    }

    public void UpdateTransitionImage(int position) {
        ViewHolder ImageViewHolder = (ViewHolder) fragment.recyclerView.findViewHolderForAdapterPosition(position);
        if (ImageViewHolder != null && ImageViewHolder instanceof ViewHolder) {
            ImageView imageView = ImageViewHolder.image;
            if (imageView != null) {
                viewer.updateTransitionImage(imageView);
            }
        }
    }

    /**
     * Method to handle image selection/deselection
     *
     * @param holder the view holder
     */
    private void onImageSelect(ViewHolder holder) {
        //set selected item value
        ClassifiedImage image = imageList.get(holder.getAdapterPosition());

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
        notifyItemChanged(holder.getAdapterPosition());

        //set text on view model
        imagesViewModel.setText(String.valueOf(selectedImages.size()));
    }

    @Override
    public int getItemCount() {
        return imageList.size();
    }


    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView classification;
        CheckBox checkBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            //initialise the views
            image = itemView.findViewById(R.id.gridImageView);
            classification = itemView.findViewById(R.id.gridTextView);
            checkBox = itemView.findViewById(R.id.imageCheckBox);
        }
    }

    /**
     * A method to confirm the deletion process via a popup before deleting images
     *
     * @param mode the action mode
     */
    public void deleteConfirmation(ActionMode mode) {
        new SweetAlertDialog(context, SweetAlertDialog.WARNING_TYPE)
                .setTitleText("Are you sure?")
                .setContentText("You won't be able to recover the image(s) after deletion")
                .setConfirmText("Delete")
                .setConfirmClickListener(sDialog -> {
                    sDialog.dismissWithAnimation();

                    //delete the selected images
                    DeleteTask deleteImages = new DeleteTask(fragment, context, selectedImages, datasetName, action, mode);
                    deleteImages.execute();
                })
                .setCancelButton("Cancel", sDialog -> {
                    //if the user cancels deletion close the popup but leave them on the selection mode
                    sDialog.dismissWithAnimation();
                })
                .show();
    }


    /**
     * A method which displays a confirmation dialog prompting the user to confirm the action of reclassifying images
     *
     * @param mode the action mode
     */
    public void confirmEditCategories(ActionMode mode) {
        final EditText editText = new EditText(context);
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
                        ReclassifyTask editCategoriesTask = new ReclassifyTask(fragment, context, selectedImages, newClassification, datasetName, action, mode);
                        editCategoriesTask.execute();

                    } else
                        editText.setError("Please enter a classification label");
                })
                .setCancelButton("Cancel", sDialog -> {
                    //if the user clicks cancel close the popup but leave them on the selection mode
                    sDialog.dismissWithAnimation();
                })
                .show();
    }

    /**
     * A method to switch the adapter list to show another list. This will be used to switch between the full sized list and the filtered list.
     *
     * @param list
     */
    public void searchCategory(ArrayList<ClassifiedImage> list) {
        imageList = list;
        notifyDataSetChanged();
    }
}

