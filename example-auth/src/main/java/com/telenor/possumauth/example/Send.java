package com.telenor.possumauth.example;

import android.content.Context;
import android.content.Intent;

public class Send {
    public static void message(Context context, int msgType) {
        Intent intent = new Intent("PossumMessage");
        intent.putExtra("msgType", msgType);
        context.sendBroadcast(intent);
    }
    public static void message(Context context, String message) {
        Intent intent = new Intent("PossumMessage");
        intent.putExtra("message", message);
        context.sendBroadcast(intent);
    }
}
