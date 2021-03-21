package io.github.waikato_ufdl.ui.images;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;

import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import io.github.waikato_ufdl.DBManager;
import io.github.waikato_ufdl.SessionManager;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public abstract class NetworkTask {
    protected int datasetPK;
    protected String datasetName;
    protected Context context;
    protected String processingMessage;
    protected String completedMessage;
    protected int progressIndex;
    protected DBManager dbManager;
    protected SweetAlertDialog loadingDialog;
    protected int totalImages;

    /**
     * The constructor for generating a Network Task
     *
     * @param context     the context
     * @param datasetName the name of the dataset
     */
    protected NetworkTask(Context context, String datasetName) {
        this.datasetName = datasetName;
        this.context = context;
        this.dbManager = new SessionManager(context).getDbManager();
        this.datasetPK = dbManager.getDatasetPK(datasetName);
        this.loadingDialog = new SweetAlertDialog(context, SweetAlertDialog.PROGRESS_TYPE);
    }

    /***
     * This method will show a progress dialog whilst performing some background task
     * @param images a list of either classifiedImage objects or a list of Uri objects depending on the task being performed
     */
    public void run(List<?> images) {
        totalImages = images.size();
        progressIndex = 1;

        Observable.fromCallable(() -> (images))
                .flatMap(selectedImages -> Observable.fromIterable(images))
                .map(image -> {
                    try {
                        //perform a task on the background thread
                        backgroundTask(image);
                    } catch (Exception e) {
                        Log.e("TAG", "Network Request Failed: only local changes will be made. " + e.getMessage());
                    }
                    return image;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorResumeNext(error -> Observable.empty())
                .subscribeOn(Schedulers.io())
                .subscribe(new io.reactivex.rxjava3.core.Observer<Object>() {

                    /** display a progress dialog */
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        runOnSubscribe(d);
                    }

                    /** Update the progress dialog upon each item processed*/
                    @Override
                    public void onNext(@NonNull Object o) {
                        runOnNext();
                    }

                    /** display an error dialog informing the user that something went wrong*/
                    @Override
                    public void onError(@NonNull Throwable e) {
                        Log.e("TAG", e.getMessage());
                    }

                    /** display a success dialog once the task has finished */
                    @Override
                    public void onComplete() {

                        //show a success popup
                        loadingDialog
                                .setTitleText("Success!")
                                .setContentText(completedMessage)
                                .showCancelButton(false)
                                .setConfirmText("OK")
                                .setConfirmClickListener(sweetAlertDialog -> {
                                    sweetAlertDialog.dismissWithAnimation();
                                    runOnCompletion();
                                })
                                .changeAlertType(SweetAlertDialog.SUCCESS_TYPE);
                    }
                });
    }

    /***
     * an abstract method to define the network task which should occur on a background thread
     * @param image either a ClassifiedImage object or URI object
     * @throws Exception if API request fails
     */
    public abstract void backgroundTask(Object image) throws Exception;

    /**
     * Executes the run method to begin the Network Task after providing it a list of objects such as classifiedImages, URIs or strings depending on the task.
     */
    public abstract void execute() throws Exception;

    /**
     * Method which runs once the network task has successfully completed. It will mainly be used to update the UI or end the action mode.
     */
    public void runOnCompletion() {
    }

    /***
     * Called when starting a network task. Setup and display a loading dialog by default once a task begins to run.
     * @param disposable the disposable resource
     */
    public void runOnSubscribe(Disposable disposable) {
        loadingDialog.getProgressHelper().stopSpinning();
        loadingDialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
        loadingDialog.setContentText(processingMessage + progressIndex + " of " + totalImages);
        loadingDialog.setCancelText("Cancel");
        loadingDialog.setCanceledOnTouchOutside(false);
        loadingDialog.setCancelClickListener(view -> {
            disposable.dispose();
            view.dismiss();
            runOnCompletion();
        });
        loadingDialog.show();
    }

    /***
     * Called upon completion of each background task. Updates the loading dialog as progress is made by default.
     */
    public void runOnNext() {
        progressIndex++;
        loadingDialog.setContentText(processingMessage + progressIndex + " of " + totalImages);
        loadingDialog.getProgressHelper().setProgress(((float) progressIndex) / totalImages);
    }
}
