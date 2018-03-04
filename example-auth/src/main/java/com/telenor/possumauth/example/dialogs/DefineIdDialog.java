package com.telenor.possumauth.example.dialogs;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatDialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.telenor.possumauth.PossumAuth;
import com.telenor.possumauth.example.MainActivity;
import com.telenor.possumauth.example.Messaging;
import com.telenor.possumauth.example.R;
import com.telenor.possumauth.example.Send;

public class DefineIdDialog extends AppCompatDialogFragment {
    private EditText uniqueId;
    private Button okButton;
    private PossumAuth possumAuth;
    private static final String tag = DefineIdDialog.class.getName();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle bundle) {
        return inflater.inflate(R.layout.dialog_define_id, parent, false);
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        possumAuth = ((MainActivity)getActivity()).possumAuth();
        uniqueId = view.findViewById(R.id.uniqueId);
        uniqueId.setText(((MainActivity) getActivity()).myId());
        okButton = view.findViewById(R.id.ok);
        okButton.setOnClickListener(v -> {
            String suggestedId = uniqueId.getText().toString();
            if (!((MainActivity)getActivity()).validId(suggestedId)) {
                if (getView() == null) {
                    Log.e(tag, "Missing getView()");
                    return;
                }
                Snackbar.make(getView(), "Invalid id - need at least two characters", Snackbar.LENGTH_LONG).show();
            } else {
                // TODO: Fix text in mainFragment for messages
                Send.message(getContext(), Messaging.READY_TO_AUTH);
                ((MainActivity)getActivity()).preferences().edit().putString("storedId", suggestedId).apply();
                possumAuth.changeUserId(suggestedId);
                dismiss();
            }
        });
    }
}