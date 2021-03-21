package io.github.waikato_ufdl.ui.images;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;

import io.github.waikato_ufdl.R;
import io.github.waikato_ufdl.SessionManager;

public class ImageListAdapter extends ListAdapter<ClassifiedImage, ImageListAdapter.ViewHolder> {
    private final InteractionListener interactionListener;
    private final Context context;

    /***
     * Constructor for image list adapter
     */
    public ImageListAdapter(Context context, InteractionListener interactionListener) {
        super(IMAGE_ITEM_DIFF_CALLBACK);
        this.context = context;
        this.interactionListener = interactionListener;
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
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.image_display, parent, false), interactionListener);
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
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        ImageView image;
        TextView classification;
        CheckBox checkBox;
        InteractionListener interactionListener;

        /***
         * The constructor for the viewholder
         * @param itemView the view
         */
        public ViewHolder(@NonNull View itemView, InteractionListener interactionListener) {
            super(itemView);

            //initialise the views
            image = itemView.findViewById(R.id.gridImageView);
            classification = itemView.findViewById(R.id.gridTextView);
            checkBox = itemView.findViewById(R.id.imageCheckBox);
            this.interactionListener = interactionListener;

            //set click listeners
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
            checkBox.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            interactionListener.onImageClick(getAdapterPosition(), getItem(getAdapterPosition()));
        }

        @Override
        public boolean onLongClick(View v) {
            interactionListener.onImageLongClick(getAdapterPosition(), getItem(getAdapterPosition()));
            return true;
        }
    }

    public ClassifiedImage getItemAtPos(int position) {
        return getItem(position);
    }

    public interface InteractionListener {
        void onImageClick(int position, ClassifiedImage image);

        void onImageLongClick(int position, ClassifiedImage image);
    }
}

