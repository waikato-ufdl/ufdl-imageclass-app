package com.example.myapplication.ui.gallery;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.text.HtmlCompat;

import com.example.myapplication.DBManager;
import com.example.myapplication.R;
import com.github.waikatoufdl.ufdl4j.action.Datasets;
import java.util.ArrayList;


public class datasetListAdapter extends ArrayAdapter<Datasets.Dataset> {
    private Context mContext;
    private int mResource;
    private DBManager dbManager;

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
        dbManager = new DBManager(context);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        //get the dataset information
        String datasetName = getItem(position).getName();
        String projectName = dbManager.getProjectName(getItem(position).getProject());
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

        //set the label colours to light blue
        String datasetNameLabel = getColoredSpanned("Dataset Name: ");
        String projectNameLabel = getColoredSpanned("Project name: ");
        String tagLabel = getColoredSpanned("Tags: ");


        //set the view holder variable text
        holder.datasetName.setText(HtmlCompat.fromHtml(datasetNameLabel + datasetName, HtmlCompat.FROM_HTML_MODE_LEGACY));
        holder.projectName.setText(HtmlCompat.fromHtml(projectNameLabel + projectName, HtmlCompat.FROM_HTML_MODE_LEGACY));
        holder.datasetTags.setText(HtmlCompat.fromHtml(tagLabel + datasetTags, HtmlCompat.FROM_HTML_MODE_LEGACY));

        return convertView;
    }

    /**
     * method to colour substrings of one text view
     * @param text The text to colour
     * @return
     */
    private String getColoredSpanned(String text) {
        String col = "#989898";
        String format = "<font color=" + col + ">" + text + "</font>";
        return format;
    }

    /**
     * view holder object stores each of the component views inside the tag field of the layout
     * making it possible to immediately access them without the needing to look them up repeatedly
     */
    static class ViewHolder
    {
         TextView datasetName;
         TextView projectName;
         TextView datasetTags;
    }
}
