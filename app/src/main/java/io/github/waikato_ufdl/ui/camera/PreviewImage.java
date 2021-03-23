package io.github.waikato_ufdl.ui.camera;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import cn.pedant.SweetAlert.SweetAlertDialog;
import io.github.waikato_ufdl.DBManager;
import io.github.waikato_ufdl.DatasetOperations;
import io.github.waikato_ufdl.ImageOperations;
import io.github.waikato_ufdl.MainActivity;
import io.github.waikato_ufdl.R;
import io.github.waikato_ufdl.SessionManager;
import io.github.waikato_ufdl.databinding.FragmentPreviewImageBinding;
import io.github.waikato_ufdl.ui.manage.ImageDataset;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;


public class PreviewImage extends Fragment {
    private File imageFile;
    private String prediction;
    private String predictedLabel;
    private String previousLabel;
    private FragmentPreviewImageBinding binding;
    private DBManager dbManager;
    private boolean imageSaved;
    private boolean isViewHidden = false;
    private float viewOriginalXPosition;

    /***
     * The default constructor for the preview image
     */
    public PreviewImage() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SessionManager sessionManager = new SessionManager(requireContext());
        dbManager = sessionManager.getDbManager();

        //get the image path, and prediction details from the camera fragment
        if (getArguments() != null) {
            imageFile = new File(getArguments().getString("imagePath"));
            prediction = getArguments().getString("predictionInfo");
            predictedLabel = getArguments().getString("predictedLabel");
        }

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // return to the camera fragment on back button press
                returnToCamera();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        imageSaved = false;
        binding = FragmentPreviewImageBinding.inflate(inflater, container, false);
        // Inflate the layout for this fragment
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (viewOriginalXPosition == 0.0f) {
            viewOriginalXPosition = binding.saveButton.getX();
        }
        view.setOnClickListener(view1 ->
        {
            if (isViewHidden) {
                binding.saveButton.show();
                binding.predictionText.setVisibility(View.VISIBLE);
                isViewHidden = false;
            } else {
                binding.saveButton.hide();
                binding.predictionText.setVisibility(View.GONE);
                isViewHidden = true;
            }
        });

        //load image into the imageview
        Glide.with(requireContext()).load(imageFile)
                .apply(new RequestOptions()
                        .fitCenter()
                        .format(DecodeFormat.PREFER_ARGB_8888)
                        .override(Target.SIZE_ORIGINAL))
                .thumbnail(0.2f)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(binding.image);

        //display the prediction info into the prediction textview
        if(prediction == null || prediction.isEmpty()) binding.predictionText.setVisibility(View.GONE);
        else binding.predictionText.setText(prediction);

