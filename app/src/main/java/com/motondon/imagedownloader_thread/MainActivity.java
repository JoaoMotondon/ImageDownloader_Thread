package com.motondon.imagedownloader_thread;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Fragment mainFragment = getSupportFragmentManager().findFragmentByTag(MainFragment.TAG);

        if (mainFragment == null) {
            mainFragment = new MainFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.main_container_id, mainFragment, MainFragment.TAG).commit();
        }
    }
}
