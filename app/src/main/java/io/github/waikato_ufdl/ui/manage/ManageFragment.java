package io.github.waikato_ufdl.ui.manage;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.pedant.SweetAlert.SweetAlertDialog;
import io.github.waikato_ufdl.DBManager;
import io.github.waikato_ufdl.DatasetOperations;
import io.github.waikato_ufdl.NetworkConnectivityMonitor;
import io.github.waikato_ufdl.R;
import io.github.waikato_ufdl.SessionManager;
import io.github.waikato_ufdl.databinding.FragmentManageBinding;
import io.github.waikato_ufdl.databinding.NewDatasetBinding;

public class ManageFragment extends Fragment {
    private NetworkConnectivityMonitor networkMonitor;
    private FragmentManageBinding binding;
    private NewDatasetBinding newDatasetBinding;
    private DatasetViewModel datasetViewModel;
    private DatasetListAdapter adapter;
    private SessionManager sessionManager;
    private DBManager dbManager;
    private ArrayAdapter<String> spinnerArrayAdapterLicenses, spinnerArrayAdapterPojects;
    private String datasetName, oldDatasetName;
    private int datasetPK;
    private ActionMode actionMode;
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    public ManageFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(requireContext());
        requireContext().setTheme(sessionManager.getTheme());
        networkMonitor = new NetworkConnectivityMonitor(requireContext());

