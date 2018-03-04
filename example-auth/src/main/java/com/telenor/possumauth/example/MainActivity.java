package com.telenor.possumauth.example;

import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.telenor.possumauth.PossumAuth;
import com.telenor.possumauth.example.dialogs.DefineIdDialog;
import com.telenor.possumauth.example.dialogs.GraphSelectionDialog;
import com.telenor.possumauth.example.fragments.MainFragment;
import com.telenor.possumauth.example.fragments.TrustFragment;
import com.telenor.possumauth.interfaces.IAuthCompleted;

import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements IAuthCompleted {
    private static final String tag = MainActivity.class.getName();
    private SharedPreferences preferences;
    private PossumAuth possumAuth;
    private JsonParser parser;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_main);
        parser = new JsonParser();
        preferences = getSharedPreferences(AppConstants.SHARED_PREFERENCES, MODE_PRIVATE);
        String userId = myId();
        possumAuth = new PossumAuth(getApplicationContext(), userId, getString(R.string.authentication_url), getString(R.string.apiKey));
        possumAuth.addAuthListener(this);
        possumAuth.setTimeOut(0); // Timeout is handled by client
        showFragment(MainFragment.class);
    }

    @Override
    public void messageReturned(String message, Exception e) {
        if (e == null) {
            JsonObject msgObj = (JsonObject) parser.parse(message);
            updateSharedPreferences(msgObj);
            JsonObject sensors = msgObj.getAsJsonObject("sensors");
            JsonArray trustScores = msgObj.getAsJsonArray("trustscore");
            for (JsonElement el : trustScores) {
                JsonObject obj = el.getAsJsonObject();
                String graphName = obj.get("name").getAsString();
                float score = obj.get("score").getAsFloat();
                ((TrustFragment) getSupportFragmentManager().getFragments().get(0)).newTrustScore(graphName, score);
            }
            Log.i(tag, "AP: Msg:" + message);
            for (Map.Entry<String, JsonElement> entry : sensors.entrySet()) {
                String detectorName = entry.getKey();
                JsonArray arr = entry.getValue().getAsJsonArray();
                for (JsonElement el : arr) {
                    JsonObject graphObj = el.getAsJsonObject();
                    String dataSetName = graphObj.get("name").getAsString();
                    String graphName = String.format(Locale.US, "%s:%s", detectorName.substring(0, 3), dataSetName.substring(0, 3));
                    ((TrustFragment) getSupportFragmentManager().getFragments().get(0)).detectorValues(detectorName, dataSetName, graphObj.get("score").getAsFloat(), graphObj.get("status").getAsFloat());
                    // TODO: Ignore unused sensors? (Bluetooth, Position, Sound)
                }
            }
        } else {
            Log.i(tag, "AP: Error when auth:", e);
            // Posts error message
            ((TrustFragment) getSupportFragmentManager().getFragments().get(0)).newTrustScore(null, -1);
        }
        Send.message(getApplicationContext(), Messaging.AUTH_RETURNED);
    }

    private void updateSharedPreferences(JsonObject object) {
        SharedPreferences prefs = getSharedPreferences(AppConstants.SHARED_PREFERENCES, MODE_PRIVATE);
        JsonArray alreadyStoredData = (JsonArray) parser.parse(prefs.getString("StoredGraphDisplay", "[]"));
        SharedPreferences.Editor editor = prefs.edit();
        JsonObject sensorsObject = object.getAsJsonObject("sensors");
        JsonArray storedData = new JsonArray();

        for (Map.Entry<String, JsonElement> entry : sensorsObject.entrySet()) {
            String detectorName = entry.getKey();
            JsonArray arr = entry.getValue().getAsJsonArray();
            for (JsonElement el : arr) {
                JsonObject graphObj = el.getAsJsonObject();
                String name = graphObj.get("name").getAsString();
                String fullName = String.format("%s:%s", detectorName, name);
                boolean alreadyExists = false;
                for (JsonElement elStored : alreadyStoredData) {
                    JsonObject objStored = elStored.getAsJsonObject();
                    if (fullName.equals(objStored.get("name").getAsString())) {
                        alreadyExists = true;
                        storedData.add(objStored);
                        break;
                    }
                }
                if (!alreadyExists) {
                    JsonObject graph = new JsonObject();
                    graph.addProperty("name", fullName);
                    graph.addProperty("isShown", true);
                    storedData.add(graph);
                }
            }
        }
        editor.putString("StoredGraphDisplay", storedData.toString());
        editor.apply();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        possumAuth.stopListening();
        possumAuth.removeAuthListener(this);
        Log.i(tag, "AP: Terminating");
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
        new DefineIdDialog().show(getSupportFragmentManager(), DefineIdDialog.class.getName());
    }

    public void setShownGraphs(MenuItem item) {
        new GraphSelectionDialog().show(getSupportFragmentManager(), GraphSelectionDialog.class.getName());
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
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        possumAuth.onConfigurationChanged(newConfig);
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