        //open save dialog when save button is pressed
        binding.saveButton.setOnClickListener(onClick -> displaySaveDialog());
    }

    /***
     * Method displays a dialog in which users must input a classification label and select a dataset
     * to save the image to.
     */
    public void displaySaveDialog() {
        //inflate the layout of the dialog & initialise the layout views
        final View layout = View.inflate(requireContext(), R.layout.save_image_dialog, null);
        final CheckBox checkbox = layout.findViewById(R.id.checkboxUsePredicted);
        final EditText labelEditText = layout.findViewById(R.id.classificationLabel);
        final Spinner spinner = layout.findViewById(R.id.datasetSpinner);

        if(predictedLabel == null || predictedLabel.isEmpty()) checkbox.setVisibility(View.GONE);

        //set on check changed listener & populate the spinner with dataset names
        checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> checkBoxChanged(isChecked, labelEditText));
        populateDatasetSpinner(spinner);

        //display the dialog
        new SweetAlertDialog(requireContext(), SweetAlertDialog.NORMAL_TYPE)
                .setConfirmText("Save Image")
                .setCustomView(layout)
                .setConfirmClickListener(dialog -> saveImage(labelEditText, spinner.getSelectedItem().toString(), dialog))
                .show();
    }

    /***
     * Method to save an image to a particular dataset
     * @param labelEditText the edittext which contains the classification label text
     * @param datasetName   the name of the selected dataset to store the image into
     * @param dialog        a reference to the dialog
     */
    private void saveImage(EditText labelEditText, String datasetName, SweetAlertDialog dialog) {
        String classificationLabel;

        //if the classification label text is empty then display an error to the user
        if (TextUtils.isEmpty(classificationLabel = labelEditText.getText().toString().trim())) {
            labelEditText.setError("Label cannot be empty");
            return;
        }

        Completable.fromCallable(() -> {
            int datasetPK = dbManager.getDatasetPK(datasetName);
            ImageOperations.uploadImage(dbManager, datasetPK, datasetName, getImageFile(datasetName), classificationLabel);
            requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), imageFile.getName() + " Successfully Saved to " + datasetName, Toast.LENGTH_SHORT).show());
            return true;
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(throwable -> Log.e("TAG", "Error:\n" + throwable.getMessage()))
                .doOnComplete(() ->
                {
                    //close the dialog and return to the camera fragment
                    dialog.dismiss();
                    imageSaved = true;
                    returnToCamera();
                })
                .subscribe();
    }

    /***
     * Method to move the current image file into the UFDL images folder.
     * @param datasetName the name f the dataset in which the image belongs
     * @return the repositioned image file
     */
    public File getImageFile(String datasetName) {
        File destinationFile = new File(DatasetOperations.getImageStorageDirectory(requireContext(), datasetName, false), imageFile.getName());
        try {
            FileUtils.moveFile(imageFile, destinationFile);
            if (destinationFile.exists()) return destinationFile;
        } catch (IOException e) {
            Log.e("TAG", "Failed to move image");
        }
        return imageFile;
    }

    /***
     * Method to return to the camera fragment
     */
    public void returnToCamera() {
        //if the user is returning to the camera fragment without saving the image, then delete the image file
        if (!imageSaved) {
            if (imageFile.delete()) Log.e("TAG", "Image Deleted");
        }

        //navigate back to the camera fragment
        Navigation.findNavController(requireView()).popBackStack();
    }

    /***
     * Method to alter the classification edittext field upon checkbox state change
     * @param isChecked     true if the checkbox is checked
     * @param labelEditText the editext to alter
     */
    private void checkBoxChanged(Boolean isChecked, EditText labelEditText) {

        //if the checkbox is checked, save the current text in the edittext and then set the field with the classifier's predicted label
        if (isChecked) {
            previousLabel = labelEditText.getText().toString().trim();
            labelEditText.setText(predictedLabel);
            labelEditText.setEnabled(false);
        } else {
            labelEditText.setEnabled(true);
            //else, set the edittext with the text which was in the text field prior to the checkbox being checked
            labelEditText.setText(previousLabel);
        }
    }

    /***
     * Method to populate the spinner with a list of dataset names
     * @param spinner the spinner to populate
     */
    private void populateDatasetSpinner(Spinner spinner) {
        ArrayList<ImageDataset> datasets = dbManager.getDatasetList();
        List<String> datasetNames = datasets.stream().map(ImageDataset::getName).collect(Collectors.toList());
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(requireContext(), R.layout.support_simple_spinner_dropdown_item, datasetNames);
        requireActivity().runOnUiThread(() -> spinner.setAdapter(arrayAdapter));
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
    }

    /***
     * Method to change the rotation of the imageview upon orientation changes
     * @param newConfig object which stores device configuration changes
     */
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            if(prediction != null && !prediction.isEmpty()) prediction = prediction.replaceAll(", ", "\n");
            binding.saveButton.setTranslationX(viewOriginalXPosition);
        } else {
            if(prediction != null && !prediction.isEmpty()) prediction = prediction.replaceAll(System.getProperty("line.separator"), ", ");
            binding.saveButton.setTranslationX(viewOriginalXPosition + dipToPixels(235));
        }

        toggleActionBar();
        binding.image.setRotation(0);
        binding.predictionText.setText(prediction);
        binding.image.invalidate();
    }

    public void toggleActionBar() {
        ActionBar supportActionBar = ((MainActivity) requireActivity()).getSupportActionBar();
        if (supportActionBar != null) {
            if (supportActionBar.isShowing()) supportActionBar.hide();
            else supportActionBar.show();
        }
    }

    /***
     * Converts dips to Pixels
     * @param dipValue the dip value to convert
     * @return the number of pixels converted from dips
     */
    public float dipToPixels(float dipValue) {
        DisplayMetrics metrics = requireContext().getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics);
    }
}