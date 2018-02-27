package com.telenor.possumauth.example;

import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.telenor.possumauth.PossumAuth;
import com.telenor.possumauth.example.dialogs.DefineIdDialog;
import com.telenor.possumauth.example.dialogs.GraphSelectionDialog;
import com.telenor.possumauth.example.fragments.MainFragment;

public class MainActivity extends AppCompatActivity {//} implements IModelLoaded {
    private static final String tag = MainActivity.class.getName();
    private SharedPreferences preferences;
    private PossumAuth possumAuth;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_main);
        possumAuth = new PossumAuth(getApplicationContext(), "userId", "uploadUrl");
        preferences = getSharedPreferences("dummyPrefs", MODE_PRIVATE);
        showFragment(MainFragment.class);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        possumAuth.stopListening();
        Log.i(tag, "Terminating");
    }

    public PossumAuth possumAuth() {
        return possumAuth;
    }

    private void showFragment(Class<? extends Fragment> fragmentClass) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        try {
            transaction.replace(R.id.mainFragment, fragmentClass.newInstance());
        } catch (Exception e) {
            Log.e(tag, "Failed to instantiate Fragment:", e);
        }
        transaction.commitAllowingStateLoss();
    }

    public SharedPreferences preferences() {
        return preferences;
    }

    public String myId() {
        return preferences.getString("storedId", "");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    public void defineIdDialog(MenuItem item) {
        DefineIdDialog dialog = new DefineIdDialog();
        dialog.show(getSupportFragmentManager(), DefineIdDialog.class.getName());
    }

    public void setShownGraphs(MenuItem item) {
        GraphSelectionDialog dialog = new GraphSelectionDialog();
        dialog.show(getSupportFragmentManager(), GraphSelectionDialog.class.getName());
    }

    @Override
    public void onPause() {
        super.onPause();
        possumAuth.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        possumAuth.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.define_id:
                defineIdDialog(menuItem);
                return true;
            case R.id.showGraph:
                setShownGraphs(menuItem);
                return true;
            case R.id.about:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.app_name);
                String appVersion = "Unknown";
                try {
                    PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                    appVersion = pInfo.versionName;
                } catch (PackageManager.NameNotFoundException ignore) {
                }
                builder.setMessage(String.format("App version:%s\n\nLibrary version:%s", appVersion, PossumAuth.version(getApplicationContext())));
                builder.create().show();
                return true;
        }
        return false;
    }

    public void showInvalidIdDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Invalid id");
        builder.setMessage("Need a valid id to proceed");
        builder.setPositiveButton("Ok", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    public boolean validId(String uniqueId) {
        return uniqueId != null && uniqueId.length() > 2;
    }
}