package com.example.myapplication.ui.images;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;
import com.example.myapplication.ui.settings.Utility;
import com.github.waikatoufdl.ufdl4j.action.ImageClassificationDatasets;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class ImagesFragment extends Fragment {
    ArrayList<ClassifiedImage> images;
    int datasetKey;

    public ImagesFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //pass context to to Utility class & set the theme
        Utility.setContext(getContext());
        getContext().setTheme(Utility.getTheme());

        //get Bundle
        datasetKey = getArguments().getInt("datasetPK");

    }

    private void setupImageGrid()
    {
        GridView gridView = getView().findViewById(R.id.imageGrid);
        imagesListAdapter adapter = new imagesListAdapter(getContext(), R.layout.image_display, images);
        gridView.setAdapter(adapter);
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v =  inflater.inflate(R.layout.fragment_images, container, false);

        Thread t = new Thread(() -> {
            try {
                LoadImages();

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        t.start();

        return v;
    }

    public void LoadImages() throws Exception {
        images = new ArrayList<>();
        byte[] img;
        int index = 0;

        ImageClassificationDatasets action = ((MainActivity) getActivity()).getClient().action(ImageClassificationDatasets.class);

        Map<String, List<String>> categories = action.getCategories(datasetKey);

        for(Map.Entry<String, List<String>> entry: categories.entrySet()) {
            System.out.println(entry.getKey());
            System.out.println(entry.getValue());

            img = ((MainActivity) getActivity()).getClient().datasets().getFile(datasetKey, entry.getKey());
            images.add(new ClassifiedImage(img, entry.getValue().get(index)));
            index++;
        }

        getActivity().runOnUiThread(() ->
        {
            setupImageGrid();
        });


    }

}