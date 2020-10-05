package com.example.myapplication.ui.images;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import java.util.List;
import android.net.Uri;

public class GallerySelectionAdapter  extends RecyclerView.Adapter<GallerySelectionAdapter.ViewHolder> {
    //initialise variables
    private Context mContext;
    private List<Uri> images;

    public GallerySelectionAdapter(Context context, List<Uri> selectedImages)
    {
        mContext = context;
        images = selectedImages;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //initialise view
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.gallery_item, parent, false);

        //return Viewholder containing the view
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        //set image view
        //use glide to load image into the image view for display
        Glide.with(mContext)
                .load(images.get(position))
                .into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        //return image view
        return images.size();
    }

    public class ViewHolder extends  RecyclerView.ViewHolder{
        //initialise imageview item
        ImageView imageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            imageView = itemView.findViewById(R.id.galleryImage);
        }
    }



}
