package com.example.myapplication.ui.images;

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
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.myapplication.MainActivity;
import com.example.myapplication.R;
import com.example.myapplication.ui.home.HomeViewModel;
import com.example.myapplication.ui.settings.Utility;
import com.github.waikatoufdl.ufdl4j.action.ImageClassificationDatasets;

import java.util.ArrayList;

import static com.google.gson.reflect.TypeToken.get;

public class ImageListAdapter extends RecyclerView.Adapter<ImageListAdapter.ViewHolder> {
    private Fragment fragment;
    private Context mContext;
    private LayoutInflater mInflater;
    private ArrayList<ClassifiedImage> images;
    private ArrayList<ClassifiedImage> selectedImages = new ArrayList<>();
    private boolean isActionMode = false;
    ImagesFragmentViewModel imagesViewModel;

    public ImageListAdapter(Fragment frag, Context context, ArrayList<ClassifiedImage> imageList)
    {
        fragment = frag;
        mContext = context;
        mInflater = LayoutInflater.from(context);
        images = imageList;
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
        //set the classification label
        holder.classification.setText(image.getClassification());

        //use glide to load image into the image view for display
        Glide.with(mContext)
                .asBitmap()
                .load(image.getImageArray())
                .placeholder(R.drawable.progress_animation)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.image);

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
                                    deleteImages();

                                    //finish action mode
                                    mode.finish();
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
     * Method to delete images from the list & backend
     */
    public void deleteImages()
    {
        for(ClassifiedImage image : selectedImages)
        {
            //remove image from list
            images.remove(image);


            //make an API request to delete an
            Thread t = new Thread(() -> {
                try {

                    int datasetPK = ((ImagesFragment) fragment).getDatasetKey();
                    ImageClassificationDatasets action = Utility.getClient().action(ImageClassificationDatasets.class);
                    ArrayList<String> imageNames = new ArrayList<>();
                    ArrayList<String> imageClassifications = new ArrayList<>();

                    imageNames.add(image.getImageFileName());
                    imageClassifications.add(image.getClassification());

                    action.removeCategories(datasetPK, imageNames, imageClassifications);
                    Utility.getClient().datasets().deleteFile(datasetPK, image.getImageFileName());
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            });
            t.start();
        }
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

        //update the view in the recycler view to show selection/deselectionamily
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
}
