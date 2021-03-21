package io.github.waikato_ufdl.ui.images;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class ImagesFragmentViewModel extends ViewModel {
    MutableLiveData<String> mutableText = new MutableLiveData<>();
    private MutableLiveData<List<ClassifiedImage>> mutableList;


    public LiveData<List<ClassifiedImage>> getImageList(){
        if (mutableList == null) {
            mutableList = new MutableLiveData<>();
        }
        return mutableList;
    }

    public void setImageList(ArrayList<ClassifiedImage> imageList) {
        mutableList.setValue(imageList);
    }

    /**
     * Method to set text
     * @param s : Text to set
     */
    public void setText(String s)
    {
        mutableText.setValue(s);
    }

    /**
     * Method to get retrieve live data
     * @return mutable live data containing set text
     */
    public MutableLiveData<String> getText()
    {
        return mutableText;
    }
}
