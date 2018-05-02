package com.telenor.possumauth;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.telenor.possumauth.interfaces.IAuthCompleted;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class AsyncRestAuthentication extends AsyncTask<JsonObject, Void, Exception> {
    private final URL url;
    private final String apiKey;
    private String successMessage;
    private IAuthCompleted listener;

    private static final String tag = AsyncRestAuthentication.class.getName();

    AsyncRestAuthentication(@NonNull String url, @NonNull String apiKey, IAuthCompleted listener) throws MalformedURLException {
        this.url = new URL(url);
        this.apiKey = apiKey;
        this.listener = listener;
    }

    @Override
    protected Exception doInBackground(JsonObject... params) {
        OutputStream os = null;
        InputStream is = null;
        Exception exception = null;
        JsonObject object = params[0];
        try {
            byte[] data = object.toString().getBytes();
/*          // Zip data
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ZipOutputStream zipOut = new ZipOutputStream(bos);
            ZipEntry entry = new ZipEntry("data");
            zipOut.putNextEntry(entry);
            zipOut.write(data);
            zipOut.close();
            byte[] dataZipped = bos.toByteArray();*/

            Log.i(tag, "AP: Start connection to auth");
            long startTime = System.currentTimeMillis();
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("x-api-key", apiKey);
            urlConnection.setConnectTimeout(0); // These timeouts should be removed later
            urlConnection.setReadTimeout(0); // These timeouts should be removed later
            urlConnection.setRequestMethod("POST");

            urlConnection.setFixedLengthStreamingMode(data.length);
            urlConnection.connect();

            os = urlConnection.getOutputStream();
            os.write(data);
            int responseCode = urlConnection.getResponseCode();
            String responseMessage = urlConnection.getResponseMessage();
            Log.d(tag, "AP: "+responseCode + " -> " + responseMessage);
            is = urlConnection.getInputStream();
            StringBuilder output = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null)
                output.append(line);
            successMessage = output.toString();
            Log.i(tag, "AP: Received upload - time spent:"+(System.currentTimeMillis()-startTime)+", bytes uploaded:"+data.length);
        } catch (Exception e) {
            Log.e(tag, "AP: Ex:", e);
            exception = e;
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    Log.e(tag, "AP: Failed to close output stream:", e);
                    exception = e;
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    Log.e(tag, "AP: Failed to close input stream:", e);
                    exception = e;
                }
            }
        }
        return exception;
    }

    @Override
    public void onPostExecute(Exception exception) {
        listener.messageReturned(successMessage, exception);
    }
}