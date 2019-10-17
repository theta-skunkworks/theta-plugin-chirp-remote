/**
 * Copyright 2018 Ricoh Company, Ltd.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
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

import java.util.function.Consumer;


public class ChangeVolumeTask extends AsyncTask<Void, Void, String> {
    private static final String TAG = "ChgVol";

    private String setVol;
    private Consumer<String> consumer ;

    public ChangeVolumeTask(Consumer<String> consumer, String setVol) {
        this.consumer = consumer;
        this.setVol = setVol;
    }

    @Override
    protected void onPreExecute() {

    }

    @Override
    synchronized protected String doInBackground(Void... params) {
        HttpConnector camera = new HttpConnector("127.0.0.1:8080");
        String strResult = "";

        strResult = camera.httpExec(HttpConnector.HTTP_POST, HttpConnector.API_URL_STAT, "");
        try {
            JSONObject output2 = new JSONObject(strResult);
            JSONObject state = output2.getJSONObject("state");
            String captureStatus = state.getString("_captureStatus");
            int recordedTime = state.getInt("_recordedTime");

            if ( captureStatus.equals("idle") && (recordedTime==0) ) {

                String strJsonGetVolume = "{\"name\": \"camera.getOptions\", \"parameters\": { \"optionNames\":[\"_shutterVolume\", \"_shutterVolumeSupport\"] } }";
                strResult = camera.httpExec(HttpConnector.HTTP_POST, HttpConnector.API_URL_CMD_EXEC, strJsonGetVolume);

                JSONObject output = new JSONObject(strResult);
                JSONObject results = output.getJSONObject("results");
                JSONObject options = results.getJSONObject("options");
                String curVol = options.getString("_shutterVolume");
                JSONObject support = options.getJSONObject("_shutterVolumeSupport");
                int volMin = support.getInt("minShutterVolume");
                int volMax = support.getInt("maxShutterVolume");

                if (setVol.equals("")) {
                    setVol = curVol;
                } else {
                    try {
                        int chkVol = Integer.getInteger(setVol);
                        if ( chkVol < volMin ) {
                            setVol = String.valueOf(volMin);
                        }
                        if ( chkVol > volMax ) {
                            setVol = String.valueOf(volMax);
                        }

                        String strJsonSetVolume = "{\"name\": \"camera.setOptions\", \"parameters\": { \"options\":{\"_shutterVolume\":" + setVol + "} } }";
                        strResult = camera.httpExec(HttpConnector.HTTP_POST, HttpConnector.API_URL_CMD_EXEC, strJsonSetVolume);
                        Log.d(TAG, "ChangeVolumeTask: Set volume=" + setVol);
                    } catch (NumberFormatException e) {
                        //パラメーターエラー
                        setVol = "ParamERR";
                    }
                }

            } else {
                //実行不可(BUSY)
                setVol = "BUSY";
            }
        } catch (JSONException e1) {
            e1.printStackTrace();
            //通信エラー
            setVol = "COM ERR";
        }

        return setVol;
    }

    @Override
    protected void onPostExecute(String result) {
        consumer.accept(result);
    }
}
