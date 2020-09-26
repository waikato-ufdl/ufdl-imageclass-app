package com.example.myapplication.ui.images;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.myapplication.R;

import java.util.ArrayList;

public class ImageListAdapter extends RecyclerView.Adapter<ImageListAdapter.ViewHolder> {
    private Context mContext;
    private LayoutInflater mInflater;
    private ArrayList<ClassifiedImage> images;

    public ImageListAdapter(Context context, ArrayList<ClassifiedImage> imageList)
    {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        images = imageList;

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.image_display, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        //set the classification label
        holder.classification.setText(images.get(position).getClassification());

        //use glide to load image into the image view for display
        Glide.with(mContext)
                .asBitmap()
                .load(images.get(position).getImageArray())
                .placeholder(R.drawable.progress_animation)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.image);
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder
    {
        ImageView image;
        TextView classification;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            //initialise the views
            image = itemView.findViewById(R.id.gridImageView);
            classification = itemView.findViewById(R.id.gridTextView);
        }
    }
}
