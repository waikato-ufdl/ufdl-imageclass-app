package io.github.waikato_ufdl.ui.settings;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

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

import io.github.waikato_ufdl.R;

import io.github.waikato_ufdl.R;


public class SettingsFragment extends Fragment {
    Button buttonToMain;
    Switch themeSwitch;
    EditText username;
    EditText password;
    EditText serverURL;
    String prevUsername;
    String prevPassword;
    String prevServerURL;

    public SettingsFragment()
    {

    }


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
        retrieveSavedSettings();

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
                    serverURL.setText("http://");
                    Selection.setSelection(serverURL.getText(), serverURL.getText().length());
            }
        }});

        if(Utility.loadDarkModeState() == true)
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
                if(valid & Utility.isValidURL(serverURL.getText().toString().trim())) {

                    //if the user has changed their details
                    if(detailsHaveChanged()) {
                        //save user details before exiting & also establish a connection to the API using the settings information
                        saveSettings();
                    }

                    Navigation.findNavController(view).popBackStack();
                }
                else
                {
                    //if the URL is invalid, inform the user about the issue
                    serverURL.setError("Invalid URL");
                }
            }
        });

        // Inflate the layout for this fragment
        return v;
    }

    public boolean detailsHaveChanged()
    {
        return (!prevUsername.equals(username.getText().toString().trim())|
                !prevPassword.equals(password.getText().toString().trim())|
                !prevServerURL.equals(serverURL.getText().toString().trim()));
    }

    /**
     * Method to retrieve a user's stored settings & display them to the EditTexts
     */
    public void retrieveSavedSettings()
    {
        prevUsername = (Utility.loadUsername() != null) ? Utility.loadUsername() : "";
        prevPassword = (Utility.loadPassword() != null) ? Utility.loadPassword() : "";
        prevServerURL = (Utility.loadServerURL() != null) ? Utility.loadServerURL() : "";

        username.setText(prevUsername);
        password.setText(prevPassword);
        serverURL.setText(prevServerURL);
    }

    /**
     * Method to save the main user settings to local storage
     */
    public void saveSettings()
    {
        Utility.saveUsername(username.getText().toString().trim());
        Utility.savePassword(password.getText().toString().trim());
        Utility.saveServerURL(serverURL.getText().toString().trim());
        Utility.connectToServer();
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
        /*
        startActivity(new Intent(getContext(), MainActivity.class));
        getActivity().finish();
         */

        getActivity().recreate();

    }

}