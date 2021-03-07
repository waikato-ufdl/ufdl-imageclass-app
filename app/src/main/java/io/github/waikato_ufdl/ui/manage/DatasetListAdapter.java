package io.github.waikato_ufdl.ui.manage;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import io.github.waikato_ufdl.DBManager;
import io.github.waikato_ufdl.R;
import io.github.waikato_ufdl.SessionManager;

public class DatasetListAdapter extends ListAdapter<ImageDataset, DatasetListAdapter.ViewHolder> {
    private final SessionManager sessionManager;
    private final DBManager dbManager;
    private RecyclerViewClickListener listener;
    private int selectedIndex = -1;


    /***
     * The
     * @param diffCallback Callback for calculating the diff between two non-null items in a list.
     * @param sessionManager The user's session manager (utility class)
     */
    protected DatasetListAdapter(@NonNull DiffUtil.ItemCallback<ImageDataset> diffCallback, SessionManager sessionManager) {
        super(diffCallback);
        this.sessionManager = sessionManager;
        this.dbManager = sessionManager.getDbManager();
    }

    /***
     * Called when RecyclerView needs a new RecyclerView.ViewHolder of the given type to represent an item.
     * @param parent The ViewGroup into which the new View will be added after it is bound to an adapter position.
     * @param viewType The view type of the new View.
     * @return A new ViewHolder that holds a View of the given view type.
     */
    @NonNull
    @Override
    public DatasetListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.dataset_display, parent, false));
    }

    /***
     * Called by RecyclerView to display the data at the specified position. This method should update the contents of the RecyclerView.ViewHolder.itemView to reflect the item at the given position.
     * @param holder The ViewHolder which should be updated to represent the contents of the item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull DatasetListAdapter.ViewHolder holder, int position) {
        ImageDataset dataset = getItem(position);

        //get the dataset information
        String datasetName = dataset.getName();
        String projectName = dbManager.getProjectName(dataset.getProject());
        String datasetTags = dataset.getTags();

        //set the label colours
        String datasetNameLabel = getColoredSpanned("Dataset name: ");
        String projectNameLabel = getColoredSpanned("Project: ");
        String tagLabel = getColoredSpanned("Tags: ");

        //set the view holder variable text
        holder.datasetName.setText(HtmlCompat.fromHtml(datasetNameLabel + datasetName, HtmlCompat.FROM_HTML_MODE_LEGACY));
        holder.projectName.setText(HtmlCompat.fromHtml(projectNameLabel + projectName, HtmlCompat.FROM_HTML_MODE_LEGACY));
        holder.datasetTags.setText(HtmlCompat.fromHtml(tagLabel + datasetTags, HtmlCompat.FROM_HTML_MODE_LEGACY));

        //highlight the selected item
        if (dataset.isSelected()) {
            //if darkmode is on, the background should be grey, else pastel yellow for light mode
            String colHex = (sessionManager.loadDarkModeState()) ? "#3d3d3d" : "#fde396";
            holder.itemView.setBackgroundColor(Color.parseColor(colHex));
        } else {
            //if the item is not selected background colour should remain as it is
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    /***
     * Returns the number of elements in the dataset list
     * @return the number of elements in the dataset list
     */
    @Override
    public int getItemCount() {
        return getCurrentList().size();
    }

    /***
     * The view holder to describe the dataset item view
     */
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        TextView datasetName;
        TextView projectName;
        TextView datasetTags;

        /***
         * The constructor for the view holder
         * @param itemView the view
         */
        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            datasetName = itemView.findViewById(R.id.dataseName);
            projectName = itemView.findViewById(R.id.projectName);
            datasetTags = itemView.findViewById(R.id.datasetTags);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        /***
         * Method triggered when dataset item is clicked
         * @param view the view which was clicked
         */
        @Override
        public void onClick(View view) {
            listener.onClick(view, getAdapterPosition());
        }

        /***
         * Method triggered when user long clicks on an item
         * @param view the view which was long clicked
         * @return true as the long click is handled
         */
        @Override
        public boolean onLongClick(View view) {
            listener.onLongClick(view, getAdapterPosition());
            return true;
        }
    }

    /***
     * Method to replace the adapter's dataset list
     * @param datasetList the list of datasets
     */
    public void setData(ArrayList<ImageDataset> datasetList) {
        submitList(datasetList);
    }

    /***
     * Setter method for setting the item click listener
     * @param listener the RecyclerViewClickListener
     */
    public void setListener(RecyclerViewClickListener listener) {
        this.listener = listener;
    }

    /***
     * Stores the index position of the selected dataset
     * @param index the index position of the selected dataset
     */
    public void setSelectedIndex(int index) {
        setDatasetSelectionState(false);
        selectedIndex = index;
        setDatasetSelectionState(true);
    }

    /***
     * Set's the selection state of a dataset at a given position
     * @param state a boolean representing the selection state. True to set dataset as selected. False to deselect a dataset.
     */
    private void setDatasetSelectionState(boolean state) {
        if (selectedIndex != -1) {
            getItem(selectedIndex).setSelected(state);
            notifyItemChanged(selectedIndex);
        }
    }

    /***
     * get the selected dataset
     * @return the selected dataset
     */
    public ImageDataset getSelectedDataset() {
        return getItem(selectedIndex);
    }

    /***
     * method to color text
     * @param text The text to colour
     * @return HTML formatted string
     */
    private String getColoredSpanned(String text) {
        String col = "#989898";
        return "<font color=" + col + ">" + text + "</font>";
    }

    /***
     * Interface for the click listeners
     */
    public interface RecyclerViewClickListener {

        /***
         * Called when a dataset is clicked
         * @param view the view that was clicked
         * @param position the index position of the dataset that was clicked
         */
        void onClick(View view, int position);

        /***
         * Called when a long click occurs on a dataset
         * @param view the view that was clicked
         * @param position the index position of the dataset that was clicked
         */
        void onLongClick(View view, int position);
    }
}
