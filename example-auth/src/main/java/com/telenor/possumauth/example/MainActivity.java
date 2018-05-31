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
import com.telenor.possumcore.PossumCore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements IAuthCompleted, SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String tag = MainActivity.class.getName();
    private SharedPreferences preferences;
    private PossumAuth possumAuth;
    private JsonParser parser;
    private int graphPosition;
//    private JsonObject graphVisibility;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_main);
        parser = new JsonParser();
        graphPosition = 0;
        preferences = getSharedPreferences(AppConstants.SHARED_PREFERENCES, MODE_PRIVATE);
        preferences.registerOnSharedPreferenceChangeListener(this);
//        graphVisibility = GraphUtil.graphVisibility(preferences);
        String userId = myId();
        possumAuth = new PossumAuth(getApplicationContext(), userId, getString(R.string.authentication_url), getString(R.string.apiKey));
        possumAuth.addAuthListener(this);
        possumAuth.setTimeOut(0); // Timeout is handled by client
        showFragment(MainFragment.class);
    }

    @Override
    public void messageReturned(String message, String responseMessage, Exception e) {
        if (e == null) {
            JsonObject msgObj = (JsonObject) parser.parse(message);
            updateSharedPreferences(msgObj);
            JsonObject sensors = msgObj.getAsJsonObject("sensors");
            JsonArray trustScores = msgObj.getAsJsonArray("trustscore");
            for (JsonElement el : trustScores) {
                JsonObject obj = el.getAsJsonObject();
                String graphName = obj.get("name").getAsString();
                float score = obj.get("score").getAsFloat();
                ((TrustFragment) getSupportFragmentManager().getFragments().get(0)).graphUpdate(graphName, graphPosition, score, -1);
            }
            Log.i(tag, "AP: Msg:" + message);
            for (Map.Entry<String, JsonElement> entry : sensors.entrySet()) {
                String detectorName = entry.getKey();
                String shortName = GraphUtil.shortHand(detectorName);
                JsonArray arr = entry.getValue().getAsJsonArray();
                for (JsonElement el : arr) {
                    JsonObject graphObj = el.getAsJsonObject();
                    String shortDataSet = GraphUtil.shortHand(graphObj.get("name").getAsString());
                    // Logging training status
                    if (!graphObj.get("status").isJsonNull()) {
                        float status = graphObj.get("status").getAsFloat();
                        if (status < 1) {
                            String trainingMessage = String.format(Locale.US, "Training %s:%s-%f", shortName, shortDataSet,status);
                            Log.i(tag, "APP: "+trainingMessage);
                            PossumCore.addLogEntry(getApplicationContext(), trainingMessage);
                        } else {
                            Log.i(tag, "APP: Fully trained:"+shortName);
                        }
                    } else {
                        Log.i(tag, "APP: Eek, wrong field:"+graphObj);
                    }

                    String shortDataSetName = GraphUtil.shortHand(graphObj.get("name").getAsString());
                    String graphName = String.format(Locale.US, "%s:%s", shortName, shortDataSetName);
                    ((TrustFragment) getSupportFragmentManager().getFragments().get(0)).graphUpdate(graphName, graphPosition, graphObj.get("score").getAsFloat(), graphObj.get("status").getAsFloat());
                }
            }
            Send.message(getApplicationContext(), Messaging.AUTH_RETURNED);
        } else {
            Log.i(tag, "AP: Error when auth:", e);
            // Posts error message
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(e.getMessage());
            builder.setMessage(String.format(Locale.US,"Msg:%s\nStack:%s", responseMessage, e.fillInStackTrace()));
            builder.create().show();
            Send.message(getApplicationContext(), Messaging.AUTH_TERMINATE);
        }
        graphPosition++;
    }

    private void updateSharedPreferences(JsonObject object) {
        JsonObject sensorsObject = object.getAsJsonObject("sensors");
        JsonObject oldStoredData = GraphUtil.graphVisibility(preferences);
        List<String> oldGraphs = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : oldStoredData.entrySet()) {
            oldGraphs.add(entry.getKey());
        }
        List<String> newGraphs = new ArrayList<>();
        for (String sensorName : GraphUtil.sensorNames(sensorsObject)) {
            String shortSensor = GraphUtil.shortHand(sensorName);
            JsonArray dataSets = sensorsObject.getAsJsonArray(sensorName);
            for (JsonElement dataSetEl : dataSets) {
                JsonObject dataSet = dataSetEl.getAsJsonObject();
                String shortDataSetName = GraphUtil.shortHand(dataSet.get("name").getAsString());
                String graphName = String.format(Locale.US, "%s:%s", shortSensor, shortDataSetName);
                newGraphs.add(graphName);
            }
        }
        // in old & in new -> keep
        // not in old & in new -> add
        // in old & not in new -> delete
        List<String> adds = new ArrayList<>(newGraphs);
        adds.removeAll(oldGraphs);
        List<String> deletes = new ArrayList<>(oldGraphs);
        deletes.removeAll(newGraphs);
        boolean isChanged = false;
        if (adds.size() > 0) {
            for (String graphName : adds) {
                oldStoredData.addProperty(graphName, true);
            }
            isChanged = true;
        }
        if (deletes.size() > 0) {
            for (String graphName : deletes) {
                oldStoredData.remove(graphName);
            }
            isChanged = true;
        }
        if (isChanged) {
            preferences.edit().putString(AppConstants.STORED_GRAPH_DISPLAY, oldStoredData.toString()).apply();
        }
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

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        for (Map.Entry<String, JsonElement> entry : GraphUtil.graphVisibility(sharedPreferences).entrySet()) {
            String graph = entry.getKey();
            boolean visible = entry.getValue().getAsBoolean();
            ((TrustFragment) getSupportFragmentManager().getFragments().get(0)).updateVisibility(graph, visible);
        }
    }
}