        //monitor the connectivity state
        networkMonitor.observe(this, isConnected ->
        {
            if (actionMode != null) {
                //disable & hide the delete & copy action menu items depending on the connectivity mode
                showActionBarOnlineModeOptions(isConnected);
            }
        });

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.context_menu_search, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setImeOptions(EditorInfo.IME_ACTION_DONE);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            /***
             * Called when the user submits the query
             * @param query The query text that is to be submitted
             * @return true if the query has been handled by the listener. False to let the SearchView perform the default action.
             */
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            /***
             * Called when the query text is changed by the user.
             * @param newText the new content of the query text field.
             * @return false if the SearchView should perform the default action of showing any suggestions if available, true if the action was handled by the listener.
             */
            @Override
            public boolean onQueryTextChange(String newText) {
                //filter the images to check for classified images with a specific label
                filter(newText);
                return false;
            }
        });

        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {

            /***
             * Called when a menu item with SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW is expanded
             * @param item Item that was expanded
             * @return true if the item should expand, false if expansion should be suppressed.
             */
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            /***
             * Called when a menu item with SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW is expanded
             * @param item Item that was collapsed
             * @return true if the item should collapse, false if collapsing should be suppressed.
             */
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                if (datasetViewModel != null && datasetViewModel.getDatasetList() != null)
                    adapter.submitList(datasetViewModel.getDatasetList().getValue());
                return true;
            }
        });
    }

    /***
     * A method to create the filtered dataset list using a keyword to filter by dataset name
     * @param keyword the keyword to filter the list by
     */
    private void filter(String keyword) {
        if (datasetViewModel == null) return;
        if (datasetViewModel.getDatasetList().getValue() == null) return;
        ArrayList<ImageDataset> filteredSearchList = new ArrayList<>();

        datasetViewModel.getDatasetList().getValue().forEach(dataset -> {
            if (dataset.getName().toLowerCase().contains(keyword.toLowerCase())) {
                filteredSearchList.add(dataset);
            }
        });

        //display the filtered list
        adapter.submitList(filteredSearchList);
    }


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentManageBinding.inflate(inflater, container, false);

        binding.buttonCreateDataset.setOnClickListener(view -> {
            //if actionMode is on, turn it off
            if (actionMode != null) {
                actionMode.finish();
            }
            //then proceed to initiate the dataset creation window
            initiateDatasetWindow(true);
        });

        dbManager = sessionManager.getDbManager();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView mRecyclerView = view.findViewById(R.id.datasetRecyclerView);
        adapter = new DatasetListAdapter(ImageDataset.datasetItemCallback, sessionManager);
        RecyclerView.LayoutManager lm = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(lm);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(adapter);
        checkSettings();
    }

    /***
     * Toggles the availability of the download and copy dataset features.
     * @param state true to enable features, false to disable.
     */
    public void showActionBarOnlineModeOptions(boolean state) {
        actionMode.getMenu().findItem(R.id.action_download_dataset).setVisible(state);
        actionMode.getMenu().findItem(R.id.action_download_dataset).setEnabled(state);

        actionMode.getMenu().findItem(R.id.action_copy_dataset).setVisible(state);
        actionMode.getMenu().findItem(R.id.action_copy_dataset).setEnabled(state);

        actionMode.invalidate();
    }

    /***
     * Method to check whether a user is logged in, if so, display datasets. Else, navigate back to settings.
     */
    public void checkSettings() {
        if (!sessionManager.isLoggedIn()) {
            Navigation.findNavController(requireView()).navigate(R.id.action_nav_gallery_to_settingsFragment);
        } else {
            initialiseDatasetViewModel();
        }
    }

    /***
     * initialises the dataset view model and sets an observer on the dataset list within the view model.
     * When the dataset list changes, the changed list will be submitted to the adapter in order to display
     * to the recycler view.
     */
    private void initialiseDatasetViewModel() {
        datasetViewModel = new ViewModelProvider(this).get(DatasetViewModel.class);
        datasetViewModel.getDatasetList().observe(getViewLifecycleOwner(), datasetList -> {
            adapter.submitList(datasetList);
            setRecyclerViewListener(adapter.getCurrentList());
            setRecyclerViewListener(datasetList);
        });

        updateUI();
    }

    /***
     * Set the click listeners for the recycler view
     * @param datasetList the dataset list
     */
    private void setRecyclerViewListener(List<ImageDataset> datasetList) {
        adapter.setListener(new DatasetListAdapter.RecyclerViewClickListener() {

            /***
             * Called when a user clicks on a dataset.
             * @param view the view that was clicked
             * @param position the index position of the dataset that was clicked
             */
            @Override
            public void onClick(View view, int position) {
                //if the user is not in action mode, move to the images fragment to display dataset contents.
                ImageDataset dataset = datasetList.get(position);
                selectDataset(position);

                if (actionMode == null) {
                    Bundle bundle = new Bundle();
                    bundle.putInt("datasetPK", dataset.getPK());
                    bundle.putString("datasetName", dataset.getName());
                    Navigation.findNavController(view).navigate(R.id.action_nav_gallery_to_imagesFragment, bundle);
                } else {
                    datasetPK = dataset.getPK();
                    datasetName = dataset.getName();
                }
            }

            /***
             * Called when a dataset is long pressed. Opens the action mode menu to display dataset operations.
             * @param view the view that was clicked
             * @param position the index position of the dataset that was clicked
             */
            @Override
            public void onLongClick(View view, int position) {
                ImageDataset dataset = datasetList.get(position);
                datasetPK = dataset.getPK();
                datasetName = dataset.getName();
                selectDataset(position);

                //if action mode is off, then start it upon long press
                if (actionMode == null) {
                    actionMode = requireActivity().startActionMode(actionModeCallback);
                }
            }
        });
    }

    /***
     * Method to create and return the layout for the download confirmation dialog.
     * @return layout for the download confirmation dialog.
     */
    @SuppressLint("SetTextI18n")
    public LinearLayout getDownloadConfirmationDialogLayout() {
        //create custom linear layout
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setHorizontalGravity(Gravity.CENTER_HORIZONTAL);

        //create views & set the view content
        CheckBox downloadCache = new CheckBox(getContext());
        downloadCache.setText("Download Full Size Images");

        TextView note = new TextView(getContext());
        note.setText("Note: If the checkbox above is not checked, only compressed images will be cached.");

        //add the views to the layout
        layout.addView(downloadCache);
        layout.addView(note);

        //set margins for each of the child views in the layout
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(5, 10, 5, 35);

        layout.getChildAt(0).setLayoutParams(params);
        layout.getChildAt(0).setPadding(20, 0, 0, 0);
        layout.getChildAt(1).setLayoutParams(params);
        layout.getChildAt(1).setPadding(20, 0, 0, 0);

        return layout;
    }

    /***
     * displays confirmation dialog for downloading datasets.
     */
    public void confirmDatasetDownload() {
        LinearLayout layout = getDownloadConfirmationDialogLayout();
        CheckBox fullSizeDownload = (CheckBox) layout.getChildAt(0);

        //if the user cancels deletion close the popup but leave them on the selection mode
        new SweetAlertDialog(requireContext(), SweetAlertDialog.WARNING_TYPE)
                .setCustomView(layout)
                .setTitleText("Confirm Download")
                .setConfirmText("Download")
                .setConfirmClickListener(sDialog -> {
                    ImageDataset selectedDataset = adapter.getSelectedDataset();
                    actionMode.finish();
                    sDialog.dismiss();
                    boolean isCache = !fullSizeDownload.isChecked();
                    executor.execute(() -> DatasetOperations.downloadDataset(requireContext(), dbManager, selectedDataset, isCache, binding.progressBar, binding.cancelDownload));
                })
                .setCancelButton("Cancel", SweetAlertDialog::dismiss)
                .show();
    }

    /**
     * Initialises & populates the project & license spinners used in the dataset creation & update popup window
     */
    public void initialiseSpinners() {
        //get licenses & projects using database manager
        final List<String> spinnerArrayLicenses = dbManager.getLicenses();
        final List<String> spinnerArrayProjects = dbManager.getProjects();

        spinnerArrayAdapterLicenses = new ArrayAdapter<>(getContext(), R.layout.spinner_item, spinnerArrayLicenses);
        spinnerArrayAdapterLicenses.setDropDownViewResource(R.layout.spinner_item);

        spinnerArrayAdapterPojects = new ArrayAdapter<>(getContext(), R.layout.spinner_item, spinnerArrayProjects);
        spinnerArrayAdapterPojects.setDropDownViewResource(R.layout.spinner_item);

        newDatasetBinding.datasetLicenseSpinner.setAdapter(spinnerArrayAdapterLicenses);
        newDatasetBinding.datasetProjectSpinner.setAdapter(spinnerArrayAdapterPojects);
    }


    /***
     * Method to display a popup window for dataset creation/update.
     * @param isDatasetCreation true if the popup is to create a dataset. False, if popup is for updating a dataset.
     */
    public void initiateDatasetWindow(boolean isDatasetCreation) {
        try {
            newDatasetBinding = NewDatasetBinding.inflate(getLayoutInflater());
            View layout = newDatasetBinding.getRoot();

            // create a PopupWindow
            final PopupWindow popupWindow = new PopupWindow(layout,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT, true);

            // display the popup in the center
            popupWindow.showAtLocation(requireView(), Gravity.CENTER, 0, 0);

            //darken the background behind the popup window
            darkenBackground(popupWindow);
            initialiseSpinners();

            //we are editing a dataset so first display the current details of the data
            if (!isDatasetCreation) {
                setEditDatasetFields();
                requireView().invalidate();
            }

            newDatasetBinding.createDatasetButton.setOnClickListener(view -> createOrUpdateDataset(isDatasetCreation, popupWindow));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /***
     * Executes either a create/update dataset operation.
     * @param isDatasetCreation boolean indicating the operation to perform. True to create a new dataset. False, if an existing dataset should be updated.
     * @param popupWindow the popup window being displayed
     */
    public void createOrUpdateDataset(boolean isDatasetCreation, PopupWindow popupWindow) {
        //check that all required fields have been filled
        if (datasetNameIsUnique(newDatasetBinding.datasetNameEditText, isDatasetCreation)) {
            String name = newDatasetBinding.datasetNameEditText.getText().toString().trim();
            String description = newDatasetBinding.datasetDescriptionEditText.getText().toString().trim();
            String tags = newDatasetBinding.datasetTagsEditText.getText().toString().trim();
            boolean isPublic = newDatasetBinding.isPublicSwitch.isChecked();
            int project = dbManager.getProjectKey(newDatasetBinding.datasetProjectSpinner.getSelectedItem().toString());
            int license = dbManager.getLicenseKey(newDatasetBinding.datasetLicenseSpinner.getSelectedItem().toString());

            executor.execute(() ->
            {
                if (isDatasetCreation) {
                    DatasetOperations.createDataset(dbManager, name, description, project, license, isPublic, tags);
                } else {
                    DatasetOperations.updateDataset(dbManager, datasetPK, name, oldDatasetName, description, project, license, isPublic, tags);
                }
                updateUI();
            });

            popupWindow.dismiss();
            if (actionMode != null) {
                actionMode.finish();
            }
        }
    }

    /***
     * Checks if a given dataset name is unique. If it isn't, display an error to the user.
     * @param editText the edit text containing the dataset name
     * @param isDatasetCreation whether the dataset is being created or updated
     * @return true if dataset name is unique, else false if the dataset name is already in use.
     */
    public boolean datasetNameIsUnique(EditText editText, boolean isDatasetCreation) {
        String datasetName = editText.getText().toString().trim();

        //check if the dataset name field is empty and if so, set an error message
        if (TextUtils.isEmpty(datasetName)) {
            editText.setError("Required");
            return false;
        }

        boolean notUnique = dbManager.getDataset(datasetName) != null;
        boolean violation = (isDatasetCreation) ? notUnique : notUnique && !oldDatasetName.equalsIgnoreCase(datasetName);

        if (violation) {
            editText.setError("Dataset with this name already exists");
            return false;
        }

        return true;
    }

    /***
     * Darkens the background when a popup window is displayed
     * @param popupWindow The popup window being displayed
     */
    public static void darkenBackground(PopupWindow popupWindow) {
        //get the popup view & context
        View container = popupWindow.getContentView().getRootView();
        Context context = popupWindow.getContentView().getContext();

        //setup window manager layout parameters to dim the background & update the view
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) container.getLayoutParams();
        layoutParams.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        layoutParams.dimAmount = 0.5f;
        windowManager.updateViewLayout(container, layoutParams);
    }

    /***
     * Shows a popup dialog to confirm the deletion of a dataset
     * @param mode the action mode
     */
    public void deleteConfirmation(ActionMode mode) {
        //if the user cancels deletion close the popup but leave them on the selection mode
        new SweetAlertDialog(requireContext(), SweetAlertDialog.WARNING_TYPE)
                .setTitleText("Are you sure?")
                .setContentText("You won't be able to recover the dataset after deletion")
                .setConfirmText("Delete")
                .setConfirmClickListener(sDialog -> {
                    //if a user accepts the deletion, delete all the selected images
                    deleteDataset();
                    mode.finish();
                    sDialog.dismiss();

                })
                .setCancelButton("Cancel", SweetAlertDialog::dismiss)
                .show();
    }


    /***
     * Method to delete the selected dataset
     */
    public void deleteDataset() {
        ImageDataset selectedDataset = adapter.getSelectedDataset();

        executor.execute(() -> {
            DatasetOperations.deleteDataset(requireContext(), dbManager, selectedDataset.getName());
            updateUI();
        });
    }

    /***
     * Displays a confirmation dialog for making a copy of a dataset.
     * @param mode        the action mode
     * @param datasetName the name to give the copied dataset
     */
    public void confirmCopyDataset(ActionMode mode, String datasetName) {
        final EditText editText = new EditText(getContext());
        editText.setHint("New Dataset Name");
        editText.setGravity(Gravity.CENTER_HORIZONTAL);
        editText.setPadding(50, 25, 50, 50);
        editText.setHorizontallyScrolling(true);

        //if the user clicks cancel close the popup but leave them on the selection mode
        new SweetAlertDialog(requireContext(), SweetAlertDialog.WARNING_TYPE)
                .setTitleText("Copy dataset " + datasetName + " as: ")
                .setConfirmText("Copy")
                .setCustomView(editText)
                .setConfirmClickListener(sweetAlertDialog -> {
                    String newDatasetName = editText.getText().toString().trim();

                    if (datasetNameIsUnique(editText, true)) {
                        copyDataset(newDatasetName);
                        mode.finish();
                        sweetAlertDialog.dismiss();
                    }
                })
                .setCancelButton("Cancel", SweetAlertDialog::dismissWithAnimation)
                .show();
    }

    /***
     * Makes a copy of a dataset.
     * @param newDatasetName the name for the copied dataset
     */
    public void copyDataset(String newDatasetName) {
        if (SessionManager.isOnlineMode) {
            executor.execute(() -> {
                if (DatasetOperations.copyDataset(dbManager, adapter.getSelectedDataset(), newDatasetName)) {
                    updateUI();
                }
            });
        }
    }

    /***
     * Displays the existing details of the selected dataset on the edit dataset dialog.
     */
    @SuppressLint("SetTextI18n")
    public void setEditDatasetFields() {
        ImageDataset selectedDataset = adapter.getSelectedDataset();

        oldDatasetName = selectedDataset.getName();
        newDatasetBinding.datasetNameEditText.setText(selectedDataset.getName());
        newDatasetBinding.datasetDescriptionEditText.setText(selectedDataset.getDescription());
        newDatasetBinding.datasetTagsEditText.setText(selectedDataset.getTags());
        newDatasetBinding.isPublicSwitch.setChecked(selectedDataset.isPublic());
        newDatasetBinding.createDatasetButton.setText("Update Dataset");

        //check the current project and license associated to this dataset
        int project = spinnerArrayAdapterPojects.getPosition(dbManager.getProjectName(selectedDataset.getProject()));
        int license = spinnerArrayAdapterLicenses.getPosition(dbManager.getLicenseName(selectedDataset.getLicense()));

        //set the spinners to reflect the current project and license details
        newDatasetBinding.datasetProjectSpinner.setSelection(project);
        newDatasetBinding.datasetLicenseSpinner.setSelection(license);
    }

    /***
     * Sets the selection focus to a dataset at a particular position.
     * @param position the position of the selected dataset in the recyclerview adapter list or -1 to clear selection.
     */
    private void selectDataset(int position) {
        adapter.setSelectedIndex(position);
    }

    /***
     * Method to update the displayed datasets upon changes to the current dataset list such as insertions, detail updates and deletions.
     */
    private void updateUI() {
        new Handler(Looper.getMainLooper()).post(() -> datasetViewModel.setDatasetList(dbManager.getDatasetList()));
    }

    /***
     * Callback interface for the action mode which configures and handles events raised by
     * the users interaction with the action mode.
     */
    final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {

        /***
         * Called when the action mode is first created
         * @param mode the ActionMode being created
         * @param menu Menu used to populate action buttons
         * @return true if the action mode should be created, false if entering this mode should be aborted
         */
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater menuInflater = mode.getMenuInflater();
            menuInflater.inflate(R.menu.context_menu_datasets, menu);

            //display all icons on the action bar rather than in a dropdown
            for (int i = 0; i < menu.size(); i++) {
                menu.getItem(i).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            }

            actionMode = mode;
            //display only the available dataset operations depending on the connectivity state.
            showActionBarOnlineModeOptions(SessionManager.isOnlineMode);
            return true;
        }

        /***
         * Called to refresh an action mode's action menu whenever it is invalidated.
         * @param mode ActionMode being prepared
         * @param menu Menu used to populate action buttons
         * @return true if the menu or action mode was updated, false otherwise.
         */
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        /***
         * Called to report a user click on an action button.
         * @param mode The current ActionMode
         * @param item The item that was clicked
         * @return true if this callback handled the event, false if the standard MenuItem invocation should continue.
         */
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int id = item.getItemId();

            //check which menu item was clicked
            if (id == R.id.action_relabel_dataset) {//when the user presses the edit icon
                initiateDatasetWindow(false);
            } else if (id == R.id.action_copy_dataset) {//when the user presses the copy icon
                confirmCopyDataset(mode, datasetName);
            } else if (id == R.id.action_delete_dataset) {//when user presses the delete icon
                deleteConfirmation(mode);
            } else if (id == R.id.action_download_dataset) {//when the user presses the download icon
                if (SessionManager.isOnlineMode) {
                    confirmDatasetDownload();
                }
            }

            return false;
        }

        /***
         * Called when an action mode is about to be exited and destroyed.
         * @param mode The current ActionMode being destroyed
         */
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            //remove the action mode reference and clear the dataset selection.
            actionMode = null;
            selectDataset(-1);
        }
    };

    /***
     * Nullify layout bindings and remove the network observer when the fragment view is destroyed
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        newDatasetBinding = null;
        networkMonitor.removeObservers(this);
    }

    /***
     * Shut down the executor service when the fragment is destroyed.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}