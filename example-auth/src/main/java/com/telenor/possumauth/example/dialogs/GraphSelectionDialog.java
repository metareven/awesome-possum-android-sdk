package com.telenor.possumauth.example.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.telenor.possumauth.example.AppConstants;
import com.telenor.possumauth.example.R;

import java.util.ArrayList;
import java.util.List;

public class GraphSelectionDialog extends AppCompatDialogFragment {
    private GraphAdapter myAdapter;
    private TextView missingTextField;
    private RecyclerView recyclerView;
    private static final String tag = GraphSelectionDialog.class.getName();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle bundle) {
        return inflater.inflate(R.layout.dialog_graph_selection, parent, false);
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        Button cancelButton = view.findViewById(R.id.cancel);
        cancelButton.setOnClickListener(v -> dismiss());
        Button okButton = view.findViewById(R.id.ok);
        missingTextField = view.findViewById(R.id.missingGraphs);

        okButton.setOnClickListener(v -> {
            myAdapter.saveCurrent();
            getContext().sendBroadcast(new Intent(AppConstants.UPDATE_GRAPHS));
            dismiss();
        });
        recyclerView = view.findViewById(R.id.recyclerView);
        myAdapter = new GraphAdapter(getActivity().getSharedPreferences(AppConstants.SHARED_PREFERENCES, Context.MODE_PRIVATE));
        recyclerView.setAdapter(myAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
    }

    protected class GraphObject {
        private final JsonObject object;

        GraphObject(@NonNull JsonObject object) {
            this.object = object;
        }

        String name() {
            return object.get("name").getAsString();
        }

        boolean isShown() {
            return object.get("isShown").getAsBoolean();
        }

        void toggleChecked() {
            boolean newValue = !isShown();
            object.remove("isShown");
            object.addProperty("isShown", newValue);
        }

        JsonObject toJson() {
            return object;
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView graphName;
        private CheckBox showGraphBox;

        ViewHolder(View itemView) {
            super(itemView);
            graphName = itemView.findViewById(R.id.graphName);
            showGraphBox = itemView.findViewById(R.id.showGraph);
        }

        void bind(GraphObject graphObject) {
            graphName.setText(graphObject.name());
            showGraphBox.setChecked(graphObject.isShown());
        }
    }

    private class GraphAdapter extends RecyclerView.Adapter<ViewHolder> implements SharedPreferences.OnSharedPreferenceChangeListener{
        private SharedPreferences preferences;
        private List<GraphObject> objects = new ArrayList<>();

        GraphAdapter(SharedPreferences preferences) {
            this.preferences = preferences;
            updateFromSharedPreferences();
            preferences.registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.graph_row, parent, false));
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.bind(objects.get(position));
            holder.itemView.setOnClickListener(v -> {
                GraphObject obj = objects.get(holder.getAdapterPosition());
                obj.toggleChecked();
                myAdapter.notifyItemChanged(holder.getAdapterPosition());
            });
        }

        void updateFromSharedPreferences() {
            if (preferences == null) {
                recyclerView.setVisibility(View.GONE);
                missingTextField.setVisibility(View.VISIBLE);
                return;
            }
            String storedPrefs = preferences.getString(AppConstants.STORED_GRAPH_DISPLAY, null);
            objects.clear();
            if (storedPrefs != null) {
                JsonParser parser = new JsonParser();
                JsonArray storedArray = parser.parse(storedPrefs).getAsJsonArray();
                for (JsonElement el : storedArray) {
                    JsonObject obj = el.getAsJsonObject();
                    objects.add(new GraphObject(obj));
                }
            }
            recyclerView.setVisibility(objects.size() == 0 ? View.GONE : View.VISIBLE);
            missingTextField.setVisibility(objects.size() == 0 ? View.VISIBLE : View.GONE);
        }

        @Override
        public int getItemCount() {
            return objects.size();
        }

        void saveCurrent() {
            if (preferences == null) return;
            SharedPreferences.Editor editor = preferences.edit();
            JsonArray saveArray = new JsonArray();
            for (GraphObject obj : objects) {
                saveArray.add(obj.toJson());
            }
            editor.putString(AppConstants.STORED_GRAPH_DISPLAY, saveArray.toString());
            editor.apply();
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (AppConstants.STORED_GRAPH_DISPLAY.equals(key)) {
                Log.i(tag, "AP: Changed shared prefs");
                updateFromSharedPreferences();
            } else {
                Log.i(tag, "AP: Unknown key changed:"+key);
            }
        }
    }
}