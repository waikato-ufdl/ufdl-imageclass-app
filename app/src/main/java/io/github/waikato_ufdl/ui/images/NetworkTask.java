package io.github.waikato_ufdl.ui.images;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.ActionMode;
import com.github.waikatoufdl.ufdl4j.action.ImageClassificationDatasets;
import java.util.List;
import cn.pedant.SweetAlert.SweetAlertDialog;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public abstract class NetworkTask {
    protected int datasetPK;
    protected ImageClassificationDatasets action;
    protected Context context;
    protected ActionMode mode;
    protected String processingMessage;
    protected String completedMessage;
    protected int progressIndex;
    protected ImagesFragment fragment;

    /**
     * The constructor for generating a Network Task
     * @param fragment the ImagesFragment
     * @param context the context
     * @param datasetPK the primary key of the dataset that will be modified
     * @param action the action used to perform operations on ImageClassification datasets
     * @param mode the action mode
     */
    public NetworkTask(ImagesFragment fragment, Context context, int datasetPK, ImageClassificationDatasets action, ActionMode mode) {
        this.fragment = fragment;
        this.datasetPK = datasetPK;
        this.action = action;
        this.context = context;
        this.mode = mode;
    }

    /**
     * This method will show a progress dialog whilst performing some background task
     * @param images a list of either classifiedImage objects or a list of Uri objects depending on the task being performed
     */
    public void run(List images) {
        SweetAlertDialog loadingDialog = new SweetAlertDialog(context, SweetAlertDialog.PROGRESS_TYPE);
        int totalImages = images.size();
        progressIndex = 1;

        Observable.fromCallable(() -> (images))
                .flatMap(selectedImages -> Observable.fromIterable(images))
                .map(image -> {
                    //perform a task on the background thread
                    backgroundTask(image);
                    return image;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new io.reactivex.rxjava3.core.Observer<Object>() {

                    /** display a progress dialog */
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        loadingDialog.getProgressHelper().stopSpinning();
                        loadingDialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
                        loadingDialog.setContentText(processingMessage + progressIndex + " of " + totalImages);
                        loadingDialog.setCancelable(false);
                        loadingDialog.show();
                    }

                    /** Update the progress dialog upon each item processed*/
                    @Override
                    public void onNext(@NonNull Object o) {
                        progressIndex++;
                        loadingDialog.setContentText(processingMessage + progressIndex + " of " + totalImages);
                        loadingDialog.getProgressHelper().setProgress(((float) progressIndex) / totalImages);
                    }

                    /** display an error dialog informing the user that something went wrong*/
                    @Override
                    public void onError(@NonNull Throwable e) {
                        Log.e("TAAAGG", e.getMessage());
                    }

                    /** display a success dialog once the task has finished */
                    @Override
                    public void onComplete() {
                        //show a success popup
                        loadingDialog
                                .setTitleText("Success!")
                                .setContentText(completedMessage)
                                .setConfirmText("OK")
                                .setConfirmClickListener(sweetAlertDialog -> {
                                    //when the user clicks ok, dismiss the popup
                                    sweetAlertDialog.dismissWithAnimation();

                                    runOnCompletion();
                                })
                                .changeAlertType(SweetAlertDialog.SUCCESS_TYPE);
                    }
                });
    }

    /**
     * an abstract method which will contain the networking code to run based on the task
     * @param image either a ClassifiedImage object or URI object
     * @throws Exception
     */
    public abstract void backgroundTask(Object image) throws Exception;

    /**
     * Method which will be used to execute the run method after providing it a list of classifiedImage objects or URI objects
     */
    public abstract void execute();

    /**
     * Method which runs once a task has successfuly completed. It will mainly be used to update the UI or end the action mode.
     */
    public void runOnCompletion()
    {
        //set the datasetModified variable to true
        fragment.setDatasetModified(true);

        //if the recylcer view is showing less images than the page limit, reload the fragment
        if (fragment.getNumImages() < fragment.PAGE_LIMIT) {
            fragment.reload(); }

        if (mode != null) {
            //finish action mode once a user has confirmed the reclassification
            mode.finish();
        }
    }
}
