package io.github.waikato_ufdl.ui.camera;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.otaliastudios.cameraview.CameraException;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.controls.Flash;
import com.otaliastudios.cameraview.controls.Hdr;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import io.github.waikato_ufdl.Classifier;
import io.github.waikato_ufdl.MainActivity;
import io.github.waikato_ufdl.Prediction;
import io.github.waikato_ufdl.R;
import io.github.waikato_ufdl.SessionManager;
import io.github.waikato_ufdl.databinding.FragmentCameraBinding;

public class cameraFragment extends Fragment implements AdapterView.OnItemSelectedListener {
    private PredictionListViewModel predictionListViewModel;
    private PredictionAdapter predictionAdapter;
    private Classifier imageClassifier;
    private FragmentCameraBinding binding;
    private boolean IMAGE_ANALYZER_ENABLED = false;
    private Spinner modelSpinner, frameworkSpinner;
    private ArrayList<String> tfliteModels, pyTorchModels;
    private ArrayAdapter<String> arrayAdapter;
    private Tuple framework, model;
    private final String FRAMEWORK_PYTORCH = "PyTorch Mobile";

    public cameraFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //inflate the layout for this fragment
        binding = FragmentCameraBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SessionManager sessionManager = new SessionManager(requireContext());
        if (!sessionManager.isLoggedIn())
            Navigation.findNavController(requireView()).navigate(R.id.action_nav_home_to_settingsFragment);

        //set the orientation to portrait and change the colour of the navigation bar
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        requireActivity().getWindow().setNavigationBarColor(Color.parseColor("#202020"));

        //initialise recyclerview and view model
        predictionListViewModel = new PredictionListViewModel();
        predictionAdapter = new PredictionAdapter(Prediction.itemCallback);
        binding.predictionRecyclerView.setAdapter(predictionAdapter);
        binding.predictionRecyclerView.setItemAnimator(null);

        //set the button click listeners
        setButtonClickListeners();

