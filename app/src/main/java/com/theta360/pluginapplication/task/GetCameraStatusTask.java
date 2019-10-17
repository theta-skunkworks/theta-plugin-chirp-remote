/**
 * Copyright 2018 Ricoh Company, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.theta360.pluginapplication.task;

import android.os.AsyncTask;
import android.util.Log;

import com.theta360.pluginapplication.network.HttpConnector;

import org.json.JSONException;
import org.json.JSONObject;


public class GetCameraStatusTask extends AsyncTask<Void, Void, String> {
    private static final String TAG = "GetCameraStatus";

    private Callback mCallback;
    public GetCameraStatusTask(GetCameraStatusTask.Callback callback) {
        this.mCallback = callback;
    }

    @Override
    protected void onPreExecute() {

    }

    @Override
    synchronized protected String doInBackground(Void... params) {
        HttpConnector camera = new HttpConnector("127.0.0.1:8080");
        String strResult = "";

        String strJsonGetOptions = "{\"name\": \"camera.getOptions\", \"parameters\": { \"optionNames\":[\"captureMode\", \"exposureProgram\", \"aperture\", \"shutterSpeed\", \"iso\", \"exposureCompensation\", \"whiteBalance\", \"_colorTemperature\", \"exposureDelay\"] } }";
        strResult = camera.httpExec(HttpConnector.HTTP_POST, HttpConnector.API_URL_CMD_EXEC, strJsonGetOptions);

        try {
            JSONObject output = new JSONObject(strResult);
            JSONObject results = output.getJSONObject("results");
            JSONObject options = results.getJSONObject("options");

            String captureMode = options.getString("captureMode");
            int exposureProgram = options.getInt("exposureProgram");
            double aperture = options.getDouble("aperture");
            int iso = options.getInt("iso");
            double shutterSpeed = options.getDouble("shutterSpeed");
            double exposureCompensation = options.getDouble("exposureCompensation");
            int exposureDelay = options.getInt("exposureDelay");
            String whiteBalance = options.getString("whiteBalance");
            int colorTemperature = options.getInt("_colorTemperature");

            mCallback.onUpdateCameraStatus(
                    captureMode,
                    exposureDelay,
                    String.valueOf(exposureProgram),
                    String.valueOf(aperture),
                    String.valueOf(shutterSpeed),
                    String.valueOf(iso),
                    String.valueOf(exposureCompensation),
                    whiteBalance,
                    String.valueOf(colorTemperature));


        } catch (JSONException e1) {
            e1.printStackTrace();
            Log.d(TAG, e1.toString());
        }

        return strResult;
    }

    @Override
    protected void onPostExecute(String result) {
    }

    public interface Callback {
        void onUpdateCameraStatus( String captureMode,
                                   int exposureDelay,
                                   String strExpProg,
                                   String strAv,
                                   String strTv,
                                   String strIso,
                                   String strExpComp,
                                   String strWb,
                                   String strColorTemperature);
    }

}
