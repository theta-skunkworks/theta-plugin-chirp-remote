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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class CheckWlanModeTask extends AsyncTask<Void, Void, String> {
    private static final String TAG = "ChkWlan";

    public static final String WLAN_OFF = "OFF";
    public static final String WLAN_AP = "AP";
    public static final String WLAN_CL = "CL";

    private Callback mCallback;

    public CheckWlanModeTask(CheckWlanModeTask.Callback callback) {
        this.mCallback = callback;
    }

    @Override
    protected void onPreExecute() {

    }

    @Override
    synchronized protected String doInBackground(Void... params) {
        HttpConnector camera = new HttpConnector("127.0.0.1:8080");
        String strResult = "";
        String networkType ="";
        boolean existCL = false;

        String strJsonGetCaptureMode = "{\"name\": \"camera.getOptions\", \"parameters\": { \"optionNames\": [\"_networkType\", \"_networkTypeSupport\"] } }";
        strResult = camera.httpExec(HttpConnector.HTTP_POST, HttpConnector.API_URL_CMD_EXEC, strJsonGetCaptureMode);

        try {
            JSONObject output = new JSONObject(strResult);
            JSONObject results = output.getJSONObject("results");
            JSONObject options = results.getJSONObject("options");
            networkType = options.getString("_networkType");
            JSONArray networkTypeSupport = options.getJSONArray("_networkTypeSupport");
            for (int i=0; i<networkTypeSupport.length(); i++) {
                String chkStr = networkTypeSupport.getString(i);
                if ( chkStr.equals(WLAN_CL) ) {
                    existCL = true;
                    break;
                }
            }

            Log.d(TAG, "networkType=" + networkType + ", existCL=" + String.valueOf(existCL));
            mCallback.onCheckWlanMode(networkType, existCL);

        } catch (JSONException e1) {
            e1.printStackTrace();
            strResult = e1.getMessage();
        }

        return strResult;
    }

    @Override
    protected void onPostExecute(String result) {
    }

    public interface Callback {
        void onCheckWlanMode(String networkType, boolean inExistCL);
    }

}
