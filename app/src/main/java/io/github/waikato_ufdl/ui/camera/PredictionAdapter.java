package io.github.waikato_ufdl.ui.camera;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import io.github.waikato_ufdl.Prediction;
import io.github.waikato_ufdl.R;

/***
 * An adapter to hold and display the top 3 predictions produced by the image classifier
 */

public class PredictionAdapter extends ListAdapter<Prediction, PredictionAdapter.PredictionViewHolder> {

    /***
     * Default constructor for the prediction adapter
     * @param diffCallback the callback class used by DiffUtil while calculating the difference between two lists.
     */
    protected PredictionAdapter(@NonNull DiffUtil.ItemCallback<Prediction> diffCallback) {
        super(diffCallback);
    }

    /***
     * Called when the recyclerview needs a new PredictionViewHolder
     * @param parent The ViewGroup into which the new View will be added after it is bound to an adapter position.
     * @param viewType The view type of the new View
     * @return a new PredictionViewHolder
     */
    @NonNull
    @Override
    public PredictionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new PredictionViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.predicted_item, parent, false));
    }

    /**
     * Method to bind the View holder to the adapter
     * @param holder The ViewHolder which should be updated to represent the contents of the item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull PredictionViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    /***
     * The View Holder to represent a Prediction item view
     */
    static class PredictionViewHolder extends RecyclerView.ViewHolder {
        TextView label, probability;

        /**
         * The constructor for the Prediction view holder
         * @param itemView the prediction item view
         */
        public PredictionViewHolder(@NonNull View itemView) {
            super(itemView);
            label = itemView.findViewById(R.id.predictedLabel);
            probability = itemView.findViewById(R.id.predictedProb);
        }

        /**
         * A method to bind the prediction data to the view holder elements
         * @param prediction the prediction object
         */
        public void bind(Prediction prediction) {
            label.setText(prediction.getLabel());
            probability.setText(prediction.getFormattedConfidence());

            Float confidence = prediction.getConfidence();
            String colour = "#00000000";

            if (confidence > 0.8) {
                colour = "#40008000";
            } else if (confidence > 0.5 && confidence < 0.8)
                colour = "#40CCCC00";

            label.setBackgroundColor(Color.parseColor(colour));
            probability.setBackgroundColor(Color.parseColor(colour));
        }
    }
}



