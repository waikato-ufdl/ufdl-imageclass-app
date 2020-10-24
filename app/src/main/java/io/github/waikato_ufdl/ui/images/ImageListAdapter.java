package io.github.waikato_ufdl.ui.images;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import io.github.waikato_ufdl.MainActivity;
import com.example.myapplication.R;

import io.github.waikato_ufdl.ui.settings.Utility;
import com.github.waikatoufdl.ufdl4j.action.ImageClassificationDatasets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import cn.pedant.SweetAlert.SweetAlertDialog;

import static com.google.gson.reflect.TypeToken.get;

public class ImageListAdapter extends RecyclerView.Adapter<ImageListAdapter.ViewHolder> {
    private Fragment fragment;
    private Context mContext;
    private LayoutInflater mInflater;
    private ArrayList<ClassifiedImage> images;
    private ArrayList<ClassifiedImage> selectedImages;
    private boolean isActionMode;
    ImagesFragmentViewModel imagesViewModel;


    public ImageListAdapter(Fragment frag, Context context, ArrayList<ClassifiedImage> imageList)
    {
        fragment = frag;
        mContext = context;
        mInflater = LayoutInflater.from(context);
        images = imageList;
        isActionMode = false;
        selectedImages =  new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //initialise view
        View view = mInflater.inflate(R.layout.image_display, parent, false);

        //initialise view model
        imagesViewModel =  ViewModelProviders.of(fragment).get(ImagesFragmentViewModel.class);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        ClassifiedImage image = images.get(position);

        if(image != null)
        {
            //set the classification label
            holder.classification.setText(image.getClassification());

            Glide.with(mContext).clear(holder.image);

            //use glide to load image into the image view for display
            Glide.with(mContext)
                    .asBitmap()
                    .load(image.getImageArray())
                    .placeholder(R.drawable.progress_animation)
                    .into(holder.image);
        }

        if(image.isSelected())
        {
            holder.checkBox.setChecked(true);
            holder.checkBox.setVisibility(View.VISIBLE);

            //set textview background colour
            holder.classification.setBackgroundColor(Color.RED);
            holder.classification.setTextColor(Color.WHITE);
        }
        else
        {
            holder.checkBox.setChecked(false);
            holder.checkBox.setVisibility(View.GONE);

            //set textview background colour
            holder.classification.setBackgroundColor(Color.TRANSPARENT);

            if(!Utility.loadDarkModeState()) {
                holder.classification.setTextColor(Color.BLACK);
            }
        }

        //on long press of an image we want to invoke the action mode
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                //if action mode in not enabled, initialise action mode
                if(!isActionMode) {
                    //initialise Action mode
                    ActionMode.Callback callback = new ActionMode.Callback() {
                        @Override
                        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                            //Initialise menu inflater & inflate menu
                            MenuInflater menuInflater = mode.getMenuInflater();
                            menuInflater.inflate(R.menu.context_menu, menu);
                            return true;
                        }

                        @Override
                        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                            //When action mode is preparing
                            isActionMode = true;
                            onImageSelect(holder);

                            //Whenever, a user selects/deselects an item
                            imagesViewModel.getText().observe(fragment, new Observer<String>() {
                                @Override
                                public void onChanged(String s) {
                                    //update the action bar title to show the number of selected images
                                    mode.setTitle(String.format("%s Selected", s));
                                }
                            });

                            return true;
                        }

                        @Override
                        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                            //handles the click of an action mode item

                            //get menu id
                            int id = item.getItemId();

                            //check which menu item was clicked
                            switch (id)
                            {
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

                        @Override
                        public void onDestroyActionMode(ActionMode mode) {
                            //when action mode is destroyed
                            isActionMode = false;

                            //go through each selected image & update it's isSelected to false
                            for (ClassifiedImage image : selectedImages) {
                                image.setSelected(false);
                            }

                            //clear selected images list & notify adapter
                            selectedImages.clear();
                            notifyDataSetChanged();
                        }
                    };
                    //Start action mode
                    ((MainActivity) v.getContext()).startActionMode(callback);
                }
                else
                {
                    //when action mode is already enabled call the onImageSelect method
                    onImageSelect(holder);
                }
                return true;
            }
        });

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if action mode is enabled
                if(isActionMode)
                {
                    //if a user selects an image,
                    onImageSelect(holder);
                }
                else
                {
                    //an image has been selected (but not in selection mode)
                    Log.v("TAG", holder.getAdapterPosition() + " ");
                }
            }
        });
    }

    /**
     * Method to delete all the selected images from recycler view & backend
     */
    public void deleteImages()
    {
        for(ClassifiedImage image : selectedImages)
        {
            //remove image from list
            images.remove(image);

            //make an API request to delete an image
            Thread t = new Thread(() -> {
                try {

                    int datasetPK = ((ImagesFragment) fragment).getDatasetKey();
                    ImageClassificationDatasets action = Utility.getClient().action(ImageClassificationDatasets.class);

                    //delete image file
                    action.deleteFile(datasetPK, image.getImageFileName());
                    Utility.saveImageList(datasetPK, images);
                    return;
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            });
            t.start();
        }
        notifyDataSetChanged();
    }

    /**
     * Method to handle image selection/deselection
     * @param holder
     */
    private void onImageSelect(ViewHolder holder) {
        //set selected item value
        ClassifiedImage image = images.get(holder.getAdapterPosition());

        //if an image has been selected
        if(!image.isSelected())
        {
            //set its isSelected state to true & add the image to the selectedImages list
            image.setSelected(true);
            selectedImages.add(image);
        }
        else
        {
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
        return images.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder
    {
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
     * @param mode the action mode
     */
    public void deleteConfirmation(ActionMode mode)
    {
        new SweetAlertDialog(mContext, SweetAlertDialog.WARNING_TYPE)
                .setTitleText("Are you sure?")
                .setContentText("You won't be able to recover the image(s) after deletion")
                .setConfirmText("Delete")
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        //if a user accepts the deletion, delete all the selected images
                        deleteImages();

                        //show a successful deletion popup
                        sDialog
                                .setTitleText("Deleted!")
                                .setContentText("The selected images have been deleted!")
                                .setConfirmText("OK")
                                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                    @Override
                                    public void onClick(SweetAlertDialog sweetAlertDialog) {

                                        //set the data set modified flag to true in the image fragment
                                        ((ImagesFragment) fragment).setDatasetModified(true);

                                        //if the recycler view has less images than the page_limit, load in the same amount of images that have been deleted
                                        if(images.size() > 0 && images.size() < ((ImagesFragment) fragment).PAGE_LIMIT)
                                        {
                                            Thread t = new Thread(() -> {((ImagesFragment) fragment).processImages(); });
                                            t.start();
                                        }

                                        //when the user clicks ok, dismiss the popup
                                        sDialog.dismissWithAnimation();
                                        //finish action mode once a user has confirmed the deletion of images, else keep users in selection mode
                                        mode.finish();
                                    }
                                })
                                .changeAlertType(SweetAlertDialog.SUCCESS_TYPE);
                    }
                })
                .setCancelButton("Cancel", new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        //if the user cancels deletion close the popup but leave them on the selection mode
                        sDialog.dismissWithAnimation();
                    }
                })
                .show();
    }


    public void confirmEditCategories(ActionMode mode) {
        final EditText editText = new EditText(mContext);
                new SweetAlertDialog(mContext, SweetAlertDialog.WARNING_TYPE)
                .setTitleText("Reclassify all selected images as: ")
                .setConfirmText("Reclassify")
                .setCustomView(editText)
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                        String newClassification = editText.getText().toString().trim();

                        //if a value has been entered
                        if(newClassification.length() > 0) {
                            //reclassify all selected images
                            editImageCategories(newClassification);

                            //show a success popup
                            sweetAlertDialog
                                    .setTitleText("Success!")
                                    .setContentText("The selected images have been reclassified as: " + newClassification)
                                    .setConfirmText("OK")
                                    .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                        @Override
                                        public void onClick(SweetAlertDialog sweetAlertDialog) {
                                            //when the user clicks ok, dismiss the popup
                                            sweetAlertDialog.dismissWithAnimation();
                                            //finish action mode once a user has confirmed the reclassification
                                            mode.finish();
                                        }
                                    })
                                    .changeAlertType(SweetAlertDialog.SUCCESS_TYPE);
                        }
                        else
                            editText.setError("Please enter a classification label");
                    }
                })
                .setCancelButton("Cancel", new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        //if the user clicks cancel close the popup but leave them on the selection mode
                        sDialog.dismissWithAnimation();
                    }
                })
                .show();
    }

    /**
     * Method to edit image categories locally and on the API
     * @param label the new classification label for the selected images
     */
    private void editImageCategories(String label)
    {
        int datasetPK = ((ImagesFragment) fragment).getDatasetKey();

        //first go through and remove the category label for each of the images & then relabel them
        removeCurrentCategories(datasetPK, label);
        addCategories(datasetPK, label);

        //set the data set modified flag to true in the image fragment
        ((ImagesFragment) fragment).setDatasetModified(true);
    }

    /**
     * A method to remove the current labels for the selected images
     * @param datasetPK the dataset primary key to indicate which dataset the images belong to
     * @param label the new classification label to set for the selected images
     */
    private void removeCurrentCategories(int datasetPK, String label)
    {
        for(ClassifiedImage image : selectedImages)
        {
            Thread t = new Thread(() -> {
                try {
                    ImageClassificationDatasets action = Utility.getClient().action(ImageClassificationDatasets.class);

                    System.out.println(image.getClassification());

                    //make an API request to remove current the categories for each image
                    action.removeCategories(datasetPK, Arrays.asList(image.getImageFileName()), Arrays.asList(image.getClassification()));
                    //action.addCategories(datasetPK, Arrays.asList(image.getImageFileName()), Arrays.asList(label));
                    //reclassify all the selected images locally with the user defined label
                    image.setClassificationLabel(label);
                    Utility.saveImageList(datasetPK, images);
                    return;
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            });
            t.start();

        }
        notifyDataSetChanged();
    }

    /**
     * A method to add categories to each of the selected images
     * @param datasetPK the primary key of the dataset where the images belong
     * @param label the new category to set for the images
     */
    private void addCategories(int datasetPK, String label)
    {
        Thread s = new Thread(() -> {
            try {
                //retrieve the image file names and make an API request to assign the new label to each of the images
                ArrayList<String> imageFileNames = (ArrayList<String>) selectedImages.stream().map(i ->  i.getImageFileName()).collect(Collectors.toList());
                ImageClassificationDatasets action = Utility.getClient().action(ImageClassificationDatasets.class);
                action.addCategories(datasetPK, imageFileNames, Arrays.asList(label));
                return;
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        });

        s.start();
    }

    /**
     * A method to switch the adapter list to show another list. This will be used to switch between the full sized list and the filtered list.
     * @param list
     */
    public void searchCategory(ArrayList<ClassifiedImage> list)
    {
        images = list;
        notifyDataSetChanged();
    }

}
