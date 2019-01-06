package org.dharmaseed.androidapp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

public class DeleteTalkFragment extends DialogFragment {

    public static final String LOG_TAG = "DeleteTalkFragment";

    public interface DeleteTalkListener {
        public void onDeleteTalkPositiveClick(DialogFragment dialogFragment);
        public void onDeleteTalkNegativeClick(DialogFragment dialogFragment);
    }

    DeleteTalkListener listener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (DeleteTalkListener) context;
        } catch (ClassCastException ccex) {
            Log.e(LOG_TAG, "Illegal class cast");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setMessage(R.string.delete_talk_confirm);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                listener.onDeleteTalkPositiveClick(DeleteTalkFragment.this);
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                listener.onDeleteTalkNegativeClick(DeleteTalkFragment.this);
            }
        });

        return builder.create();
    }

}
