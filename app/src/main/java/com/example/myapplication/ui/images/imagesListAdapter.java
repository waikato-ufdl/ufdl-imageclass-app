package com.example.myapplication.ui.images;

import android.content.Context;
import android.graphics.Color;
import android.media.Image;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.myapplication.MainActivity;
import com.example.myapplication.R;

import java.util.ArrayList;

public class imagesListAdapter extends ArrayAdapter<ClassifiedImage> {
    private Context mContext;
    private LayoutInflater mInflater;
    private int layoutResource;
    private ArrayList<ClassifiedImage> images;

    public imagesListAdapter(Context context, int layoutResource, ArrayList<ClassifiedImage> images)
    {
        super(context, layoutResource, images);
        mContext = context;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.layoutResource = layoutResource;
        this.images = images;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        final ViewHolder holder;

        //View holder design pattern
        if(convertView == null)
        {
            convertView = mInflater.inflate(layoutResource, parent, false);
            holder = new ViewHolder();;
            holder.image = (ImageView) convertView.findViewById(R.id.gridImageView);
            holder.classification = (TextView) convertView.findViewById(R.id.gridTextView);
            holder.checkBox = (CheckBox) convertView.findViewById(R.id.imageCheckBox);

            //stores the view in memory
            convertView.setTag(holder);
        }
        else
        {
            //reference the view from memory
            holder = (ViewHolder) convertView.getTag();
        }

        //use glide to load image into the image view for display
        Glide.with(getContext())
                .asBitmap()
                .load(images.get(position).getImageArray())
                .placeholder(R.drawable.progress_animation)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.image);

        //set the textview to show the classification label
        holder.classification.setText(images.get(position).getClassification());

        //if the user is in action mode (multi select mode)
        if(ImagesFragment.isActionMode)
        {
            //show the checkboxes
            holder.checkBox.setVisibility(View.VISIBLE);
        }
        else
        {
            //else don't show checkboxes on image
            holder.checkBox.setVisibility(View.GONE);
        }

        return convertView;
    }

    private static class ViewHolder
    {
        ImageView image;
        TextView classification;
        CheckBox checkBox;
    }

}
