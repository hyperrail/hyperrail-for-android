package be.hyperrail.opentransportdata.irail.util;

import android.os.AsyncTask;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import be.hyperrail.opentransportdata.common.contracts.TransportDataRequest;

public class AsyncJsonPostRequest {

    /**
     * Make a synchronous POST request with a JSON body.
     *
     * @param url     The URL to make the request to
     * @param request The request body
     * @return The return text from the server
     */
    public static void postRequestAsync(String url, TransportDataRequest<Boolean> request, JSONObject payload) {
        AsyncJsonPostRequest.PostJsonRequestTask t = new AsyncJsonPostRequest.PostJsonRequestTask(url, request);
        t.execute(payload.toString());
    }

    private static String postJsonRequest(String uri, String json) {
        HttpURLConnection urlConnection;
        String result;
        try {
            //Connect
            urlConnection = (HttpURLConnection) ((new URL(uri).openConnection()));
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setRequestMethod("POST");
            urlConnection.connect();

            //Write
            OutputStream outputStream = urlConnection.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(outputStream, "UTF-8"));
            writer.write(json);
            writer.close();
            outputStream.close();

            //Read
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(urlConnection.getInputStream(), "UTF-8"));

            String line;
            StringBuilder sb = new StringBuilder();

            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }

            bufferedReader.close();
            result = sb.toString();

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return result;
    }

    private static class PostJsonRequestTask extends AsyncTask<String, Void, String> {

        private final String url;
        private final TransportDataRequest<Boolean> request;

        public PostJsonRequestTask(String url, TransportDataRequest<Boolean> request) {
            this.url = url;
            this.request = request;
        }

        @Override
        protected String doInBackground(String... payload) {
            return postJsonRequest(this.url, payload[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if (result != null) {
                request.notifySuccessListeners(true);
            } else {
                // TODO: better exception handling
                request.notifyErrorListeners(new Exception("Failed to submit occupancy data"));
            }
        }
    }
}
