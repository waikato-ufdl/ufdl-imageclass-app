package io.github.waikato_ufdl.ui.images;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ImagesFragmentViewModel extends ViewModel {
    MutableLiveData<String> mutableLiveData = new MutableLiveData<>();

    /**
     * Method to set text
     * @param s : Text to set
     */
    public void setText(String s)
    {
        mutableLiveData.setValue(s);
    }

    /**
     * Method to get retrieve live data
     * @return mutable live data containing set text
     */
    public MutableLiveData<String> getText()
    {
        return mutableLiveData;
    }
}
