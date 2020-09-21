package com.example.myapplication.ui.gallery;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myapplication.R;
import com.github.waikatoufdl.ufdl4j.action.Datasets;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class datasetListAdapter extends ArrayAdapter<Datasets.Dataset> {
    private Context mContext;
    private int mResource;

    /**
     * Default constructor for the datasetListAdapter
     * @param context
     * @param resource
     * @param objects
     */
    public datasetListAdapter(@NonNull Context context, int resource, @NonNull ArrayList<Datasets.Dataset> objects) {
        super(context, resource, objects);
        mContext = context;
        mResource = resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        //get the dataset information
        String datasetName = getItem(position).getName();
        String projectName = Integer.toString(getItem(position).getProject());
        String datasetTags = getItem(position).getTags();

        ViewHolder holder;

        //using the view holder design pattern to store few objects ahead of time and load items as we go

        //if the position hasn't been visited, create a view and store it into memory
        if(convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(mResource, parent, false);
            holder = new ViewHolder();
            holder.datasetName = (TextView) convertView.findViewById(R.id.dataseName);
            holder.projectName = (TextView) convertView.findViewById(R.id.projectName);
            holder.datasetTags = (TextView) convertView.findViewById(R.id.datasetTags);

            convertView.setTag(holder);
        }
        else
        {
            //else position has been visited so reference view from memory
            holder = (ViewHolder) convertView.getTag();
        }

        //set the view holder variable text
        holder.datasetName.setText("Dataset Name: " + datasetName);
        holder.projectName.setText("Project name: " + projectName);
        holder.datasetTags.setText("Tags:" + datasetTags);

        return convertView;
    }

    /**
     * ViewHolder class to hold view objects
     */
    static class ViewHolder
    {
         TextView datasetName;
         TextView projectName;
         TextView datasetTags;
    }
}
