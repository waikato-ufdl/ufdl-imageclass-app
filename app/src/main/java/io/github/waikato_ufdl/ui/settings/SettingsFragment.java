package io.github.waikato_ufdl.ui.settings;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.github.waikatoufdl.ufdl4j.Client;
import com.github.waikatoufdl.ufdl4j.action.ImageClassificationDatasets;
import com.github.waikatoufdl.ufdl4j.action.Licenses;
import com.github.waikatoufdl.ufdl4j.action.Projects;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import cn.pedant.SweetAlert.SweetAlertDialog;
import io.github.waikato_ufdl.DBManager;
import io.github.waikato_ufdl.SessionManager;
import io.github.waikato_ufdl.databinding.FragmentSettingsBinding;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class SettingsFragment extends Fragment {
    SessionManager sessionManager;
    FragmentSettingsBinding binding;
    String prevUsername, prevPassword, prevServerURL;
    DBManager dbManager;
    ImageClassificationDatasets action;

    /***
     * Default constructor for the Settings Fragment
     */
    public SettingsFragment() {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(requireContext());
        requireContext().setTheme(sessionManager.getTheme());
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        dbManager = sessionManager.getDbManager();
        binding = FragmentSettingsBinding.inflate(inflater, container, false);

        //load stored settings from the previous login session
        retrieveSavedSettings(true);
        setURLTextChangeListener();
        setThemeSwitchListener();
        setLoginButtonClickListener();
        setupAutoCompleteFields();

        // Inflate the layout for this fragment
        return binding.getRoot();
    }

    /***
     * Loads the known server and user lists from the local database uses them to set the adapters for the URL & Username fields to show
     * auto complete suggestions to the user while they type.
     */
    private void setupAutoCompleteFields() {
        ArrayList<String> knownServers = dbManager.getServers();
        ArrayList<String> knownUsers = dbManager.getUsernames();

        if (!knownServers.isEmpty())
            binding.URL.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, knownServers));

        if (!knownUsers.isEmpty())
            binding.Username.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, knownUsers));
    }

    /***
     * Sets the click listener on the login button. When this button is pressed, the user input fields are checked to ensure they are non-empty
     * and the server URL is valid prior to making a login attempt.
     */
    private void setLoginButtonClickListener() {
        //set an onclick listener to the 'Login' button
        binding.loginButton.setOnClickListener(view -> {
            //only exit if the required fields are not empty & also check if the URL is valid
            if (inputFieldsAreValid()) {
                sessionManager.removeSession();
                saveSettings();
                login();
            }
        });
    }

    /***
     * Method to store the URL, username and password text to shared preferences
     */
    private void saveSettings() {
        sessionManager.saveServerURL(binding.URL.getText().toString().trim());
        sessionManager.saveUsername(binding.Username.getText().toString().trim());
        sessionManager.savePassword(binding.Password.getText().toString().trim());
    }

    /***
     * Sets a text change listener on the server URL field so that the http:// prefix is automatically added once a user starts typing the server URL.
     */
    private void setURLTextChangeListener() {
        binding.URL.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().startsWith("http://")) {
                    binding.URL.setText("http://" + s);
                    Selection.setSelection(binding.URL.getText(), binding.URL.getText().length());
                }
            }
        });
    }

    /***
     * Set the theme switch listener to toggle between light & dark mode on upon the checked state. When the checked state is true, turn on dark mode.
     * Else, set light theme when the checked state is false;
     */
    private void setThemeSwitchListener() {
        binding.themeSwitch.setChecked(sessionManager.loadDarkModeState());
        if (binding.themeSwitch.isChecked()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }

        binding.themeSwitch.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            sessionManager.saveDarkModeState(isChecked);

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });
    }

    /**
     * This method will display a loading animation while attempting to login and test the connection to the server.
     */
    public void login() {

        Observable.fromCallable(this::connectAndTestConnection)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Boolean>() {
                    final SweetAlertDialog loadingDialog = new SweetAlertDialog(requireContext(), SweetAlertDialog.PROGRESS_TYPE);

                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        //display loading dialog
                        loadingDialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
                        loadingDialog.setTitleText("Logging in...");
                        loadingDialog.setCancelable(true);
                        loadingDialog.setOnCancelListener(view -> {
                            d.dispose();
                            loadingDialog.dismiss();
                        });
                        loadingDialog.show();
                    }

                    @Override
                    public void onNext(@NonNull Boolean aBoolean) {
                        showConnectionResultDialog(aBoolean);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                    }

                    @Override
                    public void onComplete() {
                        loadingDialog.dismissWithAnimation();
                    }
                });
    }

    /***
     * A method to connect to the server & then proceed to test the connection via data retrieval
     * @return returns true if connection is successful
     */
    public boolean connectAndTestConnection() {
        sessionManager.connectToServer();

        try {
            return SessionManager.getClient().licenses().list().size() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /***
     * Displays a dialog informing the user whether their login attempt was successful or not. If it was successful,
     * also inform the user if they are in offline mode or online mode.
     * @param connectionSuccessful boolean which represents if a connection is successful (true = success, false = failed)
     */
    public void showConnectionResultDialog(boolean connectionSuccessful) {
        String serverURL = binding.URL.getText().toString().trim();
        String username = binding.Username.getText().toString().trim();
        String password = binding.Password.getText().toString().trim();
        int userPK;

        String titleText = "Login Successful";
        String contentText;
        int dialogType = SweetAlertDialog.SUCCESS_TYPE;
        SweetAlertDialog.OnSweetClickListener listener = sweetAlertDialog -> {
            sweetAlertDialog.dismissWithAnimation();
            Navigation.findNavController(requireView()).popBackStack();
        };

        //if the connection is successful, display the appropriate success dialog
        if (connectionSuccessful && SessionManager.isOnlineMode) {
            storeLoginDetails(serverURL, username, password);
            contentText = "You have successfully established a connection to the server.";
        } else if ((userPK = dbManager.getUserPK(serverURL, username, password)) != -1) {
            sessionManager.createSession(userPK);
            contentText = "Authentication successful. You are in offline mode.";
        } else {
            //display error dialog informing users to check their login details
            dialogType = SweetAlertDialog.ERROR_TYPE;
            titleText = "Authentication Failed";
            contentText = "Please check your login details and try again.";
            sessionManager.removeSession();
            listener = SweetAlertDialog::dismissWithAnimation;
        }

        new SweetAlertDialog(requireContext(), dialogType)
                .setTitleText(titleText)
                .setContentText(contentText)
                .setConfirmText("OK")
                .setConfirmClickListener(listener)
                .show();
    }

    /***
     * Stores a user's login details into the local database.
     * @param serverURL the server URL
     * @param username the username
     * @param password the password
     */
    private void storeLoginDetails(String serverURL, String username, String password) {
        new Thread(() -> {
            try {
                int pk = new Client("http://127.0.0.1:8000", "admin", "admin").users().load(username).getPK();
                boolean firstLogin = dbManager.getUserPK(serverURL, username, password) == -1;
                Log.d("TAG", "First time logged in: " + firstLogin);
                dbManager.storeUserDetails(pk, serverURL, username, password);
                sessionManager.createSession(pk);
                action = sessionManager.getDatasetAction();

                if (firstLogin) {
                    loadRequiredDataFromServer();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /***
     * Method to retrieve a user's stored settings & optionally display them to the EditTexts
     * @param setFields boolean value indicating whether to set the text fields with the previous user details
     */
    public void retrieveSavedSettings(boolean setFields) {
        prevServerURL = ((prevServerURL = sessionManager.loadServerURL()) != null) ? prevServerURL : "";
        prevUsername = ((prevUsername = sessionManager.loadUsername()) != null) ? prevUsername : "";
        prevPassword = ((prevPassword = sessionManager.loadPassword()) != null) ? prevPassword : "";

        if (setFields) {
            binding.Username.setText(prevUsername);
            binding.Password.setText(prevPassword);
            binding.URL.setText(prevServerURL);
        }
    }

    /***
     * Checks if any of the required user login fields are empty and also validates the URL.
     * @return true if all the required information has been provided and the URL is valid. Else, returns false and displays errors on the appropriate fields.
     */
    public boolean inputFieldsAreValid() {
        boolean valid = true;

        if (isEmpty(binding.Username, 1)) {
            binding.Username.setError("Required");
            valid = false;
        }
        if (isEmpty(binding.Password, 1)) {
            binding.Password.setError("Required");
            valid = false;
        }
        if (isEmpty(binding.URL, 7)) {
            binding.URL.setError("Required");
            valid = false;
        }
        if (!SessionManager.isValidURL(binding.URL.getText().toString().trim())) {
            binding.URL.setError("Invalid URL");
            valid = false;
        }

        return valid;
    }

    /***
     * Method to check if an EditText field is empty or doesn't meet the required minimum character limit.
     * @param editText  the edit text field to check
     * @param minLength minimum character length required
     * @return true if edit text field is empty or doesn't meet the minimum character requirement.
     */
    public boolean isEmpty(EditText editText, int minLength) {
        return editText.getText().toString().trim().length() < minLength;
    }

    /***
     * Loads the licenses, projects and dataset information from the server via an API request
     */
    private void loadRequiredDataFromServer() {
        Observable.fromCallable(() ->
        {
            Client client = SessionManager.getClient();
            ImageClassificationDatasets action = client.action(ImageClassificationDatasets.class);

            for (Licenses.License license : client.licenses().list())
                dbManager.insertLicense(license.getPK(), license.getName());

            for (Projects.Project project : client.projects().list())
                dbManager.insertProject(project.getPK(), project.getName());

            action.list().forEach(dataset ->
            {
                if (dataset.getCreator() == sessionManager.getUserPK()) {
                    dbManager.insertSyncedDataset(dataset.getPK(), dataset.getName(), dataset.getDescription(), dataset.getProject(), dataset.getLicense(), dataset.isPublic(), dataset.getTags());
                }
            });
            return true;
        })
                // Execute in IO thread, i.e. background thread.
                .subscribeOn(Schedulers.io())
                // report or post the result to main thread.
                .observeOn(AndroidSchedulers.mainThread())
                //if an error occurs, display the error message in the log
                .doOnError(e -> Log.e("TAG", "Failed to load data from the server \nError: " + e.getMessage()))
                // execute this
                .subscribe();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}