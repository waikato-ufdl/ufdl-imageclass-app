package com.example.myapplication.ui.images;

import android.content.Context;
import android.media.Image;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
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

            //stores the view in memory
            convertView.setTag(holder);
        }
        else
        {
            holder = (ViewHolder) convertView.getTag();
        }

        Glide.with(getContext())
                .asBitmap()
                .load(images.get(position).getImageArray())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.image);

        holder.classification.setText(images.get(position).getClassification());

        return convertView;
    }

    private static class ViewHolder
    {
        ImageView image;
        TextView classification;
    }



}
