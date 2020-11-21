package io.github.waikato_ufdl.ui.settings;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import android.os.Handler;
import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import cn.pedant.SweetAlert.SweetAlertDialog;
import io.github.waikato_ufdl.R;
import okhttp3.internal.Util;


public class SettingsFragment extends Fragment {
    Button buttonToMain;
    Switch themeSwitch;
    EditText username, password,serverURL;
    String prevUsername, prevPassword, prevServerURL;

    public SettingsFragment() { }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        //pass context to to Utility class & set the theme
        Utility.setContext(getContext());
        getContext().setTheme(Utility.getTheme());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_settings, container, false);

        //initialise views
        themeSwitch = (Switch) v.findViewById(R.id.themeSwitch);
        buttonToMain = (Button) v.findViewById(R.id.SettingsButton);
        username = (EditText) v.findViewById(R.id.Username);
        password = (EditText) v.findViewById(R.id.Password);
        serverURL = (EditText) v.findViewById(R.id.URL);

        //load any saved settings from sharedPreferences
        retrieveSavedSettings(true);

        //add text changed listener to ensure that the HTTP:// is set as the prefix
        serverURL.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(!s.toString().startsWith("http://")){
                    serverURL.setText("http://" + s);
                    Selection.setSelection(serverURL.getText(), serverURL.getText().length());
            }
        }});

        if(Utility.loadDarkModeState())
        {
            themeSwitch.setChecked(true);
        }

        themeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked)
                {
                    Utility.saveDarkModeState(true);
                }
                else
                {
                    Utility.saveDarkModeState(false);
                }

                //complete theme switch + animation by restarting the activity
                restartActivity();
            }
        });

        //set an onclick listener to the 'Save & Exit' button
        buttonToMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                boolean valid = checkDetailsEntered();

                //only exit if the required fields are not empty & also check if the URL is valid
                if (valid & Utility.isValidURL(serverURL.getText().toString().trim())) {
                    //save user details and then try to establish a connection with the server
                    saveSettings();
                    login();

                } else {
                    //if the URL is invalid, inform the user about the issue
                    serverURL.setError("Invalid URL");
                }
            }
        });

        // Inflate the layout for this fragment
        return v;
    }

    /**
     * This method will display a loading animation while attempting to login and test the connection to the server
     */
    public void login()
    {

        new AsyncTask<Void, Void, Void>() {
            SweetAlertDialog loadingDialog = new SweetAlertDialog(getContext(), SweetAlertDialog.PROGRESS_TYPE);
            Boolean connectionSuccessful;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();

                //display loading dialog
                loadingDialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
                loadingDialog.setTitleText("Logging in...");
                loadingDialog.setCancelable(false);
                loadingDialog.show();

            }

            @Override
            protected Void doInBackground(Void... params) {
                connectionSuccessful = connectAndTestConnection();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                loadingDialog.dismissWithAnimation();
                showConnectionResultDialog(connectionSuccessful);

            }
        }.execute();
    }

    /**
     * A method to connect to the server & then proceed to test the connection
     * @return returns true if connection is successful
     */
    public boolean connectAndTestConnection()
    {
        Utility.connectToServer();
        return Utility.isConnected();
    }

    /**
     * Method to display a dialog informing the user whether the connection has been established or failed
     * @param connectionSuccessful boolean which represents if a connection is successful (true = success, false = failed)
     */
    public void showConnectionResultDialog(boolean connectionSuccessful)
    {
        //if the connection is successful, display a success dialog
        if(connectionSuccessful)
        {
            new SweetAlertDialog(getContext(), SweetAlertDialog.SUCCESS_TYPE)
                    .setTitleText("Connection Successful")
                    .setContentText("You have successfully established a connection to the server!")
                    .setConfirmText("OK")
                    .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                        @Override
                        public void onClick(SweetAlertDialog sweetAlertDialog) {
                            //when the user clicks "OK" they will be taken back to the previous screen they were on prior to settings
                            sweetAlertDialog.dismissWithAnimation();
                            Navigation.findNavController(getView()).popBackStack();
                        }
                    })
                    .show();
        }
        else
        {
            //display error dialog informing users to check their login details
            new SweetAlertDialog(getContext(), SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("Connection Failed")
                    .setContentText("The connection has failed. Please check your login details and try again.")
                    .setConfirmText("OK")
                    .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                        @Override
                        public void onClick(SweetAlertDialog sweetAlertDialog) {
                            //close the dialog but stay on settings screen
                            sweetAlertDialog.dismissWithAnimation();
                        }
                    })
                    .show();
        }
    }

    /**
     * Method to retrieve a user's stored settings & optionally display them to the EditTexts
     * @param setFields boolean value indicating whether to set the text fields with the previous user details
     */
    public void retrieveSavedSettings(boolean setFields)
    {
        prevUsername = (Utility.loadUsername() != null) ? Utility.loadUsername() : "";
        prevPassword = (Utility.loadPassword() != null) ? Utility.loadPassword() : "";
        prevServerURL = (Utility.loadServerURL() != null) ? Utility.loadServerURL() : "";

        if(setFields) {
            username.setText(prevUsername);
            password.setText(prevPassword);
            serverURL.setText(prevServerURL);
        }
    }

    /**
     * Method to save the main user settings to local storage
     */
    public void saveSettings()
    {
        Utility.saveUsername(username.getText().toString().trim());
        Utility.savePassword(password.getText().toString().trim());
        Utility.saveServerURL(serverURL.getText().toString().trim());
    }

    /**
     * Method to check if any of the required user setting fields are empty
     * @return boolean
     */
    public boolean checkDetailsEntered()
    {
        boolean valid = true;

        //check if any of the inputs are empty and if so, set an error message
        if(isEmpty(username, 0)) {
            username.setError("Required");
            valid = false;
        }
        if(isEmpty(password, 0))
        {
            password.setError("Required");
            valid = false;
        }
        if(isEmpty(serverURL, 7))
        {
            serverURL.setError("Required");
            valid = false;
        }

        return valid;
    }

    /**
     * Method to check if an EditText is empty
     * @param editText The EditText to check
     * @return
     */
    public boolean isEmpty(EditText editText, int minLength)
    {
        return editText.getText().toString().trim().length() <= minLength;
    }

    /**
     * a method to restart the current fragment
     */
    public void restartActivity() {
        getActivity().recreate();
    }
}