        new Thread(this::createModelListsFromAssets).start();
        startCamera();
    }

    /***
     * method to start the camera
     */
    public void startCamera() {
        binding.camera.addCameraListener(new CameraListener() {

            /**
             * Method which runs when the camera opens
             * @param options the camera options
             */
            @Override
            public void onCameraOpened(@NonNull CameraOptions options) {
                super.onCameraOpened(options);

                //check which camera features are available & hide the support action bar
                checkFeaturesAvailable(options);
                Log.e("TAG", "Camera Started");
                hideActionBar();
            }

            /**
             * Method to capture an image and then display it in a preview fragment
             * @param result the picture result containing the captured picture
             */
            @Override
            public void onPictureTaken(@NonNull PictureResult result) {
                super.onPictureTaken(result);

                result.toBitmap(3000, 3000, bitmap -> {
                    Bundle bundle = new Bundle();
                    if (bitmap != null) {
                        Prediction prediction = imageClassifier.predict(bitmap);

                        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date());
                        String fileName = "IMG_" + timeStamp + "." + binding.camera.getPictureFormat().toString().toLowerCase();
                        String imagePath = ImageUtils.createImageFile(requireContext(), bitmap, fileName);
                        bundle.putString("imagePath", imagePath);
                        bundle.putString("predictionInfo", prediction.toString());
                        bundle.putString("predictedLabel", prediction.getLabel());
                        Navigation.findNavController(requireView()).navigate(R.id.action_nav_home_to_previewImage, bundle);
                    }
                });

                Toast.makeText(requireContext(), "Capturing Image....", Toast.LENGTH_SHORT).show();
            }

            /**
             * Method to update the zoom text value
             * @param newValue the new zoom value
             */
            @SuppressLint("SetTextI18n")
            @Override
            public void onZoomChanged(float newValue, @NonNull float[] bounds, @Nullable PointF[] fingers) {
                super.onZoomChanged(newValue, bounds, fingers);
                binding.zoomValue.setText(newValue + " ");
            }

            /**
             * method which is called when a camera exception is thrown
             * @param exception is thrown on camera failure
             */
            @Override
            public void onCameraError(@NonNull CameraException exception) {
                super.onCameraError(exception);
                startCamera();
            }
        });
    }

    /***
     * Method to hide the default action bar
     */
    public void hideActionBar() {
        ActionBar supportActionBar = ((MainActivity) requireActivity()).getSupportActionBar();
        if (supportActionBar != null) supportActionBar.hide();
    }

    /***
     * Check if the mobile device supports Flash and HDR mode. Only display the camera features that
     * are available to the user.
     * @param options the camera options
     */
    public void checkFeaturesAvailable(CameraOptions options) {

        //if the device supports Flash, then enable the flash button button and leave the icon visible to the user
        if (options.supports(Flash.ON)) {
            binding.flash.setEnabled(true);
            binding.flash.setVisibility(View.VISIBLE);
        } else {
            //disable the flash button and hide it from the user
            binding.flash.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_flash_off_24));
            binding.flash.setEnabled(false);
            binding.flash.setVisibility(View.GONE);
        }

        //if the device supports HDR mode then enable the HDR button and leave the icon visibile to the user
        if (options.supports(Hdr.ON)) {
            binding.HDR.setEnabled(true);
            binding.HDR.setVisibility(View.VISIBLE);
        } else {
            //disable and hide the HDR button
            binding.HDR.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_hdr_off_24));
            binding.HDR.setEnabled(false);
            binding.HDR.setVisibility(View.GONE);
        }
    }

    /***
     * Toggle the flash mode state and change the flash icon depending on the state
     */
    public void toggleFlash() {
        Flash currentZoomSetting = binding.camera.getFlash();

        if (currentZoomSetting == Flash.ON) {
            binding.camera.setFlash(Flash.OFF);
            binding.flash.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_flash_off_24));
        } else {
            binding.camera.setFlash(Flash.ON);
            binding.flash.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_flash_on_24));
        }
    }

    /***
     * Toggle the HDR mode state and change the HDR icon that is visible to the user depending on the state
     */
    public void toggleHDR() {
        Hdr currentZoomSetting = binding.camera.getHdr();

        if (currentZoomSetting == Hdr.OFF) {
            binding.camera.setHdr(Hdr.ON);
            binding.HDR.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_hdr_on_24));
        } else {
            binding.camera.setHdr(Hdr.OFF);
            binding.HDR.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_hdr_off_24));
        }
    }


    /***
     * Changes between the front facing camera and the back camera
     */
    public void toggleCameraFace() {
        binding.camera.toggleFacing();
    }


    /***
     * converts an image object into a bitmap
     * @param image the image to convert
     * @return a bitmap of the image
     */
    private Bitmap toBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 100, out);

        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    /**
     * Method to set an observer on the predictions list and update the prediction adapter
     */
    private void startPredictionRecycler() {
        predictionListViewModel.predictionList.observe(getViewLifecycleOwner(), predictions -> predictionAdapter.submitList(predictions));
    }

    /***
     * Clear the frame processor and remove prediction list observers
     */
    private void removeImageAnalyzer() {
        binding.camera.clearFrameProcessors();
        predictionAdapter.submitList(null);
        predictionListViewModel.predictionList.removeObservers(getViewLifecycleOwner());
    }

    /***
     * set the button click listeners for all camera buttons and features
     */
    public void setButtonClickListeners() {
        binding.capturePicture.setOnClickListener(v -> captureImage());
        binding.flash.setOnClickListener(v -> toggleFlash());
        binding.HDR.setOnClickListener(v -> toggleHDR());
        binding.flipCamera.setOnClickListener(v -> toggleCameraFace());
        binding.navButton.setOnClickListener(v -> ((MainActivity) requireActivity()).toggleDrawer());
        binding.analyzer.setOnClickListener(v -> toggleImageAnalyzer());
        binding.settings.setOnClickListener(v -> displayModelSettings());
    }

    /***
     * initialise the bottom sheet and display the framework and model spinners
     */
    private void displayModelSettings() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        bottomSheetDialog.setContentView(R.layout.camera_bottom_sheet);
        modelSpinner = bottomSheetDialog.findViewById(R.id.model_spinner);
        frameworkSpinner = bottomSheetDialog.findViewById(R.id.framework_spinner);

        //display the available models depending on the selected framework
        updateModelSpinnerEntries(framework.value);
        modelSpinner.setOnItemSelectedListener(this);
        frameworkSpinner.setOnItemSelectedListener(this);
        bottomSheetDialog.show();

        Object selectedItem;
        //if the framework hasn't been selected by the user then set the default framework
        if (framework == null && (selectedItem = frameworkSpinner.getSelectedItem()) != null) {
            framework = new Tuple(frameworkSpinner.getSelectedItemPosition(), selectedItem.toString());
        } else {
            //else set the framework to the previously selected framework
            frameworkSpinner.setSelection(framework.key);
        }

        if (model == null && (selectedItem = modelSpinner.getSelectedItem()) != null) {
            model = new Tuple(modelSpinner.getSelectedItemPosition(), selectedItem.toString());
        } else {
            modelSpinner.setSelection(model.key);
        }
    }

    /**
     * Method to update the model spinner entries to display the models associated to the framework selected
     *
     * @param framework the selected framework (Pytorch Mobile or TensorFlow Lite)
     */
    public void updateModelSpinnerEntries(String framework) {
        ArrayList<String> displayModels = framework.equals(FRAMEWORK_PYTORCH) ? pyTorchModels : tfliteModels;
        arrayAdapter = new ArrayAdapter<>(requireContext(), R.layout.support_simple_spinner_dropdown_item, displayModels);
        requireActivity().runOnUiThread(() -> modelSpinner.setAdapter(arrayAdapter));
    }

    /**
     * Load the available model names from the assets folder and store them into the appropriate model list depending on the model extension
     */
    public void createModelListsFromAssets() {
        pyTorchModels = new ArrayList<>();
        tfliteModels = new ArrayList<>();

        AssetManager assetManager = requireContext().getAssets();
        try {
            for (String modelName : assetManager.list("")) {
                if (modelName.endsWith(".pt")) {
                    pyTorchModels.add(modelName);
                } else if (modelName.endsWith(".tflite")) {
                    tfliteModels.add(modelName);
                }
            }

            if (model == null && framework == null) {
                framework = new Tuple(0, FRAMEWORK_PYTORCH);
                model = new Tuple(0, pyTorchModels.get(0));
            }

            ClassifierDetails details = ClassifierUtils.deserializeModelJSON(requireContext(), model.value);
            if (details != null) {
                imageClassifier = Classifier.createInstance(requireContext(), details);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to toggle the image analyzer on/off
     */
    private void toggleImageAnalyzer() {
        if (binding.camera.isOpened()) {
            int buttonTint = R.color.white;
            IMAGE_ANALYZER_ENABLED = !IMAGE_ANALYZER_ENABLED;
            if (IMAGE_ANALYZER_ENABLED) {
                buttonTint = R.color.cameraFeatureEnabledYellow;
                startPredictionRecycler();
                setCameraFrameProcessor();
            } else {
                removeImageAnalyzer();
            }
            binding.analyzer.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), buttonTint));
        }
    }

    /**
     * Method to add a frame processor to the camera to perform image analysis
     */
    private void setCameraFrameProcessor() {
        binding.camera.addFrameProcessor(frame -> {
            Bitmap bitmap = null;

            if (frame.getDataClass() == byte[].class) {
                byte[] data = frame.getData();
                bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            } else if (frame.getDataClass() == Image.class) {
                Image data = frame.getData();
                bitmap = toBitmap(data);
            }

            if (bitmap != null) {
                predictionListViewModel.updateData(imageClassifier.topKPredictions(bitmap));
            }
        });
    }

    /**
     * Method to take a picture if the camera is not already doing so
     */
    private void captureImage() {
        if (binding.camera.isTakingPicture()) return;
        binding.camera.takePicture();
    }


    /**
     * hide the support action bar if it isn't null
     */
    @Override
    public void onStart() {
        super.onStart();
        Log.e("TAG", "Started");
        hideActionBar();
    }

    /***
     * show the support action bar when stopping the camera fragment
     */
    @Override
    public void onStop() {
        super.onStop();
        ActionBar supportActionBar = ((MainActivity) requireActivity()).getSupportActionBar();
        if (supportActionBar != null) supportActionBar.show();
    }

    /***
     * Open the camera on fragment resume
     */
    @Override
    public void onResume() {
        super.onResume();
        new Thread(() -> binding.camera.open()).start();
    }

    /***
     * Close the camera when fragment is paused
     */
    @Override
    public void onPause() {
        super.onPause();
        new Thread(() -> binding.camera.close()).start();
    }

    /***
     * Destroy camera & set binding to null when view is destroyed
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.camera.clearFrameProcessors();
        binding.camera.clearCameraListeners();
        predictionAdapter = null;
        predictionListViewModel = null;

        new Thread(() ->
        {
            binding.camera.destroy();
            binding = null;
        }).start();

        Log.e("TAG", "Destroyed");
    }

    /***
     * Callback method to be invoked when a spinner item has been selected
     * @param parent the spinner
     * @param view the view within the spinner that was clicked
     * @param position the index position of the selected item view
     * @param id the row id of the selected item
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent.getItemAtPosition(position) == null) return;

        new Thread(() ->
        {
            if (parent == frameworkSpinner) {
                String selectedFramework = parent.getItemAtPosition(position).toString();

                if (!framework.value.equals(selectedFramework)) {
                    Log.e("TAG", "Changed Framework");
                    framework.setKey(position);
                    framework.setValue(selectedFramework);
                    updateModelSpinnerEntries(framework.value);
                }
            }

            if (parent == modelSpinner) {
                Log.e("TAG", "Model Spinner Updated");
                model.setKey(position);
                model.setValue(parent.getItemAtPosition(position).toString());

                ClassifierDetails details = ClassifierUtils.deserializeModelJSON(requireContext(), model.value);

                if (details != null)
                    imageClassifier = Classifier.createInstance(requireContext(), details);
            }
        }).start();
    }

    /***
     * callback method to be invoked when the selection disappears from the spinner
     * @param parent the spinner that now contains no selected item
     */
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }


    /**
     * Method to remove the file extension from a filename string
     *
     * @param filename            the original filename
     * @param removeAllExtensions true - to remove all extensions in the filename string
     * @return filename without any extension
     */
    public static String removeFileExtension(String filename, boolean removeAllExtensions) {
        if (filename == null || filename.isEmpty()) {
            return filename;
        }

        String extPattern = "(?<!^)[.]" + (removeAllExtensions ? ".*" : "[^.]*$");
        return filename.replaceAll(extPattern, "");
    }


    /**
     * Tuple class to store key-value pairs. In this case, the key will be the index value of the selected model in the selector and the string will represent the selected model name.
     */
    public static class Tuple {
        private int key;
        private String value;

        /**
         * Constructor for tuple objects
         *
         * @param key   the integer key - selected model index
         * @param value the string value - the selected model name
         */
        public Tuple(int key, String value) {
            this.key = key;
            this.value = value;
        }

        /**
         * setter method to change the selected index
         *
         * @param key the index position of a selected model
         */
        public void setKey(int key) {
            this.key = key;
        }

        /**
         * Setter method to change the selected model name
         *
         * @param value the selected model's name
         */
        public void setValue(String value) {
            this.value = value;
        }
    }
}