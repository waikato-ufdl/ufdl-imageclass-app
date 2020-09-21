package com.example.myapplication.ui.home;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import com.example.myapplication.MainActivity;
import com.example.myapplication.R;
import com.example.myapplication.ui.settings.Utility;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.app.Activity.RESULT_OK;

public class HomeFragment extends Fragment {
    private HomeViewModel homeViewModel;
    private ImageView capturedImage;
    private Button captureButton;
    private static final int CAMERA_REQUEST_CODE = 1;
    private String currentPhotoPath;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //pass context to to Utility class & set the theme
        Utility.setContext(getContext());
        getContext().setTheme(Utility.getTheme());
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                ViewModelProviders.of(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        /*
        final TextView textView = root.findViewById(R.id.text_home);
        homeViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });
         */

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //initialise the view components
        capturedImage = (ImageView) view.findViewById(R.id.cameraImage);
        captureButton = (Button) view.findViewById(R.id.captureImageButton);

        //set an on click listener to the button which will start the camera intent upon user press
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //check camera permission
                if(((MainActivity) getActivity()).checkPermissions(Utility.PERMISSIONS[2]))
                {
                    //start the camera intent
                    captureImageIntent();
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //if a photo has been taken
        if(requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK)
        {
                //create a new file and set the image view picture using the URI
                //File file = new File(currentPhotoPath);
                //capturedImage.setImageURI(Uri.fromFile(file));

                displayScaledImage();
        }
    }

    /**
     * A method to create and return an image file
     * @return
     * @throws IOException
     */
    private File createImageFile() throws IOException {
        // Create an image file name using timestamp
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        //get the storage directory where we want to store the image file
        File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        //create image file
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: get the absolute path of where image is saved
        //using this path we can display image to imageview or upload to server
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    /**
     * A method to start the camera intent
     */
    private void captureImageIntent() {
        //create a camera intent to take a picture using the default camera
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // check there is a camera to handle the intent
        if (cameraIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            // Create the File where the photo should go
            File pictureFile = null;
            try {
                pictureFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File...
            }
            // if the image File was successfully created
            if (pictureFile != null) {

                //create a photo URI and pass it to the camera intent
                Uri photoURI = FileProvider.getUriForFile(getContext(),
                        "com.example.android.fileprovider",
                        pictureFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

                //start the camera intent
                startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
            }
        }
    }

    /**
     * A method to scale down an image for to display only what is necessary (so that too much RAM/memory is not consumed)
     * The phone just needs to display an image scaled to the size of the image view and nothing more.
     */
    private void displayScaledImage()
    {
        capturedImage.post(() -> {
            //the dimensions of the image view which displays the image to the screen
            int imageViewWidth = capturedImage.getWidth();
            int imageViewHeight = capturedImage.getHeight();

            //need to do a dummy read to get the dimensions of the full image
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(currentPhotoPath, bmOptions);

            //the dimensions of the full sized image saved to storage
            int fullImageWidth = bmOptions.outWidth;
            int fullImageHeight = bmOptions.outHeight;

            //calculate the scale factor and feed it into inSample size
            //which will look at the scale differences between what we have (full image) and what we want (scaled image)
            //and return a smaller image if applicable in order to load into image view
            int scaleFactor = Math.min(fullImageWidth/imageViewWidth, fullImageHeight/imageViewHeight);
            bmOptions.inSampleSize = scaleFactor;
            bmOptions.inJustDecodeBounds = false;

            rotateImage((BitmapFactory.decodeFile(currentPhotoPath, bmOptions)));
        });
    }


    /**
     * A method to rotate an image back to its original orientation if it has been rotated by the camera intent
     * @param bitmap
     */
    private void rotateImage(Bitmap bitmap)
    {
        ExifInterface exifInterface = null;

        try {
            //saved images have exif data which contains the orientation of the image, retrieve exifInterface
            exifInterface = new ExifInterface(currentPhotoPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //get the orientation of the image
        int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

        //create matrix for the orientation
        Matrix matrix = new Matrix();

        //switch case for rotation purposes
        switch (orientation)
        {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;

            case ExifInterface.ORIENTATION_ROTATE_180:;
                matrix.setRotate(180);
                break;

            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(270);
                break;

            default:
        }

        //create rotated bitmap using dimensions of original bitmap and the matrix
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        capturedImage.setImageBitmap(rotatedBitmap);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("photoPath", currentPhotoPath);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        if(savedInstanceState != null) {
            try {
                currentPhotoPath = savedInstanceState.getString("photoPath");

                if(currentPhotoPath != null) {
                    displayScaledImage();
                }
            }
            catch (Exception ex){
            }
        }
    }
}