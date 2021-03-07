package io.github.waikato_ufdl.ui.images;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import io.github.waikato_ufdl.R;

/***
 * An adapter to display images in a viewpager once the user has selected them from their gallery.
 */

public class GallerySelectionAdapter extends RecyclerView.Adapter<GallerySelectionAdapter.ViewHolder> {
    private final Context mContext;
    private final List<Uri> images;

    /***
     * Constructor for the adapter
     * @param context the context
     * @param selectedImages the list of image URI's
     */
    public GallerySelectionAdapter(Context context, List<Uri> selectedImages) {
        mContext = context;
        images = selectedImages;
    }

    /***
     * Method which creates a new view holder when required by the Recyclerview
     * @param parent the ViewGroup into which the new View will be added after it is bound to an adapter position.
     * @param viewType The view type of the new View.
     * @return A new ViewHolder that holds a View
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.gallery_item, parent, false);
        return new ViewHolder(view);
    }

    /***
     * Method to display data at a specific position.
     * @param holder The viewHolder which should be updated to represent the contents of the item at the given position in the data set.
     * @param position The position of the item within the adapter's data set
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        //use glide to load image into the image view for display
        Glide.with(mContext)
                .load(images.get(position))
                .into(holder.imageView);
    }

    /***
     * Method to get the number of items in adapter
     * @return size of the adapter list
     */
    @Override
    public int getItemCount() {
        //return image view
        return images.size();
    }


    /***
     * A viewholder to describe the item view
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        /***
         * Constructor for the view holder
         * @param itemView the view
         */
        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            imageView = itemView.findViewById(R.id.galleryImage);
        }
    }
}
