package com.example.myapplication.ui.settings;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentTransaction;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import com.example.myapplication.MainActivity;
import com.example.myapplication.R;
import com.example.myapplication.ui.home.HomeFragment;
import com.google.android.material.internal.NavigationMenu;
import com.google.android.material.navigation.NavigationView;


public class SettingsFragment extends Fragment {
    Button buttonToMain;
    Switch themeSwitch;

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

        buttonToMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Navigation.findNavController(view).popBackStack();
            }
        });


        // Inflate the layout for this fragment
        return v;
    }


    //a method to restart the current fragment
    public void restartActivity() {
        /*
        startActivity(new Intent(getContext(), MainActivity.class));
        getActivity().finish();
         */

        getActivity().recreate();

    }

}