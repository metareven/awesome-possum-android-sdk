package com.telenor.possumauth.example.dialogs;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.telenor.possumauth.example.AppConstants;
import com.telenor.possumauth.example.GraphUtil;
import com.telenor.possumauth.example.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GraphSelectionDialog extends AppCompatDialogFragment {
    private GraphAdapter myAdapter;
    private TextView missingTextField;
    private RecyclerView recyclerView;
    @SuppressWarnings("unused")
    private static final String tag = GraphSelectionDialog.class.getName();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle bundle) {
        return inflater.inflate(R.layout.dialog_graph_selection, parent, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle bundle) {
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
        public final String name;
        public boolean isShown;

        GraphObject(@NonNull String name, boolean isShown) {
            this.name = name;
            this.isShown = isShown;
        }

        void toggleChecked() {
            isShown = !isShown;
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof GraphObject)) return false;
            GraphObject gObj = (GraphObject)obj;
            return gObj.name.equals(name);
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
            graphName.setText(graphObject.name);
            showGraphBox.setChecked(graphObject.isShown);
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

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.graph_row, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
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
            JsonObject storedData = GraphUtil.graphVisibility(preferences);
            List<GraphObject> oldObjects = new ArrayList<>(objects);
            List<GraphObject> newObjects = new ArrayList<>();
            for (Map.Entry<String, JsonElement> entry : storedData.entrySet()) {
                String key = entry.getKey();
                boolean visible = storedData.get(key).getAsBoolean();
                newObjects.add(new GraphObject(key, visible));
            }
            // in old, not in new -> delete
            // in new, not in old -> add
            List<GraphObject> delete = new ArrayList<>(oldObjects);
            delete.removeAll(newObjects);
            List<GraphObject> add = new ArrayList<>(newObjects);
            add.removeAll(oldObjects);

            if (add.size() > 0) {
                for (GraphObject obj : add) {
                    notifyItemInserted(objects.size()>0?objects.size()-1:0);
                    objects.add(obj);
                }
            }
            if (delete.size() > 0) {
                for (GraphObject obj : delete) {
                    int index = objects.indexOf(obj);
                    objects.remove(obj);
                    notifyItemRemoved(index);
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
            JsonObject saveObject = new JsonObject();
            for (GraphObject obj : objects) {
                saveObject.addProperty(obj.name, obj.isShown);
            }
            // TODO: Fix issue with saving sometimes does not actually save - or is rewritten..
            // Most likely issue - it receives while saving or something similar and rewrites the old. Check the
            // updateSharedPreferences function in MainActivity - only other place where it can store to preferences.
//            Log.i(tag, "APP: Saving object:"+saveObject);
            preferences.edit().putString(AppConstants.STORED_GRAPH_DISPLAY, saveObject.toString()).apply();
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (AppConstants.STORED_GRAPH_DISPLAY.equals(key)) {
                updateFromSharedPreferences();
            }
        }
    }
}