package io.github.waikato_ufdl.ui.gallery;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.github.waikatoufdl.ufdl4j.action.Datasets;
import java.util.ArrayList;

import io.github.waikato_ufdl.DBManager;
import io.github.waikato_ufdl.R;

public class datasetRecyclerAdapter extends RecyclerView.Adapter<datasetRecyclerAdapter.ViewHolder> {
    private ArrayList<Datasets.Dataset> datasetList;
    private RecyclerViewClickListener listener;
    private LayoutInflater mInflater;
    private DBManager dbManager;

    public datasetRecyclerAdapter(Context context, ArrayList<Datasets.Dataset> dsList)
    {
        mInflater = LayoutInflater.from(context);
        datasetList = dsList;
        dbManager = new DBManager(context);
    }

    @NonNull
    @Override
    public datasetRecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //initialise item view
        View view = mInflater.inflate(R.layout.dataset_display, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull datasetRecyclerAdapter.ViewHolder holder, int position) {
        //get the dataset information
        String datasetName = datasetList.get(position).getName();
        String projectName = dbManager.getProjectName(datasetList.get(position).getProject());
        String datasetTags = datasetList.get(position).getTags();

        //set the label colours
        String datasetNameLabel = getColoredSpanned("Dataset Name: ");
        String projectNameLabel = getColoredSpanned("Project name: ");
        String tagLabel = getColoredSpanned("Tags: ");


        //set the view holder variable text
        holder.datasetName.setText(HtmlCompat.fromHtml(datasetNameLabel + datasetName, HtmlCompat.FROM_HTML_MODE_LEGACY));
        holder.projectName.setText(HtmlCompat.fromHtml(projectNameLabel + projectName, HtmlCompat.FROM_HTML_MODE_LEGACY));
        holder.datasetTags.setText(HtmlCompat.fromHtml(tagLabel + datasetTags, HtmlCompat.FROM_HTML_MODE_LEGACY));
    }

    @Override
    public int getItemCount() {
        return datasetList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener
    {
        TextView datasetName;
        TextView projectName;
        TextView datasetTags;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            //initialise views
            datasetName = itemView.findViewById(R.id.dataseName);
            projectName = itemView.findViewById(R.id.projectName);
            datasetTags = itemView.findViewById(R.id.datasetTags);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View v) {
            listener.onClick(v, getAdapterPosition());
        }

        @Override
        public boolean onLongClick(View v) {
            listener.onLongClick(v, getAdapterPosition());
            return true;
        }
    }

    public void setData(ArrayList<Datasets.Dataset> dsList)
    {
        datasetList = dsList;
    }

    public void setListener(RecyclerViewClickListener lis)
    {
        listener = lis;
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

    public interface RecyclerViewClickListener{
        void onClick(View v, int position);
        void onLongClick(View v, int position);
    }
}
