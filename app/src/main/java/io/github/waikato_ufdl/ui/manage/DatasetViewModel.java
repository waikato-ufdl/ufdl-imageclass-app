package io.github.waikato_ufdl.ui.manage;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

/***
 * The DatasetViewModel class is designed to store and manage the list of datasets which are displayed
 * to the recyclerview in the dataset management screen.
 */

public class DatasetViewModel extends ViewModel {
    private MutableLiveData<List<ImageDataset>> mutableLiveData;

    /***
     * get the MutableLiveData of the list of datasets
     * @return the MutableLiveData of the list of datasets
     */
    public LiveData<List<ImageDataset>> getDatasetList() {
        if (mutableLiveData == null) {
            mutableLiveData = new MutableLiveData<>();
        }
        return mutableLiveData;
    }

    /***
     * Sets the MutableLiveData's dataset list
     * @param datasetList the list of datasets
     */
    public void setDatasetList(ArrayList<ImageDataset> datasetList) {
        mutableLiveData.setValue(datasetList);
    }
}

